FROM openjdk:17-jdk-slim

WORKDIR /graphhopper

COPY graphhopper-web-10.2.jar .
COPY config-example.yml .
COPY kazakhstan-latest.osm.pbf .

EXPOSE 8989

CMD ["java", "-Ddw.graphhopper.datareader.file=kazakhstan-latest.osm.pbf", "-jar", "graphhopper-web-10.2.jar", "server", "config-example.yml"]
