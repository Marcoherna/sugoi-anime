# =====================================================================
# Etapa 1: compilacion
# =====================================================================
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /build

# Copiamos primero solo el pom para aprovechar el cache de capas de Docker:
# si el pom no cambia, las dependencias no se vuelven a descargar.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# =====================================================================
# Etapa 2: extraccion de capas
# OJO BOOT 4.1: el jarmode "layertools" fue ELIMINADO.
# Ahora se usa "tools ... extract --layers".
# =====================================================================
FROM eclipse-temurin:25-jdk-alpine AS extractor
WORKDIR /jar
COPY --from=builder /build/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination /extracted

# =====================================================================
# Etapa 3: runtime
# =====================================================================
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Orden intencional: de lo que menos cambia a lo que mas cambia.
COPY --from=extractor --chown=spring:spring /extracted/dependencies/ ./
COPY --from=extractor --chown=spring:spring /extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=spring:spring /extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=spring:spring /extracted/application/ ./

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -q --spider http://localhost:8081/actuator/health/liveness || exit 1

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseZGC", \
  "org.springframework.boot.loader.launch.JarLauncher"]
