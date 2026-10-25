package sarangit.semin5.nev;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/settings/worklog")
public class WorklogPreferencesController {
    private final WorklogPreferencesRepository preferences;

    public WorklogPreferencesController(WorklogPreferencesRepository preferences) {
        this.preferences = preferences;
    }

    public record Position(int x, int y) {}
    public record View(boolean initialized, boolean darkMode, boolean legacyMode,
                       int fontScale, Position floatingPosition) {}

    @GetMapping
    public View get(HttpServletRequest request) {
        return preferences.findById(username(request)).map(this::view)
                .orElse(new View(false, false, false, 100, null));
    }

    @PutMapping
    @Transactional
    public View save(HttpServletRequest request, @RequestBody View input) {
        if (input.fontScale() < 100 || input.fontScale() > 140
                || input.floatingPosition() != null && (input.floatingPosition().x() < 0
                    || input.floatingPosition().y() < 0 || input.floatingPosition().x() > 10000
                    || input.floatingPosition().y() > 10000)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid worklog preferences");
        }
        String username = username(request);
        WorklogPreferences item = preferences.findById(username).orElseGet(WorklogPreferences::new);
        item.setUsername(username);
        item.setDarkMode(input.darkMode());
        item.setLegacyMode(input.legacyMode());
        item.setFontScale(input.fontScale());
        item.setFloatingX(input.floatingPosition() == null ? null : input.floatingPosition().x());
        item.setFloatingY(input.floatingPosition() == null ? null : input.floatingPosition().y());
        return view(preferences.save(item));
    }

    private String username(HttpServletRequest request) {
        return ((AuthService.Identity) request.getAttribute("identity")).username();
    }

    private View view(WorklogPreferences item) {
        Position position = item.getFloatingX() == null || item.getFloatingY() == null
                ? null : new Position(item.getFloatingX(), item.getFloatingY());
        return new View(true, item.isDarkMode(), item.isLegacyMode(), item.getFontScale(), position);
    }
}
