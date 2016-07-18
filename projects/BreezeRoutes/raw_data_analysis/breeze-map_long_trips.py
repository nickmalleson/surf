from os import listdir
from os.path import isfile, join
import math
import json
#import pickle
#import traceback
#import random
import shutil # For copying files

# Part of the preliminary analysis of the runkeeper data.
# Map long trips

datadir = "mitmount/runkeeper/runkeeper-data/boston/breeze_geo/"
outdir  =  "mitmount/runkeeper/long_trips/" # For the json files representing long trips

TRIP_DURATION= 87 # The cutoff for what is a 'long' trip in minutes. This is mean+2sd (see breeze-anaysis_data.Rmd)
SAMPLE = True

BOSTON_CENTROID = [42.3736, -71.1097] # (y,x)
FILTER_DIST = 0.05 # lat/lon distance (approx) threshold to include lines in the analysis (approx 10 miles)

def read_json(filename):
    """
    Read a json file and return a json object
    """
    with open(join(datadir,filename), 'r') as f:
        j = json.load(f)
        #print f.name, json.dumps(j, indent=4, separators=(',', ': ') )
        return j



# Parameters for optionally making a map using the folium library

#MAP_BACK = "Stamen Toner"
#MAP_BACK = "Stamen Terrain"
#MAP_BACK = "Mapbox Bright"
MAP_BACK = "" # This would use OSM
boston_map = None
def add_path(geo_path, finished=False):
    global boston_map, BOSTON_CENTROID, MAP_BACK
    if boston_map == None:
        import folium
        boston_map = folium.Map(location=BOSTON_CENTROID, tiles=MAP_BACK, zoom_start=12)
    if finished:
        boston_map.create_map(path="boston_map-long_trips.html")
        return
    boston_map.geo_json(geo_path)

def add_point(coord, marker_colour="blue"): # (Note: assumes that add_path has been called first)
    boston_map.simple_marker( [coord[1], coord[0]], marker_color = marker_colour)




failcount = 0 # Number of files that couldn't be read
print "Reading files:"

for i, f in enumerate(listdir(datadir)):
    
    try:
        if SAMPLE and i > 10000:
            break

        j = read_json(f)
        uid = j['features'][0]['properties']['userId'] # user id
        line = j['features'][0]['geometry'] # the line. it has a list of coordinates (each a three-element list)
        times = j['features'][0]['properties']['coordinateProperties']['times']
        activity = j['features'][0]['properties']['activityType']

        # The elapsed time (convert to minutes) - is it long enough for this to be included?
        elapsed_time = j['features'][0]['properties']['elapsedTime'] / 60 
        if (elapsed_time < TRIP_DURATION):
            continue # Too short

        # Check that each point has an associate time
        assert len(line['coordinates']) == len(times), "Line: {}, times: {}".format(len((line['coordinates'])), len(times) )

        # Rough filtering. Only include trips that are entirely within a distance of the centre of
        # Boston
        near_centroid = True
        for coords in line['coordinates']:
            x = coords[0]
            y = coords[1]
            if math.sqrt( (x-BOSTON_CENTROID[1])**2 + (y-BOSTON_CENTROID[0])**2 ) > FILTER_DIST:
                near_centroid = False
                break

        if not near_centroid:
            #print "\t.. outside centroid"
            continue


        # Check that the 'trip' is not just a single point
        if len(line['coordinates']) < 2:
            continue
        



        # Add this path to the map
        add_path(join(datadir,f) )

        # Can also add start and end points
        #add_point(start, marker_colour='green')
        #add_point(finish, marker_colour='red')

        # Also save the file somewhere else e.g. for a GIS
        shutil.copyfile( join(datadir,f), join(outdir,f) )


    except ValueError:
        failcount += 1

    if i % 10000 == 0:
        print "...\t",i


print "Successfully read ",i, "files. There were also",failcount,"failures"


print "Generating map"
add_path(None, finished=True)
    


print "Finished!"

