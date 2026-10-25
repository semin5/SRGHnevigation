package sarangit.semin5.nev;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DesignatedDeviceRepository extends JpaRepository<DesignatedDevice, UUID> {
    List<DesignatedDevice> findByUsernameOrderByRegisteredAtDesc(String username);
    Optional<DesignatedDevice> findByUsernameAndTokenHash(String username, String tokenHash);
}
