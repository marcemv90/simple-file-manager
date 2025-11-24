# STAGE 1: BUILD
# ----------------------------------------------------
FROM maven:3.8.4-openjdk-11 AS builder

# Set the working directory
WORKDIR /app

# Copy source files
COPY . .

# Download Maven dependencies
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# Package the application
RUN --mount=type=cache,target=/root/.m2 mvn clean package

# STAGE 2: DEPLOY
# ----------------------------------------------------
#FROM tomcat:9-jdk11-openjdk-slim
FROM tomcat:10-jdk11-openjdk-slim AS runtime

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

FROM aquasec/trivy:0.56.1 AS trivy-scan

WORKDIR /rootfs

COPY --from=runtime / ./

RUN trivy rootfs \
      --severity HIGH,CRITICAL \
      --exit-code 0 \
      --format table \
      /rootfs | tee /trivy-report.txt && touch /tmp/.trivy-scan-done

FROM runtime

COPY --from=trivy-scan /trivy-report.txt /tmp/trivy-report.txt
COPY --from=trivy-scan /tmp/.trivy-scan-done /tmp/.trivy-scan-done
RUN rm -f /tmp/.trivy-scan-done
