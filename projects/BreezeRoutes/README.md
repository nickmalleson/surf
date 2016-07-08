# Breeze Routes

Trying to compare routes from Breeze data and those produced by Google Maps

## Required Data

I've not added OSM data to the repository. You need to downloaded `*.osm.pbf` files and store them in the `map-data` directory. [GeoFabrik](http://download.geofabrik.de) has lots of OSM data.

E.g. for Massachusetts data:

```
cd map-data
wget http://download.geofabrik.de/north-america/us/massachusetts-latest.osm.pbf 
```

## Required Libraries

Libraries are in the ./lib/ directory.

### map-matching

The project uses the [map-matching](https://github.com/graphhopper/map-matching) project.

The required jars have been acquired through maven and are included here.