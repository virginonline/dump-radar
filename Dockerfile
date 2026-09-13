ARG JAVA_VERSION=21
ARG GRADLE_VERSION=9.7.1

FROM gradle:${GRADLE_VERSION}-jdk${JAVA_VERSION} AS build
WORKDIR /app
COPY --chown=gradle:gradle build.gradle.kts settings.gradle.kts gradle.properties ./
RUN gradle dependencies --no-daemon || true
COPY --chown=gradle:gradle src ./src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:${JAVA_VERSION}-jdk AS layers
WORKDIR /app
COPY --from=build app/build/libs/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:${JAVA_VERSION}-jre AS runner
WORKDIR /app
COPY --from=layers app/extracted/dependencies/ ./
COPY --from=layers app/extracted/snapshot-dependencies/ ./
COPY --from=layers app/extracted/spring-boot-loader/ ./
COPY --from=layers app/extracted/application/ ./
VOLUME /app/data
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]