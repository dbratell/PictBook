package pictbook.storage;

import pictbook.Util;
import pictbook.Configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Info for files in a directory.
 *
 * @author Daniel Bratell
 */
public class PictureDirInfo
{
    private final Map mCachedPictureInfos = new HashMap();
    private final String mName;

    private final File mPictDir;
    private final File mDataDir;
    private final Properties mData;
    private final File mDataFile;

    /**
     * Creates an object that handles the actions for one directory.
     *
     * @param pictDir The directory with the files.
     * @param dataDir The directory with the cached data and data files.
     * @throws IOException if something goes wrong I/O-wise.
     */
    public PictureDirInfo(File pictDir, File dataDir)
        throws IOException
    {
        mPictDir = pictDir;
        mDataDir = dataDir;
        mDataFile = new File(dataDir, "data");
        if (mDataFile.exists())
        {
            mData = Util.getProperties(mDataFile);
        }
        else
        {
            mData = new Properties();
        }

        mName = dataDir.getName();
    }

    /**
     * The directory where the original pictures are.
     * @return A File object representing the directory.
     */
    public File getPictDir()
    {
        return mPictDir;
    }

    private String getProperty(String name)
    {
        return mData.getProperty(name);
    }

    private File[] getImageFiles()
    {
        File pictDir = getPictDir();
        File[] images = pictDir.listFiles(new FilenameFilter(){
            // Only directories
            public boolean accept(File file, String name)
            {
                String lowerName = name.toLowerCase();
                return (lowerName.endsWith(".jpg") ||
                        lowerName.endsWith(".png") ||
                        lowerName.endsWith(".gif") ||
                        lowerName.endsWith(".mpeg") ||
                        lowerName.endsWith(".mpg") ||
                        lowerName.endsWith(".avi"));
            }
        });

        return images;
    }

    /**
     * Returns the names of the sub directories to this one. Only non hidden
     * dirs are returned.
     *
     * @return Returns an array of Strings. The array can be empty.
     */
    public String[] getVisbleSubDirs()
    {
        File pictDir = getPictDir();
        String[] dirs = pictDir.list(new FilenameFilter(){
            // Only directories
            public boolean accept(File file, String name)
            {
                boolean isDir = new File(file, name).isDirectory();
                if (!isDir)
                {
                    return false;
                }

                // Check if hidden
                return !"true".equals(getProperty(getHidePropertyName(name)));
            }
        });

        return dirs;
    }

    /**
     * The name of the file containing the cached image of something scaled.
     * The file may not exist since this method is also used to know where to
     * save the cached file.
     *
     * @param fileName The name of the original non-scaled file.
     * @param imageSize The size of the image.
     * @return The name of the cached file.
     */
    public File getCachedImageFile(String fileName, int imageSize)
    {
        String extension = Util.getExtension(fileName);
        int realNameLength = fileName.length()-extension.length()-1;
        if (extension.equalsIgnoreCase("gif"))
        {
            // Doesn't support creating GIF's. Use a PNG instead.
            realNameLength=fileName.length();
            extension = "png";
        }
        else if (Util.isMoveExtension(extension))
        {
            // We write movies with jpeg
            realNameLength=fileName.length();
            extension = "jpg";
        }
        String nameWithoutExt = fileName.substring(0,realNameLength);
        return new File(mDataDir,
                nameWithoutExt+"-s"+imageSize+"."+extension);
    }

    /**
     * Get the PictureInfo object for an image in this directory. Even if
     * the file doesn't exist, an object will be created but the data in it
     * will be dummy.
     *
     * @param name The name of the image.
     * @return The PictureInfo object.
     */
    public PictureInfo getPictureInfo(String name)
    {
        PictureInfo pi = (PictureInfo)mCachedPictureInfos.get(name);
        if (pi == null)
        {
            File imageFile = new File(getPictDir(), name);
            // XXX Should be create date of the image, not the file.
            Date createDate = new Date(imageFile.lastModified());
            // XXX: Fix correct values
            pi = new PictureInfo(imageFile, getDescription(name), createDate);
            if (hasOrder(name))
            {
                pi.setOrder(getOrder(name));
            }
            pi.setHidden(getHidden(name));
            mCachedPictureInfos.put(name, pi);
        }
        return pi;
    }

    private boolean getHidden(String name)
    {
        String hideParam = getHidePropertyName(name);
        String hideStr = getProperty(hideParam);
        return "true".equals(hideStr); // Copes with null
    }

    private int getOrder(String name)
    {
        String orderParam = getOrderPropertyName(name);
        String orderStr = getProperty(orderParam);
        if (orderStr == null)
        {
            return PictureInfo.UNDEFINED_ORDER;
        }

        try
        {
            return Integer.parseInt(orderStr);
        }
        catch (NumberFormatException nfe)
        {
            return PictureInfo.UNDEFINED_ORDER;
        }
    }

    private boolean hasOrder(String name)
    {
        String order = getProperty(getOrderPropertyName(name));
        return order != null;
    }

    private String getDescription(String name)
    {
        String descriptionParamName = getDescriptionPropertyName(name);
        String description = getProperty(descriptionParamName);
        if (description == null || description.length() == 0)
        {
            description = Configuration.getDefaultDescription(name);
        }
        return description;
    }

    /**
     * Gets all picture infos in the directory, including hidden ones.
     * @return An array of PictureInfo objects. The array can be empty.
     */
    public PictureInfo[] getAllPictureInfos()
    {
        File[] images = getImageFiles();
        PictureInfo[] pictureInfos = new PictureInfo[images.length];
        for (int i = 0; i < images.length; i++)
        {
            File file = images[i];
            String name = file.getName();
            PictureInfo pictureInfo = getPictureInfo(name);
            pictureInfos[i] = pictureInfo;
        }

        Arrays.sort(pictureInfos);
        return pictureInfos;
    }

