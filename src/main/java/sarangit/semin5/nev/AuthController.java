package sarangit.semin5.nev;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountRepository accounts;
    private final AuthService auth;
    private final DesignatedDeviceService devices;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    public AuthController(AccountRepository accounts, AuthService auth, DesignatedDeviceService devices) {
        this.accounts = accounts;
        this.auth = auth;
        this.devices = devices;
    }
    public record LoginInput(String username, String password) {}
    public record UserView(String username, String displayName, String role) {}

    @PostMapping("/login")
    public ResponseEntity<UserView> login(@RequestBody LoginInput input, HttpServletRequest request) {
        Account account = accounts.findById(input.username() == null ? "" : input.username().trim())
                .filter(Account::isActive)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));
        if (!encoder.matches(input.password() == null ? "" : input.password(), account.getPasswordHash()))
            throw new ResponseStatusException(UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie(account,
                        devices.matchingHash(account.getUsername(), cookie(request, "SRGHIT_DEVICE"))).toString())
                .body(view(account));
    }
    @GetMapping("/me")
    public ResponseEntity<UserView> me(HttpServletRequest request) {
        AuthService.Identity identity = (AuthService.Identity) request.getAttribute("identity");
        Account account = accounts.findById(identity.username()).filter(Account::isActive)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        if (identity.designatedHash() != null) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, sessionCookie(account, identity.designatedHash()).toString())
                    .header(HttpHeaders.SET_COOKIE, deviceCookie(cookie(request, "SRGHIT_DEVICE")).toString())
                    .body(view(account));
        }
        return ResponseEntity.ok(view(account));
    }
    @GetMapping("/designated-device")
    public DesignatedDeviceService.Status designatedStatus(HttpServletRequest request) {
        AuthService.Identity identity = (AuthService.Identity) request.getAttribute("identity");
        return devices.status(identity.username(), cookie(request, "SRGHIT_DEVICE"));
    }
    @PostMapping("/designated-device")
    public ResponseEntity<DesignatedDeviceService.Status> designate(HttpServletRequest request) {
        AuthService.Identity identity = (AuthService.Identity) request.getAttribute("identity");
        Account account = accounts.findById(identity.username()).filter(Account::isActive)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        DesignatedDeviceService.Registration registration = devices.designate(identity.username(),
                cookie(request, "SRGHIT_DEVICE"),
                request.getHeader("User-Agent"), request.getRemoteAddr());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deviceCookie(registration.secret()).toString())
                .header(HttpHeaders.SET_COOKIE, sessionCookie(account, registration.hash()).toString())
                .body(devices.status(identity.username(), registration.secret()));
    }
    @DeleteMapping("/designated-device")
    public ResponseEntity<Map<String, Boolean>> removeDesignation(HttpServletRequest request) {
        AuthService.Identity identity = (AuthService.Identity) request.getAttribute("identity");
        Account account = accounts.findById(identity.username()).filter(Account::isActive)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        devices.removeCurrent(identity.username(), cookie(request, "SRGHIT_DEVICE"));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearDeviceCookie().toString())
                .header(HttpHeaders.SET_COOKIE, sessionCookie(account, null).toString())
                .body(Map.of("ok", true));
    }
    public record SelectedDevices(List<UUID> ids) {}
    @PostMapping("/designated-device/remove")
    public ResponseEntity<DesignatedDeviceService.Status> removeSelectedDesignations(
            @RequestBody SelectedDevices selection, HttpServletRequest request) {
        AuthService.Identity identity = (AuthService.Identity) request.getAttribute("identity");
        Account account = accounts.findById(identity.username()).filter(Account::isActive)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        if (selection.ids() == null || selection.ids().isEmpty() || selection.ids().size() > 100)
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        String secret = cookie(request, "SRGHIT_DEVICE");
        boolean currentRemoved = devices.removeSelected(identity.username(), selection.ids(), secret);
        var response = ResponseEntity.ok();
        if (currentRemoved) response.header(HttpHeaders.SET_COOKIE, clearDeviceCookie().toString())
                .header(HttpHeaders.SET_COOKIE, sessionCookie(account, null).toString());
        return response.body(devices.status(identity.username(), currentRemoved ? null : secret));
    }
    public record AuditToken(String token) {}
    @GetMapping("/audit-token")
    public AuditToken auditToken(HttpServletRequest request) {
        AuthService.Identity identity = (AuthService.Identity) request.getAttribute("identity");
        Account account = accounts.findById(identity.username()).filter(Account::isActive)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        return new AuditToken(auth.issueAudit(account));
    }
    @GetMapping("/audit-identity")
    public UserView auditIdentity(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String username = authorization != null && authorization.startsWith("Bearer ")
                ? auth.verifyAudit(authorization.substring(7)) : null;
        if (username == null) throw new ResponseStatusException(UNAUTHORIZED);
        return accounts.findById(username).filter(Account::isActive).map(this::view)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
    }
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Boolean>> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("SRGHIT_AUTH", "").httpOnly(true).sameSite("Lax")
                .path("/").maxAge(Duration.ZERO).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearLegacyCookie().toString())
                .body(Map.of("ok", true));
    }
    private ResponseCookie sessionCookie(Account account, String designatedHash) {
        return ResponseCookie.from("SRGHIT_AUTH", auth.issue(account, designatedHash)).httpOnly(true).sameSite("Lax")
                .path("/").maxAge(designatedHash == null ? Duration.ofMinutes(10) : Duration.ofDays(3650)).build();
    }
    private ResponseCookie deviceCookie(String secret) {
        return ResponseCookie.from("SRGHIT_DEVICE", secret).httpOnly(true).sameSite("Lax")
                .path("/").maxAge(Duration.ofDays(3650)).build();
    }
    private ResponseCookie clearDeviceCookie() {
        return ResponseCookie.from("SRGHIT_DEVICE", "").httpOnly(true).sameSite("Lax")
                .path("/").maxAge(Duration.ZERO).build();
    }
    private String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) for (jakarta.servlet.http.Cookie cookie : request.getCookies())
            if (name.equals(cookie.getName())) return cookie.getValue();
        return null;
    }
    private ResponseCookie clearLegacyCookie() {
        return ResponseCookie.from("SRGHIT_AUTH", "").httpOnly(true).sameSite("Lax")
                .path("/it").maxAge(Duration.ZERO).build();
    }
    private UserView view(Account account) { return new UserView(account.getUsername(), account.getDisplayName(), account.getRole()); }
}
