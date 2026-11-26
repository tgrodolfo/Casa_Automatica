package luzes.coresystemstgr.alexa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import luzes.coresystemstgr.portao.Portao;
import luzes.coresystemstgr.portao.PortaoService;
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
    "casa-automatica.security.device-api-key=test-device-api-key-32-characters-min",
    "casa-automatica.security.alexa-skill-id=amzn1.ask.skill.test-casa"
})
class AlexaSkillServiceTest {

    @Autowired
    AlexaSkillService alexaSkillService;

    @Autowired
    PortaoService portaoService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void abrePortaoERespondeEmVoz() throws Exception {
        portaoService.fechar();
        ObjectNode envelope = intent("AbrirPortaoIntent", "amzn1.ask.skill.test-casa");

        AlexaSpeechResponse resposta = alexaSkillService.processar(envelope);
        Portao portao = portaoService.obter();

        assertEquals("open", portao.getState());
        assertTrue(resposta.getResponse().toString().toLowerCase().contains("port"));
    }

    @Test
    void rejeitaSkillIdInvalido() throws Exception {
        String antes = portaoService.obter().getState();
        ObjectNode envelope = intent("AbrirPortaoIntent", "amzn1.ask.skill.outra");
        AlexaSpeechResponse resposta = alexaSkillService.processar(envelope);
        assertTrue(resposta.getResponse().toString().toLowerCase().contains("autorizada"));
        assertEquals(antes, portaoService.obter().getState());
    }

    private ObjectNode intent(String name, String skillId) {
        ObjectNode root = mapper.createObjectNode();
        root.putObject("session")
            .putObject("application")
            .put("applicationId", skillId);
        root.putObject("request")
            .put("type", "IntentRequest")
            .putObject("intent")
            .put("name", name);
        return root;
    }
}
