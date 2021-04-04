# Buster aka Debian 10 50051:50051
FROM openjdk:8-jdk-buster as build

WORKDIR /tmp

COPY gradle gradle
COPY src src
COPY build.gradle .
COPY settings.gradle .
COPY gradlew .
COPY gradlew.bat .

RUN chmod +x ./gradlew
RUN ./gradlew build -x test
RUN ./gradlew shadowJar

# Copy and run uber jar

FROM openjdk:8-jre-buster
COPY --from=build tmp/build/libs/account-search-service.jar /app/account-search-service.jar
EXPOSE 50051
ENTRYPOINT ["java", "-jar", "/app/account-search-service.jar"]

