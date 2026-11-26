package luzes.coresystemstgr.logs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@Tag(name = "Logs", description = "Calendário e histórico por dia (chuva e porta)")
public class LogsController {

    private final LogsService service;

    public LogsController(LogsService service) {
        this.service = service;
    }

    @GetMapping("/dia")
    @Operation(summary = "Retorna momentos de chuva e entradas da porta de um dia")
    public ResponseEntity<Map<String, Object>> porDia(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        return ResponseEntity.ok(service.porDia(data));
    }

    @GetMapping("/calendario")
    @Operation(summary = "Lista dias do mês que têm registros")
    public ResponseEntity<Map<String, Object>> calendario(
        @RequestParam int ano,
        @RequestParam int mes
    ) {
        YearMonth.of(ano, mes); // valida
        return ResponseEntity.ok(service.calendario(ano, mes));
    }
}
