package cl.sugoianime.msinteracciones.service;

import cl.sugoianime.msinteracciones.client.CatalogoClient;
import cl.sugoianime.msinteracciones.client.dto.AnimeResumenExterno;
import cl.sugoianime.msinteracciones.domain.Recomendacion;
import cl.sugoianime.msinteracciones.dto.RecomendacionDTO;
import cl.sugoianime.msinteracciones.repository.FavoritoRepository;
import cl.sugoianime.msinteracciones.repository.ProgresoRepository;
import cl.sugoianime.msinteracciones.repository.RecomendacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Motor de recomendaciones por filtrado basado en contenido (RF-07).
 *
 * ============================ EL ALGORITMO ============================
 *
 * 1. VECTORIZACION. Cada anime se representa como un vector binario
 *    sobre el espacio de generos. Con generos {Accion, Comedia, Drama,
 *    Fantasia}, un anime de Accion y Drama es [1, 0, 1, 0].
 *
 * 2. PERFIL DE GUSTOS. Se suman los vectores de todos los animes con
 *    los que el perfil interactuo (favoritos y episodios completados).
 *    Si vio dos de Accion y uno de Drama, el perfil es [2, 0, 1, 0].
 *    El vector deja de ser binario y pasa a expresar INTENSIDAD de
 *    preferencia, que es justo lo que queremos.
 *
 * 3. SIMILITUD DE COSENO. Mide el angulo entre el vector del perfil y
 *    el de cada anime candidato:
 *
 *        cos(A,B) = (A · B) / (||A|| × ||B||)
 *
 *    Se usa coseno y no distancia euclidiana porque nos interesa la
 *    DIRECCION del gusto, no su magnitud. Alguien que vio 3 animes y
 *    alguien que vio 300 con las mismas proporciones de genero deben
 *    obtener recomendaciones equivalentes. La distancia euclidiana los
 *    trataria como muy distintos; el coseno los considera identicos.
 *
 * 4. FILTRADO Y ORDEN. Se excluyen los animes ya vistos y se devuelven
 *    los N con mayor puntaje.
 *
 * ======================= POR QUE ESTE ENFOQUE =========================
 *
 * El filtrado colaborativo ("usuarios como tu vieron...") suele dar
 * mejores resultados, pero sufre arranque en frio: sin miles de usuarios
 * no hay senal. En un proyecto academico con pocos usuarios de prueba,
 * el filtrado por contenido funciona desde el primer usuario y produce
 * recomendaciones EXPLICABLES: podemos decir exactamente por que se
 * recomendo cada anime.
 *
 * ======================== LEY 19.628 ==================================
 *
 * El historial solo se procesa si el usuario otorgo el consentimiento
 * especifico HISTORIAL_RECOMENDACIONES durante el registro.
 */
@Service
public class RecomendacionService {

    private static final Logger log = LoggerFactory.getLogger(RecomendacionService.class);

    private final RecomendacionRepository recomendacionRepository;
    private final FavoritoRepository favoritoRepository;
    private final ProgresoRepository progresoRepository;
    private final CatalogoClient catalogoClient;

    @Value("${recomendaciones.top-n}")
    private int topN;

    @Value("${recomendaciones.minimo-interacciones}")
    private int minimoInteracciones;

    public RecomendacionService(RecomendacionRepository recomendacionRepository,
                                FavoritoRepository favoritoRepository,
                                ProgresoRepository progresoRepository,
                                CatalogoClient catalogoClient) {
        this.recomendacionRepository = recomendacionRepository;
        this.favoritoRepository = favoritoRepository;
        this.progresoRepository = progresoRepository;
        this.catalogoClient = catalogoClient;
    }

    // =================================================================
    // NUCLEO MATEMATICO
    // =================================================================

    /**
     * Similitud de coseno entre dos vectores.
     *
     * @return valor entre 0 (sin relacion) y 1 (misma direccion)
     */
    public static double similitudCoseno(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Los vectores deben tener igual dimension");
        }

