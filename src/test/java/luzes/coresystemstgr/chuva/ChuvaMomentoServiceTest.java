package luzes.coresystemstgr.chuva;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "casa-automatica.security.user.password=test-password-12",
    "casa-automatica.security.device-api-key=test-device-api-key-32-characters-min"
})
class ChuvaMomentoServiceTest {

    @Autowired
    ChuvaService chuvaService;

    @Autowired
    MomentoChuvaRepository momentoRepository;

    @Test
    void registraInicioEFimDoMomentoDeChuva() {
        momentoRepository.deleteAll();
        chuvaService.atualizarDoEsp("no");

        chuvaService.atualizarDoEsp("yes");
        var abertos = momentoRepository.findFirstByFimIsNullOrderByInicioDesc();
        assertTrue(abertos.isPresent());
        assertNotNull(abertos.get().getInicio());
        assertNull(abertos.get().getFim());

        chuvaService.atualizarDoEsp("no");
        var fechado = momentoRepository.findById(abertos.get().getId()).orElseThrow();
        assertNotNull(fechado.getFim());
        assertFalse(fechado.isEmAndamento());
    }
}
