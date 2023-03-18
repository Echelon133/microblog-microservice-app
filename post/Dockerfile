FROM ghcr.io/graalvm/jdk:java17
RUN groupadd spring
RUN useradd -g spring spring
USER spring

EXPOSE 80

WORKDIR /app

ADD build/libs/post-0.0.1-SNAPSHOT.jar application.jar

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=$PROFILE -jar /app/application.jar" ]
