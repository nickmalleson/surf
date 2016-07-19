# Breeze Routes

Trying to compare routes from Breeze data and those produced by Google Maps. This README gives the main steps needed to re-do the work.

## Required OSM Data

Open Street Map data are required to match to the road network. 

I've not added OSM data to the repository. You need to downloaded `*.osm.pbf` files and store them in the `map-data` directory. [GeoFabrik](http://download.geofabrik.de) has lots of OSM data.

E.g. the following should work for Massachusetts data:

```
cd map-data
wget http://download.geofabrik.de/north-america/us/massachusetts-latest.osm.pbf 
```

## Required Libraries

All the required libraries are in the ./lib/ directory. 

The main third-party libraries are [Graphhopper](https://github.com/graphhopper/graphhopper) (for routing) and [map-matching](https://github.com/graphhopper/map-matching) for building a route (list of OSM street segments) from the GPS data. These have been imported with Maven.

The `BreezeRoutes` folder is a project that can be openned in the IntelliJ IDEA IDE. To see specifically which Maven libraries have been downloaded, go through IntelliJ.


## 1. Preparing GPX files with json2gpx

See [./json2gpx](./json2gpx/) directory.

The first step is to convert the raw json files to gpx. To assign the GPS coordiantes to OSM segments I'm using the [map-matching](https://github.com/graphhopper/map-matching) library. This requires GPX files, rather than geojson files. Converting geojson to gpx should be easy, but the times associated with each coordinate are stored in a non-standard place in the json:

```
features:properties:coordinateProperties:times
```

So I've used the [togpx](https://github.com/tyrasd/togpx) library to convert the geojson files to GPX files. The only tricky thing is using a callback function that specifies where the times associated with each coordinate are in the json.

For details see: `./json2gpx/breezetogpx.js`. The script reads all `.json` files in its directory:

`~/runkeeper/runkeeper-data/boston/breeze_geo/`

and writes out `.gpx` files to an output directory:

`~/runkeeper/breeze-gpx/`

To run the script, do:

```
./breezetogpx.js
```


##  2. Analysing Raw Data and Filtering Trips

The initial analysis of the raw traces is contained in the [./raw\_data\_analysis](./raw_data_analysis) directory. Note that the previous step (converting json files to gpx) isn't needed until later.

### 1-breeze-read_data.py

The first step. This reads the geojson files on the MIT server and extracts useful information to a csv file. It doesn't keep the paths themselves, just information about them. It can be configuted to igore some trips, e.g. those that are a long way from boston or those that are made up of only one point.
It can also be configured to create a map of the trips, but this only works when running on Nick's laptop because it requires the Follium library which isn't installed on the MIT server.

**OUTPUT**: ./breeze-out.csv

NOTE: that file is then renamed so that the R analysis (see next) doesn't get disrupted if I want to re-read the json data

breeze-out.csv -> breeze-simple-X.csv - where X is the distance threshold used to filter out trips

All files are read and written to the server.


### 2-breeze-analyse_data.Rmd

This reads the file created in the previous step and calculates some statistics etc.

**Important OUTPUT**: It also does some filtering to idenfity useful trips and writes the names of the original *.json files as a csv file with a similar name to the input file. E.g. `breeze-simple-inf-filtered-traces.csv`. This file is written to the server (see the DIR parameter).

### 3-breeze-copy_filtered_traces.py

This file completes the pre-analysis process by copyig the filtered traces (the csv list of filenames from `2-breeze-analyse_data.Rmd`) to a new directory ready for map matching. The map matching library needs GPX files, so it copies the raw gpx files created earlier from

`~/runkeeper/breeze-gpx/`

to

`~/mapmatching-traces/gpx/`

It is best to run this file on the MIT server.


### breeze-map_long_trips.py

This isn't actually used in the analysis, it was just a quick way of visualising some of the trips initially. It reads the JSON data and creates a map of long trips.

OUTPUT: breeze_map-long_trips.html

Note: it also copies the json files of the long trips into:

  ./mitmount/runkeeper/long_trips/

for analysis in a GIS



## 3. Map Matching with GraphHopper

### Matching

The `surf.projects.breezeroutes.MapMatcher` class does most of the work. It looks for GPX files in (the output files from step 2):

`~/mapmatching-traces/gpx/`

matches them to the OSM network, and wites out a gpx file with '-matched.gpx' and '-shortest.gpx' appended to the end of the filename into the directories:

`~/mapmatching-traces/gpx-matched/`

and

`~/mapmatching-traces/gpx-shortest/`


#### Viewing the matches

The file `traces/map_traces.Rmd` will map all of the matched and shortest traces and compare them to their originals.

### Shortest path


XXXX SHORTEST PATH
