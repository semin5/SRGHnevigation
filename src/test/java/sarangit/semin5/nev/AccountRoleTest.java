package sarangit.semin5.nev;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AccountRoleTest {
    @Test
    void superAdminCanManageAndIsDistinctFromAdmin() {
        assertTrue(AccountRole.canManage(AccountRole.SUPER_ADMIN));
        assertTrue(AccountRole.canManage("ADMIN"));
        assertFalse(AccountRole.canManage("USER"));
        assertTrue(AccountRole.isSuperAdmin(AccountRole.SUPER_ADMIN));
        assertFalse(AccountRole.isSuperAdmin("ADMIN"));
    }
}
