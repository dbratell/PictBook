package pictbook;

import pictbook.storage.PictureDirInfo;
import pictbook.storage.PictureInfo;
import pictbook.storage.Storage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * Returns an image (file) but can also scale the image before it's returned.
 * The scaling is done on-demand and cached.
 *
 *  @author Daniel Bratell
 */
public class ImageServlet extends HttpServlet
{
    private Configuration mConfig;
    private Storage mStorage;

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
        System.out.println("ImageServlet doGet: " + ServletUtil.getCurrentPageUrl(req));
        String path = ServletUtil.decodedPathInfo(req);
        try
        {
            // Strip last part which is the file name
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash == -1)
            {
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String fileName = path.substring(lastSlash+1);
            path = path.substring(0,lastSlash);
            PictureDirInfo pictDir = mStorage.getPictBookDir(path);
            String size = req.getParameter("size");
            if (size == null)
            {
                // The normal image
                sendNormalImage(req, pictDir, fileName, res);
            }
            else
            {
                int maxSize;
                try
                {
                    maxSize = Integer.parseInt(size);
                    if (maxSize < 0 || maxSize > 1000)
                    {
                        maxSize = 100;
                    }
                }
                catch (NumberFormatException e)
                {
                    maxSize = 100;
                }

                sendShrinkedImage(req, pictDir, fileName, maxSize, res);
            }
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
                                ServletUtil.decodedPathInfo(req) + " is not the path of an Image.");
                    }
                    else
                    {
                        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                String.valueOf(e));
                    }
                }
                else
                {
//                    e.printStackTrace();
                }
            }
            catch (IOException ioe2)
            {
                // Nothing to do. :-(
            }
            return;
        }
    }

    private void sendShrinkedImage(HttpServletRequest req,
                                          PictureDirInfo dirInfo,
                                   String fileName,
                                   int maxSize,
                                   HttpServletResponse res)
            throws IOException
    {
        File cachedFile = dirInfo.getCachedImageFile(fileName, maxSize);
        // Thread safety. Lock the generated "file"
        String fileNameToLock = cachedFile.toString().intern();
        synchronized (fileNameToLock)
        {
            if (!cachedFile.exists())
            {
                // Create it
                boolean createdImage =
                        createScaledImage(dirInfo, fileName,
                                          maxSize, cachedFile);
                if (!createdImage)
                {
                    // Still no scaled image. Use original
                    cachedFile = new File(dirInfo.getPictDir(), fileName);
                }
            }
        }
        sendFile(req, cachedFile, res);
    }

    private boolean createScaledImage(PictureDirInfo dirInfo, String fileName, int maxSize,
                                   File targetFile)
            throws IOException
    {
        PictureInfo imageInfo = dirInfo.getPictureInfo(fileName);
        File origFile = imageInfo.getLocalFile();
        String targetExtension = Util.getExtension(targetFile);
        if (targetExtension.equalsIgnoreCase("gif"))
            throw new IOException("Don't support writing gifs");
        boolean isMovie = imageInfo.isMovie();
        if (isMovie)
        {
            try
            {
                origFile = getGrabbedFirstFrame(dirInfo, origFile);
            }
            catch (IOException e)
            {
                // Use the fallback
                origFile = mConfig.getMoviePlaceHolder();
            }
        }

        String extension = Util.getExtension(origFile);
        Iterator readers = ImageIO.getImageReadersBySuffix(extension);
        if (!readers.hasNext())
        {
            return false;
        }

        // Get first
        ImageReader reader = (ImageReader)readers.next();
        ImageInputStream inImageStream = null;
        try
        {
            inImageStream = ImageIO.createImageInputStream(origFile);
            // Read forward only
            reader.setInput(inImageStream, true);
            // First image in the file
            final int firstImageIndex = 0;
            int width = reader.getWidth(firstImageIndex);
            int height = reader.getHeight(firstImageIndex);
            if (width > 5000 || height > 5000 || width * height > 10e6)
            {
                // Too big (might crash even if we break here)
                return false;
            }
            // Don't scale upwards in size
            if (maxSize > width)
            {
                maxSize = width;
            }
            BufferedImage image = reader.read(firstImageIndex);
            // Scale it to maxSize width, maintaining aspect ratio
            // Could scale directly in the reader, but this give a much
            // better result.
            Image resizedImage =
                    image.getScaledInstance(maxSize, -1,
                                            Image.SCALE_AREA_AVERAGING);
            BufferedImage bi = Util.Image2BufferedImage(resizedImage);
            Util.safeClose(inImageStream);
            inImageStream = null;
            if (bi == null)
            {
                return false;
            }

            if (isMovie)
                addMovieOverlay(bi);
            ImageIO.write(bi, targetExtension, targetFile);
        }
        finally
        {
            Util.safeClose(inImageStream);
            // Crashes the JVM if the image read was too big
            reader.reset();
//            resetReader(reader);
        }
        return true;
    }

    /**
     * Adds a transparent "play" symbol in the lower right
     * corner of the image.
     * @param image The image to add the symbol to. The image can't be too
     * small. It must be at least 20x25 pixels.
     */
    private static void addMovieOverlay(BufferedImage image)
    {
        Graphics2D graphics = (Graphics2D)image.getGraphics();
        try
        {
            int width = image.getWidth();
            int height = image.getHeight();
            int[] polyX = new int[] {
                width - 20, width - 5, width - 20
            };
            int[] polyY = new int[] {
                height - 25, height - 15, height - 5
            };
            Shape triangle = new Polygon(polyX, polyY, polyX.length);

            Stroke pen = new BasicStroke(3.0f); // 3 pixels wide pen
            graphics.setStroke(pen);
            graphics.setColor(Color.BLACK);

            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.draw(triangle);

            Composite instance =
                    AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                               Configuration.getMovieOverlayTransparency());
            graphics.setComposite(instance);
            graphics.setColor(Color.WHITE);
            graphics.fill(triangle);
        }
        finally
        {
            graphics.dispose();
        }
    }

    private static File getGrabbedFirstFrame(PictureDirInfo dirInfo,
                                             File origFile)
            throws IOException
    {
        File grabbedFile = dirInfo.getGrabbedImageFile(origFile.getName());
        if (!grabbedFile.exists())
        {
            MovieFrameGrabber grabber = new MovieFrameGrabber(origFile,
                                                              grabbedFile);
            if (!grabber.grabFrame())
            {
                throw new IOException("Couldn't grab first frame of movie");
            }
        }
        return grabbedFile;
    }