        double producto = 0.0;
        double normaA = 0.0;
        double normaB = 0.0;

        for (int i = 0; i < a.length; i++) {
            producto += a[i] * b[i];
            normaA   += a[i] * a[i];
            normaB   += b[i] * b[i];
        }

        // Un vector nulo no tiene direccion: la similitud es 0, no NaN.
        if (normaA == 0 || normaB == 0) {
            return 0.0;
        }

        return producto / (Math.sqrt(normaA) * Math.sqrt(normaB));
    }

    /** Construye el vector binario de generos de un anime. */
    public static double[] vectorizarAnime(AnimeResumenExterno anime,
                                           Map<Long, Integer> indiceGeneros) {
        double[] vector = new double[indiceGeneros.size()];

        for (var genero : anime.generosSeguro()) {
            Integer indice = indiceGeneros.get(genero.id());
            if (indice != null) {
                vector[indice] = 1.0;
            }
        }
        return vector;
    }

    /** Suma los vectores de los animes con los que el perfil interactuo. */
    public static double[] construirVectorPerfil(List<AnimeResumenExterno> interactuados,
                                                 Map<Long, Integer> indiceGeneros) {
        double[] perfil = new double[indiceGeneros.size()];

        for (AnimeResumenExterno anime : interactuados) {
            double[] vector = vectorizarAnime(anime, indiceGeneros);
            for (int i = 0; i < perfil.length; i++) {
                perfil[i] += vector[i];
            }
        }
        return perfil;
    }

    // =================================================================
    // GENERACION
    // =================================================================

    /**
     * Recalcula las recomendaciones de un perfil.
     *
     * Se ejecuta en el job nocturno, nunca durante un request: traer el
     * catalogo completo y calcular similitudes contra cada anime tomaria
     * cientos de milisegundos y arruinaria la latencia del home.
     */
    @Transactional
    public int generarParaPerfil(Long perfilId, List<AnimeResumenExterno> catalogo,
                                 Map<Long, Integer> indiceGeneros) {

        // 1. Animes con los que el perfil interactuo
        Set<Long> idsInteractuados = new HashSet<>();
        idsInteractuados.addAll(favoritoRepository.findAnimeIdsByPerfilId(perfilId));
        idsInteractuados.addAll(progresoRepository.findAnimeIdsVistos(perfilId));

        if (idsInteractuados.size() < minimoInteracciones) {
            log.debug("Perfil {} con solo {} interacciones: senal insuficiente",
                    perfilId, idsInteractuados.size());
            return 0;
        }

        Map<Long, AnimeResumenExterno> porId = catalogo.stream()
                .collect(Collectors.toMap(AnimeResumenExterno::id, a -> a, (x, y) -> x));

        List<AnimeResumenExterno> interactuados = idsInteractuados.stream()
                .map(porId::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (interactuados.isEmpty()) {
            return 0;
        }

        // 2. Vector de gustos del perfil
        double[] vectorPerfil = construirVectorPerfil(interactuados, indiceGeneros);

        // 3. Similitud contra cada candidato no visto
        record Candidato(Long animeId, double puntaje) {}

        List<Candidato> candidatos = new ArrayList<>();

        for (AnimeResumenExterno anime : catalogo) {
            if (idsInteractuados.contains(anime.id())) {
                continue;   // no recomendar lo ya visto
            }
            double puntaje = similitudCoseno(
                    vectorPerfil, vectorizarAnime(anime, indiceGeneros));

            if (puntaje > 0) {
                candidatos.add(new Candidato(anime.id(), puntaje));
            }
        }

        // 4. Top N
        List<Candidato> mejores = candidatos.stream()
                .sorted(Comparator.comparingDouble(Candidato::puntaje).reversed())
                .limit(topN)
                .toList();

        // Reemplazo completo: las recomendaciones anteriores quedan obsoletas
        recomendacionRepository.deleteByPerfilId(perfilId);

        List<Recomendacion> aGuardar = new ArrayList<>();
        for (int i = 0; i < mejores.size(); i++) {
            aGuardar.add(Recomendacion.builder()
                    .perfilId(perfilId)
                    .animeId(mejores.get(i).animeId())
                    .puntaje(mejores.get(i).puntaje())
                    .posicion(i + 1)
                    .build());
        }
        recomendacionRepository.saveAll(aGuardar);

        log.info("Recomendaciones generadas para el perfil {}: {}", perfilId, aGuardar.size());
        return aGuardar.size();
    }

    /** Job nocturno: recalcula para todos los perfiles con actividad. */
    @Scheduled(cron = "${recomendaciones.cron}")
    public void generarParaTodos() {
        log.info("Iniciando generacion masiva de recomendaciones");

        List<AnimeResumenExterno> catalogo = catalogoClient.obtenerCatalogoCompleto();

        if (catalogo.isEmpty()) {
            log.warn("Catalogo vacio o ms-catalogo no disponible, se aborta");
            return;
        }

        Map<Long, Integer> indiceGeneros = construirIndiceGeneros(catalogo);
        log.info("Espacio vectorial: {} generos", indiceGeneros.size());

        Set<Long> perfiles = new HashSet<>(progresoRepository.findPerfilesConActividad());
        int total = 0;

        for (Long perfilId : perfiles) {
            try {
                total += generarParaPerfil(perfilId, catalogo, indiceGeneros);
            } catch (Exception ex) {
                // Un perfil problematico no debe abortar el lote completo
                log.error("Fallo al generar recomendaciones del perfil {}", perfilId, ex);
            }
        }

        log.info("Generacion finalizada: {} recomendaciones en {} perfiles",
                total, perfiles.size());
    }

    /** Asigna un indice de vector a cada genero presente en el catalogo. */
    public static Map<Long, Integer> construirIndiceGeneros(
            List<AnimeResumenExterno> catalogo) {

        List<Long> ids = catalogo.stream()
                .flatMap(a -> a.generosSeguro().stream())
                .map(AnimeResumenExterno.GeneroExterno::id)
                .distinct()
                .sorted()
                .toList();

        Map<Long, Integer> indice = new java.util.HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            indice.put(ids.get(i), i);
        }
        return indice;
    }

    // =================================================================
    // CONSULTA
    // =================================================================

    @Transactional(readOnly = true)
    public List<RecomendacionDTO> obtener(Long perfilId) {

        List<Recomendacion> recomendaciones =
                recomendacionRepository.findByPerfilIdOrderByPosicionAsc(perfilId);

        if (recomendaciones.isEmpty()) {
            return List.of();
        }

        List<Long> ids = recomendaciones.stream().map(Recomendacion::getAnimeId).toList();
        Map<Long, AnimeResumenExterno> animes = catalogoClient.obtenerAnimes(ids);

        return recomendaciones.stream()
                .map(r -> {
                    AnimeResumenExterno anime = animes.get(r.getAnimeId());
                    if (anime == null) {
                        return null;
                    }
                    return new RecomendacionDTO(
                            r.getAnimeId(), anime.titulo(), anime.anio(), anime.puntuacion(),
                            anime.imagenUrl(), redondear(r.getPuntaje()),
                            anime.generosSeguro().stream()
                                    .map(AnimeResumenExterno.GeneroExterno::nombre).toList());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /** Recalculo bajo demanda, para demostraciones. */
    @Transactional
    public int regenerar(Long perfilId) {
        List<AnimeResumenExterno> catalogo = catalogoClient.obtenerCatalogoCompleto();
        if (catalogo.isEmpty()) {
            return 0;
        }
        return generarParaPerfil(perfilId, catalogo, construirIndiceGeneros(catalogo));
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10_000.0) / 10_000.0;
    }
}
