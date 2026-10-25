package sarangit.semin5.nev;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthInterceptorTest {
    private final AuthService auth = new AuthService("test-secret");
    private final AuthInterceptor interceptor = new AuthInterceptor(auth,
            org.mockito.Mockito.mock(DesignatedDeviceService.class));

    @Test
    void acceptsValidCookieEvenWhenStaleCookieComesLast() throws Exception {
        assertAuthenticated(true);
    }

    @Test
    void acceptsValidCookieEvenWhenStaleCookieComesFirst() throws Exception {
        assertAuthenticated(false);
    }

    @Test
    void rejectsRequestsWithoutValidCookie() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("SRGHIT_AUTH", "stale-invalid"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }

    private void assertAuthenticated(boolean validFirst) throws Exception {
        Account account = new Account();
        account.setUsername("99001");
        account.setRole("USER");
        Cookie valid = new Cookie("SRGHIT_AUTH", auth.issue(account));
        Cookie stale = new Cookie("SRGHIT_AUTH", "stale-invalid");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(validFirst ? new Cookie[]{valid, stale} : new Cookie[]{stale, valid});
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals("99001", ((AuthService.Identity) request.getAttribute("identity")).username());
    }
}
