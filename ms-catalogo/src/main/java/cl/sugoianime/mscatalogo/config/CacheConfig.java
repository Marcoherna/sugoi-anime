package cl.sugoianime.mscatalogo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    public static final String CACHE_TRADUCCIONES = "traducciones";

    /**
     * Cache en memoria para las traducciones (RF-05).
     *
     * Segunda capa de defensa: la primera es la columna sinopsis_es en BD.
     * Este cache evita traducir dos veces el mismo texto dentro de un
     * mismo job de sincronizacion, controlando el costo de la API externa.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(CACHE_TRADUCCIONES);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2_000)
                .expireAfterWrite(Duration.ofHours(24))
                .recordStats());
        return manager;
    }
}
