## Graphhopper

### Kazakhstan OSM
https://download.geofabrik.de/asia/kazakhstan-latest.osm.pbf

### Config yml
https://github.com/graphhopper/graphhopper/blob/master/config-example.yml

### Release jars
https://github.com/graphhopper/graphhopper/releases/download/10.2/graphhopper-web-10.2.jar

### Run standalone jar
```
java -Ddw.graphhopper.datareader.file=data/kazakhstan-latest.osm.pbf -jar *.jar server config-example.yml
```