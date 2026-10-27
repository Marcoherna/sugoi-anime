FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:25-jdk-alpine AS extractor
WORKDIR /jar
COPY --from=builder /build/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination /extracted

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=extractor --chown=spring:spring /extracted/dependencies/ ./
COPY --from=extractor --chown=spring:spring /extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=spring:spring /extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=spring:spring /extracted/application/ ./

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -q --spider http://localhost:8083/actuator/health/liveness || exit 1

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseZGC", \
  "org.springframework.boot.loader.launch.JarLauncher"]
