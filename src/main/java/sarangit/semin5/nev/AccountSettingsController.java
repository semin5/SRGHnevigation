package sarangit.semin5.nev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/account")
public class AccountSettingsController {
    private final AccountRepository accounts;
    private final InsaAutoLoginRepository credentials;
    private final InsaCredentialCipher cipher;
    private final ObjectMapper json;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public AccountSettingsController(AccountRepository accounts, InsaAutoLoginRepository credentials,
                                     InsaCredentialCipher cipher, ObjectMapper json) {
        this.accounts = accounts;
        this.credentials = credentials;
        this.cipher = cipher;
        this.json = json;
    }

    public record PasswordInput(String newPassword) {}
    public record InsaInput(String username, String password) {}
    public record InsaView(boolean enabled, String username) {}
    public record SessionInput(boolean force) {}
    public record SessionView(String accessToken) {}

    @PutMapping("/password")
    public void changePassword(HttpServletRequest request, @RequestBody PasswordInput input) {
        Account account = accounts.findById(owner(request))
                .filter(Account::isActive).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        String next = input == null ? null : input.newPassword();
        if (next == null || next.length() < 8 || next.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "새 비밀번호는 8자 이상, UTF-8 기준 72바이트 이하여야 합니다.");
        account.setPasswordHash(encoder.encode(next));
        accounts.saveAndFlush(account);
    }

    @GetMapping("/insa")
    public InsaView getInsa(HttpServletRequest request) {
        return credentials.findById(owner(request))
                .map(value -> new InsaView(true, value.getInsaUsername()))
                .orElse(new InsaView(false, ""));
    }

    @PutMapping("/insa")
    public InsaView saveInsa(HttpServletRequest request, @RequestBody InsaInput input) {
        String username = input == null || input.username() == null ? "" : input.username().trim();
        String password = input == null ? null : input.password();
        if (username.isEmpty() || username.length() > 100 || password == null || password.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인사관리 계정과 비밀번호를 입력해 주세요.");
        String owner = owner(request);
        InsaAutoLoginCredential value = credentials.findById(owner).orElseGet(InsaAutoLoginCredential::new);
        value.setPortalUsername(owner);
        value.setInsaUsername(username);
        value.setPasswordCiphertext(cipher.encrypt(password, owner));
        credentials.saveAndFlush(value);
        return new InsaView(true, username);
    }

    @DeleteMapping("/insa")
    public void deleteInsa(HttpServletRequest request) {
        credentials.deleteById(owner(request));
    }

    @PostMapping("/insa/session")
    public SessionView insaSession(HttpServletRequest request, @RequestBody(required = false) SessionInput input) {
        String owner = owner(request);
        InsaAutoLoginCredential value = credentials.findById(owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저장된 인사관리 계정이 없습니다."));
        String password = cipher.decrypt(value.getPasswordCiphertext(), owner);
        String body = "username=" + URLEncoder.encode(value.getInsaUsername(), StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
        HttpRequest upstream = HttpRequest.newBuilder(URI.create("http://127.0.0.1:18104/token"
                        + (input != null && input.force() ? "/force" : "")))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try {
            HttpResponse<String> response = http.send(upstream, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 400)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "저장된 인사관리 계정 또는 비밀번호를 확인해 주세요.");
            if (response.statusCode() == 409)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "다른 인사관리 세션이 이미 사용 중입니다.");
            if (response.statusCode() != 200)
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "인사관리 로그인 서버에 연결하지 못했습니다.");
            JsonNode token = json.readTree(response.body()).path("access_token");
            if (!token.isTextual() || token.asText().isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "인사관리 로그인 응답이 올바르지 않습니다.");
            return new SessionView(token.asText());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "인사관리 로그인 서버에 연결하지 못했습니다.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "인사관리 로그인이 중단되었습니다.");
        }
    }

    private String owner(HttpServletRequest request) {
        return ((AuthService.Identity) request.getAttribute("identity")).username();
    }
}
