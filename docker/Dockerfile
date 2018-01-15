FROM openjdk:8-jre

EXPOSE 8080

COPY configmap-operator.jar /deployment/

WORKDIR /deployment
CMD ["java", "-jar", "configmap-operator.jar"]
