package luzes.coresystemstgr.porta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EntradaPortaRepository extends JpaRepository<EntradaPorta, Long> {

    List<EntradaPorta> findAllByOrderByOcorridoEmDesc();

    List<EntradaPorta> findByOcorridoEmBetweenOrderByOcorridoEmDesc(LocalDateTime inicio, LocalDateTime fim);
}
