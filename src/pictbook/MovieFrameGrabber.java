package pictbook;

// import com.sun.media.ExtBuffer;

import javax.imageio.ImageIO;
import javax.media.Buffer;
import javax.media.ConfigureCompleteEvent;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.NoDataSourceException;
import javax.media.NoProcessorException;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.Renderer;
import javax.media.ResourceUnavailableEvent;
import javax.media.ResourceUnavailableException;
import javax.media.StopEvent;
import javax.media.Time;
import javax.media.UnsupportedPlugInException;
import javax.media.control.TrackControl;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;
import javax.media.protocol.DataSource;
import javax.media.util.BufferToImage;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * This class can grab an interestring image from the beginning of a movie.
 * It avoids taking "boring" images.
 *
 * @author Daniel Bratell
 */
class MovieFrameGrabber implements ControllerListener
{
    private Processor mProcessor;
    private final Object mStatusChangeLock = new Object();
    private final Object mConversionEndedLock = new Object();
    private boolean mStillRunning = true;

    private final File mSourceFile;
    private final File mDestFile;

    /**
     * Constructs a MovieFrameGrabber object with a movie as source and a
     * file to put the grab in.
     * @param sourceFile The movie file.
     * @param destFile The name of the file to create.
     */
    public MovieFrameGrabber(File sourceFile, File destFile)
    {
        mSourceFile = sourceFile;
        mDestFile = destFile;
    }

