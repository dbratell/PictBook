package pictbook;

import pictbook.storage.PictureDirInfo;
import pictbook.storage.PictureInfo;
import pictbook.storage.Storage;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;

/**
 * This servlet generates a page with lots of images.
 *
 * @author Daniel Bratell
 */
public class BookServlet extends HttpServlet
{
    private static final String SIZE_PARAMETER_NAME = "size";

    private Configuration mConfig;
    private Storage mStorage;
    private static final int MIN_ALLOWED_PIC_BLOCK_WIDTH = 150;

    /**
     * Called by the servlet engine when this class is started to be used.
     */
    public void init()
    {
        mConfig = new Configuration(getServletConfig());
        mStorage = new Storage(mConfig);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException
    {
        System.out.println("BookServlet doGet: " + ServletUtil.getCurrentPageUrl(req));
        String path = req.getPathInfo();
        if (path == null || path.equals("/"))
        {
            sendDefaultPage(req, res);
            return;
        }

        sendPicturePage(req, res, path);
    }

    private void sendPicturePage(HttpServletRequest req,
                                 HttpServletResponse res,
                                 String path)
            throws ServletException
    {
        res.setContentType("text/html;charset=UTF-8");

        // First init everything
        String action = req.getParameter("action");
        int thumbnailSize = getAndSaveIntParameter(req, res,
                                                   SIZE_PARAMETER_NAME, Configuration.getDefaultThumbnailSize(),
                                                   Configuration.getMinThumbnailSize(), Configuration.getMaxThumbnailSize());
        String currentUrl = ServletUtil.getCurrentPageUrl(req);

        boolean editMode = "edit".equals(action) && ServletUtil.isLoggedIn(req);
        try
        {
            PictureDirInfo picDirInfo = mStorage.getPictBookDir(path);
            if ("save".equals(action) && ServletUtil.isLoggedIn(req))
            {
                picDirInfo.saveChanges(req.getParameterMap());
                // Redirect to ourself
                res.sendRedirect(currentUrl);
                return;
            }

            String title = picDirInfo.getTitle();

            Writer out = res.getWriter();
            String style = createBookStyle(req, thumbnailSize);

            String stylesheet = Util.addToDirUrl(req.getContextPath(),
                                                 "pictbook.css");
            Util.writeHTMLDocHeader(out, title, stylesheet, style);

            if (editMode)
            {
                // Back to this
                out.write("<form action=\"" +
                          Util.htmlEncode(Util.urlEncode(currentUrl)) +
                          "\" method=\"POST\" " +
                          //   "enctype=\"multipart/form-data\""+
                          ">");
                writeInputElement(out, "hidden", "action", "save");
                out.write("<h1>");
                writeInputElement(out, "text", "title", picDirInfo.getTitle());
                out.write("</h1>\n");
            }
            else
            {
                out.write("<h1>" + Util.htmlEncode(picDirInfo.getTitle()) + "</h1>");
                writeLinkLine(req, currentUrl, out, thumbnailSize);
            }

            writeDirList(out, picDirInfo.getVisbleSubDirs(), req);

            PictureInfo[] images = picDirInfo.getAllPictureInfos();
            out.write("<div class=\"pictures\">");
            int noOfImages = images.length;
            for (int i = 0; i < noOfImages; i++)
            {
                PictureInfo pictureInfo = images[i];
                writeImageBlock(req, out, pictureInfo, path,
                                thumbnailSize, editMode, noOfImages);
            }
            out.write("</div>"); // class="pictures"

            if (editMode)
            {
                writeInputElement(out, "submit", null, "Spara");
                out.write("</form>");
            }

            Util.writeHTMLDocFooter(out);
        }
        catch (IOException e)
        {
            try
            {
                if (!res.isCommitted())
                {
                    if (e instanceof FileNotFoundException)
                    {
                        res.sendError(HttpServletResponse.SC_NOT_FOUND,
                                      e.getMessage());
                    }
                    else
                    {
                        // Normal I/O exception
                        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                      String.valueOf(e));
                    }
                }
            }
            catch (IOException e1)
            {
                throw new ServletException(e1);
            }
        }
    }

