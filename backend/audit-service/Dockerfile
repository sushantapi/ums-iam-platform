FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/*.war app.war

ENTRYPOINT ["java","-jar","app.war"]
