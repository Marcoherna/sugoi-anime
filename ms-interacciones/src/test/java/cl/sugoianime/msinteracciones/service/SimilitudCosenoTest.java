package cl.sugoianime.msinteracciones.service;

import cl.sugoianime.msinteracciones.client.dto.AnimeResumenExterno;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static cl.sugoianime.msinteracciones.service.RecomendacionService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Motor de recomendaciones - algoritmo de similitud (RF-07)")
class SimilitudCosenoTest {

    private static final double TOLERANCIA = 0.0001;

    @Nested
    @DisplayName("Similitud de coseno")
    class Coseno {

        @Test
        @DisplayName("vectores identicos dan similitud 1")
        void vectoresIdenticos() {
            double[] a = {1, 0, 1, 0};
            assertThat(similitudCoseno(a, a)).isCloseTo(1.0, within());
        }

        @Test
        @DisplayName("vectores ortogonales dan similitud 0")
        void vectoresOrtogonales() {
            double[] a = {1, 0, 0, 0};
            double[] b = {0, 1, 0, 0};
            assertThat(similitudCoseno(a, b)).isCloseTo(0.0, within());
        }

        @Test
        @DisplayName("caso calculado a mano: [1,1,0] vs [1,0,0] = 0.7071")
        void casoCalculadoAMano() {
            // producto punto = 1*1 + 1*0 + 0*0 = 1
            // ||a|| = sqrt(2), ||b|| = 1
            // cos = 1 / (sqrt(2) * 1) = 0.70710678...
            double[] a = {1, 1, 0};
            double[] b = {1, 0, 0};
            assertThat(similitudCoseno(a, b)).isCloseTo(0.7071, within());
        }

        @Test
        @DisplayName("la magnitud no altera el resultado: es la propiedad clave del coseno")
        void magnitudNoImporta() {
            double[] pocoVisto  = {1, 1, 0};
            double[] muchoVisto = {100, 100, 0};
            double[] candidato  = {1, 0, 1};

            // Mismo gusto proporcional -> misma recomendacion,
            // sin importar cuantos animes vio cada uno.
            assertThat(similitudCoseno(pocoVisto, candidato))
                    .isCloseTo(similitudCoseno(muchoVisto, candidato), within());
        }

        @Test
        @DisplayName("un vector nulo da 0, nunca NaN")
        void vectorNulo() {
            double[] nulo = {0, 0, 0};
            double[] otro = {1, 1, 0};

            double resultado = similitudCoseno(nulo, otro);

            assertThat(resultado).isZero();
            assertThat(Double.isNaN(resultado)).isFalse();
        }

        @Test
        @DisplayName("dimensiones distintas lanzan excepcion")
        void dimensionesDistintas() {
            assertThatThrownBy(() ->
                    similitudCoseno(new double[]{1, 0}, new double[]{1, 0, 1}))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("el resultado siempre esta entre 0 y 1 con vectores no negativos")
        void rangoAcotado() {
            double[] a = {3, 1, 0, 2};
            double[] b = {1, 1, 1, 0};
            assertThat(similitudCoseno(a, b)).isBetween(0.0, 1.0);
        }
    }

    @Nested
    @DisplayName("Vectorizacion")
    class Vectorizacion {

        private final Map<Long, Integer> indice = Map.of(1L, 0, 2L, 1, 4L, 2, 8L, 3);

        private AnimeResumenExterno animeCon(Long... generoIds) {
            return new AnimeResumenExterno(99L, "Test", 2024, null, null,
                    List.of(generoIds).stream()
                            .map(id -> new AnimeResumenExterno.GeneroExterno(id, "G" + id))
                            .toList());
        }

        @Test
        @DisplayName("marca con 1 solo las posiciones de sus generos")
        void vectorizaCorrectamente() {
            double[] vector = vectorizarAnime(animeCon(1L, 8L), indice);
            assertThat(vector).containsExactly(1.0, 0.0, 0.0, 1.0);
        }

        @Test
        @DisplayName("un anime sin generos da un vector nulo")
        void animeSinGeneros() {
            double[] vector = vectorizarAnime(animeCon(), indice);
            assertThat(vector).containsExactly(0.0, 0.0, 0.0, 0.0);
        }

        @Test
        @DisplayName("ignora generos que no estan en el indice")
        void generoDesconocido() {
            double[] vector = vectorizarAnime(animeCon(1L, 999L), indice);
            assertThat(vector).containsExactly(1.0, 0.0, 0.0, 0.0);
        }

        @Test
        @DisplayName("el vector del perfil acumula intensidad de preferencia")
        void perfilAcumula() {
            double[] perfil = construirVectorPerfil(
                    List.of(animeCon(1L), animeCon(1L), animeCon(2L)), indice);

            // Dos animes del genero 1 y uno del genero 2
            assertThat(perfil).containsExactly(2.0, 1.0, 0.0, 0.0);
        }
    }

    @Nested
    @DisplayName("Escenario realista")
    class Escenario {

        @Test
        @DisplayName("recomienda mejor el anime que comparte mas generos con el perfil")
        void ordenPorAfinidad() {
            // Espacio: [Accion, Drama, Comedia, Romance]
            Map<Long, Integer> indice = Map.of(1L, 0, 8L, 1, 4L, 2, 22L, 3);

            // Al usuario le gusta Accion + Drama
            double[] perfil = {2.0, 2.0, 0.0, 0.0};

            double[] accionDrama   = {1, 1, 0, 0};   // muy afin
            double[] soloAccion    = {1, 0, 0, 0};   // parcialmente afin
            double[] comediaRomance = {0, 0, 1, 1};  // nada afin

            double s1 = similitudCoseno(perfil, accionDrama);
            double s2 = similitudCoseno(perfil, soloAccion);
            double s3 = similitudCoseno(perfil, comediaRomance);

            assertThat(s1).isGreaterThan(s2);
            assertThat(s2).isGreaterThan(s3);
            assertThat(s3).isZero();
            assertThat(s1).isCloseTo(1.0, within());
        }
    }

    private static org.assertj.core.data.Offset<Double> within() {
        return org.assertj.core.data.Offset.offset(TOLERANCIA);
    }
}
