package pictbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;

/**
 * Collects utility functions related to servlets.
 *
 * @author Daniel Bratell
 */
public class ServletUtil
{
    /**
     * The name of the attribute to put in the session as login signal.
     */
    public static final String LOGGED_IN_ATTRIBUTE = "loggedin";

    /**
     * Private to prevent instances.
     */
    private ServletUtil()
    {
        // No creation of this object.
    }

    /**
     * Checks if the user is logged in. This is done by looking for a session
     * with the logged.in-object set.
     *
     * @param req The request.
     * @return true if the user is logged in
     *
     * @see #LOGGED_IN_ATTRIBUTE
     */
    public static boolean isLoggedIn(HttpServletRequest req)
    {
        HttpSession session = req.getSession(false);
        if (session == null)
        {
            return false;
        }

        Object loginMarker = session.getAttribute(LOGGED_IN_ATTRIBUTE);
        if (loginMarker == null)
        {
            return false;
        }

        return loginMarker instanceof LoginInfo;
    }

    /**
     * The URL of the current page is divided into several parts by the
     * HttpServletRequest object and this method puts them together again.
     * The URL becomes relative from the root (starts with /...).
     * @param req The HttpServletRequest object.
     * @return A string with the url to the page.
     */
    public static String getCurrentPageUrl(HttpServletRequest req)
    {
        StringBuffer buf = new StringBuffer(req.getContextPath());
        String servletPath = req.getServletPath();
        if (servletPath != null)
        {
            buf.append(servletPath);
        }
        String pathInfo = decodedPathInfo(req);
        if (pathInfo != null)
        {
            buf.append(pathInfo);
        }
        return buf.toString();
    }

    /**
     * We may get the path info as an UTF-8 string decoded as ASCII. :-(
     * @param req The request
     * @return Something that should work better than calling it directly
     */
    public static String decodedPathInfo(HttpServletRequest req)
    {
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.indexOf('Ã') != -1)
        {
            // very unlikely char in a normal string. Try to decode as UTF-8
            try
            {
                byte[] bytes = pathInfo.getBytes("iso-8859-1");
                return new String(bytes, "utf-8");
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
            catch (RuntimeException e)
            {
                e.printStackTrace();
            }
        }

        // Nothing else worked or it was no utf-8 string
        return pathInfo;
    }
}
