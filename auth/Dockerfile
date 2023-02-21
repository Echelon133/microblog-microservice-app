FROM ghcr.io/graalvm/jdk:java17
RUN groupadd spring && useradd -g spring spring
USER spring
WORKDIR /app

EXPOSE 80

ADD build/libs/auth-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar" ]