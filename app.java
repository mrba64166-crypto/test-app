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

    // Allowlist لمنع SSRF
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

        if (!dbUrl.startsWith("jdbc:mysql://")) {
            throw new SecurityException("Invalid database protocol");
        }

        URI uri = new URI(dbUrl.replace("jdbc:", ""));
        String host = uri.getHost();

        if (host == null || !ALLOWED_DB_HOSTS.contains(host)) {
            throw new SecurityException("Invalid database host");
        }

        return DriverManager.getConnection(dbUrl, user, pass);
    }

    // ✅ A3: SQL Injection
    public void login(HttpServletRequest request) {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || password == null) {
            throw new SecurityException("Invalid credentials");
        }

        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";

        try (
            Connection conn = connectDB();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                HttpSession session = request.getSession(true);
                session.setAttribute("userId", rs.getInt("id"));
                session.setAttribute("role", "USER");
                logger.info("Login successful");
            }
        } catch (Exception e) {
            logger.warning("Login attempt failed");
        }
    }

    // ✅ A1: Broken Access Control + IDOR FIX
    public void deleteUser(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null || !"ADMIN".equals(session.getAttribute("role"))) {
            throw new SecurityException("Access denied");
        }

        String userIdParam = request.getParameter("id");

        if (userIdParam == null || !userIdParam.matches("\\d+")) {
            throw new SecurityException("Invalid user ID");
        }

        int userId = Integer.parseInt(userIdParam);

        // حذف آمن (محاكاة)
        logger.info("User deletion executed for ID: " + userId);
    }

    // ✅ A3: Command Injection (ممنوع نهائيًا)
    public void executeCommand(HttpServletRequest request) {
        throw new SecurityException("Command execution disabled");
    }

    // ✅ A5: Path Traversal
    public void readFile(HttpServletRequest request) {

        try {
            String filename = request.getParameter("file");

            if (filename == null) {
                throw new SecurityException("Invalid file request");
            }

            Path baseDir = Paths.get("/var/data").toRealPath();
            Path requestedPath = baseDir.resolve(filename).normalize();

            if (!requestedPath.startsWith(baseDir)) {
                throw new SecurityException("Path traversal attempt detected");
            }

            File file = requestedPath.toFile();
            logger.info("File accessed: " + file.getName());

        } catch (Exception e) {
            logger.warning("File access denied");
        }
    }

    // ✅ A9: Security Logging & Monitoring
    public void processPayment() {
        logger.info("Payment processed successfully");
    }
}
