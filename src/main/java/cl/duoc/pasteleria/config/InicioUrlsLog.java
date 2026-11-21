package cl.duoc.pasteleria.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Al terminar el arranque, imprime en consola las URLs útiles (API y documentación).
 * Spring Boot no muestra el enlace a Swagger por defecto.
 */
@Component
public class InicioUrlsLog implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(InicioUrlsLog.class);

    private final Environment environment;

    public InicioUrlsLog(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String puerto = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
        String host = environment.getProperty("server.address", "localhost");
        String base = "http://" + host + ":" + puerto;

        log.info("""
                
                ========== Pastelería API ==========
                API:        {}
                Swagger UI: {}/swagger-ui/index.html
                OpenAPI:    {}/v3/api-docs
                =====================================
                """, base, base, base);
    }
}