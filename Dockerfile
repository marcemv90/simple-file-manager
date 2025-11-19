# STAGE 1: BUILD
# ----------------------------------------------------
FROM maven:3.8.4-openjdk-11 AS builder

# Set the working directory
WORKDIR /app

# Copy source files
COPY . .

# Download Maven dependencies
RUN mvn dependency:go-offline

# Package the application
RUN mvn clean package

# STAGE 2: DEPLOY
# ----------------------------------------------------
#FROM tomcat:9-jdk11-openjdk-slim
FROM tomcat:10-jdk11-openjdk-slim

# Set the working directory
WORKDIR /usr/local/tomcat

# Remove default web apps (optional)
RUN rm -rf webapps/*

# Copy the .war file from the build stage
COPY --from=builder /app/target/*.war /usr/local/tomcat/webapps/sfm.war

# Expose the default Tomcat port
EXPOSE 8080

# Start Tomcat
CMD ["catalina.sh", "run"]
