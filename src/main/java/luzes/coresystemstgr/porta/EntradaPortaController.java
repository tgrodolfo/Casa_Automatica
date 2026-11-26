package luzes.coresystemstgr.porta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/porta/entradas")
@Tag(name = "Porta", description = "Registro de entradas pela porta (RFID)")
public class EntradaPortaController {

    private final EntradaPortaService service;

    public EntradaPortaController(EntradaPortaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista entradas pela porta com data/hora")
    public ResponseEntity<Map<String, Object>> listar() {
        return ResponseEntity.ok(service.listarResposta());
    }

    @PostMapping
    @Operation(summary = "Registra entrada pela porta (ESP32; horário via Java se omitido)")
    public ResponseEntity<EntradaPorta> registrar(@RequestBody(required = false) EntradaPortaRequest body) {
        return ResponseEntity.ok(service.registrar(body != null ? body : new EntradaPortaRequest()));
    }
}
