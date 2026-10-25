package sarangit.semin5.nev;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AccountSettingsControllerTest {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired InsaAutoLoginRepository credentials;
    @Autowired AuthService auth;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void createAccount() {
        credentials.deleteAll();
        Account account = accounts.findById("test-portal-user").orElseGet(Account::new);
        account.setUsername("test-portal-user");
        account.setDisplayName("Test");
        account.setRole("USER");
        account.setActive(true);
        account.setPasswordHash(encoder.encode("old-password"));
        accounts.saveAndFlush(account);
    }

    @Test
    void changesOwnPasswordWithoutCurrentPassword() throws Exception {
        mvc.perform(put("/api/account/password").cookie(cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/account/password").cookie(cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\":\"new-password\"}"))
                .andExpect(status().isOk());
        assertTrue(encoder.matches("new-password", accounts.findById("test-portal-user").orElseThrow().getPasswordHash()));
    }

    @Test
    void savesEncryptedInsaCredentialWithoutReturningPassword() throws Exception {
        mvc.perform(put("/api/account/insa").cookie(cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"insa-user\",\"password\":\"insa-secret\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.username").value("insa-user"));
        InsaAutoLoginCredential stored = credentials.findById("test-portal-user").orElseThrow();
        assertFalse(stored.getPasswordCiphertext().contains("insa-secret"));
        mvc.perform(get("/api/account/insa").cookie(cookie()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("insa-user"))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(delete("/api/account/insa").cookie(cookie())).andExpect(status().isOk());
        assertFalse(credentials.existsById("test-portal-user"));
    }

    private Cookie cookie() {
        return new Cookie("SRGHIT_AUTH", auth.issue(accounts.findById("test-portal-user").orElseThrow()));
    }
}
