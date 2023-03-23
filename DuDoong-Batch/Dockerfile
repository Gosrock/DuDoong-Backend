FROM openjdk:17-alpine

EXPOSE 8080

COPY ./build/libs/*.jar app.jar
ARG PROFILE=dev
ENV PROFILE=${PROFILE}

ENTRYPOINT ["java","-Dspring.profiles.active=${PROFILE}", "-Djava.security.egd=file:/dev/./urandom","-jar","-Duser.timezone=Asia/Seoul","/app.jar"]