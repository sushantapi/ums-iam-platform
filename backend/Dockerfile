# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk AS build

ENV MAVEN_OPTS="-Djava.net.preferIPv4Stack=true"

WORKDIR /workspace

COPY . .

RUN chmod +x mvnw

ARG SERVICE_MODULE

RUN test -n "$SERVICE_MODULE"

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -U \
    -pl "${SERVICE_MODULE}" \
    -am \
    package \
    -DskipTests


FROM eclipse-temurin:21-jre AS discovery-service
WORKDIR /app
COPY --from=build /workspace/discovery-service/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java","-jar","app.jar"]


FROM eclipse-temurin:21-jre AS config-service
WORKDIR /app
COPY --from=build /workspace/config-service/target/*.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java","-jar","app.jar"]


FROM eclipse-temurin:21-jre AS authentication-service
WORKDIR /app
COPY --from=build /workspace/authentication-service/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java","-jar","app.jar"]


FROM eclipse-temurin:21-jre AS user-service
WORKDIR /app
COPY --from=build /workspace/user-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]


FROM eclipse-temurin:21-jre AS authorization-service
WORKDIR /app
COPY --from=build /workspace/authorization-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","app.jar"]


FROM eclipse-temurin:21-jre AS organization-service
WORKDIR /app
COPY --from=build /workspace/organization-service/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java","-jar","app.jar"]


FROM eclipse-temurin:21-jre AS notification-service
WORKDIR /app
COPY --from=build /workspace/notification-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java","-jar","app.jar"]


FROM eclipse-temurin:21-jre AS audit-service
WORKDIR /app
COPY --from=build /workspace/audit-service/target/*.war app.war
EXPOSE 8089
ENTRYPOINT ["java","-jar","app.war"]


FROM eclipse-temurin:21-jre AS admin-service
WORKDIR /app
COPY --from=build /workspace/admin-service/target/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java","-jar","app.jar"]


FROM eclipse-temurin:21-jre AS api-gateway
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
