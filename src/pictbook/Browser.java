package pictbook;

/**
 * Code taken from USENet and modified.
 *
 * @author jonathan
 * @version 1.0
 *
 * Who to insert this code into your JSP page:
 * <%@ page language="java" import="personal.util.Browser"%>
 * <%
 * Browser client = new Browser(mRequest, mSession);
 * out.println(client.getBrowserInfo());
 * %>
 */

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.util.Hashtable;

/**
 * Code copied from Internet for User Agent sniffing.
 *
 * @author Someone on Usenet but fixed and cleanad up somewhat by
 * Daniel Bratell.
 */
public class Browser extends HttpServlet
{
    private final HttpServletRequest mRequest;
    // --Recycle Bin (10/24/02 11:11 AM): private final HttpSession mSession;

    private String mUserAgent;
    private String mCompany;
    private String mName;
//    private String mVersion;
// --Recycle Bin START (10/28/02 5:32 PM):
//    // --Recycle Bin (10/28/02 5:32 PM): private String mMainVersion;
//    private String mMinorVersion;
// --Recycle Bin STOP (10/28/02 5:32 PM)
    // --Recycle Bin (10/28/02 5:32 PM): private String mOs;
//    private String mLanguage = "en";
    // --Recycle Bin (10/28/02 5:32 PM): private Locale mLocale;


    private Hashtable supportedLanguages;
    /**
     * Company constant for mozilla.org.
     */
    public static final String COMPANY_MOZILLA = "Mozilla";
    /**
     * Company constant for Microsoft.
     */
    public static final String COMPANY_MICROSOFT = "Microsoft";
    /**
     * Company constant for Opera Software.
     */
    public static final String COMPANY_OPERA = "Opera Software";
    /**
     * Company constant for Netscape.
     */
    public static final String COMPANY_NETSCAPE = "Netscape Communications";
    /**
     * Company constant for any other company.
     */
    public static final String COMPANY_UNKNOWN = "unknown";
    /**
     * Browser constant for MSIE.
     */
    public static final String BROWSER_MSIE = "Microsoft Internet Explorer";
    /**
     * Browser constant for Mozilla and other Gecko based browsers including
     * NS6+.
     */
    public static final String BROWSER_MOZILLA = "Mozilla";
    /**
     * Browser constant for NS4 and earlier.
     */
    public static final String BROWSER_NETSCAPE = "Netscape Navigator";
    /**
     * Browser constant for Opera.
     */
    public static final String BROWSER_OPERA = "Opera";

    /**
     * Creates a Browser object that can be used to get information about
     * the user.
     * @param request The request from the user.
     */
    public Browser(HttpServletRequest request/*, HttpSession session */)
    {
        initialize();
        mRequest = request;
//        mSession = session;
        setUserAgent(mRequest.getHeader("User-Agent"));
        setCompany();
        setName();
//        setVersion();
//        setMainVersion();
//        setMinorVersion();
//        setOs();
//        setLanguage();
//        setLocale();
    }

    private void initialize()
    {
        supportedLanguages = new Hashtable(2);
        supportedLanguages.put("en", "");
        supportedLanguages.put("fr", "");
    }

    private void setUserAgent(String httpUserAgent)
    {
        mUserAgent = httpUserAgent.toLowerCase();
    }

    private void setCompany()
    {
        if (mUserAgent.indexOf("opera") != -1)
        {
            mCompany = COMPANY_OPERA;
        }
        else if (mUserAgent.indexOf("msie") != -1)
        {
            mCompany = COMPANY_MICROSOFT;
        }
        else  if (mUserAgent.indexOf("gecko") != -1)
        {
            mCompany = COMPANY_MOZILLA;
        }
        else if (mUserAgent.indexOf("mozilla") != -1)
        {
            mCompany = COMPANY_NETSCAPE;
        }
        else
        {
            mCompany = COMPANY_UNKNOWN;
        }
    }

// --Recycle Bin START (10/28/02 1:08 PM):
//    public String getCompany()
//    {
//        return mCompany;
//    }
// --Recycle Bin STOP (10/28/02 1:08 PM)

    private void setName()
    {
        if (mCompany.equals(COMPANY_MICROSOFT))
        {
            mName = BROWSER_MSIE;
        }
        else if (mCompany.equals(COMPANY_MOZILLA))
        {
            mName = BROWSER_MOZILLA;
        }
        else if (mCompany.equals(COMPANY_NETSCAPE))
        {
            mName = BROWSER_NETSCAPE;
        }
        else if (mCompany.equals(COMPANY_OPERA))
        {
            mName = BROWSER_OPERA;
        }
        else
        {
            mName = "unknown";
        }
    }

