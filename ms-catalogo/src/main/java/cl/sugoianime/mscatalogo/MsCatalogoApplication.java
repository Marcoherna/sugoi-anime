package cl.sugoianime.mscatalogo;

import cl.sugoianime.mscatalogo.config.StreamingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // habilita el job de sincronizacion Jikan (RF-04)
@EnableCaching      // habilita el cache de traducciones (RF-05)
@EnableConfigurationProperties(StreamingProperties.class)
public class MsCatalogoApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsCatalogoApplication.class, args);
	}

}
