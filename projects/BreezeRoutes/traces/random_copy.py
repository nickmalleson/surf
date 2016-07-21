# Randomly copy some gpx files from the MIT server (mounted) in here for temporary analysis

import os
from random import shuffle
import shutil

N = 20 # NUmber to copy

DIR = "/Users/nick/research_not_syncd/mapping_local/projects/runkeeper/mitmount/runkeeper/breeze-gpx/"

print "Listing directory", DIR
files = os.listdir( DIR )
print "Found {} files".format(len(files))

shuffle(files)

for i in range(N):
    f = files[i]
    print "Copying", DIR+f, "./"+f
    shutil.copyfile(DIR+f, "./"+f)

print "Finished"
