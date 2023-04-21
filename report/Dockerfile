FROM ghcr.io/graalvm/jdk:java17
RUN groupadd spring
RUN useradd -g spring spring
USER spring
EXPOSE 80
WORKDIR /app
ADD build/libs/report-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar" ]