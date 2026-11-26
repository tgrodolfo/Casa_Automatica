package luzes.coresystemstgr.porta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/porta/cartoes")
@Tag(name = "Cartões", description = "Nome da pessoa associada a cada cartão RFID")
public class CartaoPessoaController {

    private final CartaoPessoaService service;

    public CartaoPessoaController(CartaoPessoaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista cartões conhecidos e nomes cadastrados")
    public ResponseEntity<Map<String, Object>> listar() {
        return ResponseEntity.ok(service.listarResposta());
    }

    @PutMapping("/{cardUid}")
    @Operation(summary = "Salva ou atualiza o nome da pessoa do cartão")
    public ResponseEntity<CartaoPessoa> salvar(
        @PathVariable String cardUid,
        @RequestBody CartaoPessoaRequest body
    ) {
        return ResponseEntity.ok(service.salvar(cardUid, body != null ? body.getNome() : null));
    }
}
