package pictbook.storage;

import pictbook.Util;

import java.io.File;
import java.util.Date;

/**
 * Information about an image. Name, description and date. This class has no
 * real logic.
 *
 * @author Daniel Bratell
 */
public class PictureInfo implements Comparable
{
    private final String mDescription;
    private final Date mDate;
    private final File mLocalFile;
    private boolean mHidden;
    private int mOrder;


    /**
     * Creates a picture info object.
     *
     * @param file The original file.
     * @param description The description of the image. This should be
     * displayed along with the image.
     * @param date The date associated with the picture.
     */
    PictureInfo(File file, String description, Date date)
    {
        mLocalFile = file;
        mDescription = description;
        mDate = date;
        mOrder = UNDEFINED_ORDER;
    }

    /**
     * The order is not set. Date will be used to sort.
     * Undefined order must be larger than any legal orders.
     */
    public final static int UNDEFINED_ORDER = Integer.MAX_VALUE;

    /**
     * Checks if the image should be hidden.
     * @return True if the image should be hidden.
     */
    public boolean isHidden()
    {
        return mHidden;
    }

    /**
     * Set if the image is hidden or not.
     * @param hidden If the image should be hidden.
     */
    public void setHidden(boolean hidden)
    {
        mHidden = hidden;
    }

    /**
     * The order of the image. It can be UNDEFINED_ORDER if no order is set
     * in which case the date should be used.
     * @return The order.
     */
    public int getOrder()
    {
        return mOrder;
    }

    /**
     * Set it to UNDEFINED_ORDER to remove the order number.
     * @param order A number specifying the order or UNDEFINED_ORDER if dates
     * should be used.
     */
    public void setOrder(int order)
    {
        mOrder = order;
    }

    /**
     * The file that this object is connected to.
     * @return The file object.
     */
    public File getLocalFile()
    {
        return mLocalFile;
    }

// --Recycle Bin START (10/28/02 5:33 PM):
//    public void setLocalFile(File localFile)
//    {
//        mLocalFile = localFile;
//    }
// --Recycle Bin STOP (10/28/02 5:33 PM)

    /**
     * The name of the image.
     * @return Returns the name.
     */
    public String getName()
    {
        return mLocalFile.getName();
    }

// --Recycle Bin START (10/28/02 5:33 PM):
//    public void setName(String name)
//    {
//        mName = name;
//    }
// --Recycle Bin STOP (10/28/02 5:33 PM)

    /**
     * The description of the picture.
     *
     * @return The description.
     */
    public String getDescription()
    {
        return mDescription;
    }

// --Recycle Bin START (10/28/02 5:33 PM):
//    public void setDescription(String description)
//    {
//        mDescription = description;
//    }
// --Recycle Bin STOP (10/28/02 5:33 PM)

    /**
     * The date of the object.
     * @return the Date
     */
    public Date getDate()
    {
        return mDate;
    }

// --Recycle Bin START (10/28/02 5:33 PM):
//    public void setDate(Date date)
//    {
//        mDate = date;
//    }
// --Recycle Bin STOP (10/28/02 5:33 PM)

    /**
     * Uses |this| to make the code more readable. First compares mOrder
     * and if that is undefined, compares dates, and then the name.
     * @param o - The object to compare to.
     * @return - see the Comparable interface.
     * @see Comparable
     */
    public int compareTo(Object o)
    {
        if (this.equals(o))
        {
            return 0;
        }

        if (!(o instanceof PictureInfo))
            return -1;

        PictureInfo other = (PictureInfo)o;
        int order;

        order = this.mOrder - other.mOrder;
        if (order == 0)
        {
            // Equal order - compare date
            order = this.mDate.compareTo(other.mDate);
            if (order == 0)
            {
                // Equal order and date - compare name
                order = this.getName().compareTo(other.getName());
            }
        }
        return order;
    }

    /**
     * If the "Image" really is a movie.
     *
     * @return True if it is a movie.
     */
    public boolean isMovie()
    {
        String extension = Util.getExtension(mLocalFile);
        return Util.isMoveExtension(extension);
    }

    /**
     * The size of the image file.
     * @return Returns the size of the Image file or 0 if the file has
     * disappeared.
     */
    public long getFileSize()
    {
        return mLocalFile.length();
    }
}
