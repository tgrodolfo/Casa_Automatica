package luzes.coresystemstgr.chuva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MomentoChuvaRepository extends JpaRepository<MomentoChuva, Long> {

    Optional<MomentoChuva> findFirstByFimIsNullOrderByInicioDesc();

    List<MomentoChuva> findAllByOrderByInicioDesc();
}
