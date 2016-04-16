# Checks that the road network is connected
# Expects a single command-line argument: the name of the input roads shapefile
env JAVA_OPTS="-Xmx4G" \
scala -cp ../../surf-mason/mason/mason/:\
../../surf-mason/geomason-1.5/:\
.././lib/*:\
.././build/:\
../../resources/config/config/target/classes/\
 surf.abm.surfutil.CheckConnectedNetwork "$@"
