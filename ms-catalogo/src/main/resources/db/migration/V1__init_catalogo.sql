-- =====================================================================
-- V1: Esquema inicial del dominio Catalogo
-- Cubre RF-01 (navegacion), RF-02 (reproduccion), RF-05 (traduccion)
-- =====================================================================

CREATE TABLE genero (
                        id          BIGSERIAL PRIMARY KEY,
                        mal_id      INTEGER      NOT NULL UNIQUE,
                        nombre      VARCHAR(100) NOT NULL,
                        nombre_es   VARCHAR(100)
);

COMMENT ON COLUMN genero.mal_id IS 'Identificador del genero en MyAnimeList (RF-04)';

CREATE TABLE anime (
                       id              BIGSERIAL PRIMARY KEY,
                       mal_id          INTEGER       NOT NULL UNIQUE,
                       titulo          VARCHAR(500)  NOT NULL,
                       titulo_japones  VARCHAR(500),
                       sinopsis        TEXT,
                       sinopsis_es     TEXT,
                       anio            INTEGER,
                       episodios       INTEGER,
                       puntuacion      NUMERIC(4,2),
                       imagen_key      VARCHAR(1000),
                       estado          VARCHAR(30)   NOT NULL DEFAULT 'DESCONOCIDO',
                       creado_en       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                       actualizado_en  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

                       CONSTRAINT chk_anime_anio       CHECK (anio IS NULL OR anio BETWEEN 1900 AND 2100),
                       CONSTRAINT chk_anime_puntuacion CHECK (puntuacion IS NULL OR puntuacion BETWEEN 0 AND 10)
);

COMMENT ON COLUMN anime.sinopsis_es IS 'Traduccion cacheada en BD para no llamar a la API en cada request (RF-05)';
COMMENT ON COLUMN anime.imagen_key  IS 'Key del objeto en S3, NO la URL completa (RNF-07)';

CREATE TABLE anime_genero (
                              anime_id  BIGINT NOT NULL REFERENCES anime(id)  ON DELETE CASCADE,
                              genero_id BIGINT NOT NULL REFERENCES genero(id) ON DELETE CASCADE,
                              PRIMARY KEY (anime_id, genero_id)
);

CREATE TABLE episodio (
                          id            BIGSERIAL PRIMARY KEY,
                          anime_id      BIGINT       NOT NULL REFERENCES anime(id) ON DELETE CASCADE,
                          numero        INTEGER      NOT NULL,
                          titulo        VARCHAR(500),
                          duracion_seg  INTEGER,
                          video_key     VARCHAR(1000),
                          licencia      VARCHAR(200),
                          fuente_url    VARCHAR(1000),

                          CONSTRAINT uq_episodio_anime_numero UNIQUE (anime_id, numero),
                          CONSTRAINT chk_episodio_numero      CHECK (numero > 0)
);

-- Trazabilidad legal: Ley 17.336 / Riesgo R04.
-- Estas dos columnas son la evidencia de que el contenido es licito.
COMMENT ON COLUMN episodio.licencia   IS 'Licencia del contenido: CC-BY, CC0, DOMINIO_PUBLICO, TRAILER_OFICIAL';
COMMENT ON COLUMN episodio.fuente_url IS 'URL original de descarga, para auditoria de derechos de autor';

-- ---------------------------------------------------------------------
-- Indices que soportan los filtros de RF-01
-- ---------------------------------------------------------------------
CREATE INDEX idx_anime_anio        ON anime(anio);
CREATE INDEX idx_anime_puntuacion  ON anime(puntuacion DESC NULLS LAST);
CREATE INDEX idx_anime_estado      ON anime(estado);
CREATE INDEX idx_anime_titulo_trgm ON anime USING gin (to_tsvector('spanish', titulo));
CREATE INDEX idx_anime_genero_gid  ON anime_genero(genero_id);
CREATE INDEX idx_episodio_anime    ON episodio(anime_id);

-- Traducciones pendientes: acelera el job de RF-05
CREATE INDEX idx_anime_sin_traducir ON anime(id) WHERE sinopsis_es IS NULL;
