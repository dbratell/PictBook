package pictbook;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.Properties;

/**
 * Collects utility functions.
 *
 * @author Daniel Bratell
 */
public class Util
{

    /**
     * Private to prevent instances.
     */
    private Util()
    {
        // No creation of this object.
    }
    /**
     * Calls close on the stream if it's not null and catches any
     * exceptions.
     * @param is - the stream
     */
    public static void safeClose(InputStream is)
    {
        if (is != null)
        {
            try
            {
                is.close();
            }
            catch (IOException ioe)
            {
                // Nothing to do (log?)
            }
        }
    }

    /**
     * Calls close on the stream if it's not null and catches any
     * exceptions.
     * @param os - the stream
     */
    public static void safeClose(OutputStream os)
    {
        if (os != null)
        {
            try
            {
                os.close();
            }
            catch (IOException ioe)
            {
                // Nothing to do (log?)
            }
        }
    }

    /**
     * Writes the HTML head to the Writer.
     * @param out The writer to write to.
     * @param title The title of the page.
     * @param stylesheetUrl The url to the stylesheet file or null.
     * @param style The CSS style to be applied to the page or null.
     * @throws IOException if an I/O error occurs.
     */
    public static void writeHTMLDocHeader(Writer out,
                                          String title,
                                          String stylesheetUrl,
                                          String style)
    throws IOException
    {
        String docType = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" " +
                "\"http://www.w3.org/TR/html4/strict.dtd\">\n";
        String metaContentType =
                "<meta http-equiv=\"Content-Type\" "+
                "content=\"text/html; charset=UTF-8\">\n";
        String stylesheetLink;
        if (stylesheetUrl != null)
        {
            stylesheetLink = "<link rel=\"stylesheet\" " +
                "type=\"text/css\" href=\"" + htmlEncode(urlEncode(stylesheetUrl)) +
                    "\">";
        }
        else
        {
            stylesheetLink = null;
        }

        StringBuffer buf = new StringBuffer();
        buf.append(docType +
                "<html><head>\n"
                + metaContentType +
                "<title>" + htmlEncode(title) + "</title>\n");
        if (stylesheetLink != null)
        {
            buf.append(stylesheetLink);
        }
        if (style != null)
        {
            buf.append("<style type=\"text/css\">\n");
            buf.append(htmlEncode(style));
            buf.append("\n</style>\n");
        }
        buf.append("</head><body>\n");
        out.write(buf.toString());
    }

    /**
     * Escapes characters that must not be in text in HTML source. For instance
     * &gt; is replaced by &amp;gt;
     *
     * @param string The string to escape. Should not be null.
     * @return The escaped string.
     */
    public static String htmlEncode(String string)
    {
        return htmlEncode(string, false);
    }
    /**
     * Not very efficient yet.
     *
     * Escapes characters that must not be in text in HTML source. For instance
     * &gt; is replaced by &amp;gt;
     * @param string The string to encode. Please no nulls.
     * @param nonBreakingSpace True if spaces should be converted to &amp;nbsp;
     * @return The encoded string.
     */
    public static String htmlEncode(String string, boolean nonBreakingSpace)
    {
        if (string == null)
            return "<i>null</i>";

        StringBuffer buf = new StringBuffer(string.length());
        for (int charNo = 0; charNo < string.length(); charNo++)
        {
            String toInsert;
            switch(string.charAt(charNo))
            {
                case '&':
                    toInsert = "&amp;";
                    break;
                case '<':
                    toInsert = "&lt;";
                    break;
                case '>':
                    toInsert = "&gt;";
                    break;
                case ' ':
                    if (nonBreakingSpace)
                    {
                        toInsert = "&nbsp;";
                        break;
                        // else skip to 'default' below
                    }
                default:
                    toInsert = String.valueOf(string.charAt(charNo));
            }
            buf.append(toInsert);
        }
        return buf.toString();
    }

    /**
     * Writes &lt;/body&gt;&lt;/html&gt; to the Writer.
     * @param out The writer to write too.
     * @throws IOException if the write fails.
     */
    public static void writeHTMLDocFooter(Writer out)
      throws IOException
    {
        out.write("<div class=\"footer\">&copy; Daniel Bratell ");
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        String seperator = "";
        for (int year = 2002; year <= thisYear; year++)
        {
            out.write(seperator + year);
            seperator = ", ";
        }
        out.write("</div>\n");
        out.write("</body></html>\n");
    }

