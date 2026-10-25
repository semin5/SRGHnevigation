package sarangit.semin5.nev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WorklogPreferencesControllerTest {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired WorklogPreferencesRepository preferences;
    @Autowired AuthService auth;

    @BeforeEach
    void createAccount() {
        preferences.deleteAll();
        Account account = accounts.findById("worklog-test-user").orElseGet(Account::new);
        account.setUsername("worklog-test-user");
        account.setDisplayName("Worklog Test");
        account.setRole("USER");
        account.setPasswordHash("unused-test-hash");
        accounts.saveAndFlush(account);
    }

    @Test
    void settingsArePrivateToTheAuthenticatedAccount() throws Exception {
        mvc.perform(get("/api/settings/worklog"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/settings/worklog").cookie(cookie()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.initialized").value(false));
        mvc.perform(put("/api/settings/worklog").cookie(cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"darkMode\":true,\"legacyMode\":true,\"fontScale\":115,\"floatingPosition\":{\"x\":120,\"y\":250}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.initialized").value(true));
        mvc.perform(get("/api/settings/worklog").cookie(cookie()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.darkMode").value(true))
                .andExpect(jsonPath("$.legacyMode").value(true))
                .andExpect(jsonPath("$.fontScale").value(115))
                .andExpect(jsonPath("$.floatingPosition.x").value(120));
        assertEquals("worklog-test-user", preferences.findAll().getFirst().getUsername());
    }

    @Test
    void rejectsInvalidFontScale() throws Exception {
        mvc.perform(put("/api/settings/worklog").cookie(cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"darkMode\":false,\"legacyMode\":false,\"fontScale\":0,\"floatingPosition\":null}"))
                .andExpect(status().isBadRequest());
    }

    private Cookie cookie() {
        return new Cookie("SRGHIT_AUTH", auth.issue(accounts.findById("worklog-test-user").orElseThrow()));
    }
}
