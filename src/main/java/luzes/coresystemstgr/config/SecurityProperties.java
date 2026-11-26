package luzes.coresystemstgr.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "casa-automatica.security")
@Validated
public class SecurityProperties {

    @Valid
    private User user = new User();

    @NotBlank(message = "Defina casa-automatica.security.device-api-key ou CASA_AUTOMATICA_DEVICE_API_KEY")
    @Size(min = 32, message = "A chave do ESP32 deve ter pelo menos 32 caracteres")
    private String deviceApiKey;

    /**
     * Application ID da Custom Skill Alexa (amzn1.ask.skill...).
     * Se preenchido, o POST /api/alexa só aceita pedidos dessa skill.
     */
    private String alexaSkillId = "";

    @Valid
    private RateLimit rateLimit = new RateLimit();

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDeviceApiKey() {
        return deviceApiKey;
    }

    public void setDeviceApiKey(String deviceApiKey) {
        this.deviceApiKey = deviceApiKey;
    }

    public String getAlexaSkillId() {
        return alexaSkillId;
    }

    public void setAlexaSkillId(String alexaSkillId) {
        this.alexaSkillId = alexaSkillId;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    @Validated
    public static class User {

        @NotBlank(message = "Defina casa-automatica.security.user.name ou CASA_AUTOMATICA_USER")
        private String name = "admin";

        @NotBlank(message = "Defina casa-automatica.security.user.password ou CASA_AUTOMATICA_PASSWORD")
        @Size(min = 12, message = "A senha do usuário deve ter pelo menos 12 caracteres")
        private String password;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RateLimit {
        private int requestsPerMinute = 120;

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }
    }
}
