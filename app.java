import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.logging.Logger;

public class VulnerableApp extends HttpServlet {

    // =====================================
    // 🔴 1. تخزين بيانات حساسة بشكل غير آمن
    // =====================================
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "admin123"; // Hardcoded secret
    private static final String API_KEY = "sk_test_ABC123";

    private static final Logger logger = Logger.getLogger("AppLogger");

    // =====================================
    // 🔴 2. حقن SQL (SQL Injection)
    // =====================================
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String action = request.getParameter("action");

        if ("login".equals(action)) {
            sqlInjection(request, response);
        } else if ("admin".equals(action)) {
            adminPanel(request, response);
        } else if ("adminDebug".equals(action)) {
            adminDebug(request, response);
        } else if ("ping".equals(action)) {
            commandInjection(request, response);
        } else if ("readFile".equals(action)) {
            pathTraversal(request, response);
        } else if ("transfer".equals(action)) {
            noLogging(request, response);
        }
    }

    // =====================================
    // 🔴 SQL Injection
    // =====================================
    private void sqlInjection(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String username = request.getParameter("user");
        String password = request.getParameter("pass");

        try {
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/appdb",
                    DB_USER,
                    DB_PASS
            );

            Statement stmt = conn.createStatement();

            // ❌ SQL Injection
            String query = "SELECT * FROM users WHERE username = '"
                    + username + "' AND password = '" + password + "'";

            ResultSet rs = stmt.executeQuery(query);

            response.getWriter().println(rs.next() ? "Login OK" : "Login Failed");

        } catch (Exception e) {
            response.getWriter().println("DB Error");
        }
    }

    // =====================================
    // ✅ Endpoint محمي (تحكم صحيح)
    // =====================================
    private void adminPanel(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession();

        if (!"admin".equals(session.getAttribute("role"))) {
            response.sendError(403, "Access Denied");
            return;
        }

        response.getWriter().println("Welcome Admin Panel");
    }

    // =====================================
    // 🔴 3. Broken Access Control
    // =====================================
    private void adminDebug(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // ❌ تجاوز التحقق من الصلاحيات
        response.getWriter().println("Welcome Admin Panel (Bypassed)");
    }

    // =====================================
    // 🔴 4. Command Injection
    // =====================================
    private void commandInjection(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String host = request.getParameter("host");

        // ❌ Command Injection
        Runtime.getRuntime().exec("ping -c 1 " + host);

        response.getWriter().println("Ping executed");
    }

    // =====================================
    // 🔴 5. Path Traversal
    // =====================================
    private void pathTraversal(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String file = request.getParameter("file");

        // ❌ Path Traversal
        BufferedReader reader = new BufferedReader(new FileReader(file));
        response.getWriter().println(reader.readLine());
        reader.close();
    }

    // =====================================
    // 🔴 6. فشل تسجيل ومراقبة الأحداث الأمنية
    // =====================================
    private void noLogging(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String amount = request.getParameter("amount");
        String to = request.getParameter("to");

        // ❌ لا يوجد logging أو monitoring
        response.getWriter().println("Transferred " + amount + " to " + to);
    }
}
