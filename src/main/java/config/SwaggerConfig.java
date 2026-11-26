package config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Casa Automática API",
        description = "API protegida. Use sessão web (login) ou X-API-Key para o ESP32 nos endpoints permitidos.",
        version = "v1.0.0"
    ),
    security = @SecurityRequirement(name = "sessionAuth")
)
@SecurityScheme(
    name = "sessionAuth",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.COOKIE,
    paramName = "JSESSIONID",
    description = "Faça login em /login para obter a sessão."
)
@SecurityScheme(
    name = "deviceApiKey",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER,
    paramName = "X-API-Key",
    description = "Chave do ESP32. Permitido: GET /api/quartos, GET /api/portao, GET /api/janelas, POST /api/chuva, POST /api/porta/entradas."
)
public class SwaggerConfig {
}
