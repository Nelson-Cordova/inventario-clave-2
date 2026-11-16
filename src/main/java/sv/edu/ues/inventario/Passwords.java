package sv.edu.ues.inventario;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.Base64;

public final class Passwords {
    private Passwords() {}
    public static String salt() {
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    public static String hash(char[] password, String salt) {
        PBEKeySpec spec = null;
        try {
            spec = new PBEKeySpec(password, Base64.getDecoder().decode(salt), 600000, 256);
            return Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded());
        } catch (Exception e) { throw new InventoryException("No se pudo verificar la contraseña.", e); }
        finally { if (spec != null) spec.clearPassword(); }
    }
    public static boolean verify(char[] password, String salt, String expected) {
        return MessageDigest.isEqual(hash(password, salt).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}

