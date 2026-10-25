package sarangit.semin5.nev;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    private final AuthService auth;
    private final DesignatedDeviceService devices;
    public AuthInterceptor(AuthService auth, DesignatedDeviceService devices) {
        this.auth = auth;
        this.devices = devices;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        AuthService.Identity identity = null;
        int authCookies = 0;
        String deviceSecret = null;
        if (request.getCookies() != null) for (Cookie cookie : request.getCookies())
            if ("SRGHIT_DEVICE".equals(cookie.getName())) deviceSecret = cookie.getValue();
        if (request.getCookies() != null) for (Cookie cookie : request.getCookies()) {
            if (!"SRGHIT_AUTH".equals(cookie.getName())) continue;
            authCookies++;
            identity = auth.verify(cookie.getValue());
            if (identity != null && identity.designatedHash() != null
                    && !identity.designatedHash().equals(devices.matchingHash(identity.username(), deviceSecret)))
                identity = null;
            if (identity != null) break;
        }
        if (identity == null) {
            if (!request.getRequestURI().endsWith("/api/auth/me"))
                log.warn("Portal authentication rejected: path={}, client={}, authCookieCount={}",
                        request.getRequestURI(), request.getHeader("X-Real-IP"), authCookies);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        request.setAttribute("identity", identity);
        return true;
    }
}
