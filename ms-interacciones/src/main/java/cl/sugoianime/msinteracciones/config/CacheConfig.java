package cl.sugoianime.msinteracciones.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    public static final String CACHE_ANIMES = "animes-externos";
    public static final String CACHE_PERFILES = "perfiles-usuario";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager =
                new CaffeineCacheManager(CACHE_ANIMES, CACHE_PERFILES);

        // TTL corto en perfiles: si el usuario crea o borra un perfil,
        // el cambio debe reflejarse pronto. 60 segundos es suficiente
        // para absorber una rafaga de peticiones sin quedar desfasado.
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofSeconds(60))
                .recordStats());

        return manager;
    }
}
