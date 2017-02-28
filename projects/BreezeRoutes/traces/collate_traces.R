# The analyse_traces-server*.R files read all of the traces in the gpx, gpx-matched, and
# gpx-shortest directories. This takes a while, and gets slower with each file (?!) so it works
# better to do the reading as four different processes simultaneously.
# Each process saves it's traces to a traces-server*.RData file.
# This script reads in the data from the traces-server files and collates them into a single data
# file with all traces in it.

# Load the traces
load("traces-server1.RData")

# Rename the lists of traces, these will become the master lists
mmatched <- matched
mmatched.ma <- matched.ma 
morig <- orig
morig.ma <- orig.ma
mshortest <- shortest
mshortest.ma <- shortest.ma
muserid <- userid
mstarttime <- starttime 
mendtime <- endtime

# Now read the three remaining files and append the list rows

for (f in c(  "traces-server2.RData", "traces-server3.RData", "traces-server4.RData") ) {
    print(f)
    load(f)
    mmatched <- rbind(mmatched,matched)
    mmatched.ma <- rbind(mmatched.ma, matched.ma)
    morig <- rbind(morig, orig)
    morig.ma <- rbind(morig.ma, orig.ma)
    mshortest <- rbind(mshortest, shortest)
    mshortest.ma <- rbind(mshortest.ma, shortest.ma)
    muserid <- rbind(muserid, userid)
    mstarttime <- rbind(mstarttime, starttime)
    mendtime <- rbind(mendtime, endtime)
}

# Rename the objects
matched <- mmatched
matched.ma <- mmatched.ma 
orig <- morig
orig.ma <- morig.ma
shortest <- mshortest
shortest.ma <- mshortest.ma
userid <- muserid
starttime <- mstarttime 
endtime <- mendtime

# Save them
save(orig, matched, shortest, orig.ma, matched.ma, shortest.ma, userid, starttime, endtime, file="traces-server-all.RData")
print("FINISHED")
