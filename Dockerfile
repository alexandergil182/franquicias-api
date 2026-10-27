# ---- Etapa 1: compilar con Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Primero solo el pom: así Docker cachea las dependencias si el código cambia pero el pom no
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Etapa 2: imagen final, solo con el JRE y el jar ----
# Imagen basada en Ubuntu: funciona en Mac con chip Apple (arm64) y en Intel (amd64)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# curl para el healthcheck de docker compose
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# No se ejecuta como root
RUN groupadd --system app && useradd --system --gid app app
USER app

COPY --from=build /app/target/franquicias-api-*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
