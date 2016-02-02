# Reads a file and writes out lines that have a URL in them (basically something starting with http or https)

import sys
import re

if len(sys.argv) != 3:
    print "USage: python find_urls.py <infile> <outfile>"

infile = sys.argv[1]
outfile = sys.argv[2]

print "Reading file '{}' and writing to file '{}'".format(infile, outfile)

# Make a regular expression for finding URLs
r = re.compile(r'(http|https)://[^/"]+')

#strings = ["sometthing with http://nickmalleson.co.uk", "not one", "another not : http"]

#for s in strings:
#    print str(r.search(s))

total = 0
matches=0
with open(infile, 'r') as inf:
    with open(outfile,'w') as out:
        for line in inf:
            total += 1
            if r.search(line)==None:
                continue
            # Have a match
            out.write(line)
            matches += 1

print "Finished. Found {} matches out of {} lines".format(matches, total)
