package pictbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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
        String pathInfo = req.getPathInfo();
        if (pathInfo != null)
        {
            buf.append(pathInfo);
        }
        return buf.toString();
    }

}
