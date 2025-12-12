import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class PasswordUtil {
    // Prefer jBCrypt if available at runtime (no compile-time dependency).
    public static String hash(String plain) {
        try {
            Class<?> bc = Class.forName("org.mindrot.jbcrypt.BCrypt");
            Method gensalt = bc.getMethod("gensalt", int.class);
            String salt = (String) gensalt.invoke(null, 12);
            Method hashpw = bc.getMethod("hashpw", String.class, String.class);
            return (String) hashpw.invoke(null, plain, salt);
        } catch (ClassNotFoundException e) {
            // fallback: simple SHA-256 (less secure). Add jBCrypt to lib/ to use BCrypt.
            return "sha256:" + sha256(plain);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean verify(String plain, String stored) {
        if (stored == null) return false;
        try {
            if (stored.startsWith("$2a$") || stored.startsWith("$2y$") || stored.startsWith("$2b$")) {
                // likely a bcrypt hash; use jBCrypt if available
                Class<?> bc = Class.forName("org.mindrot.jbcrypt.BCrypt");
                Method check = bc.getMethod("checkpw", String.class, String.class);
                return (Boolean) check.invoke(null, plain, stored);
            }
        } catch (ClassNotFoundException e) {
            // jBCrypt not available; fall through to check sha256 prefix
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        if (stored.startsWith("sha256:")) {
            String h = stored.substring("sha256:".length());
            return sha256(plain).equals(h);
        }
        // unknown format: direct compare (legacy plaintext)
        return stored.equals(plain);
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}

