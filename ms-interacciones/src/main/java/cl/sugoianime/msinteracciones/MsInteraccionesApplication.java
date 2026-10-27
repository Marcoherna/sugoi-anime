package cl.sugoianime.msinteracciones;

import cl.sugoianime.msinteracciones.config.ServiciosProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableConfigurationProperties(ServiciosProperties.class)
public class MsInteraccionesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsInteraccionesApplication.class, args);
	}

}