    /**
     * Saves new information about pictures in a directory.
     *
     * @param attributes - As got directly from HttpServletRequest.getParamMap.
     * @throws IOException
     */
    public void saveChanges(Map attributes) throws IOException
    {
        attributes = convertMapToRightCharset(attributes);
        File[] files = getImageFiles();
        boolean propsNeedSaving = false;
        for (int i = 0; i < files.length; i++)
        {
            File file = files[i];
            String name = file.getName();
            PictureInfo pictureInfo = getPictureInfo(name);
            String descrAttrName = getDescriptionPropertyName(pictureInfo.getName());
            String defaultDescription = Configuration.getDefaultDescription(name);
            propsNeedSaving = setPropertyIfNeeded(mData, attributes,
                                                  descrAttrName, defaultDescription) ||
                    propsNeedSaving;

            String orderAttrName = getOrderPropertyName(name);
            String defaultOrder = String.valueOf(PictureInfo.UNDEFINED_ORDER);
            propsNeedSaving = setPropertyIfNeeded(mData, attributes,
                                                  orderAttrName, defaultOrder) ||
                    propsNeedSaving;

            String hideAttrName = getHidePropertyName(name);
            String defaultHide = "false";
            propsNeedSaving = setPropertyIfNeeded(mData, attributes,
                                                  hideAttrName, defaultHide) ||
                    propsNeedSaving;
        }

        String[] titles = (String[])attributes.get("title");
        String title;
        if (titles != null && titles.length > 0 &&
                (title = titles[0]).length()>0)
        {
            // Compare with current
            if (!title.equals(getTitle()))
            {
                mData.setProperty("title", title);
                propsNeedSaving = true;
            }
        }

        if (propsNeedSaving)
        {
            FileOutputStream outStream = null;
            try
            {
                outStream = new FileOutputStream(mDataFile);
                mData.store(outStream, "Data for PictBook - generated by a web app");
            }
            finally
            {
                Util.safeClose(outStream);
            }
        }

    }

    private static boolean setPropertyIfNeeded(Properties data, Map attributes, String attrName, String defaultValue)
    {
        String[] values = (String[])attributes.get(attrName);
        String currentValue = data.getProperty(attrName);
        boolean dataChanged = false;
        String value;
        if (values != null &&
                values.length > 0)
        {
            value = values[0];
        }
        else
        {
            value = defaultValue;
        }

        if(value.length() > 0)
        {
            // Compare with current value
            if (currentValue == null)
            {
                currentValue = defaultValue;
            }
            if (!value.equals(currentValue))
            {
                data.setProperty(attrName, value);
                dataChanged = true;
            }
        }

        return dataChanged;
    }

    /**
     * Tomcat assumes that the data that comes in is in ISO-8859-1 but I know
     * that it is in UTF-8 so I have to correct what Tomcat did.
     * @param badMap The parameterMap from Tomcat
     * @return A new Map with better values.
     */
    private static Map convertMapToRightCharset(Map badMap)
    {
        HashMap goodMap = new HashMap();
        Set badKeys = badMap.keySet();
        Iterator badKeyIt = badKeys.iterator();
        while (badKeyIt.hasNext())
        {
            String badKey = (String) badKeyIt.next();
            String[] badValues = (String[])badMap.get(badKey);
            String goodKey = Util.reinterpretFrom8859ToUTF8(badKey);
            String[] goodValues = null;
            if (badValues != null)
            {
                goodValues = new String[badValues.length];
                for (int i = 0; i < badValues.length; i++)
                {
                    String badValue = badValues[i];
                    goodValues[i] = Util.reinterpretFrom8859ToUTF8(badValue);
                }
            }

            goodMap.put(goodKey, goodValues);
        }

        return goodMap;
    }

    /**
     * The title of the Picture Book. This is, unless something else is set,
     * the name of the directory.
     *
     * @return The title
     */
    public String getTitle()
    {
        String title = mData.getProperty("title");
        if (title == null)
        {
            title = getDefaultTitle();
        }

        return title;
    }

    /**
     * The default title for a directory with images.
     * @return The title when no title is specified.
     */
    private String getDefaultTitle()
    {
        return mName;
    }

    /**
     * The name of the file containing the image which was grabbed from
     * the movie. It may not exist yet if it hasn't been created. This is just
     * the name.
     * @param fileName The movie file name..
     * @return The name of the grabbed image.
     */
    public File getGrabbedImageFile(String fileName)
    {
        String extension = Util.getExtension(fileName);
        if (!Util.isMoveExtension(extension))
        {
            throw new IllegalArgumentException("Must be a movie");
        }
        String grabbedName = fileName+"-grabbed.jpg";
        return new File(mDataDir, grabbedName);
    }

    /**
     * The name of the description property.
     * @param name The image it concerns
     * @return The name of the property.
     */
    public static String getDescriptionPropertyName(String name)
    {
        return name+".description";
    }

    /**
     * The name of the order property.
     * @param name The image it concerns
     * @return The name of the property.
     */
    public static String getOrderPropertyName(String name)
    {
        return name+".order";
    }

    /**
     * The name of the hide property.
     * @param name The image it concerns
     * @return The name of the property.
     */
    public static String getHidePropertyName(String name)
    {
        return name+".hide";
    }
}
