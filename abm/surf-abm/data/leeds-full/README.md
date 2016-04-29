# Full Leeds GIS Data

Here are instructions for how to obtain and prepare the road and buildings data required for the surf-abm model from Open Street Map

 1. Download roads and buidings shapefiles for West Yorkshire from [geofabrik](http://download.geofabrik.de/europe/great-britain/england/west-yorkshire.html)
 1. Extract Leeds using the LAD boundary and `intersect` tool in QGIS.
 1. Find disconnected roads using `disconnected-islands` (might need to be installed first).
 1. Extract buildings from the LAD boundary using `intersect`

_Note for Nick: original OSM data are in: `~/mapping/attribute_data/osm/west-yorkshire-2016-04-29`