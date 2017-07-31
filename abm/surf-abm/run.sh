# Run the model.
# Command-line options that are defined by MASON can be passed to configure the run. 
# E.g. for a simulation of 5000 iterations, summarising info every 1000 iterations, and a seed of 1:
#  ./run.sh -for 5000 -time 1000 -seed 1
env JAVA_OPTS="-Xmx4G" \
scala -cp ../resources/mason/mason/build/:\
../resources/mason/contrib/geomason/build/:\
../resources/mason_resources/*:\
./lib/*:\
./build/:\
../resources/config/classes/\
 surf.abm.main.SurfABM "$@"
