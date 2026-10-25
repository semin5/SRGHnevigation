package sarangit.semin5.nev;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AuthAuditTest {
    @Test
    void auditAssertionIsSignedAndScoped() {
        AuthService auth = new AuthService("test-secret");
        Account account = new Account();
        account.setUsername("99001");
        String token = auth.issueAudit(account);
        assertEquals("99001", auth.verifyAudit(token));
        assertNull(auth.verify(token));
        assertNull(auth.verifyAudit(token + "tampered"));
        assertNull(auth.verifyAudit(auth.issue(account)));
    }
}
