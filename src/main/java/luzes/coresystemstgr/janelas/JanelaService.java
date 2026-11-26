package luzes.coresystemstgr.janelas;

import luzes.coresystemstgr.chuva.ChuvaService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JanelaService {

    private final JanelaRepository repository;
    private final ChuvaService chuvaService;

    public JanelaService(JanelaRepository repository, ChuvaService chuvaService) {
        this.repository = repository;
        this.chuvaService = chuvaService;
    }

    public boolean janelasBloqueadas() {
        return "yes".equals(chuvaService.obterEstado().getRain());
    }

    public List<Janela> listarTodos() {
        return repository.findAll();
    }

    public Janela buscarPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Janela não encontrada: " + id));
    }

    public Janela criar(Janela janela) {
        aplicarStatus(janela);
        if ("open".equals(janela.getState())) {
            verificarPodeAbrir();
        }
        return repository.save(janela);
    }

    public Janela atualizar(Long id, Janela dados) {
        Janela janela = buscarPorId(id);
        janela.setWindowName(dados.getWindowName());
        if (dados.getState() != null) {
            janela.setState(normalizarState(dados.getState()));
        }
        aplicarStatus(janela);
        if ("open".equals(janela.getState())) {
            verificarPodeAbrir();
        }
        return repository.save(janela);
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }

    public void toggle(Long id) {
        Janela janela = buscarPorId(id);
        if ("closed".equals(janela.getState())) {
            verificarPodeAbrir();
            janela.setState("open");
            janela.setStatus("aberto");
        } else {
            janela.setState("closed");
            janela.setStatus("fechado");
        }
        repository.save(janela);
    }

    public void abrirTodas() {
        verificarPodeAbrir();
        for (Janela janela : listarTodos()) {
            janela.setState("open");
            janela.setStatus("aberto");
            repository.save(janela);
        }
    }

    public void fecharTodas() {
        for (Janela janela : repository.findAll()) {
            janela.setState("closed");
            janela.setStatus("fechado");
            repository.save(janela);
        }
    }

    public Janela abrirPorNome(String nome) {
        verificarPodeAbrir();
        Janela janela = buscarPorNome(nome);
        janela.setState("open");
        janela.setStatus("aberto");
        return repository.save(janela);
    }

    public Janela fecharPorNome(String nome) {
        Janela janela = buscarPorNome(nome);
        janela.setState("closed");
        janela.setStatus("fechado");
        return repository.save(janela);
    }

    public Janela buscarPorNome(String nome) {
        String alvo = normalizarNome(nome);
        return listarTodos().stream()
            .filter(j -> normalizarNome(j.getWindowName()).equals(alvo))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Janela não encontrada: " + nome));
    }

    private String normalizarNome(String nome) {
        if (nome == null) {
            return "";
        }
        return nome.trim().toLowerCase()
            .replace("á", "a").replace("ã", "a").replace("â", "a")
            .replace("é", "e").replace("ê", "e")
            .replace("í", "i")
            .replace("ó", "o").replace("ô", "o").replace("õ", "o")
            .replace("ú", "u").replace("ç", "c");
    }

    private void verificarPodeAbrir() {
        if (janelasBloqueadas()) {
            throw new JanelaBloqueadaPorChuvaException();
        }
    }

    private void aplicarStatus(Janela janela) {
        janela.setState(normalizarState(janela.getState()));
        janela.setStatus("open".equals(janela.getState()) ? "aberto" : "fechado");
    }

    private String normalizarState(String state) {
        if (state == null) {
            return "closed";
        }
        String value = state.trim().toLowerCase();
        if ("open".equals(value) || "aberto".equals(value) || "aberta".equals(value)) {
            return "open";
        }
        return "closed";
    }
}
