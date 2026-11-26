package luzes.coresystemstgr.alexa;

import com.fasterxml.jackson.databind.JsonNode;
import luzes.coresystemstgr.config.SecurityProperties;
import luzes.coresystemstgr.janelas.JanelaBloqueadaPorChuvaException;
import luzes.coresystemstgr.janelas.JanelaService;
import luzes.coresystemstgr.portao.Portao;
import luzes.coresystemstgr.portao.PortaoService;
import luzes.coresystemstgr.quartos.Quartos;
import luzes.coresystemstgr.quartos.QuartosService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AlexaSkillService {

    private final QuartosService quartosService;
    private final JanelaService janelaService;
    private final PortaoService portaoService;
    private final SecurityProperties securityProperties;

    public AlexaSkillService(
        QuartosService quartosService,
        JanelaService janelaService,
        PortaoService portaoService,
        SecurityProperties securityProperties
    ) {
        this.quartosService = quartosService;
        this.janelaService = janelaService;
        this.portaoService = portaoService;
        this.securityProperties = securityProperties;
    }

    public AlexaSpeechResponse processar(JsonNode envelope) {
        if (!skillAutorizada(envelope)) {
            return AlexaSpeechResponse.speak("Skill não autorizada.");
        }

        String type = text(envelope, "request", "type");
        if ("LaunchRequest".equals(type)) {
            return AlexaSpeechResponse.speak(
                "Casa Automática pronta. Peça para acender luzes, abrir janelas ou abrir o portão."
            );
        }
        if ("SessionEndedRequest".equals(type)) {
            return AlexaSpeechResponse.speak("Até logo.");
        }
        if (!"IntentRequest".equals(type)) {
            return AlexaSpeechResponse.speak("Não entendi o pedido.");
        }

        String intent = text(envelope, "request", "intent", "name");
        if (!StringUtils.hasText(intent)) {
            return AlexaSpeechResponse.speak("Não identifiquei a intenção.");
        }

        try {
            return switch (intent) {
                case "AMAZON.HelpIntent" -> AlexaSpeechResponse.speak(
                    "Você pode dizer: acenda a luz da sala, abra as janelas, ou abra o portão."
                );
                case "AMAZON.CancelIntent", "AMAZON.StopIntent" -> AlexaSpeechResponse.speak("Ok.");
                case "AcenderLuzIntent" -> acenderLuz(slot(envelope, "ambiente"));
                case "ApagarLuzIntent" -> apagarLuz(slot(envelope, "ambiente"));
                case "AcenderTodasLuzesIntent" -> {
                    quartosService.setAllLight();
                    yield AlexaSpeechResponse.speak("Todas as luzes acesas.");
                }
                case "ApagarTodasLuzesIntent" -> {
                    quartosService.setAllDark();
                    yield AlexaSpeechResponse.speak("Todas as luzes apagadas.");
                }
                case "AbrirJanelaIntent" -> abrirJanela(slot(envelope, "ambiente"));
                case "FecharJanelaIntent" -> fecharJanela(slot(envelope, "ambiente"));
                case "AbrirTodasJanelasIntent" -> {
                    janelaService.abrirTodas();
                    yield AlexaSpeechResponse.speak("Todas as janelas abertas.");
                }
                case "FecharTodasJanelasIntent" -> {
                    janelaService.fecharTodas();
                    yield AlexaSpeechResponse.speak("Todas as janelas fechadas.");
                }
                case "AbrirPortaoIntent" -> {
                    Portao portao = portaoService.abrir();
                    yield AlexaSpeechResponse.speak("Portão " + portao.getStatus() + ".");
                }
                case "FecharPortaoIntent" -> {
                    Portao portao = portaoService.fechar();
                    yield AlexaSpeechResponse.speak("Portão " + portao.getStatus() + ".");
                }
                case "AlternarPortaoIntent" -> {
                    Portao portao = portaoService.toggle();
                    yield AlexaSpeechResponse.speak("Portão " + portao.getStatus() + ".");
                }
                default -> AlexaSpeechResponse.speak("Ainda não sei fazer isso. Tente luzes, janelas ou portão.");
            };
        } catch (JanelaBloqueadaPorChuvaException ex) {
            return AlexaSpeechResponse.speak("Não posso abrir as janelas agora porque está chovendo.");
        } catch (IllegalArgumentException ex) {
            return AlexaSpeechResponse.speak(ex.getMessage());
        } catch (RuntimeException ex) {
            return AlexaSpeechResponse.speak("Não consegui completar o pedido.");
        }
    }

    private AlexaSpeechResponse acenderLuz(String ambiente) {
        if (!StringUtils.hasText(ambiente)) {
            quartosService.setAllLight();
            return AlexaSpeechResponse.speak("Todas as luzes acesas.");
        }
        Quartos quarto = quartosService.acenderPorNome(ambiente);
        return AlexaSpeechResponse.speak("Luz da " + quarto.getRoom() + " acesa.");
    }

    private AlexaSpeechResponse apagarLuz(String ambiente) {
        if (!StringUtils.hasText(ambiente)) {
            quartosService.setAllDark();
            return AlexaSpeechResponse.speak("Todas as luzes apagadas.");
        }
        Quartos quarto = quartosService.apagarPorNome(ambiente);
        return AlexaSpeechResponse.speak("Luz da " + quarto.getRoom() + " apagada.");
    }

    private AlexaSpeechResponse abrirJanela(String ambiente) {
        if (!StringUtils.hasText(ambiente)) {
            janelaService.abrirTodas();
            return AlexaSpeechResponse.speak("Todas as janelas abertas.");
        }
        var janela = janelaService.abrirPorNome(ambiente);
        return AlexaSpeechResponse.speak("Janela da " + janela.getWindowName() + " aberta.");
    }

    private AlexaSpeechResponse fecharJanela(String ambiente) {
        if (!StringUtils.hasText(ambiente)) {
            janelaService.fecharTodas();
            return AlexaSpeechResponse.speak("Todas as janelas fechadas.");
        }
        var janela = janelaService.fecharPorNome(ambiente);
        return AlexaSpeechResponse.speak("Janela da " + janela.getWindowName() + " fechada.");
    }

    private boolean skillAutorizada(JsonNode envelope) {
        String esperado = securityProperties.getAlexaSkillId();
        if (!StringUtils.hasText(esperado)) {
            return true;
        }
        String recebido = text(envelope, "session", "application", "applicationId");
        return esperado.equals(recebido);
    }

    private String slot(JsonNode envelope, String name) {
        JsonNode slot = envelope.path("request").path("intent").path("slots").path(name);
        String value = slot.path("value").asText(null);
        if (!StringUtils.hasText(value)) {
            value = slot.path("resolutions")
                .path("resolutionsPerAuthority")
                .path(0)
                .path("values")
                .path(0)
                .path("value")
                .path("name")
                .asText(null);
        }
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String text(JsonNode node, String... path) {
        JsonNode current = node;
        for (String part : path) {
            current = current.path(part);
        }
        String value = current.asText(null);
        return StringUtils.hasText(value) ? value : null;
    }
}
