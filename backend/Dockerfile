# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY settings-docker.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn -B -s /root/.m2/settings.xml dependency:go-offline
COPY src ./src
RUN mvn -B -s /root/.m2/settings.xml clean package -DskipTests

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/coldchain-1.0.0.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=postgres
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
