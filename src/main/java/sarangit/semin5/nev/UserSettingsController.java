package sarangit.semin5.nev;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserSettingsController {
    private final UserSettingsService settings;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    public record SiteCheck(String normalizedUrl, boolean reachable, boolean embeddable, String message) {}

    public UserSettingsController(UserSettingsService settings) { this.settings = settings; }

    @GetMapping("/api/settings")
    public UserSettingsService.View get(HttpServletRequest request) {
        return settings.getOrCreate(identity(request).username());
    }

    @PutMapping("/api/settings")
    public UserSettingsService.View save(HttpServletRequest request, @RequestBody UserSettingsService.View input) {
        return settings.save(identity(request).username(), input);
    }

    @GetMapping("/api/settings/site-check")
    public SiteCheck checkSite(HttpServletRequest request, @org.springframework.web.bind.annotation.RequestParam String url) {
        identity(request);
        String normalized = normalize(url);
        if (normalized.isEmpty()) return new SiteCheck("", false, false, "주소를 입력해 주세요.");
        if (normalized.startsWith("/")) return new SiteCheck(normalized, true, true, "화면 안에서 열 수 있습니다.");
        try {
            URI uri = URI.create(normalized);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null)
                return new SiteCheck(normalized, false, false, "올바른 주소를 입력해 주세요.");
            HttpRequest head = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(6)).method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<Void> response = httpClient.send(head, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 405 || response.statusCode() == 501) {
                HttpRequest get = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(6)).GET().build();
                response = httpClient.send(get, HttpResponse.BodyHandlers.discarding());
            }
            boolean reachable = response.statusCode() >= 200 && response.statusCode() < 400;
            String xFrame = response.headers().firstValue("x-frame-options").orElse("").toLowerCase(Locale.ROOT);
            String csp = response.headers().firstValue("content-security-policy").orElse("").toLowerCase(Locale.ROOT);
            boolean samePortal = "sarangmc.kr".equalsIgnoreCase(uri.getHost());
            boolean frameBlocked = xFrame.contains("deny") || (xFrame.contains("sameorigin") && !samePortal)
                    || csp.contains("frame-ancestors 'none'")
                    || (csp.contains("frame-ancestors 'self'") && !samePortal);
            boolean embeddable = reachable && !frameBlocked;
            return new SiteCheck(normalized, reachable, embeddable,
                    embeddable ? "화면 안에서 열 수 있습니다." : "화면 안에서 열 수 없는 주소입니다. 외부 사이트를 체크해 주세요.");
        } catch (Exception exception) {
            return new SiteCheck(normalized, false, false, "주소에 연결할 수 없습니다. 외부 사이트를 체크해 주세요.");
        }
    }

    private AuthService.Identity identity(HttpServletRequest request) {
        return (AuthService.Identity) request.getAttribute("identity");
    }

    private String normalize(String value) {
        String url = value == null ? "" : value.trim();
        if (url.isEmpty() || url.startsWith("/")) return url;
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        return "https://" + url;
    }
}
