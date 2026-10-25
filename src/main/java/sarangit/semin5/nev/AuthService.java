package sarangit.semin5.nev;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final byte[] secret;
    public AuthService(@Value("${app.auth-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }
    public String issue(Account account) {
        return issue(account, null);
    }
    public String issue(Account account, String designatedHash) {
        long expires = designatedHash == null ? Instant.now().plusSeconds(10 * 60).getEpochSecond() : 0;
        String payload = "v2|" + account.getUsername() + "|" + account.getRole() + "|" + expires
                + "|" + (designatedHash == null ? "" : designatedHash);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encoded + "." + sign(encoded);
    }
    public Identity verify(String token) {
        try {
            String[] tokenParts = token.split("\\.", 2);
            if (tokenParts.length != 2 || !constantTime(sign(tokenParts[0]), tokenParts[1])) return null;
            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[0]), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|", -1);
            if (parts.length != 5 || !"v2".equals(parts[0]) || parts[1].isBlank()) return null;
            long expires = Long.parseLong(parts[3]);
            if (expires == 0 && !parts[4].matches("[0-9a-f]{64}")) return null;
            if (expires != 0 && (expires < Instant.now().getEpochSecond() || !parts[4].isEmpty())) return null;
            return new Identity(parts[1], parts[2], expires == 0 ? parts[4] : null);
        } catch (Exception ignored) { return null; }
    }
    public String issueAudit(Account account) {
        long expires = Instant.now().plusSeconds(60).getEpochSecond();
        String payload = "audit|" + account.getUsername() + "|history|" + expires;
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encoded + "." + sign(encoded);
    }
    public String verifyAudit(String token) {
        try {
            String[] tokenParts = token.split("\\.", 2);
            if (tokenParts.length != 2 || !constantTime(sign(tokenParts[0]), tokenParts[1])) return null;
            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[0]), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|", 4);
            if (parts.length != 4 || !"audit".equals(parts[0]) || !"history".equals(parts[2])
                    || Long.parseLong(parts[3]) < Instant.now().getEpochSecond()) return null;
            return parts[1];
        } catch (Exception ignored) { return null; }
    }
    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private boolean constantTime(String a, String b) {
        return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
    public record Identity(String username, String role, String designatedHash) {}
}
