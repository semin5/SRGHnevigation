package sarangit.semin5.nev;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InsaCredentialCipherTest {
    @Test
    void encryptsWithRandomNonceAndBindsToPortalAccount() {
        InsaCredentialCipher cipher = new InsaCredentialCipher("shared-test-secret");
        String first = cipher.encrypt("insa-password", "99001");
        String second = cipher.encrypt("insa-password", "99001");
        assertNotEquals(first, second);
        assertFalse(first.contains("insa-password"));
        assertEquals("insa-password", cipher.decrypt(first, "99001"));
        assertThrows(IllegalStateException.class, () -> cipher.decrypt(first, "other-user"));
    }
}
