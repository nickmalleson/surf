# Breeze Routes

Trying to compare routes from Breeze data and those produced by Google Maps

## Required Libraries

Libraries are in the ./lib/ directory.

### map-matching

The project uses the [map-matching](https://github.com/graphhopper/map-matching) project.

The required jars have been acquired through maven and are included here.




XXXX OLD:

This is used to map the GPS coordinates onto OSM road segments. It might need to be downloaded separately from github (_I've not tested this_):

```
git submodule init
git submodule update
```

Then in needs to be compiled. This needs maven (`brew install mvnvm`)

```
cd lib/map-matching
mvn compile
```