    private static String createBookStyle(HttpServletRequest req,
                                          int thumbnailSize)
    {
        String desiredPicBlockWidth =
                Math.max(MIN_ALLOWED_PIC_BLOCK_WIDTH,
                         thumbnailSize) + "px;";
        Browser browser = new Browser(req);
        StringBuffer style = new StringBuffer();
        style.append("div.picture-block {" +
                     "display: inline-block; " +
                     "border: 1px dotted; " +
                     "margin: 1ex; " +
                     "max-width: " + desiredPicBlockWidth +
                     "min-width: " + desiredPicBlockWidth +
//                    "width: "+ desiredPicBlockWidth +
                     "vertical-align: middle " +
                     "}\n" +
                     ".inner-picture-block {" +
                     "margin: 1ex; " +
                     "margin-left: auto; " +
                     "margin-right: auto; " +
                     "background: #f1f1ff;" +
                     "color: black;" +
                     "}\n");
        if (browser.getName().equals(Browser.BROWSER_MOZILLA))
        {
            // Mozilla bug work around
            // Mozilla (2002-10-29) has no inline-block but it has a
            // -moz-inline-block that is close enough.
            style.append("div.picture-block { display:-moz-inline-block }\n");
        }
        else if (browser.getName().equals(Browser.BROWSER_MSIE))
        {
            // MSIE bug exploit to create an inline block
            // XXX MSIE: Setting a
            // width or height on an inline element will create an inline-level block,
            // as will intrinsic elements (as in other browsers).
            style.append("div.picture-block { " +
                         "display: inline; " +
                         "height: auto;" + // Needed?
                         "width:" + desiredPicBlockWidth + "}\n");
        }

        return style.toString();
    }

    private static void writeInputElement(Writer out,
                                          String type,
                                          String name,
                                          String value)
            throws IOException
    {
        StringBuffer buf = new StringBuffer();
        buf.append("<input type=\"" + Util.htmlEncode(type) +
                   "\"");
        if (name != null)
        {
            buf.append(" name=\"" + Util.htmlEncode(name) +
                       "\"");
        }
        if (value != null)
        {
            buf.append(" value=\"" + Util.htmlEncode(value) +
                       "\"");
        }
        buf.append(">\n");
        out.write(buf.toString());
    }

    private static void writeLinkLine(HttpServletRequest req, String currentUrl1, Writer out, int thumbnailSize) throws IOException
    {
        out.write("<div class=\"linkline\">");
        if (ServletUtil.isLoggedIn(req))
        {
            String editUrl = Util.addToDirUrl(currentUrl1, "?action=edit");
            out.write(Util.makeLink("Redigera", editUrl));
        }
        else
        {
            String escapedUrl = Util.urlEncode(currentUrl1);
            String loginUrl = Util.addToDirUrl(req.getContextPath()
                                               , "login?url=" + escapedUrl);
            out.write(Util.makeLink("Logga in", loginUrl));
        }
        out.write(" - ");
        String upwardsUrl = Util.addToDirUrl(currentUrl1, "../");
        out.write(Util.makeLink("Uppåt", upwardsUrl));
        writeChangeSizeLinks(req, out, thumbnailSize);
        out.write("</div>"); // class="linkline"
    }

    private static void writeImageBlock(HttpServletRequest req,
                                        Writer out,
                                        PictureInfo pictureInfo,
                                        String path,
                                        int thumbnailSize,
                                        boolean editMode,
                                        int noOfImages)
            throws IOException
    {
        // Don't display hidden images in normal mode
        if (!editMode && pictureInfo.isHidden())
        {
            return;
        }

        out.write("<div class=\"picture-block\">");
        out.write("<table cellpadding=0 cellspacing=0 " +
                  "class=\"inner-picture-block\">" +
                  "<tr><td align=\"center\">");
        File file = pictureInfo.getLocalFile();
        String fileName = file.getName();
        String imageUrl = Util.addToDirUrl(
                Util.addToDirUrl(
                        Util.addToDirUrl(req.getContextPath(), "images"),
                        path),
                fileName);
        String smallImageUrl = imageUrl + "?size=" + thumbnailSize;
        out.write(Util.makeImageLink(smallImageUrl, imageUrl));
        out.write("</td></tr>\n");
        out.write("<tr><td align=\"center\">");
        out.write("<table><tr><td align=\"center\">");
        if (editMode)
        {
            writeInputElement(out, "text",
                              PictureDirInfo.getDescriptionPropertyName(pictureInfo.getName()),
                              pictureInfo.getDescription());

            // Write combo for position
            writeOrderCombo(out, pictureInfo, noOfImages);
            writeHideCheckbox(out, pictureInfo);
        }
        else
        {
            out.write(Util.makeLink(pictureInfo.getDescription(),
                                    imageUrl));
        }
        out.write("</td></tr>\n");
        DateFormat dateFormatter = DateFormat.getDateTimeInstance();
        out.write("<tr><td align=\"center\">" + dateFormatter.format(pictureInfo.getDate()));
        String encodedSizeString = Util.htmlEncode(Util.byteCountToString(pictureInfo.getFileSize()), true);
        out.write(" <small>(" + encodedSizeString + ")</small>");
        out.write("</td></tr></table>\n"); // block for info text
        out.write("</td></tr></table>\n"); // class="inner-picture-block"
        out.write("</div>\n"); // class="picture-block"
    }

