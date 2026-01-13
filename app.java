import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class OWASP_Secure_App {

    private static final Logger logger = Logger.getLogger(OWASP_Secure_App.class.getName());

    // ✅ A2: Cryptographic Failures (إزالة البيانات الصلبة)
    public Connection connectDB() throws Exception {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String pass = System.getenv("DB_PASS");

        return DriverManager.getConnection(url, user, pass);
    }

    // ✅ A3: Injection (SQL Injection) باستخدام PreparedStatement
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

    // ✅ A1: Broken Access Control (التحقق من الصلاحيات)
    public void deleteUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null || !"ADMIN".equals(session.getAttribute("role"))) {
            throw new SecurityException("Access denied");
        }

        String userId = request.getParameter("id");
        System.out.println("User with ID " + userId + " deleted");
    }

    // ✅ A3: Command Injection (إزالة التنفيذ المباشر)
    public void executeCommand(HttpServletRequest request) {
        throw new SecurityException("Command execution is disabled");
    }

    // ✅ A5: Path Traversal (تقييد المسار)
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

    // ✅ A9: Security Logging and Monitoring
    public void processPayment(String cardNumber) {
        // تسجيل بدون بيانات حساسة
        logger.info("Payment processed successfully");
    }
}
