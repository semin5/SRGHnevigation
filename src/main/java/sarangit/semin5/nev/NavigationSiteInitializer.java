package sarangit.semin5.nev;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class NavigationSiteInitializer implements CommandLineRunner {
    private final NavigationSiteRepository sites;
    public NavigationSiteInitializer(NavigationSiteRepository sites) { this.sites = sites; }
    @Override
    public void run(String... args) {
        if (sites.count() > 0) return;
        sites.saveAllAndFlush(List.of(
                site("work", "업무 관리", "/worklog", "work", 10),
                site("insa", "인사 관리", "/insa", "users", 20),
                site("ip", "IP 관리", "/ip", "network", 30),
                site("management", "자산 관리", "/management", "database", 40),
                site("register", "관리대장", "/register", "clipboard", 50),
                site("gitea", "Gitea", "/gitea", "cloud", 60),
                site("development", "개발·배포", "/development", "tools", 70, true)
        ));
    }
    private NavigationSite site(String code, String name, String url, String icon, int order) {
        return site(code, name, url, icon, order, false);
    }
    private NavigationSite site(String code, String name, String url, String icon, int order, boolean external) {
        NavigationSite site = new NavigationSite();
        site.setSiteCode(code); site.setName(name); site.setUrl(url); site.setIconKey(icon); site.setSortOrder(order);
        site.setExternal(external);
        return site;
    }
}