    private static void writeHideCheckbox(Writer out, PictureInfo pictureInfo)
            throws IOException
    {
        out.write("<label>Göm");
        writeInputElement(out, "checkbox",
                          PictureDirInfo.getHidePropertyName(pictureInfo.getName())
                          ,
                          "true");
        if (pictureInfo.isHidden())
        {
            out.write(" checked");
        }
        out.write("></label>");
    }

    private static void writeOrderCombo(Writer out,
                                        PictureInfo pictureInfo,
                                        int noOfImages)
            throws IOException
    {
        out.write("<select " +
                  "name=\"" +
                  Util.htmlEncode(PictureDirInfo.getOrderPropertyName(pictureInfo.getName())) +
                  "\">");
        String currentOrder = String.valueOf(pictureInfo.getOrder());
        writeOptionInCombo(out, String.valueOf(PictureInfo.UNDEFINED_ORDER),
                           "Datum", currentOrder);

        // loop from 1 to the maximum number to display.
        for (int i = 1; i <= noOfImages; i++)
        {
            writeOptionInCombo(out, String.valueOf(i),
                               null, currentOrder);
        }
        out.write("</select>");
    }

    /**
     * Writing an option with as few chars as possible by only writing
     * value when it differs from the label and such.
     * @param out - The Writer to write to.
     * @param optionValue - The value of option
     * @param label - The text to display or null to display the value
     * @param currentSetValue - The value that is set in the &lt;option&gt;.
     * This is used to determine if the option should be |selected|.
     * @throws IOException
     */
    private static void writeOptionInCombo(Writer out,
                                           String optionValue,
                                           String label,
                                           String currentSetValue)
            throws IOException
    {
        out.write("<option");

        if (label == null)
        {
            label = optionValue;
        }
        if (!label.equals(optionValue))
        {
            out.write(" value=\"" + optionValue + "\"");
        }
        if (currentSetValue.equals(optionValue))
        {
            out.write(" selected>");
        }
        else
        {
            out.write(">");
        }
        out.write(label + "</option>");
    }

    private static void writeChangeSizeLinks(HttpServletRequest req,
                                             Writer out,
                                             int currentSize)
            throws IOException
    {
        int smallerSize = 2 * currentSize / 3;
        int minThumbnailSize = Configuration.getMinThumbnailSize();
        if (smallerSize < minThumbnailSize)
        {
            smallerSize = minThumbnailSize;
        }
        // Small slack so that we snap to the default
        // size from both ends
        int defaultThumbnailSize = Configuration.getDefaultThumbnailSize();
        if (currentSize > defaultThumbnailSize &&
                smallerSize - 5 < defaultThumbnailSize)
        {
            smallerSize = defaultThumbnailSize;
        }
        if (smallerSize < currentSize)
        {
            writeOtherSizeLink(req, out, smallerSize, "Mindre bilder");
        }

        int biggerSize = 3 * currentSize / 2;
        int maxThumbnailSize = Configuration.getMaxThumbnailSize();
        if (biggerSize > maxThumbnailSize)
        {
            biggerSize = maxThumbnailSize;
        }
        // Small slack so that we snap to the default
        // size from both ends
        if (currentSize < defaultThumbnailSize &&
                biggerSize + 5 > defaultThumbnailSize)
        {
            biggerSize = defaultThumbnailSize;
        }
        if (biggerSize > currentSize)
        {
            writeOtherSizeLink(req, out, biggerSize, "Större bilder");
        }

        if (currentSize != defaultThumbnailSize)
        {
            writeOtherSizeLink(req, out, defaultThumbnailSize,
                               "Normal storlek");
        }
    }

