import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.logging.Logger;

public class OWASP_Secure_App {

    private static final Logger logger = Logger.getLogger(OWASP_Secure_App.class.getName());

    // Allowlist للمضيفين المسموحين (تمنع SSRF)
    private static final Set<String> ALLOWED_DB_HOSTS = Set.of(
        "localhost",
        "127.0.0.1"
    );

    // ✅ A2 + SSRF FIX
    public Connection connectDB() throws Exception {

        String dbUrl = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String pass = System.getenv("DB_PASS");

        if (dbUrl == null || user == null || pass == null) {
            throw new SecurityException("Database configuration missing");
        }

        // السماح فقط بـ JDBC
        if (!dbUrl.startsWith("jdbc:mysql://")) {
            throw new SecurityException("Invalid database protocol");
        }

        // تحليل الـ URL للتحقق من الـ host
        URI uri = new URI(dbUrl.replace("jdbc:", ""));
        String host = uri.getHost();

        if (host == null || !ALLOWED_DB_HOSTS.contains(host)) {
            throw new SecurityException("SSRF attempt detected: Invalid DB host");
        }

        return DriverManager.getConnection(dbUrl, user, pass);
    }

    // ✅ A3: SQL Injection
    public void login(HttpServletRequest request) {
        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";

        try (
            Connection conn = connectDB();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, request.getParameter("username"));
            ps.setString(2, request.getParameter("password"));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                HttpSession session = request.getSession();
                session.setAttribute("userId", rs.getInt("id"));
                System.out.println("Login successful");
            }
        } catch (Exception e) {
            logger.severe("Login failed");
        }
    }

    // ✅ A1: Broken Access Control
    public void deleteUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null || !"ADMIN".equals(session.getAttribute("role"))) {
            throw new SecurityException("Access denied");
        }

        String userId = request.getParameter("id");
        System.out.println("User with ID " + userId + " deleted");
    }

    // ✅ A3: Command Injection
    public void executeCommand(HttpServletRequest request) {
        throw new SecurityException("Command execution is disabled");
    }

    // ✅ A5: Path Traversal
    public void readFile(HttpServletRequest request) {
        try {
            Path baseDir = Paths.get("/var/data").toRealPath();
            Path requestedFile = baseDir.resolve(request.getParameter("file")).normalize();

            if (!requestedFile.startsWith(baseDir)) {
                throw new SecurityException("Invalid file access");
            }

            File file = requestedFile.toFile();
            System.out.println("Reading file: " + file.getName());

        } catch (Exception e) {
            logger.warning("File access denied");
        }
    }

    // ✅ A9: Logging بدون بيانات حساسة
    public void processPayment(String cardNumber) {
        logger.info("Payment processed successfully");
    }
}
