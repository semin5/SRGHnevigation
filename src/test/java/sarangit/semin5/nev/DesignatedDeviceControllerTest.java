package sarangit.semin5.nev;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DesignatedDeviceControllerTest {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired DesignatedDeviceRepository devices;

    @BeforeEach
    void account() {
        devices.deleteAll(devices.findByUsernameOrderByRegisteredAtDesc("designated-device-test"));
        devices.flush();
        Account account = accounts.findById("designated-device-test").orElseGet(Account::new);
        account.setUsername("designated-device-test");
        account.setDisplayName("Device Test");
        account.setRole("USER");
        account.setActive(true);
        account.setPasswordHash(new BCryptPasswordEncoder().encode("password"));
        accounts.saveAndFlush(account);
    }

    @Test
    void otherPcExpiresAfterTenMinutesWithoutRenewal() throws Exception {
        MockHttpServletResponse login = login(null);
        Cookie auth = cookie(login, "SRGHIT_AUTH");
        String[] parts = payload(auth).split("\\|", -1);
        assertEquals("v2", parts[0]);
        assertTrue(Long.parseLong(parts[3]) <= Instant.now().getEpochSecond() + 600);
        assertTrue(Long.parseLong(parts[3]) > Instant.now().getEpochSecond() + 590);
        assertTrue(login.getHeader(HttpHeaders.SET_COOKIE).contains("Max-Age=600"));
        mvc.perform(get("/api/auth/me").cookie(auth)).andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void onlyRegisteredBrowserGetsRevocableUnlimitedSession() throws Exception {
        Cookie ordinary = cookie(login(null), "SRGHIT_AUTH");
        MockHttpServletResponse activated = mvc.perform(post("/api/auth/designated-device")
                .cookie(ordinary).header("User-Agent", "Test Browser"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.thisDevice").value(true))
                .andReturn().getResponse();
        Cookie auth = cookie(activated, "SRGHIT_AUTH");
        Cookie device = cookie(activated, "SRGHIT_DEVICE");
        assertEquals("0", payload(auth).split("\\|", -1)[3]);
        assertFalse(devices.findByUsernameOrderByRegisteredAtDesc("designated-device-test").getFirst().getTokenHash()
                .contains(device.getValue()));
        mvc.perform(get("/api/auth/me").cookie(auth, device)).andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
        mvc.perform(get("/api/auth/me").cookie(auth)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").cookie(auth, new Cookie("SRGHIT_DEVICE", "wrong")))
                .andExpect(status().isUnauthorized());
        assertNotEquals("0", payload(cookie(login(null), "SRGHIT_AUTH")).split("\\|", -1)[3]);
        mvc.perform(delete("/api/auth/designated-device").cookie(auth, device))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").cookie(auth, device)).andExpect(status().isUnauthorized());
    }

    @Test
    void multipleBrowsersStayDesignatedAndOnlySelectedOneIsRevoked() throws Exception {
        Cookie firstOrdinary = cookie(login(null), "SRGHIT_AUTH");
        MockHttpServletResponse first = mvc.perform(post("/api/auth/designated-device")
                .cookie(firstOrdinary).header("User-Agent", "First Browser"))
                .andExpect(status().isOk()).andReturn().getResponse();
        Cookie firstAuth = cookie(first, "SRGHIT_AUTH");
        Cookie firstDevice = cookie(first, "SRGHIT_DEVICE");

        Cookie secondOrdinary = cookie(login(null), "SRGHIT_AUTH");
        MockHttpServletResponse second = mvc.perform(post("/api/auth/designated-device")
                .cookie(secondOrdinary).header("User-Agent", "Second Browser"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.devices.length()").value(2))
                .andReturn().getResponse();
        Cookie secondAuth = cookie(second, "SRGHIT_AUTH");
        Cookie secondDevice = cookie(second, "SRGHIT_DEVICE");
        var registered = devices.findByUsernameOrderByRegisteredAtDesc("designated-device-test");
        assertEquals(2, registered.size());
        mvc.perform(get("/api/auth/me").cookie(firstAuth, firstDevice)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").cookie(secondAuth, secondDevice)).andExpect(status().isOk());

        var firstRecord = registered.stream().filter(d -> "First Browser".equals(d.getUserAgent()))
                .findFirst().orElseThrow();
        mvc.perform(post("/api/auth/designated-device/remove")
                .cookie(secondAuth, secondDevice).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[\"" + firstRecord.getId() + "\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.thisDevice").value(true))
                .andExpect(jsonPath("$.devices.length()").value(1));
        mvc.perform(get("/api/auth/me").cookie(firstAuth, firstDevice)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").cookie(secondAuth, secondDevice)).andExpect(status().isOk());
    }

    private MockHttpServletResponse login(Cookie device) throws Exception {
        var request = post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"designated-device-test\",\"password\":\"password\"}");
        if (device != null) request.cookie(device);
        return mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse();
    }
    private Cookie cookie(MockHttpServletResponse response, String name) {
        String header = response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(value -> value.startsWith(name + "=")).findFirst().orElseThrow();
        return new Cookie(name, header.substring(name.length() + 1).split(";", 2)[0]);
    }
    private String payload(Cookie cookie) {
        return new String(Base64.getUrlDecoder().decode(cookie.getValue().split("\\.")[0]), StandardCharsets.UTF_8);
    }
}
