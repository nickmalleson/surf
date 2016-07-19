from os import listdir
from os.path import isfile, join
import math
import json
import pickle
import traceback
import random

# Preliminary analysis of the runkeeper data.
# Main job of the script is to extract some useful data and write it out in a
# smaller file for further analysis in R / GIS.

# Use these paths if running on Nick's laptop:
datadir = "~/mapping/projects/runkeeper/mitmount/runkeeper/runkeeper-data/boston/breeze_geo/"
outfile = "~/mapping/projects/runkeeper/mitmount/runkeeper/breeze-out.csv"

# Use these paths if running on the MIT server (in the ~/runkeeper directory)`:
#datadir = "runkeeper-data/boston/breeze_geo/"
#outfile = "breeze-out.csv"


VISUALISE = False # Whether or not to draw a map of all the routes. Requires folium library
SAMPLE = True # Only choose a small sample of the data. Good for debugging and visualising.
BOSTON_CENTROID = [42.3736, -71.1097] # (y,x)
#FILTER_DIST = 0.15 # lat/lon distance (approx) threshold to include lines in the analysis
FILTER_DIST = float('inf') # No filter distance 
HEADER = "userid, filename, activity, start_time, end_time, start_x, start_y, end_x, end_y, utc_offset, distance, steps, elapsedTime" # Output columns



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
MAP_BACK = "Mapbox Bright"
#MAP_BACK = "" # This would use OSM
boston_map = None
def add_path(geo_path, finished=False):
    global boston_map, BOSTON_CENTROID, MAP_BACK
    if boston_map == None:
        import folium
        boston_map = folium.Map(location=BOSTON_CENTROID, tiles=MAP_BACK, zoom_start=12)
    if finished:
        boston_map.create_map(path="boston_map.html")
        return
    boston_map.geo_json(geo_path)
    #boston_map.geo_json(geo_path, line_opacity = 0.5, line_color='YlGn')

def add_point(coord, marker_colour="blue"): # (Note: assumes that add_path has been called first)
    boston_map.simple_marker( [coord[1], coord[0]], marker_color = marker_colour)




failcount = 0 # Count the number of files that weren't read

inside = 0  # Count the number of traces inside and outside the filter area
outside = 0 
points = 0 # Count the number of trips with just one point

# Arrays to store the different pieces of information about each trace. Indices need to be equivalent

userids = []
filenames = []
start_times = [] # The start and end times for traces
end_times = []
start_locs = []  # The start and finish locations ( (x,y) tuples )
finish_locs = []
activity_types = [] # Text description of the type of activity
utc_offsets = [] # The UTC offsets (to work out what the times are in GMT)
distances = [] # The trip distances (estimate)
steps = [] # The total number of steps per trip
elapsed_time = [] # The elapsed time
paths = [] # Also remember the paths themselves
user_count = {}  # A map of userid -> count of traces

# Useful to remember what the lists are
all_lists = { "userids":userids, "filenames":filenames, "start_times":start_times, "end_times":end_times,\
        "start_locs":start_locs, "finish_locs":finish_locs, "activity_types":activity_types,\
        "paths":paths, "utc_offsets":utc_offsets, \
        "distances":distances, "steps":steps, "elapsed_time":elapsed_time
        }


