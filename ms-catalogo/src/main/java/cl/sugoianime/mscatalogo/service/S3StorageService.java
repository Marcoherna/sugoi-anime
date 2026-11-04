package cl.sugoianime.mscatalogo.service;

import cl.sugoianime.mscatalogo.dto.S3ObjetoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Integracion nativa con AWS S3 mediante firma AWS Signature Version 4 (HMAC-SHA256).
 * Permite listar objetos del bucket y subir nuevos archivos de video sin dependencias pesadas.
 */
@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);
    private static final DateTimeFormatter AMZ_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_STAMP_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final StreamingService streamingService;
    private final HttpClient httpClient;

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.s3-bucket:}")
    private String bucket;

    @Value("${aws.access-key-id:}")
    private String accessKeyId;

    @Value("${aws.secret-access-key:}")
    private String secretAccessKey;

    public S3StorageService(StreamingService streamingService) {
        this.streamingService = streamingService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean estaConfigurado() {
        return bucket != null && !bucket.isBlank()
                && accessKeyId != null && !accessKeyId.isBlank()
                && secretAccessKey != null && !secretAccessKey.isBlank();
    }

    /** Lista los archivos de video disponibles en el bucket S3. */
    public List<S3ObjetoDTO> listarVideos() {
        if (!estaConfigurado()) {
            return List.of();
        }

        try {
            String host = bucket + ".s3." + region + ".amazonaws.com";
            String query = "list-type=2";
            Instant ahora = Instant.now();
            String amzDate = AMZ_DATE_FMT.format(ahora);
            String dateStamp = DATE_STAMP_FMT.format(ahora);
            String payloadHash = sha256Hex(new byte[0]);

            String canonicalHeaders = "host:" + host + "\n"
                    + "x-amz-content-sha256:" + payloadHash + "\n"
                    + "x-amz-date:" + amzDate + "\n";
            String signedHeaders = "host;x-amz-content-sha256;x-amz-date";

            String canonicalRequest = "GET\n/\n" + query + "\n"
                    + canonicalHeaders + "\n" + signedHeaders + "\n" + payloadHash;

            String authHeader = construirAuthorizationHeader(
                    amzDate, dateStamp, signedHeaders, canonicalRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + host + "/?" + query))
                    .header("x-amz-date", amzDate)
                    .header("x-amz-content-sha256", payloadHash)
                    .header("Authorization", authHeader)
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                log.warn("S3 ListObjects devolvio estado {}", response.statusCode());
                return List.of();
            }

            return parsearXmlObjetos(response.body());
        } catch (Exception ex) {
            log.warn("No se pudo listar objetos de S3: {}", ex.getMessage());
            return List.of();
        }
    }

    /** Sube un archivo de video a S3 y devuelve los metadatos del objeto creado. */
    public S3ObjetoDTO subirVideo(MultipartFile archivo, String carpeta) {
        if (!estaConfigurado()) {
            throw new IllegalArgumentException(
                    "Las credenciales de AWS S3 no estan configuradas en el servidor");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo de video esta vacio");
        }

        try {
            String nombreOriginal = archivo.getOriginalFilename() == null
                    ? "episodio.mp4" : archivo.getOriginalFilename();
            String nombreLimpio = nombreOriginal.replaceAll("[^a-zA-Z0-9._-]", "_");
            String prefijo = (carpeta == null || carpeta.isBlank())
                    ? "demo" : carpeta.replaceAll("^/+|/+$", "");
            String objectKey = prefijo + "/" + nombreLimpio;

            byte[] bytes = archivo.getBytes();
            String contentType = archivo.getContentType() != null
                    ? archivo.getContentType() : "video/mp4";

            String host = bucket + ".s3." + region + ".amazonaws.com";
            String canonicalUri = "/" + Arrays.stream(objectKey.split("/"))
                    .map(seg -> URLEncoder.encode(seg, StandardCharsets.UTF_8).replace("+", "%20"))
                    .collect(Collectors.joining("/"));

            Instant ahora = Instant.now();
            String amzDate = AMZ_DATE_FMT.format(ahora);
            String dateStamp = DATE_STAMP_FMT.format(ahora);
            String payloadHash = sha256Hex(bytes);

            String canonicalHeaders = "content-type:" + contentType + "\n"
                    + "host:" + host + "\n"
                    + "x-amz-content-sha256:" + payloadHash + "\n"
                    + "x-amz-date:" + amzDate + "\n";
            String signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date";

            String canonicalRequest = "PUT\n" + canonicalUri + "\n\n"
                    + canonicalHeaders + "\n" + signedHeaders + "\n" + payloadHash;

            String authHeader = construirAuthorizationHeader(
                    amzDate, dateStamp, signedHeaders, canonicalRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + host + canonicalUri))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", contentType)
                    .header("x-amz-date", amzDate)
                    .header("x-amz-content-sha256", payloadHash)
                    .header("Authorization", authHeader)
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Fallo subida a S3 ({}): {}", response.statusCode(), response.body());
                throw new IllegalArgumentException(
                        "AWS S3 rechazo la subida con codigo HTTP " + response.statusCode());
            }

            log.info("Video subido exitosamente a S3: key={} ({} bytes)", objectKey, bytes.length);
            return new S3ObjetoDTO(
                    objectKey,
                    bytes.length,
                    ahora.toString(),
                    streamingService.construirUrlPublica(objectKey)
            );
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error al subir archivo a S3", ex);
            throw new IllegalArgumentException("No se pudo subir el archivo a S3: " + ex.getMessage());
        }
    }

    private List<S3ObjetoDTO> parsearXmlObjetos(byte[] xmlBytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes));
        NodeList contents = doc.getElementsByTagName("Contents");
        List<S3ObjetoDTO> resultado = new ArrayList<>();

        for (int i = 0; i < contents.getLength(); i++) {
            Element el = (Element) contents.item(i);
            String key = textoElemento(el, "Key");
            long size = Long.parseLong(textoElemento(el, "Size"));
            String lastMod = textoElemento(el, "LastModified");

            // Filtramos archivos reales (> 1 KB) para omitir placeholders vacios
            if (key != null && !key.endsWith("/") && size > 1024) {
                resultado.add(new S3ObjetoDTO(
                        key,
                        size,
                        lastMod,
                        streamingService.construirUrlPublica(key)
                ));
            }
        }
        return resultado;
    }

    private String textoElemento(Element padre, String tag) {
        NodeList list = padre.getElementsByTagName(tag);
        return list.getLength() > 0 ? list.item(0).getTextContent() : "";
    }

    private String construirAuthorizationHeader(String amzDate, String dateStamp,
                                                String signedHeaders, String canonicalRequest) throws Exception {
        String algorithm = "AWS4-HMAC-SHA256";
        String credentialScope = dateStamp + "/" + region + "/s3/aws4_request";
        String stringToSign = algorithm + "\n" + amzDate + "\n" + credentialScope + "\n"
                + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

        byte[] kSecret = ("AWS4" + secretAccessKey).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSha256(kSecret, dateStamp);
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, "s3");
        byte[] kSigning = hmacSha256(kService, "aws4_request");
        String signature = HexFormat.of().formatHex(hmacSha256(kSigning, stringToSign));

        return algorithm + " Credential=" + accessKeyId + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + signature;
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data));
    }
}

