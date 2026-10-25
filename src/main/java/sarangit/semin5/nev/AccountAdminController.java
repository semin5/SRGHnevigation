package sarangit.semin5.nev;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/accounts")
public class AccountAdminController {
    private final AccountRepository accounts;
    private final UserSettingsService settings;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    public AccountAdminController(AccountRepository accounts, UserSettingsService settings) {
        this.accounts = accounts;
        this.settings = settings;
    }
    public record AccountInput(String username, String displayName, String password, String role) {}
    public record PasswordInput(String password) {}
    public record AccountView(String username, String displayName, String role, boolean active, LocalDateTime createdAt) {}

    @GetMapping
    public List<AccountView> list(HttpServletRequest request) {
        requireAdmin(request);
        return accounts.findAllByOrderByUsernameAsc().stream().map(this::view).toList();
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView create(HttpServletRequest request, @RequestBody AccountInput input) {
        requireAdmin(request);
        String username = value(input.username());
        String password = value(input.password());
        if (username.isEmpty() || password.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디와 비밀번호를 입력해 주세요.");
        if (accounts.existsById(username)) throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다.");
        Account account = new Account();
        account.setUsername(username);
        account.setDisplayName(value(input.displayName()).isEmpty() ? username : value(input.displayName()));
        account.setPasswordHash(encoder.encode(password));
        account.setRole("ADMIN".equalsIgnoreCase(input.role()) ? "ADMIN" : "USER");
        Account saved = accounts.saveAndFlush(account);
        settings.initialize(saved.getUsername());
        return view(saved);
    }
    @PutMapping("/{username}/password")
    public AccountView password(HttpServletRequest request, @PathVariable String username, @RequestBody PasswordInput input) {
        requireAdmin(request);
        String password = value(input.password());
        if (password.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "새 비밀번호를 입력해 주세요.");
        Account account = accounts.findById(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        AuthService.Identity identity = (AuthService.Identity) request.getAttribute("identity");
        if (AccountRole.isSuperAdmin(account.getRole()) && !AccountRole.isSuperAdmin(identity.role()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        account.setPasswordHash(encoder.encode(password));
        return view(accounts.saveAndFlush(account));
    }
    private void requireAdmin(HttpServletRequest request) {
        AuthService.Identity identity = (AuthService.Identity) request.getAttribute("identity");
        if (identity == null || !AccountRole.canManage(identity.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    private String value(String value) { return value == null ? "" : value.trim(); }
    private AccountView view(Account account) { return new AccountView(account.getUsername(), account.getDisplayName(), account.getRole(), account.isActive(), account.getCreatedAt()); }
}