    /**
     * Does the actal grabbing.
     *
     * @return Returns true  if the grab was successful.
     * @throws IOException
     */
    public boolean grabFrame()
            throws IOException
    {
        URL sourceFileUrl = mSourceFile.toURL();
        System.out.println("sourceFileUrl = " + sourceFileUrl);
        DataSource source;
        try
        {
            source = Manager.createDataSource(sourceFileUrl);
        }
        catch (NoDataSourceException e)
        {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
            throw new IOException("Couldn't read "+sourceFileUrl+": "+e);
        }
        try
        {
            mProcessor = Manager.createProcessor(source);
        }
        catch (NoProcessorException e)
        {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
            throw new IOException("Couldn't decode "+sourceFileUrl+": "+e);
        }
        mProcessor.addControllerListener(this);
        mProcessor.configure();
        if (!waitForState(Processor.Configured))
        {
            return false;
        }

        // So that it plays itself without someone reading from the output
        // datasource
        mProcessor.setContentDescriptor(null);
        TrackControl[] trackControls = mProcessor.getTrackControls();

        // Search for the track control for the video track and disable the
        // rest
        TrackControl videoTrack = null;
        for (int i = 0; i < trackControls.length; i++)
        {
            TrackControl trackControl = trackControls[i];
            if (trackControl.getFormat() instanceof VideoFormat &&
                videoTrack == null)
            {
                videoTrack = trackControl;
            }
            else
            {
                trackControl.setEnabled(false);
            }
        }

        // Check if videoTrack is null which can happen if it was a movie with
        // only sound.
        if (videoTrack == null)
        {
            return false;
        }

        FileRenderer fileRenderer = new FileRenderer(mDestFile, mConversionEndedLock);
        try
        {
            videoTrack.setRenderer(fileRenderer);
        }
        catch (UnsupportedPlugInException e)
        {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
            throw new IOException("Our renderer didn't understand "+sourceFileUrl+": "+e);
        }

        mProcessor.realize();
        if(!waitForState(Controller.Realized))
        {
            return false;
        }
        mProcessor.prefetch();
        if(!waitForState(Controller.Prefetched))
        {
            return false;
        }
        synchronized (mConversionEndedLock)
        {
            // Play ten seconds
            mProcessor.setStopTime(new Time(10.0));
//            mProcessor.setRate(5); // As fast as possible
            mProcessor.start();
            try
            {
                mConversionEndedLock.wait();
                System.out.println("Stoppsignal: "+mDestFile);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        mProcessor.removeControllerListener(this);
        mProcessor.stop();
        mProcessor.close();
        mProcessor.deallocate();
        System.out.println("Avrättat mProcessor");
        return fileRenderer.saveGrab();
    }

    /**
     *  The event listener... Sends signals when funny stuff happens.
     * @param event The event that has occurred/is occurring
     */
    public void controllerUpdate(ControllerEvent event)
    {
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
     *
     * @param state The state to wait for
     * @return Returns true if the state was entered and false if an error
     * has occurred.
     */
    private boolean waitForState(int state)
    {
        synchronized (mStatusChangeLock)
        {
            while (mProcessor.getState() != state && mStillRunning)
            {
                try
                {
                    mStatusChangeLock.wait();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return mStillRunning;
    }


    /**
     * A renderer that finds an interesting image in the file. Maybe this
     * should be implemented as a Codec instead.
     *
     * @author Daniel Bratell
     */
    static class FileRenderer implements Renderer
    {
        private static final String RENDERER_NAME = "My FileRenderer";
        private static final Format[] ALLOWED_FORMATS =
                new Format[]{new VideoFormat(null)}; // All formats

//        private final Processor mProcessor;
        private Format mSetFormat;
        private boolean mHasGoodImage = false;

        private final Object mStopSignal;

        private BufferedImage mCurrentBestGrab;

        private final File mFile;
        private long mBadImageTimeStamp;
        private static final boolean DEBUG_DIVX_5 = true;

        private FileRenderer(File file, Object stopSignal)
        {
//            mProcessor = processor;
            mFile = file;
            mStopSignal = stopSignal;
        }

        public Format[] getSupportedInputFormats()
        {
            return ALLOWED_FORMATS; // All formats
        }

        /**
         * This is a little magic. We said that we accepted all formats but
         * in reality we only support RGB and YUV formats since those are
         * the formats that BufferToImage support. This isn't documented
         * as far as I know. If the format isn't to our liking, we return null.
         *
         * @param format The format that will be used for the input.
         * @return Returns the format if it's accepted or null if it's declined.
         */
        public Format setInputFormat(Format format)
        {
            if (format instanceof RGBFormat ||
                format instanceof YUVFormat)
            {
                mSetFormat = format;
            }
            else if (DEBUG_DIVX_5 && format.toString().startsWith("DX50"))
            {
                // Experimenting
                System.out.println("DivX 5.0");
                mSetFormat = format;
            }
            else
            {
//            AviVideoFormat stuff = new AviVideoFormat("hej");
                System.out.println("Denying format = " + format + " ("+
                                   Util.getClassNameChain(format.getClass())+")");
                System.out.println("DataType: "+Util.getClassNameChain(format.getDataType())+
                                   ", encoding: "+format.getEncoding());
                mSetFormat = null;
            }

            return mSetFormat;
        }

        public void start()
        {
            // This is a NOOP
        }

        public void stop()
        {
            // In case |process| is never called.
            System.out.println("Renderer.stop called so we send the stop signal");
            synchronized (mStopSignal)
            {
                mStopSignal.notifyAll(); // We are finished
            }
            // This is a NOOP
        }

        public int process(Buffer buffer)
        {
            if (DEBUG_DIVX_5 && mSetFormat.toString().startsWith("DX50"))
            {
//                 com.sun.media.ExtBuffer buffer2 = (ExtBuffer)buffer;
//                buffer2.
                printBufferInfo(buffer);
                return PLUGIN_TERMINATED;
            }

            if (mHasGoodImage)
            {
                return BUFFER_PROCESSED_OK;
            }


            long seqNo = buffer.getSequenceNumber();
            long timeStamp = buffer.getTimeStamp();
            System.out.println("FileRenderer.process: "+mFile);

            // Skip all images within one second of a bad image
            if (mCurrentBestGrab != null &&
                    timeStamp < mBadImageTimeStamp + 1e9)
            {
                System.out.println("Still within one second of a bad image");
                return BUFFER_PROCESSED_OK;
            }
            System.out.println("seqNo = " + seqNo);
            System.out.println("timeStamp = " + timeStamp/1e9);

            VideoFormat videoFormat = (VideoFormat) buffer.getFormat();

            BufferToImage buffer2image = new BufferToImage(videoFormat);
            Image image = buffer2image.createImage(buffer);
            if (image != null)
            {
                mCurrentBestGrab = Util.Image2BufferedImage(image);
                Raster raster = mCurrentBestGrab.getData();
                boolean goodImage = examineRaster(raster);
                // We only look 10 seconds into the movie
                if (goodImage || timeStamp > 10e9)
                {
                    mHasGoodImage = true;
                    System.out.println("Stoppar: "+mFile+
                                       " ty goodImage = "+goodImage+
                                       " och timeStamp = "+ timeStamp);
                    synchronized (mStopSignal)
                    {
                        mStopSignal.notifyAll(); // We are finished
                    }
                }
                else
                {
                    mBadImageTimeStamp = timeStamp;
                }
            }
            else
            {
                System.out.println("BufferToImage cannot handle " + mSetFormat);
                System.out.println("Stoppar: "+mFile);
                synchronized (mStopSignal)
                {
                    mStopSignal.notifyAll(); // We are finished
                }
            }

            // Extra call. Not really needed I think.
//            synchronized (mStopSignal)
//            {
//                mStopSignal.notifyAll(); // We are finished
//            }
            return BUFFER_PROCESSED_OK;
        }

        private void printBufferInfo(Buffer buffer)
        {
            System.out.println("buffer = " + buffer);
            System.out.println("buffer.getData() = " + buffer.getData());
            System.out.println("buffer.getFormat() = " + buffer.getFormat());
            System.out.println("buffer.getHeader() = " + buffer.getHeader());
            System.out.println("buffer.getOffset() = " + buffer.getOffset());
            System.out.println("buffer.getLength() = " + buffer.getLength());
            System.out.println("buffer.getClass() = " + Util.getClassNameChain(buffer.getClass()));
            System.out.println("buffer.getHeader().getClass() = " + Util.getClassNameChain(buffer.getHeader().getClass()));
            System.out.println("buffer.getData().getClass() = " + Util.getClassNameChain(buffer.getData().getClass()));

        }

        /**
         * Check if the picture is interesting enough to use.
         * @param raster - The raster to examing. It must be in YUV or RGB. The format used by the renderer
         * is used to extract format information.
         * @return Returns true if the raster is "interesting". False if it's "boring".
         */
        private boolean examineRaster(Raster raster)
        {
            int numBands = raster.getNumBands();
            System.out.print("numBands = " + numBands);
            int width = raster.getWidth();
            System.out.print(", width = " + width);
            int height = raster.getHeight();
            System.out.println(", height = " + height);
            int goodImagePartCounter = 0;
            if (mSetFormat instanceof RGBFormat)
            {
                // Check one quarter of the image at a time
                boolean goodImagePart = examineRGBRasterPart(raster, 0, 0, width/2, height/2);
                if (goodImagePart)
                    goodImagePartCounter++;
                goodImagePart = examineRGBRasterPart(raster, width/2, 0, width/2, height/2);
                if (goodImagePart)
                    goodImagePartCounter++;
                goodImagePart = examineRGBRasterPart(raster, 0, height/2, width/2, height/2);
                if (goodImagePart)
                    goodImagePartCounter++;
                goodImagePart = examineRGBRasterPart(raster, width/2, height/2, width/2, height/2);
                if (goodImagePart)
                    goodImagePartCounter++;
            }
            else if (mSetFormat instanceof YUVFormat)
            {
                // Check one quarter of the image at a time
                boolean goodImagePart = examineYUVRasterPart(raster, 0, 0, width/2, height/2);
                if (goodImagePart)
                    goodImagePartCounter++;
                    goodImagePart = examineYUVRasterPart(raster, width/2, 0, width/2, height/2);
                if (goodImagePart)
                    goodImagePartCounter++;
                    goodImagePart = examineYUVRasterPart(raster, 0, height/2, width/2, height/2);
                if (goodImagePart)
                    goodImagePartCounter++;
                    goodImagePart = examineYUVRasterPart(raster, width/2, height/2, width/2, height/2);
                if (goodImagePart)
                    goodImagePartCounter++;
            }
            else
            {
                System.out.println("Not an RGB image. Assuming good.");
                System.out.println("mSetFormat.class = " + mSetFormat.getClass().getName());
            }
            return goodImagePartCounter >= 2;
        }

        /**
         * Examines the raster to see if it's interesting.
         *
         * Uses formulas from http://www.via.ecp.fr/~remi/ecp/tpi/rapport/yuv.html
         * <pre>r = 1 * y - 0.0009267*(u-128) + 1.4016868*(v-128)
         * g = 1 * y - 0.3436954*(u-128) - 0.7141690*(v-128)
         * b = 1 * y + 1.7721604*(u-128) + 0.0009902*(v-128)</pre>
         *
         * @param raster The raster
         * @param startX The x coordinate for the top left corner of the area
         * to examine.
         * @param startY The y coordinate for the top left corner of the area
         * to examine.
         * @param width The width of the area to examine.
         * @param height The height of the area to examine.
         * @return Returns true if the area is "interesting", and false if
         * it's "boring".
         */
        private boolean examineYUVRasterPart(Raster raster, int startX, int startY, int width, int height)
        {
            System.out.print("startX = " + startX);
            System.out.print(", startY = " + startY);
            System.out.print(", width = " + width);
            System.out.println(", height = " + height);
            System.out.println("Converting from YUV to RGB");
            int[] Y = raster.getSamples(startX, startY, width, height, 0, (int[])null);
            int[] U = raster.getSamples(startX, startY, width, height, 1, (int[])null);
            int[] V = raster.getSamples(startX, startY, width, height, 2, (int[])null);
            int[] red = new int[Y.length];
            int[] green = new int[Y.length];
            int[] blue = new int[Y.length];
            // Convert to RGB ... Maybe it would be better to just look at
            // for instance Y
            for (int i = 0; i < Y.length; i++)
            {
                int y = Y[i];
                int u = U[i];
                int v = V[i];
                red[i] = (int)(1 * y - 0.0009267*(u-128) + 1.4016868*(v-128));
                green[i] = (int)(1 * y - 0.3436954*(u-128) - 0.7141690*(v-128));
                blue[i] = (int)(1 * y + 1.7721604*(u-128) + 0.0009902*(v-128));
            }
            boolean goodImagePart = examineRGBArrays(red, green, blue);

            return goodImagePart;
        }

        /**
         * Examines the raster to see if it's interesting.
         *
         * @param raster The raster
         * @param startX The x coordinate for the top left corner of the area
         * to examine.
         * @param startY The y coordinate for the top left corner of the area
         * to examine.
         * @param width The width of the area to examine.
         * @param height The height of the area to examine.
         * @return Returns true if the area is "interesting", and false if
         * it's "boring".
         */
        private boolean examineRGBRasterPart(Raster raster, int startX, int startY, int width, int height)
        {
            System.out.print("startX = " + startX);
            System.out.print(", startY = " + startY);
            System.out.print(", width = " + width);
            System.out.println(", height = " + height);
            int[] red = raster.getSamples(startX, startY, width, height, 0, (int[])null);
            int[] green = raster.getSamples(startX, startY, width, height, 1, (int[])null);
            int[] blue = raster.getSamples(startX, startY, width, height, 2, (int[])null);
            boolean goodImagePart = examineRGBArrays(red, green, blue);

            return goodImagePart;
        }

        private boolean examineRGBArrays(int[] red, int[] green, int[] blue)
        {
            boolean boringImage = false;
            int pixelCount = red.length;
            System.out.println("pixelCount = " + pixelCount);

            float redAverage = calculateAverage(red);
            System.out.print("redAverage = " + redAverage);
            float greenAverage = calculateAverage(green);
            System.out.print(", greenAverage = " + greenAverage);
            float blueAverage = calculateAverage(blue);
            System.out.println(", blueAverage = " + blueAverage);

            float redVar = calculateVariance(red, redAverage);
            System.out.print("redVar = " + redVar);
            float greenVar = calculateVariance(green, greenAverage);
            System.out.print(", greenVar = " + greenVar);
            float blueVar = calculateVariance(blue, blueAverage);
            System.out.println(", blueVar = " + blueVar);

            final float treshold = 40*40;
            if (redVar < treshold && greenVar < treshold && blueVar < treshold)
            {
                // It's one coloured
                System.out.println("The picture has no content");
                boringImage = true;
            }
            return !boringImage;
        }

        private float calculateAverage(int[] data)
        {
            long sum = 0;
            for (int i = 0; i < data.length; i++)
            {
                sum += data[i];
            }
            return sum/(float)data.length;
        }

        private float calculateVariance(int[] data, float average)
        {
            float sum = 0;
            for (int i = 0; i < data.length; i++)
            {
                float diff = data[i]-average;
                sum += diff*diff;
            }
            return sum/data.length;
        }

        public String getName()
        {
            return RENDERER_NAME;
        }

        public void open() throws ResourceUnavailableException
        {
            // NOOP
        }

        public void close()
        {
            // NOOP
        }

        public void reset()
        {
            // NOOP
        }

        public Object[] getControls()
        {
            // We have no controls
            return new Object[0];
        }

        public Object getControl(String s)
        {
            // We have no controls
            return null;
        }

        private boolean saveGrab()
            throws IOException
        {
            if (mCurrentBestGrab == null)
                return false;

            ImageIO.write(mCurrentBestGrab, "jpg", mFile);
            return true;
        }
    }

}
