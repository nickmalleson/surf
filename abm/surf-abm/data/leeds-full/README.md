# Full Leeds GIS Data

Here are instructions for how to obtain and prepare the road and buildings data required for the surf-abm model from Open Street Map

 1. Download roads and buidings shapefiles for West Yorkshire from [geofabrik](http://download.geofabrik.de/europe/great-britain/england/west-yorkshire.html)
 1. Extract Leeds using the LAD boundary and `intersect` tool in QGIS.
 1. Find disconnected roads using `disconnected-islands` (might need to be installed first).
 1. Extract buildings from the LAD boundary using `intersect`

Extra steps for roads:

 - After the above, I got a wierd `NumberFormatException` error when trying to read the roads data. To fix it, I opened the data in ArcMap and saved a new shapefile to replace the original. Not sure what was wrong, but maybe QGIS had not saved the shapefile correctly.
 - The roads data didn't work properly, and I think that this is because with OSM data there aren't always nodes at intersections. I.e. two roads can cross but not share a vertex. To fix this:

   -  use `Feature to Line` tool in ArcMap (with tolerance 0.1m). 
   - recalculate the `ID` field (using python expression `int(!FID!)`. (The OSM ID no longer works because some roads have been split into 2+).
   - re-run `disconnected islands`

_Note for Nick: original OSM data are in: `~/mapping/attribute_data/osm/west-yorkshire-2016-04-29`_