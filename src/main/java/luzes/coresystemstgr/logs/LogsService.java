package luzes.coresystemstgr.logs;

import luzes.coresystemstgr.chuva.MomentoChuva;
import luzes.coresystemstgr.chuva.MomentoChuvaRepository;
import luzes.coresystemstgr.porta.CartaoPessoaService;
import luzes.coresystemstgr.porta.EntradaPorta;
import luzes.coresystemstgr.porta.EntradaPortaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LogsService {

    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    private final MomentoChuvaRepository momentoRepository;
    private final EntradaPortaRepository entradaRepository;
    private final CartaoPessoaService cartaoPessoaService;

    public LogsService(
        MomentoChuvaRepository momentoRepository,
        EntradaPortaRepository entradaRepository,
        CartaoPessoaService cartaoPessoaService
    ) {
        this.momentoRepository = momentoRepository;
        this.entradaRepository = entradaRepository;
        this.cartaoPessoaService = cartaoPessoaService;
    }

    public Map<String, Object> porDia(LocalDate dia) {
        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fim = dia.atTime(LocalTime.MAX);
        List<MomentoChuva> moments = momentoRepository.findAllByOrderByInicioDesc().stream()
            .filter(m -> sobrepoeDia(m, inicio, fim))
            .collect(Collectors.toList());
        Map<String, String> nomes = cartaoPessoaService.mapaNomes();
        List<EntradaPorta> entries = entradaRepository
            .findByOcorridoEmBetweenOrderByOcorridoEmDesc(inicio, fim);
        for (EntradaPorta entrada : entries) {
            if (entrada.getCardUid() != null) {
                entrada.setNome(nomes.get(entrada.getCardUid()));
            }
        }
        return Map.of(
            "date", dia.toString(),
            "moments", moments,
            "entries", entries
        );
    }

    public Map<String, Object> calendario(int ano, int mes) {
        YearMonth ym = YearMonth.of(ano, mes);
        LocalDateTime inicioMes = ym.atDay(1).atStartOfDay();
        LocalDateTime fimMes = ym.atEndOfMonth().atTime(LocalTime.MAX);
        LocalDate hoje = LocalDate.now(ZONA);

        Set<String> days = new LinkedHashSet<>();
        for (MomentoChuva m : momentoRepository.findAllByOrderByInicioDesc()) {
            if (m.getInicio() == null) {
                continue;
            }
            LocalDate start = m.getInicio().toLocalDate();
            LocalDate end = m.getFim() != null ? m.getFim().toLocalDate() : hoje;
            LocalDate cursor = start.isBefore(ym.atDay(1)) ? ym.atDay(1) : start;
            LocalDate last = end.isAfter(ym.atEndOfMonth()) ? ym.atEndOfMonth() : end;
            if (cursor.isAfter(last)) {
                continue;
            }
            for (LocalDate d = cursor; !d.isAfter(last); d = d.plusDays(1)) {
                days.add(d.toString());
            }
        }
        for (EntradaPorta e : entradaRepository.findByOcorridoEmBetweenOrderByOcorridoEmDesc(inicioMes, fimMes)) {
            if (e.getOcorridoEm() != null) {
                days.add(e.getOcorridoEm().toLocalDate().toString());
            }
        }
        return Map.of(
            "year", ano,
            "month", mes,
            "days", days.stream().sorted().toList()
        );
    }

    private boolean sobrepoeDia(MomentoChuva m, LocalDateTime diaInicio, LocalDateTime diaFim) {
        if (m.getInicio() == null || m.getInicio().isAfter(diaFim)) {
            return false;
        }
        if (m.getFim() == null) {
            return true;
        }
        return !m.getFim().isBefore(diaInicio);
    }
}
