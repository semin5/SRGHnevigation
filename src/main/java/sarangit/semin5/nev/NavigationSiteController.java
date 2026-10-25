package sarangit.semin5.nev;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class NavigationSiteController {
    private final NavigationSiteRepository sites;
    public NavigationSiteController(NavigationSiteRepository sites) { this.sites = sites; }
    public record SiteInput(String name, String url, String iconKey) {}

    @GetMapping("/api/navigation-sites")
    public List<NavigationSite> list() { return sites.findAllByOrderBySortOrderAscIdAsc(); }

    @PostMapping("/api/admin/navigation-sites")
    @ResponseStatus(HttpStatus.CREATED)
    public NavigationSite create(HttpServletRequest request, @RequestBody SiteInput input) {
        requireAdmin(request);
        NavigationSite site = new NavigationSite();
        site.setSiteCode("custom-" + UUID.randomUUID());
        site.setSortOrder(sites.findAllByOrderBySortOrderAscIdAsc().stream()
                .mapToInt(NavigationSite::getSortOrder).max().orElse(0) + 10);
        apply(site, input);
        return sites.saveAndFlush(site);
    }

    @PutMapping("/api/admin/navigation-sites/{id}")
    public NavigationSite update(HttpServletRequest request, @PathVariable Long id, @RequestBody SiteInput input) {
        requireAdmin(request);
        NavigationSite site = sites.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        apply(site, input);
        return sites.saveAndFlush(site);
    }

    @DeleteMapping("/api/admin/navigation-sites/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(HttpServletRequest request, @PathVariable Long id) {
        requireAdmin(request);
        if (!sites.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        sites.deleteById(id);
    }

    private void apply(NavigationSite site, SiteInput input) {
        String name = clean(input.name());
        String url = clean(input.url());
        if (name.isEmpty() || url.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사이트명과 주소를 입력해 주세요.");
        if (!(url.startsWith("/") || url.startsWith("http://") || url.startsWith("https://")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 주소를 입력해 주세요.");
        site.setName(name);
        site.setUrl(url);
        site.setIconKey(clean(input.iconKey()).isEmpty() ? "link" : clean(input.iconKey()));
    }
    private void requireAdmin(HttpServletRequest request) {
        AuthService.Identity identity = (AuthService.Identity) request.getAttribute("identity");
        if (identity == null || !AccountRole.canManage(identity.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    private String clean(String value) { return value == null ? "" : value.trim(); }
}
