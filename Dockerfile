FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

COPY pom.xml .
COPY libra-core/pom.xml libra-core/
COPY libra-api/pom.xml libra-api/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline -pl libra-api -am

COPY libra-core/src libra-core/src
COPY libra-api/src libra-api/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q package -pl libra-api -am -DskipTests \
 && cp libra-api/target/libra-api-*.jar /app.jar


FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 libra
WORKDIR /app
COPY --from=build /app.jar app.jar
USER libra
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
