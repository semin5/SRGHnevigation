package sarangit.semin5.nev;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavigationSiteRepository extends JpaRepository<NavigationSite, Long> {
    List<NavigationSite> findAllByOrderBySortOrderAscIdAsc();
}
