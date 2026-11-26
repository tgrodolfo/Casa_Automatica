package luzes.coresystemstgr.chuva;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class ChuvaService {

    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    private final ChuvaRepository repository;
    private final MomentoChuvaRepository momentoRepository;

    public ChuvaService(ChuvaRepository repository, MomentoChuvaRepository momentoRepository) {
        this.repository = repository;
        this.momentoRepository = momentoRepository;
    }

    public Chuva obterEstado() {
        return repository.findById(Chuva.SENSOR_ID).orElseGet(this::criarPadrao);
    }

    public List<MomentoChuva> listarMomentos() {
        return momentoRepository.findAllByOrderByInicioDesc();
    }

    public Map<String, Object> listarMomentosResposta() {
        return Map.of("moments", listarMomentos());
    }

    @Transactional
    public Chuva atualizarDoEsp(String rainInput) {
        Chuva chuva = obterEstado();
        String rainAnterior = chuva.getRain();
        String rain = normalizarRain(rainInput);

        if (!rain.equals(rainAnterior)) {
            LocalDateTime agora = LocalDateTime.now(ZONA);
            if ("yes".equals(rain)) {
                abrirMomento(agora);
            } else if ("yes".equals(rainAnterior)) {
                fecharMomentoAberto(agora);
            }
        }

        chuva.setRain(rain);
        chuva.setStatus("yes".equals(rain) ? "chovendo" : "sem chuva");
        return repository.save(chuva);
    }

    public Chuva criarPadrao() {
        Chuva chuva = new Chuva();
        chuva.setId(Chuva.SENSOR_ID);
        chuva.setRain("no");
        chuva.setStatus("sem chuva");
        return repository.save(chuva);
    }

    private void abrirMomento(LocalDateTime inicio) {
        momentoRepository.findFirstByFimIsNullOrderByInicioDesc().ifPresent(aberto -> {
            aberto.setFim(inicio);
            momentoRepository.save(aberto);
        });
        MomentoChuva momento = new MomentoChuva();
        momento.setInicio(inicio);
        momentoRepository.save(momento);
    }

    private void fecharMomentoAberto(LocalDateTime fim) {
        momentoRepository.findFirstByFimIsNullOrderByInicioDesc().ifPresent(aberto -> {
            aberto.setFim(fim);
            momentoRepository.save(aberto);
        });
    }

    private String normalizarRain(String rainInput) {
        if (rainInput == null || rainInput.isBlank()) {
            throw new IllegalArgumentException("Campo rain é obrigatório (yes/no ou on/off)");
        }
        String value = rainInput.trim().toLowerCase();
        if ("yes".equals(value) || "on".equals(value) || "true".equals(value) || "1".equals(value)
            || "chovendo".equals(value)) {
            return "yes";
        }
        if ("no".equals(value) || "off".equals(value) || "false".equals(value) || "0".equals(value)
            || "sem chuva".equals(value) || "nao".equals(value) || "não".equals(value)) {
            return "no";
        }
        throw new IllegalArgumentException("Valor de rain inválido: " + rainInput);
    }
}
