FROM openjdk:21-jdk-slim

# Installa Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .

# Ricopia la cartella sito nella root, anche se Maven la sposta o la elimina
COPY sito sito

RUN mvn clean package

EXPOSE $PORT

CMD ["java", "-cp", "target/classes:target/lib/*", "controller.Controller"]