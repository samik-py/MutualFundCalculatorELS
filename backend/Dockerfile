# ---- Build stage ----------------------------------------------------------
# Use a JDK image with Maven preinstalled to compile the Spring Boot jar.
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy only the POM first so Docker can cache the dependency layer
# whenever source files change but pom.xml does not.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# Now copy the source and build the executable jar.
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Runtime stage --------------------------------------------------------
# Slim JRE-only image keeps the final container small.
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the fat jar produced by the Spring Boot Maven plugin.
COPY --from=build /app/target/*.jar app.jar

# Render (and most PaaS providers) inject a PORT env var that the app
# must bind to. application.properties already reads ${PORT:8080}.
EXPOSE 8080

# Persist the H2 file DB inside /app/data so it survives within a single
# container lifetime. Note: Render's free tier wipes this on every redeploy.
RUN mkdir -p /app/data
VOLUME ["/app/data"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
