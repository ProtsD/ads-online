FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /ads-online
RUN git clone https://github.com/ProtsD/ads-online.git .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /ads-online
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]