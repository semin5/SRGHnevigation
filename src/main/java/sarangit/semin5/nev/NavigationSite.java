package sarangit.semin5.nev;

import jakarta.persistence.*;

@Entity
@Table(name = "navigation_site", uniqueConstraints = {
        @UniqueConstraint(name = "uk_navigation_site_code", columnNames = "site_code")
})
public class NavigationSite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "site_code", nullable = false, length = 80)
    private String siteCode;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 500)
    private String url;
    @Column(name = "icon_key", nullable = false, length = 40)
    private String iconKey;
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
    @Column(nullable = false)
    private boolean external;

    public Long getId() { return id; }
    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getIconKey() { return iconKey; }
    public void setIconKey(String iconKey) { this.iconKey = iconKey; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public boolean isExternal() { return external; }
    public void setExternal(boolean external) { this.external = external; }
}
