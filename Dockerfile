FROM openjdk:21-jdk-slim

# Installa Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copia il progetto
WORKDIR /app
COPY . .

# Compila il progetto
RUN mvn clean package

# Esponi la porta che Render assegna
EXPOSE $PORT

# Avvia l'applicazione
CMD ["java", "-cp", "target/classes:target/lib/*", "controller.Controller"]