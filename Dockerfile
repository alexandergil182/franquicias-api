# ---- Etapa 1: compilar con Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Primero solo el pom: así Docker cachea las dependencias si el código cambia pero el pom no
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Etapa 2: imagen final, solo con el JRE y el jar ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# No se ejecuta como root
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/target/franquicias-api-*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
