package luzes.coresystemstgr.logs;

import luzes.coresystemstgr.porta.EntradaPortaRequest;
import luzes.coresystemstgr.porta.EntradaPortaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
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
class LogsServiceTest {

    @Autowired
    LogsService logsService;

    @Autowired
    EntradaPortaService entradaPortaService;

    @Test
    void calendarioMarcaDiaComEntrada() {
        LocalDate dia = LocalDate.of(2026, 11, 25);
        EntradaPortaRequest req = new EntradaPortaRequest();
        req.setCardUid("ABCDEF01");
        req.setOcorridoEm(LocalDateTime.of(2026, 11, 25, 9, 30));
        entradaPortaService.registrar(req);

        Map<String, Object> cal = logsService.calendario(2026, 11);
        @SuppressWarnings("unchecked")
        List<String> days = (List<String>) cal.get("days");
        assertTrue(days.contains(dia.toString()));

        Map<String, Object> detalhe = logsService.porDia(dia);
        assertEquals(dia.toString(), detalhe.get("date"));
        assertFalse(((List<?>) detalhe.get("entries")).isEmpty());
    }
}
