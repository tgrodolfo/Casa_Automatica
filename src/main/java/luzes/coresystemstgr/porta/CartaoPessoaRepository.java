package luzes.coresystemstgr.porta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartaoPessoaRepository extends JpaRepository<CartaoPessoa, String> {

    List<CartaoPessoa> findAllByOrderByNomeAsc();
}
