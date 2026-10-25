package sarangit.semin5.nev;

final class AccountRole {
    static final String SUPER_ADMIN = "SUPER_ADMIN";

    private AccountRole() {}

    static boolean canManage(String role) {
        return "ADMIN".equals(role) || SUPER_ADMIN.equals(role);
    }

    static boolean isSuperAdmin(String role) {
        return SUPER_ADMIN.equals(role);
    }
}
