/*
 * Created by IntelliJ IDEA.
 * User: Bratell
 * Date: Oct 16, 2002
 * Time: 4:57:53 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package pictbook;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Writer;

/**
 * Marks the user logged in, which means that this must be
 * protected in some way.
 *
 * @author Daniel Bratell
 */
public class LoginServlet  extends HttpServlet
{

    /**
     * Called by the servlet engine when this class is started to be used.
     * @throws ServletException if the init fails.
     */
//    public void init() throws ServletException
//    {
//        super.init();
//    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException
    {
        HttpSession session = req.getSession();
        session.setAttribute(ServletUtil.LOGGED_IN_ATTRIBUTE, new LoginInfo());

        String newUrl = req.getParameter("url");
        if (newUrl != null)
        {
            res.sendRedirect(newUrl);
        }
        else
        {
            res.setContentType("text/html;charset=UTF-8");
            Writer out = res.getWriter();
            String stylesheet = Util.addToDirUrl(req.getContextPath(), "pictbook.css");
            Util.writeHTMLDocHeader(out, "Inloggad i PictBook", stylesheet, null);
            out.write("<h1>Du är nu inloggad</h1>");
            Util.writeHTMLDocFooter(out);
        }
    }
}
