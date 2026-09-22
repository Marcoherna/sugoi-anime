# SugoiAnime (すごいアニメ)
### Plataforma de Streaming de Anime Basada en Microservicios

![Java 25](https://img.shields.io/badge/Java-25-orange.svg)
![Spring Boot 4.1.1](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)
![Spring Cloud 2025.1.3](https://img.shields.io/badge/Spring%20Cloud-2025.1.3-blue.svg)
![React 19](https://img.shields.io/badge/React-19.2-61DAFB.svg?logo=react)
![Vite 8](https://img.shields.io/badge/Vite-8.2-646CFF.svg?logo=vite)
![PostgreSQL 17](https://img.shields.io/badge/PostgreSQL-17-336791.svg?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?logo=docker)
![Resilience4j](https://img.shields.io/badge/Resilience4j-Circuit%20Breaker-red.svg)

**SugoiAnime** es una plataforma moderna de streaming y catálogo de anime diseñada bajo una **arquitectura de microservicios** distribuida, altamente escalable, resiliente y orientada al estricto cumplimiento normativo y legal chileno. 

El sistema incluye reproducción de video streaming, sincronización externa con metadatos de anime, gestión de identidades y perfiles familiares, seguimiento de progreso en tiempo real, favoritos y un **motor de recomendaciones personalizadas** basado en aprendizaje de contenido (Similitud de Coseno).

---

## 🏗️ Arquitectura del Sistema

El sistema implementa una arquitectura desacoplada orientada a microservicios:

```mermaid
flowchart TB
    subgraph Cliente["Navegador Web / Cliente"]
        UI["Frontend SPA (React 19 + Vite)<br>Puerto: 5173 / 80"]
    end

    subgraph Perimetro["Perímetro de Seguridad (Ley 21.459)"]
        GW["API Gateway (Spring Cloud Gateway WebFlux)<br>Puerto: 8080<br>• Validación JWT Centralizada<br>• Circuit Breakers & TimeLimiters<br>• Swagger UI Unificado<br>• CORS Global"]
    end

    subgraph RedInterna["Red Docker Interna (sugoi-net)"]
        MS_AUTH["ms-auth (:8082)<br>• Cuentas y Roles<br>• Perfiles (RF-09)<br>• Consentimiento (Ley 19.628)<br>• JWT & Refresh Tokens"]
        MS_CAT["ms-catalogo (:8081)<br>• Catálogo & Búsqueda<br>• Streaming & Licencias (Ley 17.336)<br>• Sync Jikan (RF-04)<br>• Traducción (RF-05)"]
        MS_INT["ms-interacciones (:8083)<br>• Progreso & Continuar viendo<br>• Favoritos (RF-06)<br>• Motor Recomendaciones (RF-07)"]
        MS_PAG["ms-pagos (:8084)<br>• Suscripciones Premium<br>• Eventos Asíncronos"]

        DB_AUTH[("db-auth<br>PostgreSQL 17<br>sugoi_auth")]
        DB_CAT[("db-catalogo<br>PostgreSQL 17<br>sugoi_catalogo")]
        DB_INT[("db-interacciones<br>PostgreSQL 17<br>sugoi_interacciones")]
        RABBIT[("RabbitMQ 4<br>Mensajería Asíncrona")]
    end

    subgraph Externos["Servicios Externos y Cloud"]
        JIKAN["Jikan API v4<br>(MyAnimeList)"]
        TRANS["LibreTranslate API"]
        CLOUDFRONT["AWS CloudFront / S3<br>(CDN Multimedia)"]
    end

    UI -->|HTTP / REST| GW
    GW -->|/api/auth/**<br>/api/perfiles/**| MS_AUTH
    GW -->|/api/catalogo/**| MS_CAT
    GW -->|/api/interacciones/**| MS_INT

    MS_AUTH --> DB_AUTH
    MS_CAT --> DB_CAT
    MS_INT --> DB_INT
    MS_PAG --> RABBIT

    MS_INT -.->|HTTP RestClient| MS_CAT
    MS_INT -.->|HTTP RestClient| MS_AUTH

    MS_CAT -->|Sincronización| JIKAN
    MS_CAT -->|Traducción| TRANS
    MS_CAT -.->|URLs CDN| CLOUDFRONT
```

---

## 📦 Mapa de Microservicios y Componentes

| Servicio / Contenedor | Puerto Interno (Docker) | Puerto Host (Dev) | Base de Datos / Storage | Responsabilidad Principal |
|---|:---:|:---:|---|---|
| **`gateway`** | `8080` | `8080` | N/A (Stateless) | Punto de entrada unificado, enrutamiento reactivo, filtro perimetral JWT, Resilience4j Circuit Breakers, CORS y documentación Swagger agregada. |
| **`ms-auth`** | `8082` | `5433` *(BD)* | `sugoi_auth` (PostgreSQL 17) | Gestión de identidades, emisión/rotación de tokens JWT, perfiles por cuenta y auditoría de consentimiento de datos personales. |
| **`ms-catalogo`** | `8081` | `5434` *(BD)* | `sugoi_catalogo` (PostgreSQL 17) | Catálogo de anime, géneros, gestión de episodios, streaming CDN y sincronización externa con Jikan. |
| **`ms-interacciones`** | `8083` | `5435` *(BD)* | `sugoi_interacciones` (PostgreSQL 17) | Registro de reproducciones, favoritos de usuarios y motor algebraico de recomendaciones personalizadas. |
| **`ms-pagos`** | `8084` | `5672 / 15672` *(MQ)* | RabbitMQ | Módulo para eventos asíncronos de membresías y suscripciones premium. |
| **`frontend`** | `80` | `5173` | Nginx / React SPA | Interfaz de usuario rica, reproductor HTML5 con control de posición y catálogo interactivo. |

---

## 🛠️ Stack Tecnológico

### Backend
- **Lenguaje:** Java 25 con hilos virtuales habilitados (*Project Loom / Virtual Threads*) para alto rendimiento en I/O bloqueante.
- **Framework Base:** Spring Boot 4.1.1.
- **Cloud & Resiliencia:** Spring Cloud 2025.1.3, Spring Cloud Gateway Server WebFlux, Resilience4j (CircuitBreaker y TimeLimiter).
- **Seguridad:** Spring Security con OAuth2 Resource Server (stateless JWT HMAC-SHA256).
- **Persistencia & ORM:** Spring Data JPA, Hibernate, HikariCP, Flyway para versionado de esquemas.
- **Caché:** Caffeine Cache local de alto rendimiento.
- **Mensajería:** Spring AMQP + RabbitMQ 4.
- **Documentación de API:** SpringDoc OpenAPI 3.1.0 / Swagger UI.

### Frontend
- **Librería Core:** React 19 + ReactDOM 19.
- **Enrutamiento:** React Router DOM v7.
- **Tooling & Bundler:** Vite 8.
- **Estilos:** Vanilla CSS modular con variables de diseño, modo oscuro cinematográfico y diseño responsivo.
- **Servidor Web Producción:** Nginx Alpine.

### Infraestructura y Datos
- **Bases de Datos:** PostgreSQL 17 Alpine (3 instancias independientes).
- **Contenedores:** Docker & Docker Compose con builds multi-etapa (*multi-stage builds*).

---
