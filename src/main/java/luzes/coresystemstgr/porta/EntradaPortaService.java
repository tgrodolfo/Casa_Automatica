package luzes.coresystemstgr.porta;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class EntradaPortaService {

    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    private final EntradaPortaRepository repository;
    private final CartaoPessoaService cartaoPessoaService;

    public EntradaPortaService(EntradaPortaRepository repository, CartaoPessoaService cartaoPessoaService) {
        this.repository = repository;
        this.cartaoPessoaService = cartaoPessoaService;
    }

    public List<EntradaPorta> listar() {
        Map<String, String> nomes = cartaoPessoaService.mapaNomes();
        List<EntradaPorta> entradas = repository.findAllByOrderByOcorridoEmDesc();
        for (EntradaPorta entrada : entradas) {
            if (entrada.getCardUid() != null) {
                entrada.setNome(nomes.get(entrada.getCardUid()));
            }
        }
        return entradas;
    }

    public EntradaPorta registrar(EntradaPortaRequest request) {
        LocalDateTime agora = LocalDateTime.now(ZONA);
        EntradaPorta entrada = new EntradaPorta();
        entrada.setCardUid(normalizarUid(request != null ? request.getCardUid() : null));
        entrada.setSincronizadoEm(agora);
        boolean offline = request != null && Boolean.TRUE.equals(request.getOffline());
        entrada.setOffline(offline);

        LocalDateTime ocorrido = request != null ? request.getOcorridoEm() : null;
        if (ocorrido == null) {
            ocorrido = agora;
        }
        entrada.setOcorridoEm(ocorrido);
        EntradaPorta salva = repository.save(entrada);
        if (salva.getCardUid() != null) {
            salva.setNome(cartaoPessoaService.nomeDe(salva.getCardUid()));
        }
        return salva;
    }

    public Map<String, Object> listarResposta() {
        return Map.of("entries", listar());
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
