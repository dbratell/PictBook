/*
 * Created by IntelliJ IDEA.
 * User: Bratell
 * Date: 2002-okt-14
 * Time: 17:57:37
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package test;

import pictbook.Util;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Attempt of reproducing crash but also test bench for ImageIO code.
 */
public class CrashReader
{
    public static void main(String[] args) throws IOException
    {
        String[] formats = ImageIO.getReaderFormatNames();
        System.out.println("Supported formats: ");
        for (int i = 0; i < formats.length; i++)
        {
            String format = formats[i];
            System.out.println(format);
        }
        File dir =  new File("j:\\documents and settings\\bratell\\desktop\\bilder");
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++)
        {
            File file = files[i];
            if (file.getName().endsWith("jpg") &&
                    !file.getName().equals("xxxdungeon siege multi player map.jpg"))
            {
                System.out.print("Converting "+file.getName()+"...");
                convertImage(file);
                System.out.println("");
            }
        }
    }

    private static void convertImage(File imageFile) throws IOException
    {
        File targetFile = new File("j:\\small.jpg");
        String extension = Util.getExtension(imageFile);
        String targetExtension = Util.getExtension(targetFile);
        Iterator readers = ImageIO.getImageReadersBySuffix(extension);

        // Get first
        ImageReader reader = (ImageReader)readers.next();
        ImageInputStream inImageStream = null;
        ImageOutputStream outImageStream = null;
        ImageWriter writer = null;
        try
        {
            inImageStream = ImageIO.createImageInputStream(imageFile);
            // Read forward only
            reader.setInput(inImageStream, true);
            // First image in the file
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            System.out.print(" ("+width+"x"+height+") ");
            if (width > 5000 || height > 5000 || width * height > 10e6)
            {
                System.out.print(" too big ");
                return;
            }
//            ImageReadParam param = new ImageReadParam();
            BufferedImage image = reader.read(0);
            // Scale it to maxSize width, maintaining aspect ratio
            System.out.print(" 1 ");
            Image resizedImage =
                    image.getScaledInstance(100, -1, Image.SCALE_AREA_AVERAGING);
            System.out.print(" 2 ");
            BufferedImage bi = Image2BufferedImage(resizedImage);
            System.out.print(" 3 ");
            Util.safeClose(inImageStream);
            inImageStream = null;
            if (bi == null)
            {
                return; // Exception?
            }
            System.out.print(" 4 ");
            Iterator writers = ImageIO.getImageWritersBySuffix(targetExtension);
            if (!writers.hasNext())
            {
                return; // Exception?
            }

            writer = (ImageWriter)writers.next();
            System.out.print(" 5 ");
            outImageStream = ImageIO.createImageOutputStream(targetFile);
            System.out.print(" 6 ");
            writer.setOutput(outImageStream);
            System.out.print(" 7 ");
            writer.write(bi);
            System.out.print(" 8 ");
            Util.safeClose(outImageStream);
            outImageStream = null;
        }
        finally
        {
            Util.safeClose(inImageStream);
            Util.safeClose(outImageStream);
      reader.reset();
            if (writer != null)
                writer.reset();
        }
    }

    private static BufferedImage Image2BufferedImage(Image image)
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
        // Not hardcode type?
        System.out.print(" 2a ");
        BufferedImage out = new BufferedImage(size.width, size.height,
                BufferedImage.TYPE_INT_RGB);
        System.out.print(" 2b ");
        Graphics2D outContext = out.createGraphics();
        System.out.print(" 2c ");
        outContext.drawImage(image, 0, 0, null);
        System.out.print(" 2d ");
        outContext.dispose();

        return out;
	}

    private static Dimension getImageSize(Image image)
    {
/*
        ImageLoaderClass imageLoader = new ImageLoaderClass();
        if (!imageLoader.waitForImage(image))
        {
            // ERROR (timeout or image error)
//            return null;
        }
*/

        int width = image.getWidth(null); // Will block. I hope.
        int height = image.getHeight(null);
        if (width == -1 || height == -1)
        {
            // We don't know the size
            return null;
        }
        return new Dimension(width, height);
    }


}
