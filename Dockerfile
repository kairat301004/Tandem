# ============================
#   1) Stage: Build
# ============================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Сначала копируем pom — так кешируются зависимости
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Потом копируем весь проект
COPY . .

# Сборка jar
RUN mvn -q -e -DskipTests package

# ============================
#   2) Stage: Runtime
# ============================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Параметры JVM (можешь менять)
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Копируем jar из builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]