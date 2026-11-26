package luzes.coresystemstgr.quartos;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class QuartosService {

    private final QuartosRepository repository;

    public QuartosService(QuartosRepository repository) {
        this.repository = repository;
    }

    public List<Quartos> listarTodos() {
        return repository.findAll();
    }

    public Quartos buscarPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Quarto não encontrado: " + id));
    }

    public Quartos criar(Quartos quarto) {
        return repository.save(quarto);
    }

    public Quartos atualizar(Long id, Quartos dados) {
        Quartos quarto = buscarPorId(id);
        quarto.setRoom(dados.getRoom());
        quarto.setLight(dados.getLight());
        quarto.setStatus(dados.getStatus());
        return repository.save(quarto);
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }
    public void toggleLight(Long id) {
        Quartos quarto = buscarPorId(id);
        if ("on".equals(quarto.getLight())) {
            apagar(quarto);
        } else {
            acender(quarto);
        }
    }

    public Quartos acenderPorNome(String nome) {
        Quartos quarto = buscarPorNome(nome);
        return acender(quarto);
    }

    public Quartos apagarPorNome(String nome) {
        Quartos quarto = buscarPorNome(nome);
        return apagar(quarto);
    }

    public Quartos buscarPorNome(String nome) {
        String alvo = normalizarNome(nome);
        return listarTodos().stream()
            .filter(q -> normalizarNome(q.getRoom()).equals(alvo))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Ambiente não encontrado: " + nome));
    }

    private Quartos acender(Quartos quarto) {
        quarto.setLight("on");
        quarto.setStatus("iluminado");
        return repository.save(quarto);
    }

    private Quartos apagar(Quartos quarto) {
        quarto.setLight("off");
        quarto.setStatus("apagado");
        return repository.save(quarto);
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
    public void setAllLight() {
        List<Quartos> quartos = listarTodos();
        for (Quartos quarto : quartos) {
            quarto.setLight("on");
            quarto.setStatus("iluminado");
            repository.save(quarto);
        }
    }
    public void setAllDark() {
        List<Quartos> quartos = listarTodos();
        for (Quartos quarto : quartos) {
            quarto.setLight("off");
            quarto.setStatus("apagado");
            repository.save(quarto);
        }
    }
}