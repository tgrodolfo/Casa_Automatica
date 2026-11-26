package luzes.coresystemstgr.porta;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class CartaoPessoaService {

    private final CartaoPessoaRepository repository;
    private final EntradaPortaRepository entradaRepository;

    public CartaoPessoaService(CartaoPessoaRepository repository, EntradaPortaRepository entradaRepository) {
        this.repository = repository;
        this.entradaRepository = entradaRepository;
    }

    public Optional<CartaoPessoa> buscar(String cardUid) {
        String uid = normalizarUid(cardUid);
        if (uid == null) {
            return Optional.empty();
        }
        return repository.findById(uid);
    }

    public String nomeDe(String cardUid) {
        return buscar(cardUid).map(CartaoPessoa::getNome).orElse(null);
    }

    public CartaoPessoa salvar(String cardUid, String nome) {
        String uid = normalizarUid(cardUid);
        if (uid == null) {
            throw new IllegalArgumentException("cardUid é obrigatório");
        }
        String nomeLimpo = normalizarNome(nome);
        CartaoPessoa cartao = repository.findById(uid).orElseGet(CartaoPessoa::new);
        cartao.setCardUid(uid);
        cartao.setNome(nomeLimpo);
        return repository.save(cartao);
    }

    public Map<String, Object> listarResposta() {
        Map<String, String> nomes = new LinkedHashMap<>();
        for (CartaoPessoa c : repository.findAllByOrderByNomeAsc()) {
            nomes.put(c.getCardUid(), c.getNome());
        }

        Set<String> uids = new LinkedHashSet<>(nomes.keySet());
        for (EntradaPorta e : entradaRepository.findAllByOrderByOcorridoEmDesc()) {
            if (StringUtils.hasText(e.getCardUid())) {
                uids.add(e.getCardUid());
            }
        }

        List<Map<String, Object>> cards = new ArrayList<>();
        for (String uid : uids) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("cardUid", uid);
            item.put("nome", nomes.get(uid));
            cards.add(item);
        }
        return Map.of("cards", cards);
    }

    public Map<String, String> mapaNomes() {
        Map<String, String> mapa = new LinkedHashMap<>();
        for (CartaoPessoa c : repository.findAll()) {
            mapa.put(c.getCardUid(), c.getNome());
        }
        return mapa;
    }

    private String normalizarNome(String nome) {
        if (!StringUtils.hasText(nome)) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }
        String limpo = nome.trim().replaceAll("\\s+", " ");
        if (limpo.length() > 120) {
            throw new IllegalArgumentException("Nome muito longo (máx. 120)");
        }
        return limpo;
    }

    private String normalizarUid(String uid) {
        if (uid == null || uid.isBlank()) {
            return null;
        }
        String limpo = uid.trim().toUpperCase().replaceAll("[^0-9A-F]", "");
        if (limpo.isEmpty()) {
            throw new IllegalArgumentException("cardUid inválido");
        }
        if (limpo.length() > 64) {
            throw new IllegalArgumentException("cardUid muito longo");
        }
        return limpo;
    }
}
