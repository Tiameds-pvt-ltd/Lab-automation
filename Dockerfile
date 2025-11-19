## Use an OpenJDK base image (lightweight and secure)
#FROM openjdk:17-jdk-alpine
#
## Set the working directory
#WORKDIR /app
#
## Install dependencies and update packages
#RUN apk update && apk upgrade && apk add --no-cache bash curl
#
## Copy the application JAR file to the image
#COPY target/app.jar /app/app.jar
#
## Expose the application's port
#EXPOSE 8080
#
## Create a non-root user and switch to it
#RUN addgroup -S appgroup && adduser -S appuser -G appgroup
#USER appuser
#
## Run the application
#CMD ["java", "-jar", "app.jar"]







# Use a supported OpenJDK base image (Alpine variant)
FROM eclipse-temurin:17-jdk-alpine

# Set the working directory
WORKDIR /app

# Install dependencies and update packages
RUN apk update && apk upgrade && apk add --no-cache bash curl

# Copy the application JAR file to the image
COPY target/app.jar /app/app.jar

# Expose the application's port
EXPOSE 8080

# Create a non-root user and switch to it
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Run the application
CMD ["java", "-jar", "app.jar"]