print "Reading a sample of the files, not all" if SAMPLE else "Reading all data"
print "Visualising the traces" if VISUALISE else "Not visualising traces"
print "Reading files:"
for i, f in enumerate(listdir(datadir)):
    
    if SAMPLE:
        # Choose randomly which paths to plot and exit if we have had enough
        if random.random() > 0.01:
            continue
        if len(userids) > 5000:
            break 
    
    try:
        j = read_json(f)
        uid = j['features'][0]['properties']['userId'] # user id
        line = j['features'][0]['geometry'] # the line. it has a list of coordinates (each a three-element list)
        times = j['features'][0]['properties']['coordinateProperties']['times']
        activity = j['features'][0]['properties']['activityType']

        # Check that each point has an associate time
        assert len(line['coordinates']) == len(times), "Line: {}, times: {}".format(len((line['coordinates'])), len(times) )

        #print uid

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
            outside += 1
            continue

        #print "\t.. inside centroid"
        inside += 1

        # Check that the 'trip' is not just a single point
        if len(line['coordinates']) < 2:
            points += 1
            continue
        

        # Remember user id, and the number of traces for this user
        userids.append(uid)
        if uid in user_count: 
            user_count[uid] += 1
        else:
            user_count[uid] = 0

        # Remember the filename of the trace
        filenames.append(f)
        
        # Remember the line
        paths.append(line)
        
        # Estimate the distance travelled along the line
        dist = 0
        for a in range(len(line)-1): # Iterate over all coordinates and calculate distance between coincident coords
            x1=line['coordinates'][a][0]
            y1=line['coordinates'][a][1]
            x2=line['coordinates'][a+1][0]
            y2=line['coordinates'][a+1][1]
            dist += math.sqrt( (x1-x2)**2 + (y1-y2)**2 )
        distances.append(dist) # Append the distance 


        # Find the start and finish times of the routes
        start_times.append(times[0])
        end_times.append(times[-1])
        
        # Find the UTC Offset and convert from seconds to hours
        utc_sec = int(j['features'][0]['properties']['utcOffset'])
        utc_hr = utc_sec / 3600
        utc_offsets.append(utc_hr)

        # Find start and finish location (stored as a tuple of (x,y) ). The line itself is
        # a list of three-element lists, each one representing x,y,z of the coord.
        start = ( line['coordinates'][0 ][0] ,  line['coordinates'][0 ][1] ) 
        finish = ( line['coordinates'][-1][0] ,  line['coordinates'][-1][1] )
        start_locs.append( start )
        finish_locs.append( finish )

        # The total number of steps
        try:
            steps.append(j['features'][0]['properties']['totalSteps'])
        except KeyError:
            # Sometimes there is no 'totalSteps' field
            steps.append(-1)

        # The elapsed time
        elapsed_time.append( int(j['features'][0]['properties']['elapsedTime']) )

        # Activity types
        activity_types.append(activity)

        # Check that all arrays are the same length
        lengths = map(len, all_lists.values())
        assert False not in ( x == len(userids) for x in lengths ), \
                "Arrays are not the same length {}".format(lengths)


        # Add to a map?
        if VISUALISE:
            add_path(join(datadir,f) )
            # Can also add start and end points
            #add_point(start, marker_colour='green')
            #add_point(finish, marker_colour='red')


    except ValueError:
        failcount += 1

    if i % 10000 == 0:
        print "...\t",i





print "Successfully read ",i, "files. There were also",failcount,"failures"

print "There were {} traces inside the boundary, and {} outside".format(inside, outside)

print "Of the trips within the boundary, {} of them were points (not included)".format(points)

print "There were {} unique users".format(len(user_count))

print "Writing output"

with open (outfile, 'w') as f:
    f.write(HEADER+"\n")
    for i in range(len(userids)):
        f.write("{uid},{fn},{act},{st},{et},{sx},{sy},{ex},{ey},{utcoff},{dist},{steps},{elapsed}\n".format( \
                uid = userids[i], fn=filenames[i], act=activity_types[i], st=start_times[i], et=end_times[i],\
                sx = start_locs[i][0], sy = start_locs[i][1],\
                ex = finish_locs[i][0], ey = finish_locs[i][1], \
                utcoff = utc_offsets[i],
                dist = distances[i], steps=steps[i], elapsed=elapsed_time[i]\
                ) )

if VISUALISE:
    print "Generating map"
    add_path(None, finished=True)
    

# Attempt to save the data using pickle and ipython. These only work on Nick's laptop.
print "Attempting to pickle the output"
try:
    for name, obj in all_lists.iteritems():
        fname = "/Users/nick/mapping/projects/runkeeper/mitmount/runkeeper/ipython_store/{l}.pickle".format(l=name)
        if name == "paths":
            continue # Takes too long
        with open(fname,'w') as f:
            print "\tpickling",name,"to",f.name
            pickle.dump(obj, f)
except:
    print "Could not save the pickled data"
    traceback.print_exc()

print "Attempting to use ipython to save the outputs"
try:    
    from IPython import get_ipython
    ipython = get_ipython()
    for listname in all_lists.keys():
        if listname == "paths":
            continue # Takes too long
        ipython.magic("store {l} > ~/mapping/projects/runkeeper/mitmount/runkeeper/ipython_store/{l}".format(l=listname))
except:
    print "Could not save the IPython data. This doesn't necessarily matter."
    traceback.print_exc()

print "Finished!"

