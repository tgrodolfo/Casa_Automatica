package luzes.coresystemstgr.portao;

import org.springframework.stereotype.Service;

@Service
public class PortaoService {

    private final PortaoRepository repository;

    public PortaoService(PortaoRepository repository) {
        this.repository = repository;
    }

    public Portao obter() {
        return repository.findById(Portao.PORTAO_ID).orElseGet(this::criarPadrao);
    }

    public Portao toggle() {
        Portao portao = obter();
        if ("open".equals(portao.getState())) {
            return fechar();
        }
        return abrir();
    }

    public Portao abrir() {
        Portao portao = obter();
        portao.setState("open");
        portao.setStatus("aberto");
        return repository.save(portao);
    }

    public Portao fechar() {
        Portao portao = obter();
        portao.setState("closed");
        portao.setStatus("fechado");
        return repository.save(portao);
    }

    public Portao criarPadrao() {
        Portao portao = new Portao();
        portao.setId(Portao.PORTAO_ID);
        portao.setGateName("entrada");
        portao.setState("closed");
        portao.setStatus("fechado");
        return repository.save(portao);
    }
}