    /**
     * Creates the HTML code for a link.
     * @param text The text for the link.
     * @param url The url for the link.
     * @return The HTML source for the link.
     */
    public static String makeLink(String text, String url)
    {
        return "<a href=\""+htmlEncode(urlEncode(url))+"\">"+
                htmlEncode(text)+"</a>";
    }


    /**
     *  Get properties from a file. This handles all the file opening
     * and closing.
     * @param propertiesFile The file
     * @return A properties object
     * @throws IOException
     */
    public static Properties getProperties(File propertiesFile)
            throws IOException
    {
        FileInputStream propsFile = null;
        try
        {
            Properties props = new Properties();
            propsFile = new FileInputStream(propertiesFile);
            props.load(propsFile);
            return props;
        }
        finally
        {
            safeClose(propsFile);
        }
    }

    /**
     * Copies from one stream to another with a buffer, until the in
     * stream is empty.
     *
     * @param in The in stream.
     * @param out The out stream.
     * @throws IOException If any read or write fails.
     */
    public static void copyStreams(InputStream in, OutputStream out)
            throws IOException
    {
        byte[] buffer = new byte[4096];
        int count;
        while ((count = in.read(buffer)) > -1)
        {
            out.write(buffer, 0, count);
        }
    }

    /**
     * Creats a string containing the HTML code for an image link.
     * @param imageUrl The url of the image.
     * @param linkUrl The url of the link.
     * @return The string with everything properly encoded.
     */
    public static String makeImageLink(String imageUrl,
                                       String linkUrl)
    {
        return "<a href=\""+htmlEncode(urlEncode(linkUrl))+"\">"+
                "<img src=\""+htmlEncode(urlEncode(imageUrl))+"\" alt=\"\"></a>";
    }

    /**
     * Escapes common characters that are not allowed or are unsuitable
     * in an URL. For instance
     * '%', ' ', '(', ')'.
     * @param url The url to escape
     * @return The escaped url.
     */
    public static String urlEncode(String url)
    {
        if (url == null)
            return null;

        StringBuffer buf = new StringBuffer(url.length());
        for (int charNo = 0; charNo < url.length(); charNo++)
        {
            String toInsert;
            char c = url.charAt(charNo);
            switch(c)
            {
                case '(':
                case ')':
                case ':':
                case '%':
                case ' ':
                    toInsert = "";
                    try
                    {
                        byte[] bytes = String.valueOf(c).getBytes("UTF-8");
                        for (int i = 0; i < bytes.length; i++)
                        {
                            byte aByte = bytes[i];
                            String hexString = "0"+Integer.toHexString(aByte); // >= 2 chars
                            hexString = hexString.substring(hexString.length()-2);
                            toInsert += "%"+hexString;
                        }
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        e.printStackTrace();  //To change body of catch statement use Options | File Templates.
                    }
                    break;
                default:
                    toInsert = String.valueOf(c);
            }
            buf.append(toInsert);
        }
        return buf.toString();
    }

    /**
     * Extract extension (without the dot).
     * @param file The file.
     * @return The extension. For instance "jpg".
     * @see #getExtension(String)
     */
    public static String getExtension(File file)
    {
        String name = file.getName();
        return getExtension(name);
    }

    /**
     * Extract extension (without the dot). This could be the empty string if
     * there are no dots in the file name.
     * @param fileName The file name.
     * @return The extension. For instance "jpg".
     */
    public static String getExtension(String fileName)
    {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot != -1)
        {
            return fileName.substring(lastDot+1);
        }

