package pictbook.storage;

import pictbook.Configuration;
import pictbook.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Class (and package) that contains code used to find meta data about
 * the picture book.
 *
 * @author Daniel Bratell
 */
public class Storage
{
    private final Configuration mConfig;
    private static final String TOP_INFO_FILENAME = "top";
    private static final String BOOK_PREFIX = "book";

    /**
     * Creates a Storage object.
     * @param config The configuration for this Storage object.
     */
    public Storage(Configuration config)
    {
        mConfig = config;
    }

//    /**
//     * @return An array of strings of books.
//     */
//    public String[] getVisiblePictBooks()
//    {
//        return getVisibleTopLevelBooks();
//    }

    /**
     * This extracts the list of visible top level books. A book may be hidden
     * in which case it's not returned by this method.
     * @return A String array with book. The array may be empty.
     */
    public String[] getVisibleTopLevelBooks()
    {
        try
        {
            Properties infoFileData = getTopLevelProperties();

            if (infoFileData == null)
            {
                // File missing
                return new String[0];
            }

            ArrayList topBooks = new ArrayList();
            String bookName;
            int i = 1;
            while ((bookName = infoFileData.getProperty(BOOK_PREFIX+i)) != null)
            {
                if (!"true".equals(infoFileData.getProperty(bookName+".hide")))
                {
                    topBooks.add(bookName);
                }
                i++;
            }

            return (String[])topBooks.toArray(new String[]{});
        }
        catch (IOException e)
        {
            return new String[] {e.getMessage()};
        }
    }

    private Properties getTopLevelProperties() throws IOException
    {
        Properties infoFileData = null;
        File configDir = mConfig.getConfigDir();
        File infoFile = new File(configDir, TOP_INFO_FILENAME);
        if (infoFile.exists())
        {
            infoFileData = Util.getProperties(infoFile);
        }
        return infoFileData;
    }

    /**
     * The PictureBookDir for a special path.
     * 
     * @param path  The path to the pictures.
     * @return The dir where the images are and the dir where the meta data is
     * @throws FileNotFoundException if the dir doesn't exist
     * @throws IOException if any other IO problem occurs
     */
    public PictureDirInfo getPictBookDir(String path)
            throws IOException
    {
        StringTokenizer strTok = new StringTokenizer(path, "/");
        if (!strTok.hasMoreTokens())
        {
            throw new FileNotFoundException("Empty request.");
        }
        Properties props = getTopLevelProperties();
        // Top level dir
        String pathElement = strTok.nextToken();
        String pictPath = props.getProperty(pathElement+".path");
        File pictDir = new File(pictPath);
        if (!pictDir.exists())
        {
            throw new FileNotFoundException("Top level dir \""+
                    pathElement+"\" not found.");
        }

        File dataDir = mConfig.getConfigDir();
        dataDir = Util.subdirOrFail(dataDir, pathElement);

        StringBuffer parsedPath = new StringBuffer("/"+pathElement);
        while (strTok.hasMoreTokens())
        {
            String pathElement = strTok.nextToken();
            pictDir = new File(pictDir, pathElement);
            if (!pictDir.exists())
            {
                throw new FileNotFoundException("Directory \""+
                        pathElement+"\" not found in "+
                        parsedPath+".");
            }

            // Check that we aren't fooled to move to somewhere else in the
            // file tree. This won't work with symbolic links in the data dir.
//            File parent = dataDir;
            dataDir = new File(dataDir, pathElement);
//            if (!dataDir.getParent().equals(parent))
//            {
//                throw new FileNotFoundException("Strange chars in the request " +
//                                                "that confused PictBook. " +
//                                                "Please, don't try any hacking. " +
//                                                "It probably won't work.");
//            }
            if (!dataDir.exists())
            {
                dataDir.mkdir();
            }
            parsedPath.append("/"+pathElement);
        }

        if (pictDir == null || dataDir == null)
        {
            throw new FileNotFoundException("Empty request.");
        }

        return new PictureDirInfo(pictDir, dataDir);
    }
}
