package sarangit.semin5.nev;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "worklog_user_preferences")
public class WorklogPreferences {
    @Id
    @Column(length = 50)
    private String username;
    @Column(name = "dark_mode", nullable = false)
    private boolean darkMode;
    @Column(name = "legacy_mode", nullable = false)
    private boolean legacyMode;
    @Column(name = "font_scale", nullable = false)
    private int fontScale;
    @Column(name = "floating_x")
    private Integer floatingX;
    @Column(name = "floating_y")
    private Integer floatingY;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public boolean isDarkMode() { return darkMode; }
    public void setDarkMode(boolean darkMode) { this.darkMode = darkMode; }
    public boolean isLegacyMode() { return legacyMode; }
    public void setLegacyMode(boolean legacyMode) { this.legacyMode = legacyMode; }
    public int getFontScale() { return fontScale; }
    public void setFontScale(int fontScale) { this.fontScale = fontScale; }
    public Integer getFloatingX() { return floatingX; }
    public void setFloatingX(Integer floatingX) { this.floatingX = floatingX; }
    public Integer getFloatingY() { return floatingY; }
    public void setFloatingY(Integer floatingY) { this.floatingY = floatingY; }
}
