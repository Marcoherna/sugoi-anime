-- =====================================================================
-- V1: Esquema del dominio Identidad
-- Cubre RF-08 (cuentas), RF-09 (perfiles), Ley 19.628 (consentimiento)
-- =====================================================================

CREATE TABLE usuario (
                         id                  BIGSERIAL PRIMARY KEY,
                         email               VARCHAR(255) NOT NULL UNIQUE,
                         password_hash       VARCHAR(100) NOT NULL,
                         nombre              VARCHAR(150) NOT NULL,
                         activo              BOOLEAN      NOT NULL DEFAULT TRUE,
                         es_premium          BOOLEAN      NOT NULL DEFAULT FALSE,
                         ultimo_acceso       TIMESTAMPTZ,
                         creado_en           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                         actualizado_en      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                         CONSTRAINT chk_usuario_email CHECK (email ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$')
    );

COMMENT ON COLUMN usuario.password_hash IS 'Hash BCrypt. NUNCA se almacena la contrasena en claro.';
COMMENT ON COLUMN usuario.es_premium    IS 'Se actualiza desde ms-pagos via evento asincrono (RF-10, RNF-08)';

-- Roles. Tabla aparte para permitir varios roles por usuario.
CREATE TABLE usuario_rol (
                             usuario_id  BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                             rol         VARCHAR(30) NOT NULL,
                             PRIMARY KEY (usuario_id, rol)
);

-- ---------------------------------------------------------------------
-- RF-09: multiples perfiles bajo una misma cuenta matriz
-- ---------------------------------------------------------------------
CREATE TABLE perfil (
                        id          BIGSERIAL PRIMARY KEY,
                        usuario_id  BIGINT       NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                        nombre      VARCHAR(50)  NOT NULL,
                        avatar      VARCHAR(50)  NOT NULL DEFAULT 'default',
                        es_infantil BOOLEAN      NOT NULL DEFAULT FALSE,
                        creado_en   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                        CONSTRAINT uq_perfil_usuario_nombre UNIQUE (usuario_id, nombre)
);

COMMENT ON COLUMN perfil.es_infantil IS 'Habilita el filtrado de contenido por edad (base para RF-11)';

-- ---------------------------------------------------------------------
-- LEY 19.628: evidencia persistida del consentimiento.
-- Un checkbox en React no es evidencia; esta tabla si lo es.
-- ---------------------------------------------------------------------
CREATE TABLE consentimiento (
                                id               BIGSERIAL PRIMARY KEY,
                                usuario_id       BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                                version_terminos VARCHAR(20) NOT NULL,
                                tipo             VARCHAR(50) NOT NULL,
                                aceptado         BOOLEAN     NOT NULL,
                                aceptado_en      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                ip_origen        VARCHAR(45)
);

COMMENT ON TABLE  consentimiento IS 'Registro auditable exigido por la Ley 19.628 sobre proteccion de datos personales';
COMMENT ON COLUMN consentimiento.version_terminos IS 'Version de los terminos aceptada. Permite saber que acepto cada usuario.';
COMMENT ON COLUMN consentimiento.tipo IS 'TERMINOS_Y_CONDICIONES, TRATAMIENTO_DATOS, HISTORIAL_RECOMENDACIONES';
COMMENT ON COLUMN consentimiento.ip_origen IS 'IPv4 o IPv6 desde donde se otorgo el consentimiento';

-- ---------------------------------------------------------------------
-- Refresh tokens: hacen posible el cierre de sesion real (RF-08).
-- Un JWT sin estado NO se puede invalidar; un refresh token en BD si.
-- ---------------------------------------------------------------------
CREATE TABLE refresh_token (
                               id           BIGSERIAL PRIMARY KEY,
                               usuario_id   BIGINT       NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                               token        VARCHAR(255) NOT NULL UNIQUE,
                               expira_en    TIMESTAMPTZ  NOT NULL,
                               revocado     BOOLEAN      NOT NULL DEFAULT FALSE,
                               creado_en    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_usuario_email        ON usuario(email);
CREATE INDEX idx_perfil_usuario       ON perfil(usuario_id);
CREATE INDEX idx_consentimiento_user  ON consentimiento(usuario_id);
CREATE INDEX idx_refresh_token_token  ON refresh_token(token);
CREATE INDEX idx_refresh_token_user   ON refresh_token(usuario_id) WHERE revocado = FALSE;
