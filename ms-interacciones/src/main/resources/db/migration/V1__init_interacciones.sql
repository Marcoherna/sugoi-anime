-- =====================================================================
-- V1: Esquema del dominio Interacciones
-- Cubre RF-03 (progreso), RF-06 (favoritos), RF-07 (recomendaciones)
--
-- NOTA DE ARQUITECTURA (RNF-02, Database-per-Service):
-- perfil_id vive en ms-auth y anime_id vive en ms-catalogo.
-- Son bases de datos SEPARADAS, por lo tanto NO hay FOREIGN KEY hacia
-- ellas. La integridad referencial es responsabilidad de la aplicacion,
-- no del motor de base de datos. Esto es intencional.
-- =====================================================================

CREATE TABLE favorito (
                          id          BIGSERIAL PRIMARY KEY,
                          perfil_id   BIGINT      NOT NULL,
                          usuario_id  BIGINT      NOT NULL,
                          anime_id    BIGINT      NOT NULL,
                          creado_en   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                          CONSTRAINT uq_favorito_perfil_anime UNIQUE (perfil_id, anime_id)
);

COMMENT ON COLUMN favorito.perfil_id  IS 'Referencia LOGICA a perfil.id de ms-auth. Sin FK: base de datos distinta.';
COMMENT ON COLUMN favorito.anime_id   IS 'Referencia LOGICA a anime.id de ms-catalogo. Sin FK: base de datos distinta.';
COMMENT ON COLUMN favorito.usuario_id IS 'Denormalizado desde el JWT. Permite validar propiedad sin llamar a ms-auth en cada lectura.';

-- ---------------------------------------------------------------------
-- RF-03: progreso de reproduccion
-- ---------------------------------------------------------------------
CREATE TABLE progreso_visualizacion (
                                        id              BIGSERIAL PRIMARY KEY,
                                        perfil_id       BIGINT      NOT NULL,
                                        usuario_id      BIGINT      NOT NULL,
                                        anime_id        BIGINT      NOT NULL,
                                        episodio_id     BIGINT      NOT NULL,
                                        numero_episodio INTEGER,
                                        segundo         INTEGER     NOT NULL DEFAULT 0,
                                        duracion_seg    INTEGER,
                                        completado      BOOLEAN     NOT NULL DEFAULT FALSE,
                                        actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                        CONSTRAINT uq_progreso_perfil_episodio UNIQUE (perfil_id, episodio_id),
                                        CONSTRAINT chk_progreso_segundo CHECK (segundo >= 0)
);

COMMENT ON TABLE progreso_visualizacion IS 'Un registro por perfil y episodio. Se sobreescribe, no se acumula historial.';

-- ---------------------------------------------------------------------
-- RF-07: recomendaciones precalculadas
-- Se calculan por job nocturno, NUNCA en tiempo de request.
-- ---------------------------------------------------------------------
CREATE TABLE recomendacion (
                               id           BIGSERIAL PRIMARY KEY,
                               perfil_id    BIGINT           NOT NULL,
                               anime_id     BIGINT           NOT NULL,
                               puntaje      DOUBLE PRECISION NOT NULL,
                               posicion     INTEGER          NOT NULL,
                               generado_en  TIMESTAMPTZ      NOT NULL DEFAULT NOW(),

                               CONSTRAINT uq_recomendacion_perfil_anime UNIQUE (perfil_id, anime_id),
                               CONSTRAINT chk_recomendacion_puntaje CHECK (puntaje >= 0 AND puntaje <= 1)
);

COMMENT ON COLUMN recomendacion.puntaje IS 'Similitud de coseno entre el vector de generos del perfil y el del anime. Rango 0 a 1.';

CREATE INDEX idx_favorito_perfil        ON favorito(perfil_id);
CREATE INDEX idx_favorito_usuario       ON favorito(usuario_id);
CREATE INDEX idx_progreso_perfil        ON progreso_visualizacion(perfil_id);
CREATE INDEX idx_progreso_continuar     ON progreso_visualizacion(perfil_id, actualizado_en DESC)
    WHERE completado = FALSE;
CREATE INDEX idx_recomendacion_perfil   ON recomendacion(perfil_id, posicion);
