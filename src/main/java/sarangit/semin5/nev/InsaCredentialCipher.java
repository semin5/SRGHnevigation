package sarangit.semin5.nev;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InsaCredentialCipher {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SecretKeySpec key;

    public InsaCredentialCipher(@Value("${app.auth-secret}") String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            key = new SecretKeySpec(mac.doFinal("insa-auto-login-v1".getBytes(StandardCharsets.UTF_8)), "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("자동 로그인 암호화 키를 만들지 못했습니다.", ex);
        }
    }

    public String encrypt(String value, String owner) {
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            cipher.updateAAD(owner.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            throw new IllegalStateException("자동 로그인 정보를 암호화하지 못했습니다.", ex);
        }
    }

    public String decrypt(String value, String owner) {
        try {
            byte[] combined = Base64.getDecoder().decode(value);
            byte[] iv = new byte[12];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            cipher.updateAAD(owner.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(combined, iv.length, combined.length - iv.length), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("저장된 자동 로그인 정보를 읽지 못했습니다.", ex);
        }
    }
}