        return "";
    }

    /**
     * Calls close on the stream if it's not null and catches any
     * exceptions.
     * @param is - the stream
     */
    public static void safeClose(ImageInputStream is)
    {
        if (is != null)
        {
            try
            {
                is.close();
            }
            catch (IOException ioe)
            {
                // Nothing to do (log?)
            }
        }
    }

    /**
     * Calls close on the stream if it's not null and catches any
     * exceptions.
     * @param os - the stream
     */
    public static void safeClose(ImageOutputStream os)
    {
        if (os != null)
        {
            try
            {
                os.close();
            }
            catch (IOException ioe)
            {
                // Nothing to do (log?)
            }
        }
    }

    /**
     * Takes a string that was wrongfully interpreted as ISO-8859-1 when it
     * was represented as raw byte, into what would have been the result if
     * it had been interpreted as UTF-8 bytes.
     * @param as8859 The string that was interpreted wrongly.
     * @return The same string as it should have been.
     */
    public static String reinterpretFrom8859ToUTF8(String as8859)
    {
        try
        {
            byte[] rawBytes = as8859.getBytes("ISO-8859-1");
            return new String(rawBytes,"UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
            return as8859;
        }
    }

    /**
     * Sometimes we need a BufferedImage instead of the more abstract Image.
     * This converts from an Image to a BufferedImage. That can be slow since
     * that can trigger a fetch and decode of a lazily loaded Image.
     *
     * If the image already is a BufferedImage it is just cast to that and
     * return.
     *
     * @param image The image to convert.
     * @return The new image or the old if it was already an BufferedImage.
     *
     * #see BufferedImage
     * #see Image
     */
    public static BufferedImage Image2BufferedImage(Image image)
    {
        // Save a lot of work - if in is already of type BufferedImage then cast it and return
        if( image instanceof BufferedImage )
        {
            return (BufferedImage)image;
        }

        Dimension size = getImageSize(image);		// Does waitForImage()
        if (size == null)
        {
            // Problem with image loading
            return null;
        }
        // XXX Not hardcode type?
        BufferedImage out = new BufferedImage(size.width, size.height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D outContext = out.createGraphics();
        outContext.drawImage(image, 0, 0, null);
        outContext.dispose();

        return out;
	}

    /**
     * Used as much to force an image load as for really getting the
     * size of the image.
     * @param image The image to load and check the dimension of.
     * @return The image size in a Dimension object or null if the dimension
     * couldn't be extracted.
     */
    private static Dimension getImageSize(Image image)
    {
        int width = image.getWidth(null); // Will block. I hope.
        int height = image.getHeight(null);
        if (width == -1 || height == -1)
        {
            // We don't know the size
            return null;
        }
        return new Dimension(width, height);
    }

    /**
     * Checks an extension (without the dot) to see if it's a movie.
     *
     * @param extension The extension (without the dot)
     * @return True if it's a movie.
     */
    public static boolean isMoveExtension(String extension)
    {
        return extension.equalsIgnoreCase("avi") ||
                        extension.equalsIgnoreCase("mpeg") ||
                        extension.equalsIgnoreCase("mpg");
    }

    /**
     * Converts a byte count to a string with a small number and a unit.
     * For instance, 2324 is converted to "2 KB". Units up to TB is supported.
     * @param byteCount The number of bytes.
     * @return The string.
     */
    public static String byteCountToString(long byteCount)
    {
        String[] units = new String[] {"bytes", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        while (unitIndex+1 < units.length && byteCount > 900)
        {
            byteCount = (byteCount+512) / 1024; // Not correct but simple
            unitIndex ++;
        }

        return byteCount + " " + units[unitIndex];
    }

    /**
     * Adds a new part to an url representing a directory. Care is taken to
     * get a /, and only one / between the parts.
     * @param dirUrl - The base of the url. May end with a /.
     * @param toAdd - The part to add. May start with a / but then dirUrl
     * shouldn't end with one.
     * @return The concatenated url.
     */
    public static String addToDirUrl(String dirUrl, String toAdd)
    {
        if (dirUrl.endsWith("/") || toAdd.startsWith("/"))
        {
            return dirUrl + toAdd;
        }
        return dirUrl + "/" + toAdd;
    }

    /**
     * Constructs a string describing a class, including interfaces.
     *
     * @param aClass The class to describe
     * @return The describing string. For instance
     * <code>com.sun.media.format.AviVideoFormat -> javax.media.format.VideoFormat -> javax.media.Format</code>
     */
    public static String getClassNameChain(Class aClass)
    {
        StringBuffer buf = new StringBuffer();
        Class[] interfaces = aClass.getInterfaces();
        String seperator = "";
        do
        {
            buf.append(seperator);
            buf.append(aClass.getName());
            seperator = " -> ";
        }
        while ((aClass = aClass.getSuperclass()) != null &&
                !aClass.equals(Object.class));

        if (interfaces.length != 0)
        {
            seperator = "";
            buf.append(" (");
            for (int i = 0; i < interfaces.length; i++)
            {
                Class aInterface = interfaces[i];
                buf.append(seperator);
                buf.append(aInterface.getName());
                seperator = ", ";
            }
            buf.append(")");
        }

        return buf.toString();
    }

    public static File subdirOrFail(File dir, String subdirName)
        throws FileNotFoundException
    {
        // Check that we aren't fooled to move to somewhere else in the
        // file tree. This won't work with symbolic links in the data dir.
        File subdir = new File(dir, subdirName);
        if (!new File(subdir.getParent()).equals(dir))
        {
            throw new FileNotFoundException("Strange chars in the request " +
                                            "that confused PictBook. " +
                                            "Please, don't try any hacking. " +
                                            "It probably won't work.");
        }
        return subdir;
    }
}
