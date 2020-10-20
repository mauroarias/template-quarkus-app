# template-quarkus-app
template-quarkus-app

# run locally
mvn clean compile quarkus:dev

# build as native
mvn clean package -Pnative -Dquarkus.native.container-build=true

# Create container using docker after building with maven
docker build -f src/main/docker/Dockerfile.native -t mauroarias/session .

# Or create container directly using maven
mvn package -Pnative -Dquarkus.native.container-build=true -Dquarkus.container-image.build=true

# run container
docker run -i --rm -p 8080:8080 mauroarias/session