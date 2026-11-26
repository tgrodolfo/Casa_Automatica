package luzes.coresystemstgr.chuva;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import luzes.coresystemstgr.janelas.JanelaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chuva")
@Tag(name = "Chuva", description = "Sensor de chuva (atualizado pelo ESP32)")
public class ChuvaController {

    private final ChuvaService service;
    private final JanelaService janelaService;

    public ChuvaController(ChuvaService service, JanelaService janelaService) {
        this.service = service;
        this.janelaService = janelaService;
    }

    @GetMapping
    @Operation(summary = "Retorna se está chovendo")
    public ResponseEntity<Chuva> obterEstado() {
        return ResponseEntity.ok(service.obterEstado());
    }

    @GetMapping("/momentos")
    @Operation(summary = "Lista momentos de chuva (início e fim com data/hora)")
    public ResponseEntity<?> listarMomentos() {
        return ResponseEntity.ok(service.listarMomentosResposta());
    }

    @PostMapping
    @Operation(summary = "Atualiza o estado da chuva (ESP32)")
    public ResponseEntity<Chuva> atualizarDoEsp(@RequestBody ChuvaUpdateRequest body) {
        Chuva depois = service.atualizarDoEsp(body.getRain());
        if ("yes".equals(depois.getRain())) {
            janelaService.fecharTodas();
        }
        return ResponseEntity.ok(depois);
    }
}
