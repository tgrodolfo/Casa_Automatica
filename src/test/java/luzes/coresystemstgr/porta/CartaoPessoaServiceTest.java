package luzes.coresystemstgr.porta;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "casa-automatica.security.user.password=test-password-12",
    "casa-automatica.security.device-api-key=test-device-api-key-32-characters-min"
})
class CartaoPessoaServiceTest {

    @Autowired
    CartaoPessoaService cartaoPessoaService;

    @Autowired
    EntradaPortaService entradaPortaService;

    @Test
    void salvaNomeEApareceNaEntrada() {
        EntradaPortaRequest entrada = new EntradaPortaRequest();
        entrada.setCardUid("AABBCCDD");
        entrada.setOcorridoEm(LocalDateTime.of(2026, 11, 25, 16, 0));
        entradaPortaService.registrar(entrada);

        CartaoPessoa salvo = cartaoPessoaService.salvar("aa:bb:cc:dd", "Tales Gabriel");
        assertEquals("AABBCCDD", salvo.getCardUid());
        assertEquals("Tales Gabriel", salvo.getNome());

        List<EntradaPorta> lista = entradaPortaService.listar();
        EntradaPorta encontrada = lista.stream()
            .filter(e -> "AABBCCDD".equals(e.getCardUid()))
            .findFirst()
            .orElseThrow();
        assertEquals("Tales Gabriel", encontrada.getNome());

        Map<String, Object> cards = cartaoPessoaService.listarResposta();
        assertFalse(((List<?>) cards.get("cards")).isEmpty());
    }
}
