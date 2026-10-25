package sarangit.semin5.nev;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSettingsService {
    public record Site(String id, String siteCode, String name, String url, String iconKey, int sortOrder, boolean external) {}
    public record Group(String id, String name, List<String> siteCodes, boolean collapsed) {}
    public record View(String theme, String workPath, List<Site> sites, List<String> order,
                       Integer iconSize, Integer iconGap, String iconColor, Map<String, String> iconColors,
                       List<String> hiddenSiteCodes, Boolean navCollapsedDefault, List<Group> groups,
                       Boolean showExternalGroup) {}

    private final UserSettingsRepository repository;
    private final NavigationSiteRepository navigationSites;
    private final ObjectMapper objectMapper;
    private static final Set<String> LEGACY_DEFAULT_CODES = Set.of("work", "insa", "ip", "management", "register");

    public UserSettingsService(UserSettingsRepository repository, NavigationSiteRepository navigationSites,
                               ObjectMapper objectMapper) {
        this.repository = repository;
        this.navigationSites = navigationSites;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public View getOrCreate(String username) {
        return view(repository.findById(username).orElseGet(() -> repository.save(defaults(username))));
    }

    @Transactional
    public View save(String username, View input) {
        UserSettings settings = repository.findById(username).orElseGet(() -> defaults(username));
        List<Site> shared = sharedSites();
        List<Site> sites = mergeSites(shared, input.sites() == null ? readSites(settings.getSitesJson()) : input.sites());
        List<String> available = sites.stream().map(Site::siteCode).toList();
        List<Group> groups = validGroups(
                input.groups() == null ? readGroups(settings.getGroupsJson()) : input.groups(), available);
        List<String> colorKeys = new ArrayList<>(available);
        groups.stream().map(Group::id).forEach(colorKeys::add);
        List<String> order = new ArrayList<>();
        if (input.order() != null) input.order().stream().filter(available::contains).filter(key -> !order.contains(key)).forEach(order::add);
        insertMissingSites(order, available);
        settings.setTheme("dark".equals(input.theme()) ? "dark" : "light");
        settings.setWorkPath("/worklog");
        settings.setSitesJson(write(sitesToStore(sites, shared)));
        settings.setOrderJson(write(order));
        settings.setIconSize(clamp(input.iconSize(), 8, 30, 18));
        settings.setIconGap(clamp(input.iconGap(), 0, 20, 5));
        settings.setNavCollapsedDefault(input.navCollapsedDefault() == null
                ? Boolean.TRUE.equals(settings.getNavCollapsedDefault()) : input.navCollapsedDefault());
        settings.setIconColor(color(input.iconColor()));
        settings.setIconColorsJson(write(sanitizeColors(input.iconColors(), colorKeys)));
        settings.setHiddenSiteCodesJson(write(validHiddenCodes(
                input.hiddenSiteCodes() == null ? readHiddenCodes(settings.getHiddenSiteCodesJson()) : input.hiddenSiteCodes(),
                available)));
        settings.setGroupsJson(write(groups));
        settings.setShowExternalGroup(input.showExternalGroup() == null
                ? !Boolean.FALSE.equals(settings.getShowExternalGroup()) : input.showExternalGroup());
        return view(repository.saveAndFlush(settings));
    }

    @Transactional
    public void initialize(String username) {
        if (!repository.existsById(username)) repository.save(defaults(username));
    }

    private UserSettings defaults(String username) {
        UserSettings settings = new UserSettings();
        settings.setUsername(username);
        settings.setTheme("light");
        settings.setWorkPath("/worklog");
        List<Site> sites = sharedSites();
        settings.setSitesJson("[]");
        settings.setOrderJson(write(sites.stream().map(Site::siteCode).toList()));
        settings.setIconSize(18);
        settings.setIconGap(5);
        settings.setNavCollapsedDefault(false);
        settings.setIconColor("#2563eb");
        settings.setIconColorsJson(write(defaultColors(sites.stream().map(Site::siteCode).toList())));
        settings.setHiddenSiteCodesJson("[]");
        settings.setGroupsJson("[]");
        settings.setShowExternalGroup(true);
        return settings;
    }

    private View view(UserSettings settings) {
        try {
            List<Site> sites = mergeSites(sharedSites(), readSites(settings.getSitesJson()));
            List<String> savedOrder = objectMapper.readValue(settings.getOrderJson(), new TypeReference<>() {});
            List<String> available = sites.stream().map(Site::siteCode).toList();
            List<Group> groups = validGroups(readGroups(settings.getGroupsJson()), available);
            List<String> colorKeys = new ArrayList<>(available);
            groups.stream().map(Group::id).forEach(colorKeys::add);
            List<String> order = new ArrayList<>();
            if (savedOrder != null) savedOrder.stream().filter(available::contains)
                    .filter(key -> !order.contains(key)).forEach(order::add);
            insertMissingSites(order, available);
            Map<String, String> colors = settings.getIconColorsJson() == null
                    ? defaultColors(sites.stream().map(Site::siteCode).toList())
                    : objectMapper.readValue(settings.getIconColorsJson(), new TypeReference<>() {});
            return new View(settings.getTheme(), "/worklog", sites, order,
                    clamp(settings.getIconSize(), 8, 30, 18), clamp(settings.getIconGap(), 0, 20, 5),
                    color(settings.getIconColor()), sanitizeColors(colors, colorKeys),
                    validHiddenCodes(readHiddenCodes(settings.getHiddenSiteCodesJson()), available),
                    Boolean.TRUE.equals(settings.getNavCollapsedDefault()),
                    groups, !Boolean.FALSE.equals(settings.getShowExternalGroup()));
        } catch (Exception exception) {
            List<Site> sites = sharedSites();
            List<String> keys = sites.stream().map(Site::siteCode).toList();
            return new View("light", "/worklog", sites, keys, 18, 5, "#2563eb", defaultColors(keys), List.of(), false, List.of(), true);
        }
    }

    private List<Site> sanitizeSites(List<Site> input) {
        List<Site> result = new ArrayList<>();
        int fallbackOrder = 10;
        for (Site site : input) {
            String code = clean(site.siteCode());
            String name = clean(site.name());
            String url = normalizeUrl(site.url());
            if (code.isEmpty() || name.isEmpty() || url.isEmpty()) continue;
            if (result.stream().anyMatch(saved -> saved.siteCode().equals(code))) continue;
            result.add(new Site(clean(site.id()).isEmpty() ? code : clean(site.id()), code, name, url,
                    clean(site.iconKey()).isEmpty() ? "link" : clean(site.iconKey()),
                    site.sortOrder() > 0 ? site.sortOrder() : fallbackOrder, site.external()));
            fallbackOrder += 10;
        }
        return result;
    }

    private List<Site> readSites(String json) {
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception exception) { throw new IllegalStateException("Invalid saved navigation sites", exception); }
    }

    private List<String> readHiddenCodes(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception exception) { throw new IllegalStateException("Invalid hidden navigation sites", exception); }
    }

    private List<Group> readGroups(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception exception) { throw new IllegalStateException("Invalid saved navigation groups", exception); }
    }

    private List<Group> validGroups(List<Group> requested, List<String> available) {
        if (requested == null) return List.of();
        List<Group> result = new ArrayList<>();
        Set<String> usedSites = new java.util.HashSet<>();
        Set<String> usedIds = new java.util.HashSet<>();
        for (Group group : requested) {
            if (group == null) continue;
            String id = clean(group.id());
            String name = clean(group.name());
            if (!id.matches("group-[a-zA-Z0-9-]{1,80}") || name.isEmpty() || !usedIds.add(id)) continue;
            List<String> siteCodes = group.siteCodes() == null ? List.of() : group.siteCodes().stream()
                    .filter(available::contains).filter(usedSites::add).toList();
            result.add(new Group(id, name.substring(0, Math.min(name.length(), 50)), siteCodes, group.collapsed()));
        }
        return result;
    }

    private void insertMissingSites(List<String> order, List<String> available) {
        for (int i = 0; i < available.size(); i++) {
            String key = available.get(i);
            if (order.contains(key)) continue;
            int insertAt = -1;
            for (int j = i - 1; j >= 0; j--) {
                int predecessor = order.indexOf(available.get(j));
                if (predecessor >= 0) { insertAt = predecessor + 1; break; }
            }
            if (insertAt < 0) {
                for (int j = i + 1; j < available.size(); j++) {
                    int successor = order.indexOf(available.get(j));
                    if (successor >= 0) { insertAt = successor; break; }
                }
            }
            order.add(insertAt < 0 ? order.size() : insertAt, key);
        }
    }

    private List<String> validHiddenCodes(List<String> requested, List<String> available) {
        if (requested == null) return List.of();
        return requested.stream().filter(available::contains).distinct().toList();
    }

    private List<Site> sharedSites() {
        return navigationSites.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(site -> new Site("global:" + site.getId(), site.getSiteCode(), site.getName(),
                        "work".equals(site.getSiteCode()) ? "/worklog" : site.getUrl(),
                        site.getIconKey(), site.getSortOrder(), site.isExternal()))
                .toList();
    }

    private List<Site> personalSites(List<Site> sites, List<Site> shared) {
        Set<String> sharedCodes = shared.stream().map(Site::siteCode).collect(java.util.stream.Collectors.toSet());
        return sanitizeSites(sites).stream()
                .filter(site -> !sharedCodes.contains(site.siteCode()))
                .filter(site -> !site.id().startsWith("global:"))
                .filter(site -> !(LEGACY_DEFAULT_CODES.contains(site.siteCode()) && site.id().equals(site.siteCode())))
                .toList();
    }

    private List<Site> sitesToStore(List<Site> sites, List<Site> shared) {
        List<Site> stored = new ArrayList<>(personalSites(sites, shared));
        for (Site original : shared) {
            Site customized = sites.stream().filter(site -> site.siteCode().equals(original.siteCode()))
                    .findFirst().orElse(original);
            if (!customized.iconKey().equals(original.iconKey()) || customized.external() != original.external()) {
                stored.add(new Site(original.id(), original.siteCode(), original.name(), original.url(),
                        customized.iconKey(), original.sortOrder(), customized.external()));
            }
        }
        return stored;
    }

    private List<Site> mergeSites(List<Site> shared, List<Site> saved) {
        List<Site> source = saved == null ? List.of() : saved;
        List<Site> merged = new ArrayList<>();
        for (Site original : shared) {
            Site override = source.stream().filter(site -> original.id().equals(site.id())
                    && original.siteCode().equals(site.siteCode())).findFirst().orElse(null);
            merged.add(override == null ? original : new Site(original.id(), original.siteCode(), original.name(),
                    original.url(), clean(override.iconKey()).isEmpty() ? original.iconKey() : clean(override.iconKey()),
                    original.sortOrder(), override.external()));
        }
        merged.addAll(personalSites(source, shared));
        return merged;
    }

    private String write(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
    private int clamp(Integer value, int min, int max, int fallback) {
        if (value == null) return fallback;
        return Math.max(min, Math.min(max, value));
    }
    private String color(String value) {
        String normalized = clean(value).toLowerCase();
        return normalized.matches("#[0-9a-f]{6}") ? normalized : "#2563eb";
    }
    private Map<String, String> defaultColors(List<String> siteKeys) {
        Map<String, String> result = new LinkedHashMap<>();
        siteKeys.forEach(key -> result.put(key, "#2563eb"));
        result.put("external", "#2563eb");
        result.put("settings", "#2563eb");
        result.put("admin", "#2563eb");
        result.put("logout", "#2563eb");
        return result;
    }
    private Map<String, String> sanitizeColors(Map<String, String> input, List<String> siteKeys) {
        Map<String, String> result = defaultColors(siteKeys);
        if (input != null) input.forEach((key, value) -> {
            if (result.containsKey(key)) result.put(key, color(value));
        });
        return result;
    }
    private String normalizeUrl(String value) {
        String url = clean(value);
        if (url.isEmpty() || url.startsWith("/")) return url;
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        return "https://" + url;
    }
}
