
FROM eclipse-temurin:11-jdk
ARG PROJECT_VERSION=0.1.0
RUN mkdir -p /home/app
WORKDIR /home/app
COPY target/service-discovery-v${PROJECT_VERSION}.jar service-discovery.jar
EXPOSE 8761
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev} -jar service-discovery.jar"]


