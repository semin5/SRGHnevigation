package sarangit.semin5.nev;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {
    private final AccountRepository accounts;
    public AdminInitializer(AccountRepository accounts) { this.accounts = accounts; }
    @Override
    public void run(String... args) {
        if (accounts.existsById("99001")) return;
        Account admin = new Account();
        admin.setUsername("99001");
        admin.setDisplayName("관리자");
        admin.setRole("ADMIN");
        admin.setPasswordHash(new BCryptPasswordEncoder().encode("1"));
        accounts.saveAndFlush(admin);
    }
}
