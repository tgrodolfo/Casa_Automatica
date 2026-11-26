package luzes.coresystemstgr.alexa;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlexaSpeechResponse {

    private String version = "1.0";
    private Map<String, Object> response;

    public static AlexaSpeechResponse speak(String text) {
        AlexaSpeechResponse body = new AlexaSpeechResponse();
        body.response = Map.of(
            "outputSpeech", Map.of(
                "type", "PlainText",
                "text", text
            ),
            "shouldEndSession", true
        );
        return body;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @JsonProperty("response")
    public Map<String, Object> getResponse() {
        return response;
    }

    public void setResponse(Map<String, Object> response) {
        this.response = response;
    }
}
