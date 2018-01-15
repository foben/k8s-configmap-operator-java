mvn clean package -DskipTests
cp -v ./target/configmap-operator*.jar ./docker/configmap-operator.jar
docker build -t foben/configmap-operator:local docker
rm -v ./docker/configmap-operator.jar
