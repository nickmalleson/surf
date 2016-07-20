# Copies filtered files from the source directory to a target directory ready for further analysis.

# Assumes that it is being run on the MIT server in the ~/runkeeper/ directory.

import os
import sys
import shutil

DEBUG = True

# For runing on MIT server
#ROOT = "./"
# For running on Nick's laptop
ROOT = "/Users/nick/mapping/projects/runkeeper/mitmount/runkeeper/"

SOURCE_DIR = ROOT + "breeze-gpx/"
FILE_LIST  = ROOT + "breeze-simple-inf-filtered-traces.csv"
DEST_DIR   = ROOT + "mapmatching-traces/gpx/"

# Check the parameters
if not os.path.isdir(SOURCE_DIR):
    print "Error, the source directory is not actually a directory", SOURCE_DIR
    sys.exit()

if not os.path.isdir(DEST_DIR):
    print "Error, the destination directory is not actually a directory", DEST_DIR
    sys.exit()

if not os.path.isfile(FILE_LIST):
    print "Error, input file with the filtered json filenames cannot be found",FILE_LIST
    sys.exit()

print "Debug mode:", "on" if DEBUG else "off"
print "Parameters:\n\tSource dir:{},\n\tFile list:{}\n\tDest dir:{}".format(SOURCE_DIR, FILE_LIST, DEST_DIR)

# Read the list of files to copy

files = []
with open(FILE_LIST, 'r') as infile:
    infile.readline() # Ignore the first line - file header
    for line in infile:
        ls = line.split(',')
        f = ls[2].strip('"') # The file associated with this trace is in the third column
        files.append(f)

print "Read", len(files), "files from", FILE_LIST

# Now copy each file from its source directory to the destination.
count = 0
for fname in files:
    # Change the 'geojson' extension in the filename to 'gpx'. The R code dealt with json files, but
    # this is going to copy GPX files.
    f_str = fname[:-7]+"gpx"
    filename = SOURCE_DIR + f_str
    if os.path.isfile(filename):

        outfilename = DEST_DIR + f_str

        if os.path.isfile(outfilename):
            print "\tFile '{}' already exists, ignoring".format(outfilename)
            continue
        if not DEBUG:
            shutil.copyfile(filename, outfilename)
        #print ("Would copy" if DEBUG else "Copying")+":\n\t{}\n\t{}".format(filename, outfilename)
        count += 1
    else:
        print "File not found, ignoring: ", filename

    if DEBUG:
        if count > 10:
            print "In debug mode. Stopping now."
            break

    if count % 10000 == 0:
        print"\tHave processed {} / {} files".format(count, len(files))

print "Copied {} / {} files".format(count,len(files))
print "Finished"
