package pictbook;

import javax.servlet.ServletConfig;
import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * This class tries to collect all configuration settings.
 *
 * @author Daniel Bratell
 */
public class Configuration
{
    private final HashMap mProperties;

    private static final int DEFAULT_THUMBNAIL_SIZE = 150;
    private static final int MIN_THUMBNAIL_SIZE = 32;
    private static final int MAX_THUMBNAIL_SIZE = 640;
    private static final float MOVIE_OVERLAY_TRANSPARENCY = 0.5f;


    /**
     * The default thumbnail size if the user hasn't specified anything.
     * @return The size in pixels
     */
    public static int getDefaultThumbnailSize()
    {
        return DEFAULT_THUMBNAIL_SIZE;
    }

    /**
     * The minimum size allowed for thumbnails in the UI.
     * @return The size in pixels
     */
    public static int getMinThumbnailSize()
    {
        return MIN_THUMBNAIL_SIZE;
    }

    /**
     * The maximum size allowed for thumbnails in the UI.
     * @return The size in pixels
     */
    public static int getMaxThumbnailSize()
    {
        return MAX_THUMBNAIL_SIZE;
    }

//    public Configuration(HashMap properties)
//    {
//        mProperties = properties;
//        validateProperties();
//    }
//
    /**
     * Creates a configuration object. This is not a static object because a
     * site could have several parallell picture books. It fetches the
     * information from the servlet config. Maybe it would be better to have a
     * parameter somewhere else, where it's easier to change it.
     *
     * @param servletConfig The servletConfig for the servlet using
     * this configuration.
     */
    public Configuration(ServletConfig servletConfig)
    {
        HashMap properties = new HashMap();
        Enumeration paramNames = servletConfig.getInitParameterNames();
        while (paramNames.hasMoreElements())
        {
            String name = (String) paramNames.nextElement();
            properties.put(name, servletConfig.getInitParameter(name));
        }

        mProperties = properties;
        validateProperties();
    }

    private void validateProperties()
    throws IllegalArgumentException
    {
        if (mProperties.get("config.dir") == null)
            throw new IllegalArgumentException("Init param \"config.dir\" "+
                "is missing");
    }

    /**
     * Returns the config dir, where the caches and data registers are.
     * @return a File object with the config dir.
     */
    public File getConfigDir()
    {
        return new File(getProperty("config.dir"));
    }

    /**
     * Fetches a String property from the property object.
     * @param name The name of the property.
     * @return The value of the property.
     */
    private String getProperty(String name)
    {
        return (String) mProperties.get(name);
    }

    /**
     * The default description of the picture. This is normally the name
     * of the file.
     * @param name The name of the original file.
     * @return The decription.
     */
    public static String getDefaultDescription(String name)
    {
        // Could remove extension, but that would make it less obvioius that
        // no explicit description was set.
        return name;
    }

    /**
     * How transparent the movie overlay should be.
     *
     * @return A float between 0.0 and 1.0 describing the how transparent it
     * should be. It should be used directly in AlphaComposite.getInstance.
     *
     * @see java.awt.AlphaComposite#getInstance(int, float)
     */
    public static float getMovieOverlayTransparency()
    {
        return MOVIE_OVERLAY_TRANSPARENCY;
    }

    /**
     * Returns the file that contains a "movie" image to use when a
     * grabbed frame cannot be used.
     * @return The file withthe "movie image".
     */
    public File getMoviePlaceHolder()
    {
        return new File(getConfigDir(), "movie.png");
    }
}