    private static void writeOtherSizeLink(HttpServletRequest req,
                                           Writer out,
                                           int otherSize,
                                           String text)
            throws IOException
    {
        String url = Util.addToDirUrl(ServletUtil.getCurrentPageUrl(req),
                                      "?size=" + otherSize);
        out.write(" - " + Util.makeLink(text, url));
    }

    /**
     *  Get an integer value from the explicit parameters of from
     *  the cookies.
     * @param req - The request
     * @param res - The response - used to set a cookie. Can be null.
     * @param parameterName - The name of the parameter (and cookie)
     * @param defaultValue - The value to use if there was no specified value
     * or the specified value was illegal.
     * @param minValue - The smallest legal value
     * @param maxValue - The largest legal value
     * @return The value or the defaultValue if no value was specified.
     */
    private static int getAndSaveIntParameter(HttpServletRequest req,
                                              HttpServletResponse res,
                                              String parameterName,
                                              int defaultValue,
                                              int minValue,
                                              int maxValue)
    {
        int value = defaultValue;
        try
        {
            boolean setCookie = false;
            String sizeStr = req.getParameter(parameterName);
            if (sizeStr == null)
            {
                // Check cookies
                Cookie[] cookies = req.getCookies();
                if (cookies != null)
                {
                    for (int i = 0; i < cookies.length; i++)
                    {
                        Cookie cookie = cookies[i];
                        if (cookie.getName().equals(parameterName))
                        {
                            sizeStr = cookie.getValue();
                            break;
                        }
                    }
                }
            }
            else
            {
                setCookie = true;
            }

            if (sizeStr != null)
            {
                value = Integer.parseInt(sizeStr);
                if (value < minValue ||
                        value > maxValue)
                {
                    value = defaultValue;
                }
                else
                {
                    if (setCookie && res != null)
                    {
                        // A new explicit value. Save as cookie
                        Cookie cookie = new Cookie(parameterName,
                                                   String.valueOf(value));
                        // One half year
                        cookie.setMaxAge(24 * 3600 * 180);
                        cookie.setPath(req.getContextPath());
                        res.addCookie(cookie);
                    }
                }
            }
        }
        catch (NumberFormatException nfe)
        {
            // Nothing to do, we use the default value
        }
        return value;
    }

    private void sendDefaultPage(HttpServletRequest req,
                                 HttpServletResponse res)
    {
        try
        {
            res.setContentType("text/html;charset=UTF-8");
            Writer out = res.getWriter();
            String stylesheet = Util.addToDirUrl(req.getContextPath(),
                                                 "pictbook.css");
//            "h1, h2, h3, p, td {" +
//                "font-family: sans-serif; " +
//            "}\n"+
//            "a:hover {" +
//                "background: #ffffcc; " +
//            "}";
            Util.writeHTMLDocHeader(out, "PictBook Defaultsida",
                                    stylesheet, null);

            out.write("<h1>Bilderböcker</h1>");
            String[] topBooks = mStorage.getVisibleTopLevelBooks();
            if (topBooks.length == 0)
            {
                out.write("<p><i>No books</i></p>");
            }
            else
            {
                writeDirList(out, topBooks, req);
            }
//            out.write("<p>" + req.getPathTranslated() +
//                      "," + req.getPathInfo() +
//                      "," + req.getServletPath() +
//                      "," + req.getContextPath() + "</p>");
            Util.writeHTMLDocFooter(out);
        }
        catch (IOException e)
        {
            System.err.println("Failed to send default page: " + e);
        }
    }

    private static void writeDirList(Writer out,
                                     String[] dirs,
                                     HttpServletRequest req)
            throws IOException
    {
        if (dirs.length == 0)
        {
            return;
        }

        out.write("<ul>\n");
        for (int i = 0; i < dirs.length; i++)
        {
            String dirName = dirs[i];
            String dirUrl = Util.addToDirUrl(ServletUtil.getCurrentPageUrl(req),
                                             dirName);
            out.write("<li>" + Util.makeLink(dirName, dirUrl) +
                      "</li>\n");
        }
        out.write("</ul>\n");
    }

    public void doPost(HttpServletRequest httpServletRequest,
                       HttpServletResponse httpServletResponse)
            throws ServletException, IOException
    {
        // The same as GET for us
        doGet(httpServletRequest, httpServletResponse);
    }
}
