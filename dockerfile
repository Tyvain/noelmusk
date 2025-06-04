# Étape 1 : Build avec Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build

# Copier les fichiers de configuration
COPY pom.xml ./

# Télécharger les dépendances
RUN mvn dependency:go-offline

# Copier le reste du code
COPY . .

# Compiler en mode production (notez le profil -Pproduction)
RUN mvn clean package -Pproduction -DskipTests
# OU si pas de profil custom : mvn clean package -Dvaadin.productionMode=true -DskipTests

# Étape 2 : Image d'exécution
FROM openjdk:21-jdk-slim

WORKDIR /app

# Copier le JAR compilé
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
