package sarangit.semin5.nev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserSettingsServiceTest {
    private final UserSettingsRepository settingsRepository = mock(UserSettingsRepository.class);
    private final NavigationSiteRepository navigationSites = mock(NavigationSiteRepository.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private UserSettingsService service;

    @BeforeEach
    void setUp() {
        service = new UserSettingsService(settingsRepository, navigationSites, mapper);
        when(settingsRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(settingsRepository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void newAccountReceivesDatabaseSites() {
        when(settingsRepository.findById("new-user")).thenReturn(Optional.empty());
        when(navigationSites.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                site(1L, "work", "업무 관리", "/work", 10),
                site(2L, "extension", "내선번호", "/extension", 20)));

        UserSettingsService.View view = service.getOrCreate("new-user");

        assertThat(view.sites()).extracting(UserSettingsService.Site::siteCode)
                .containsExactly("work", "extension");
        assertThat(view.sites().get(0).url()).isEqualTo("/worklog");
        assertThat(view.workPath()).isEqualTo("/worklog");
        assertThat(view.order()).containsExactly("work", "extension");
        assertThat(view.navCollapsedDefault()).isFalse();
    }

    @Test
    void newAccountReceivesExternalClassificationFromDatabase() {
        when(settingsRepository.findById("new-user")).thenReturn(Optional.empty());
        NavigationSite nms = site(16L, "nms", "nms", "/nms", 14);
        NavigationSite monitoring = site(17L, "monitoring", "모니터링", "/monitoring/d/server-monitoring/server-monitoring", 15);
        nms.setExternal(true);
        monitoring.setExternal(true);
        when(navigationSites.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(nms, monitoring));

        assertThat(service.getOrCreate("new-user").sites())
                .extracting(UserSettingsService.Site::external)
                .containsExactly(true, true);
    }

    @Test
    void existingAccountGetsNewAndUpdatedSharedSitesWithoutLosingPersonalSettings() throws Exception {
        UserSettings saved = new UserSettings();
        saved.setUsername("existing");
        saved.setTheme("dark");
        saved.setWorkPath("/work");
        saved.setSitesJson(mapper.writeValueAsString(List.of(
                new UserSettingsService.Site("work", "work", "오래된 이름", "/old", "work", 10, false),
                new UserSettingsService.Site("custom-1", "custom-1", "개인 사이트", "https://example.org", "link", 30, true))));
        saved.setOrderJson("[\"custom-1\",\"work\"]");
        saved.setIconColorsJson("{\"custom-1\":\"#123456\"}");
        when(settingsRepository.findById("existing")).thenReturn(Optional.of(saved));
        when(navigationSites.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                site(1L, "work", "새 이름", "/work", 10),
                site(2L, "extension", "내선번호", "/extension", 20)));

        UserSettingsService.View view = service.getOrCreate("existing");

        assertThat(view.sites()).extracting(UserSettingsService.Site::siteCode)
                .containsExactly("work", "extension", "custom-1");
        assertThat(view.sites().get(0).name()).isEqualTo("새 이름");
        assertThat(view.sites().get(0).url()).isEqualTo("/worklog");
        assertThat(view.workPath()).isEqualTo("/worklog");
        assertThat(view.order()).containsExactly("custom-1", "work", "extension");
        assertThat(view.theme()).isEqualTo("dark");
        assertThat(view.iconColors()).containsEntry("custom-1", "#123456");
    }

    @Test
    void newlySharedSiteUsesDatabasePositionWithoutChangingExistingRelativeOrder() {
        UserSettings saved = new UserSettings();
        saved.setUsername("existing");
        saved.setSitesJson("[]");
        saved.setOrderJson("[\"work\",\"insa\",\"ip\",\"management\",\"register\",\"server\"]");
        when(settingsRepository.findById("existing")).thenReturn(Optional.of(saved));
        when(navigationSites.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                site(1L, "work", "업무", "/work", 1),
                site(2L, "insa", "인사", "/insa", 2),
                site(3L, "ip", "IP", "/ip", 3),
                site(8L, "extension", "내선번호", "/extension", 4),
                site(4L, "management", "자산", "/management", 5),
                site(5L, "register", "관리대장", "/register", 6),
                site(7L, "server", "서버", "/server", 7)));

        assertThat(service.getOrCreate("existing").order())
                .containsExactly("work", "insa", "ip", "extension", "management", "register", "server");
        assertThat(saved.getOrderJson()).doesNotContain("extension");
    }

    @Test
    void savingCannotOverwriteSharedSiteButKeepsPersonalSite() throws Exception {
        UserSettings saved = new UserSettings();
        saved.setUsername("existing");
        saved.setSitesJson("[]");
        saved.setOrderJson("[]");
        when(settingsRepository.findById("existing")).thenReturn(Optional.of(saved));
        when(navigationSites.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                site(1L, "work", "공통 업무", "/work", 10)));
        UserSettingsService.View input = new UserSettingsService.View("light", "/work", List.of(
                new UserSettingsService.Site("global:1", "work", "개인 수정", "/wrong", "link", 10, false),
                new UserSettingsService.Site("custom-1", "custom-1", "개인 사이트", "https://example.org", "link", 20, true)),
                List.of("custom-1", "work"), 18, 5, "#2563eb", null, List.of("work", "missing"), true, null, null);

        UserSettingsService.View view = service.save("existing", input);

        assertThat(view.sites().get(0).name()).isEqualTo("공통 업무");
        assertThat(view.order()).containsExactly("custom-1", "work");
        assertThat(saved.getSitesJson()).contains("custom-1").doesNotContain("global:1");
        assertThat(view.hiddenSiteCodes()).containsExactly("work");
        assertThat(saved.getHiddenSiteCodesJson()).isEqualTo("[\"work\"]");
        assertThat(view.navCollapsedDefault()).isTrue();
    }

    @Test
    void existingAccountKeepsHiddenSitesWhenOtherSettingsAreSaved() throws Exception {
        UserSettings saved = new UserSettings();
        saved.setUsername("existing");
        saved.setSitesJson("[]");
        saved.setOrderJson("[\"work\"]");
        saved.setHiddenSiteCodesJson("[\"work\"]");
        when(settingsRepository.findById("existing")).thenReturn(Optional.of(saved));
        when(navigationSites.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                site(1L, "work", "업무 관리", "/work", 10)));

        UserSettingsService.View input = new UserSettingsService.View("dark", "/work", null,
                List.of("work"), 18, 5, "#2563eb", null, null, null, null, null);

        UserSettingsService.View view = service.save("existing", input);

        assertThat(view.hiddenSiteCodes()).containsExactly("work");
    }

    @Test
    void existingAccountKeepsCollapsedDefaultWhenOlderClientSavesOtherSettings() {
        UserSettings saved = new UserSettings();
        saved.setUsername("existing");
        saved.setSitesJson("[]");
        saved.setOrderJson("[]");
        saved.setNavCollapsedDefault(true);
        when(settingsRepository.findById("existing")).thenReturn(Optional.of(saved));
        when(navigationSites.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of());

        UserSettingsService.View input = new UserSettingsService.View("dark", "/work", null,
                List.of(), 18, 5, "#2563eb", null, null, null, null, null);

        assertThat(service.save("existing", input).navCollapsedDefault()).isTrue();
    }

    @Test
    void foldersArePerAccountAndOnlyContainAvailableSitesOnce() {
        UserSettings saved = new UserSettings();
        saved.setUsername("existing");
        saved.setSitesJson("[]");
        saved.setOrderJson("[\"work\",\"insa\"]");
        when(settingsRepository.findById("existing")).thenReturn(Optional.of(saved));
        when(navigationSites.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                site(1L, "work", "업무", "/work", 1), site(2L, "insa", "인사", "/insa", 2)));

        UserSettingsService.View input = new UserSettingsService.View("light", "/work", null,
                List.of("work", "insa"), 18, 5, "#2563eb", Map.of("group-first", "#123456"), null, null, List.of(
                new UserSettingsService.Group("group-first", "업무 폴더", List.of("work", "missing"), true),
                new UserSettingsService.Group("group-second", "기타", List.of("work", "insa"), false)), null);

        UserSettingsService.View view = service.save("existing", input);

        assertThat(view.groups()).extracting(UserSettingsService.Group::name)
                .containsExactly("업무 폴더", "기타");
        assertThat(view.groups().get(0).siteCodes()).containsExactly("work");
        assertThat(view.groups().get(0).collapsed()).isTrue();
        assertThat(view.groups().get(1).siteCodes()).containsExactly("insa");
        assertThat(saved.getGroupsJson()).contains("group-first", "group-second");
        assertThat(view.iconColors()).containsEntry("group-first", "#123456")
                .containsEntry("group-second", "#2563eb");
        assertThat(saved.getIconColorsJson()).contains("group-first");
    }

    @Test
    void sharedSiteKeepsDatabaseNameAndUrlButSavesPerAccountIconAndExternalMode() throws Exception {
        UserSettings saved = new UserSettings();
        saved.setUsername("existing");
        saved.setSitesJson("[]");
        saved.setOrderJson("[\"work\"]");
        when(settingsRepository.findById("existing")).thenReturn(Optional.of(saved));
        when(navigationSites.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                site(1L, "work", "공통 업무", "/work", 10)));

        UserSettingsService.View input = new UserSettingsService.View("light", "/work", List.of(
                new UserSettingsService.Site("global:1", "work", "변조된 이름", "/wrong", "folder", 99, true)),
                List.of("work"), 18, 5, "#2563eb", null, null, null, null, null);

        UserSettingsService.View result = service.save("existing", input);

        assertThat(result.sites().get(0).name()).isEqualTo("공통 업무");
        assertThat(result.sites().get(0).url()).isEqualTo("/worklog");
        assertThat(result.workPath()).isEqualTo("/worklog");
        assertThat(saved.getWorkPath()).isEqualTo("/worklog");
        assertThat(result.sites().get(0).iconKey()).isEqualTo("folder");
        assertThat(result.sites().get(0).external()).isTrue();
        assertThat(saved.getSitesJson()).contains("global:1", "folder").doesNotContain("변조된 이름", "/wrong");
        assertThat(service.getOrCreate("existing").sites().get(0).iconKey()).isEqualTo("folder");
    }

    @Test
    void externalGroupVisibilityIsSavedPerAccountAndPreservedByOlderClients() {
        UserSettings saved = new UserSettings();
        saved.setUsername("existing");
        saved.setSitesJson("[]");
        saved.setOrderJson("[]");
        when(settingsRepository.findById("existing")).thenReturn(Optional.of(saved));
        when(navigationSites.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of());

        UserSettingsService.View hide = new UserSettingsService.View("light", "/work", null,
                List.of(), 18, 5, "#2563eb", null, null, null, null, false);
        assertThat(service.save("existing", hide).showExternalGroup()).isFalse();
        assertThat(saved.getShowExternalGroup()).isFalse();

        UserSettingsService.View oldClient = new UserSettingsService.View("light", "/work", null,
                List.of(), 18, 5, "#2563eb", null, null, null, null, null);
        assertThat(service.save("existing", oldClient).showExternalGroup()).isFalse();
    }

    private NavigationSite site(Long id, String code, String name, String url, int order) {
        NavigationSite site = new NavigationSite();
        ReflectionTestUtils.setField(site, "id", id);
        site.setSiteCode(code);
        site.setName(name);
        site.setUrl(url);
        site.setIconKey("link");
        site.setSortOrder(order);
        return site;
    }
}
