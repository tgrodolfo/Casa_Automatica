package luzes.coresystemstgr.porta;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "casa-automatica.security.user.password=test-password-12",
    "casa-automatica.security.device-api-key=test-device-api-key-32-characters-min"
})
class EntradaPortaServiceTest {

    @Autowired
    EntradaPortaService service;

    @Autowired
    EntradaPortaRepository repository;

    @Test
    void registraHoraViaJavaQuandoOcorridoEmOmitido() {
        repository.deleteAll();
        EntradaPortaRequest req = new EntradaPortaRequest();
        req.setCardUid("a1:b2:c3:d4");
        EntradaPorta salva = service.registrar(req);

        assertEquals("A1B2C3D4", salva.getCardUid());
        assertNotNull(salva.getOcorridoEm());
        assertNotNull(salva.getSincronizadoEm());
        assertFalse(salva.isOffline());
    }

    @Test
    void preservaOcorridoEmDaFilaOffline() {
        repository.deleteAll();
        LocalDateTime passado = LocalDateTime.of(2026, 11, 25, 10, 15, 30);
        EntradaPortaRequest req = new EntradaPortaRequest();
        req.setCardUid("DEADBEEF");
        req.setOcorridoEm(passado);
        req.setOffline(true);

        EntradaPorta salva = service.registrar(req);
        assertEquals(passado, salva.getOcorridoEm());
        assertTrue(salva.isOffline());
        assertTrue(salva.getSincronizadoEm().isAfter(passado) || salva.getSincronizadoEm().isEqual(passado));
    }
}