//    private void resetReader(ImageReader reader)
//    {
//        reader.setInput(null);
//        reader.setLocale(null);
//        reader.removeAllIIOReadUpdateListeners();
//        reader.removeAllIIOReadWarningListeners();
//        reader.removeAllIIOReadProgressListeners();
////        reader.clearAbortRequest();
//  }

    private static void sendNormalImage(HttpServletRequest req,
                                        PictureDirInfo pictDir,
                                        String fileName,
                                        HttpServletResponse res)
            throws IOException
    {
        File imageFile = new File(pictDir.getPictDir(), fileName);
        sendFile(req, imageFile, res);
    }

    private static void sendFile(HttpServletRequest req,
                                 File fileName, HttpServletResponse res)
            throws IOException
    {
//        String cacheControl = req.getHeader("cache-control");
//        System.out.println("cacheControl = " + cacheControl);
//        Enumeration headers = req.getHeaderNames();
//        while (headers.hasMoreElements())
//        {
//            String header = (String) headers.nextElement();
//            System.out.println("header = " + header);
//        }
        long modifiedSince = req.getDateHeader("If-Modified-Since");
        long modifiedDate = fileName.lastModified();
        if (modifiedSince != -1 &&
                modifiedDate != 0 &&
                modifiedDate <= modifiedSince)
        {
            // it has not been modified since the cached copy was requested
            res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        OutputStream browserOut = null;
        FileInputStream imageFileStream = null;
        try
        {
            long size = fileName.length();
            imageFileStream = new FileInputStream(fileName);
            String contentType = guessContentType(fileName.getName());
            res.setContentType(contentType);
            res.setContentLength((int)size); // It better not be larger than MAX_INT
            res.setDateHeader("Last-Modified", fileName.lastModified());
            res.setHeader("Cache-Control", "public"); // It's cacheable
            browserOut = res.getOutputStream();
            Util.copyStreams(imageFileStream, browserOut);
        }
        finally
        {
            Util.safeClose(browserOut);
            Util.safeClose(imageFileStream);
        }
    }

    private static String guessContentType(String fileName)
    {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg"))
            return "image/jpeg";

        if (lowerName.endsWith(".png"))
            return "image/png";

        if (lowerName.endsWith(".gif"))
            return "image/gif";

        return "appliction/octetstream";
    }
}

