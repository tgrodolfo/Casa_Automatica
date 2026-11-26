package luzes.coresystemstgr.alexa;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alexa")
@Tag(name = "Alexa", description = "Endpoint da Custom Skill Alexa (luzes, janelas e portão)")
public class AlexaController {

    private final AlexaSkillService service;

    public AlexaController(AlexaSkillService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Recebe pedidos da Alexa e responde em voz")
    public ResponseEntity<AlexaSpeechResponse> receber(@RequestBody JsonNode envelope) {
        return ResponseEntity.ok(service.processar(envelope));
    }
}
