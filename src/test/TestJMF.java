/*
 * Created by IntelliJ IDEA.
 * User: Bratell
 * Date: Oct 17, 2002
 * Time: 5:20:16 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package test;

import pictbook.Util;

import javax.media.Manager;
import javax.media.NoDataSourceException;
import javax.media.NoPlayerException;
import javax.media.ControllerListener;
import javax.media.ControllerEvent;
import javax.media.Controller;
import javax.media.Processor;
import javax.media.Buffer;
import javax.media.Renderer;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.UnsupportedPlugInException;
import javax.media.ConfigureCompleteEvent;
import javax.media.RealizeCompleteEvent;
import javax.media.PrefetchCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.EndOfMediaEvent;
import javax.media.StopEvent;
import javax.media.format.VideoFormat;
import javax.media.format.RGBFormat;
import javax.media.util.BufferToImage;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.TrackControl;
import javax.media.protocol.DataSource;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class TestJMF implements ControllerListener
{
    private Processor mProcessor;
    private final Object mStatusChangeLock = new Object();
    private boolean mStillRunning = true;
    private final Object mConversionEndedLock = new Object();

    public static void main(String[] args)
            throws IOException, NoDataSourceException, NoPlayerException, InterruptedException, UnsupportedPlugInException
    {
        new TestJMF().testJMF();
    }

    private void testJMF()
            throws IOException, NoDataSourceException, NoPlayerException, InterruptedException, UnsupportedPlugInException
    {
        File testFile = new File("J:\\Hem\\Bratell\\My Pictures\\2002_09_23\\111-1193_MVI.AVI");
        URL testFileUrl = testFile.toURL();
        System.out.println("testFileUrl = " + testFileUrl);
        DataSource source = Manager.createDataSource(testFileUrl);
        String contentType = source.getContentType();
        System.out.println("contentType = " + contentType);
        mProcessor = Manager.createProcessor(source);
        System.out.println("processor = " + mProcessor);
        mProcessor.addControllerListener(this);
        FrameGrabbingControl frameGrabber =
                (FrameGrabbingControl) mProcessor.getControl("javax.media.control.FrameGrabbingControl");
        System.out.println("frameGrabber = " + frameGrabber);
        System.out.println("1");
        mProcessor.configure();
        if(!waitForState(Processor.Configured))
        {
            return;
        }
        // So that it plays itself without someone reading from the output
        // datasource
        mProcessor.setContentDescriptor(null);
        TrackControl[] trackControls = mProcessor.getTrackControls();

        // Search for the track control for the video track.
        TrackControl videoTrack = null;
        for (int i = 0; i < trackControls.length; i++)
        {
            TrackControl trackControl = trackControls[i];
            if (videoTrack == null &&
                    trackControl.getFormat() instanceof VideoFormat)
            {
                videoTrack = trackControl;
            }
            else
            {
                trackControl.setEnabled(false);
            }
        }

        // Check if videoTrack is null
        if (videoTrack == null)
            return;
        videoTrack.setRenderer(new FileRenderer(mProcessor,
                                                new File("j:\\framedump.jpg")));

        System.out.println("1.5");
        mProcessor.realize();
        if (!waitForState(Controller.Realized))
        {
            return;
        }
        System.out.println("2");
        mProcessor.prefetch();
        if (!waitForState(Controller.Prefetched))
        {
            return;
        }
        System.out.println("2.5");
        mProcessor.start();
        synchronized (mConversionEndedLock)
        {
            mConversionEndedLock.wait();
        }
        System.out.println("3");
        mProcessor.close();
        System.out.println("4");
        mProcessor.deallocate();
        System.out.println("5");
        mProcessor.removeControllerListener(this);
    }

    public void controllerUpdate(ControllerEvent event)
    {
        System.out.println("event = " + event);
        if (event instanceof ConfigureCompleteEvent ||
                event instanceof RealizeCompleteEvent ||
                event instanceof PrefetchCompleteEvent)
        {
            synchronized (mStatusChangeLock)
            {
                mStatusChangeLock.notifyAll();
            }
        }
        else if (event instanceof ResourceUnavailableEvent)
        {
            synchronized (mStatusChangeLock)
            {
                mStillRunning = false;
                mStatusChangeLock.notifyAll();
            }
        }
        else if (event instanceof EndOfMediaEvent ||
                event instanceof StopEvent)
        {
            synchronized (mConversionEndedLock)
            {
                mConversionEndedLock.notifyAll();
            }
        }
    }

    /**
     * Block until the processor has transitioned to the given state.
     * Return false if the transition failed.
     */
    private boolean waitForState(int state)
    {
        synchronized (mStatusChangeLock)
        {
            try
            {
                while (mProcessor.getState() != state && mStillRunning)
                    mStatusChangeLock.wait();
            }
            catch (Exception e)
            {
            }
        }
        return mStillRunning;
    }


    static class FileRenderer implements Renderer
    {
        private static final String RENDERER_NAME = "My FileRenderer";
        private static final Format[] ALLOWED_FORMATS =
                new Format[]{new VideoFormat(null)}; // All formats

        private final Processor mProcessor;
        private Format mSetFormat;

        private final File mFile;
        private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

        private FileRenderer(Processor processor, File file)
        {
            System.out.println("FileRenderer.FileRenderer");
            mProcessor = processor;
            mFile = file;
        }

        public Format[] getSupportedInputFormats()
        {
            System.out.println("FileRenderer.getSupportedInputFormats");
            return ALLOWED_FORMATS;
        }

        /**
         * This is a little magic. We said that we accepted all formats but
         * in reality we only support RGB (and YUV?) formats since those are
         * the formats that BufferToImage support. This isn't documented
         * as far as I know. If the format isn't to our liking, we return null.
         *
         * @param format
         * @return
         */
        public Format setInputFormat(Format format)
        {
            System.out.println("FileRenderer.setInputFormat");

            System.out.println("format = " + format);
            System.out.println("RGB:" + (format instanceof RGBFormat));

            if (format instanceof RGBFormat)
            {
                mSetFormat = format;

                return mSetFormat;
            }
            return null;
        }

        public void start()
        {
            System.out.println("FileRenderer.start");
        }

        public void stop()
        {
            System.out.println("FileRenderer.stop");
        }

        public int process(Buffer buffer)
        {
            System.out.println("FileRenderer.process");
            System.out.println("buffer = " + buffer);
            VideoFormat videoFormat = (VideoFormat) buffer.getFormat();
            System.out.println("videoFormat = " + videoFormat);
            System.out.println("RGB:" + (mSetFormat instanceof RGBFormat));

            BufferToImage buffer2image = new BufferToImage(videoFormat);
            Image image = buffer2image.createImage(buffer);
            System.out.println("image = " + image);
            BufferedImage bufferedImage = Util.Image2BufferedImage(image);
            try
            {
                ImageIO.write(bufferedImage, "jpg", mFile);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            mProcessor.stop(); // We are finished
            return BUFFER_PROCESSED_OK; // Error handling?
        }

        public String getName()
        {
            System.out.println("FileRenderer.getName");
            return RENDERER_NAME;
        }

        public void open() throws ResourceUnavailableException
        {
            System.out.println("FileRenderer.open");
        }

        public void close()
        {
            System.out.println("FileRenderer.close");
        }

        public void reset()
        {
            System.out.println("FileRenderer.reset");
        }

        public Object[] getControls()
        {
            System.out.println("FileRenderer.getControls");
            return EMPTY_OBJECT_ARRAY;
        }

        public Object getControl(String s)
        {
            System.out.println("FileRenderer.getControl");
            return null;
        }
    }
}
