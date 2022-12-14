# Backend of Document to Speech Conversion

This project was bootstrapped using the Spring Initializer Generator in IntelliJ (Ultimate Edition).

## Introduction
The project consists the backend code for document to speech conversion. It is developed using Spring Boot framework. It is responsible to upload/download source document to AWS S3 bucket.

## Build Automation Tool
The build automation tool used for this project is Gradle - Groovy. All the third-party dependencies were added to build.gradle

## Gradle commands used to build project
````
gradle clean build
````

If new dependencies are added, use the following command:
````
gradle build --refresh-dependencies
````

These commands generate build folder which consists jar under the libs folder

## AWS Configuration
Go to src > main > resources > application.yml and update the following keys:
````
cloud:
  aws:
    credentials:
      access-key: xxxxx
      secret-key: xxxxxxxxxx
    region:
      static: us-east-1
````

## Run the application
By default, Spring Boot provides an embedded Apache Tomcat Server which runs on the port 8080. IntelliJ automatically detects java files. In run/debug configurations, select the main spring boot class and application starts running on:
````
http://localhost:8080
````

## Versions
- Java 11
* Gradle 7.5.1

## Swagger - API documentation
To maintain API documentation, Swagger 2 with Springfox implementation has been used. After running the application, all the API endpoints documentation is visible on following URL:
````
http://localhost:8080/swagger-ui.html#/
````

## Dockerfile
To run this application as a docker container, a Dockerfile is present at project level.
1. To build image:
````
docker build -t backend-app .
````
2. Check if image is created:
````
docker images
````
3. To run the docker image:
````
docker run -p 8080:8080 backend-app
````
