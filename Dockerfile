FROM gradle:8.7-jdk21 AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Xmx2g", "-Xms2g", "-Duser.timezone=Asia/Seoul", "app.jar"]