
build:
	docker run -it --rm --name my-maven-project -v .:/usr/src/mymaven -v maven-store:/root/.m2 -w /usr/src/mymaven maven:3.9.9-eclipse-temurin-21 mvn clean install