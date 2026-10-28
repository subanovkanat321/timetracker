# Сборка: Maven Wrapper внутри образа, тесты не запускаются — им нужен Docker
# (Testcontainers), которого внутри docker build нет. Тесты гоняет ./mvnw verify и CI.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Зависимости отдельным слоем: при изменении кода они не скачиваются заново
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=build /src/target/timetracker-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
