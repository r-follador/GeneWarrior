FROM eclipse-temurin:17-jre-focal as primer3_build
RUN mkdir -p /app/bin
WORKDIR /app/bin
RUN apt update -y
RUN apt install -y build-essential g++ cmake git-all
RUN git clone --depth 1 --branch v2.6.1 https://github.com/primer3-org/primer3.git primer3
WORKDIR /app/bin/primer3/src
RUN make

FROM gradle:jdk17-jammy as java_build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon -x test

FROM eclipse-temurin:17-jre-focal
# copy application and config
COPY --from=java_build /home/gradle/src/build/libs/*SNAPSHOT.jar /app/spring-boot-application.jar
COPY ./application.properties /app/application.properties

# copy 3rd party deps
COPY --from=primer3_build /app/bin/primer3/src/primer3_* /app/bin/
WORKDIR /app/bin
RUN apt update -y
RUN apt install -y muscle pip
RUN pip3 install weblogo
RUN mv /usr/bin/muscle /app/bin/muscle
RUN mv /usr/local/bin/weblogo /app/bin/weblogo

WORKDIR /app
ENTRYPOINT ["java", "-jar", "/app/spring-boot-application.jar"]
