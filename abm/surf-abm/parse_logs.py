# Parse the surfabm log (surfabm.log.txt) to extract all entries that are associated with a particular agent
# Usage: python parse_logs.py <agent_id>

import sys

if len(sys.argv) != 2:
    print("Usage: python parse_logs.py <agent id>")
    sys.exit(1)

aid = int(sys.argv[1])

print("Will parse surfabm.log.txt for agent {}".format(aid) )

l = [] # The lines to extract from the logs

with open("surfabm.log.txt") as f:
    for line in f:
        if "Agent [{}]".format(aid) in line:
            l.append(line.strip())

print("Found {} lines".format(len(l)))
fname = "surfabm.log-agent{}.txt".format(aid)
print("Writing to file: "+fname)

with open(fname, 'w') as f:
    f.write("\n".join(l))

print("Finished")
