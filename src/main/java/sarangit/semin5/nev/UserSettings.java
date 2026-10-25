package sarangit.semin5.nev;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_settings")
public class UserSettings {
    @Id
    @Column(length = 80)
    private String username;
    @Column(nullable = false, length = 20)
    private String theme;
    @Column(name = "work_path", nullable = false, length = 100)
    private String workPath;
    @Column(name = "sites_json", nullable = false, columnDefinition = "text")
    private String sitesJson;
    @Column(name = "order_json", nullable = false, columnDefinition = "text")
    private String orderJson;
    @Column(name = "icon_size")
    private Integer iconSize;
    @Column(name = "icon_gap")
    private Integer iconGap;
    @Column(name = "nav_collapsed_default")
    private Boolean navCollapsedDefault;
    @Column(name = "icon_color", length = 7)
    private String iconColor;
    @Column(name = "icon_colors_json", columnDefinition = "text")
    private String iconColorsJson;
    @Column(name = "hidden_site_codes_json", columnDefinition = "text")
    private String hiddenSiteCodesJson;
    @Column(name = "groups_json", columnDefinition = "text")
    private String groupsJson;
    @Column(name = "show_external_group")
    private Boolean showExternalGroup;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getWorkPath() { return workPath; }
    public void setWorkPath(String workPath) { this.workPath = workPath; }
    public String getSitesJson() { return sitesJson; }
    public void setSitesJson(String sitesJson) { this.sitesJson = sitesJson; }
    public String getOrderJson() { return orderJson; }
    public void setOrderJson(String orderJson) { this.orderJson = orderJson; }
    public Integer getIconSize() { return iconSize; }
    public void setIconSize(Integer iconSize) { this.iconSize = iconSize; }
    public Integer getIconGap() { return iconGap; }
    public void setIconGap(Integer iconGap) { this.iconGap = iconGap; }
    public Boolean getNavCollapsedDefault() { return navCollapsedDefault; }
    public void setNavCollapsedDefault(Boolean navCollapsedDefault) { this.navCollapsedDefault = navCollapsedDefault; }
    public String getIconColor() { return iconColor; }
    public void setIconColor(String iconColor) { this.iconColor = iconColor; }
    public String getIconColorsJson() { return iconColorsJson; }
    public void setIconColorsJson(String iconColorsJson) { this.iconColorsJson = iconColorsJson; }
    public String getHiddenSiteCodesJson() { return hiddenSiteCodesJson; }
    public void setHiddenSiteCodesJson(String hiddenSiteCodesJson) { this.hiddenSiteCodesJson = hiddenSiteCodesJson; }
    public String getGroupsJson() { return groupsJson; }
    public void setGroupsJson(String groupsJson) { this.groupsJson = groupsJson; }
    public Boolean getShowExternalGroup() { return showExternalGroup; }
    public void setShowExternalGroup(Boolean showExternalGroup) { this.showExternalGroup = showExternalGroup; }
}