    /**
     * Returns the name of the browser. Compare it with one of the browser
     * constants.
     *
     * @return The name.
     */
    public String getName()
    {
        return mName;
    }

//    private void setVersion()
//    {
//        int tmpPos;
//        String tmpString;
//
//        if (mCompany.equals(COMPANY_MICROSOFT))
//        {
//            String str = mUserAgent.substring(mUserAgent.indexOf("msie") + 5);
//            mVersion = str.substring(0, str.indexOf(";"));
//        }
//        else
//        {
//            tmpString = (mUserAgent.substring(tmpPos = (mUserAgent.indexOf("/")) + 1,
//                                                  tmpPos + mUserAgent.indexOf(" "))).trim();
//            mVersion = tmpString.substring(0, tmpString.indexOf(" "));
//        }
//    }


// --Recycle Bin START (10/28/02 1:07 PM):
//    public String getVersion()
//    {
//        return mVersion;
//    }
// --Recycle Bin STOP (10/28/02 1:07 PM)

//    private void setMainVersion()
//    {
//        mMainVersion = mVersion.substring(0, mVersion.indexOf("."));
//    }

// --Recycle Bin START (10/28/02 1:08 PM):
//    public String getMainVersion()
//    {
//        return mMainVersion;
//    }
// --Recycle Bin STOP (10/28/02 1:08 PM)

//    private void setMinorVersion()
//    {
//        mMinorVersion = mVersion.substring(mVersion.indexOf(".") + 1).trim();
//    }

// --Recycle Bin START (10/28/02 1:08 PM):
//    public String getMinorVersion()
//    {
//        return mMinorVersion;
//    }
// --Recycle Bin STOP (10/28/02 1:08 PM)

//    private void setOs()
//    {
//        if (mUserAgent.indexOf("win") > -1)
//        {
//            if (mUserAgent.indexOf("windows 95") > -1 || mUserAgent.indexOf("win95") > -1)
//            {
//                mOs = "Windows 95";
//            }
//            if (mUserAgent.indexOf("windows 98") > -1 || mUserAgent.indexOf("win98") > -1)
//            {
//                mOs = "Windows 98";
//            }
//            if (mUserAgent.indexOf("windows nt") > -1 || mUserAgent.indexOf("winnt") > -1)
//            {
//                mOs = "Windows NT";
//            }
//            if (mUserAgent.indexOf("win16") > -1 || mUserAgent.indexOf("windows 3.") > -1)
//            {
//                mOs = "Windows 3.x";
//            }
//        }
//    }

// --Recycle Bin START (10/28/02 1:08 PM):
//    public String getOs()
//    {
//        return mOs;
//    }
// --Recycle Bin STOP (10/28/02 1:08 PM)

//    private void setLanguage()
//    {
//        String prefLanguage = mRequest.getHeader("Accept-Language");
//        if (prefLanguage != null)
//        {
//            StringTokenizer st = new StringTokenizer(prefLanguage, ",");
//            int elements = st.countTokens();
//            for (int idx = 0; idx < elements; idx++)
//            {
//                String language = st.nextToken();
//                if (supportedLanguages.containsKey(language))
//                {
//                    mLanguage = parseLocale(language);
//                }
//            }
//        }
//    }

//    private static String parseLocale(String language)
//    {
//        StringTokenizer st = new StringTokenizer(language, "-");
//        if (st.countTokens() == 2)
//        {
//            return st.nextToken();
//        }
//        else
//        {
//            return language;
//        }
//    }

// --Recycle Bin START (10/28/02 1:08 PM):
//    public String getLanguage()
//    {
//        return mLanguage;
//    }
// --Recycle Bin STOP (10/28/02 1:08 PM)

//    private void setLocale()
//    {
//        mLocale = new Locale(mLanguage, "");
//    }

// --Recycle Bin START (10/28/02 1:08 PM):
//    public Locale getLocale()
//    {
//        return mLocale;
//    }
// --Recycle Bin STOP (10/28/02 1:08 PM)

// --Recycle Bin START (10/28/02 1:07 PM):
//    public String getProtocol()
//    {
//        return mRequest.getProtocol();
//    }
// --Recycle Bin STOP (10/28/02 1:07 PM)

// --Recycle Bin START (10/28/02 1:08 PM):
//    public String getContentType()
//    {
//        return mRequest.getContentType();
//    }
// --Recycle Bin STOP (10/28/02 1:08 PM)

// --Recycle Bin START (10/28/02 1:07 PM):
//    public String getCharacterEncoding()
//    {
//        return mRequest.getCharacterEncoding();
//    }
// --Recycle Bin STOP (10/28/02 1:07 PM)

// --Recycle Bin START (10/28/02 1:07 PM):
//    public String getRemoteAddr()
//    {
//        return mRequest.getRemoteAddr();
//    }
// --Recycle Bin STOP (10/28/02 1:07 PM)

// --Recycle Bin START (10/28/02 1:07 PM):
//    public String getRemoteHost()
//    {
//        return mRequest.getRemoteHost();
//    }
// --Recycle Bin STOP (10/28/02 1:07 PM)

// --Recycle Bin START (10/28/02 1:07 PM):
//    public String getScheme()
//    {
//        return mRequest.getScheme();
//    }
// --Recycle Bin STOP (10/28/02 1:07 PM)

// --Recycle Bin START (10/28/02 1:07 PM):
//    public String getServerName()
//    {
//        return mRequest.getServerName();
//    }
// --Recycle Bin STOP (10/28/02 1:07 PM)

// --Recycle Bin START (10/28/02 1:07 PM):
//    public int getServerPort()
//    {
//        return mRequest.getServerPort();
//    }
// --Recycle Bin STOP (10/28/02 1:07 PM)

// --Recycle Bin START (10/28/02 1:07 PM):
//    public String getPathInfo()
//    {
//        return mRequest.getPathInfo();
//    }
// --Recycle Bin STOP (10/28/02 1:07 PM)

// --Recycle Bin START (10/28/02 1:07 PM):
//    public boolean isSecure()
//    {
//        return mRequest.isSecure();
//    }
// --Recycle Bin STOP (10/28/02 1:07 PM)

// --Recycle Bin START (10/28/02 1:07 PM):
//    public String getUserAgent()
//    {
//        // mUserAgent is converted to lower case
//        return mRequest.getHeader("User-Agent");
//    }
// --Recycle Bin STOP (10/28/02 1:07 PM)

// --Recycle Bin START (10/28/02 1:05 PM):
//    public String getBrowserInfo()
//    {
//        StringBuffer out = new StringBuffer();
//        out.append("<table border='0' width='70%' bgcolor='#EEEEEE'>");
//        out.append("<tr><td><CENTER><H1>Browser Info</td></tr><tr><td><CENTER>");
//        out.append("<table border='1' width='80%' bgcolor='#FBFBE4'>");
//        out.append("<tr><td align='right'><h5> Os : </h5></td><td><h5><b>" + getOs() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> Company : </h5></td><td><h5><b>" + getCompany() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> Language : </h5></td><td><h5><b>" + getLanguage() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> User-Agent : </h5></td><td><h5><b>" + getUserAgent() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> Locale : </h5></td><td><h5><b>" + getLocale() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> Name : </h5></td><td><h5><b>" + getName() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> Version : </h5></td><td><h5><b>" + getVersion() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> MainVersion : </h5></td><td><h5><b>" + getMainVersion() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> MinorVersion : </h5></td><td><h5><b>" + getMinorVersion() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> ServerName : </h5></td><td><h5><b>" + getServerName() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> ServerPort : </h5></td><td><h5><b>" + getServerPort() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> Protocol : </h5></td><td><h5><b>" + getProtocol() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> ContentType : </h5></td><td><h5><b>" + getContentType() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> Charactappending : </h5></td><td><h5><b>" + getCharacterEncoding() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> RemoteAddr : </h5></td><td><h5><b>" + getRemoteAddr() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> RemoteHost : </h5></td><td><h5><b>" + getRemoteHost() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> Scheme : </h5></td><td><h5><b>" + getScheme() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> ServerName : </h5></td><td><h5><b>" + getServerName() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> ServerPort : </h5></td><td><h5><b>" + getServerPort() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> Secure : </h5></td><td><h5><b>" + isSecure() + "</b></h5></td></tr>");
//        out.append("<tr><td align='right'><h5> PathInfo : </h5></td><td><h5><b>" + getPathInfo() + "</b></h5></td></tr>");
//        out.append("</table>");
//        out.append("</td></tr>");
//        out.append("</table>");
//        return out.toString();
//    }
// --Recycle Bin STOP (10/28/02 1:05 PM)
}