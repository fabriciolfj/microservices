FROM adoptopenjdk/openjdk11

EXPOSE 9999

ADD ./build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]