package sarangit.semin5.nev;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findAllByOrderByUsernameAsc();
}
