package sarangit.semin5.nev;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "insa_auto_login")
public class InsaAutoLoginCredential {
    @Id
    @Column(name = "portal_username", length = 50)
    private String portalUsername;
    @Column(name = "insa_username", nullable = false, length = 100)
    private String insaUsername;
    @Column(name = "password_ciphertext", nullable = false, length = 1024)
    private String passwordCiphertext;

    public String getPortalUsername() { return portalUsername; }
    public void setPortalUsername(String portalUsername) { this.portalUsername = portalUsername; }
    public String getInsaUsername() { return insaUsername; }
    public void setInsaUsername(String insaUsername) { this.insaUsername = insaUsername; }
    public String getPasswordCiphertext() { return passwordCiphertext; }
    public void setPasswordCiphertext(String passwordCiphertext) { this.passwordCiphertext = passwordCiphertext; }
}
