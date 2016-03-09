# Script to start the SimBurglar model

# Set up the classpath, making sure that the model libraries come first
export CLASSPATH=\
${PWD}/lib/*:\
${PWD}/../resources/jts-1.13.jar:\
${PWD}/../resources/libraries/*:\
${PWD}/../mason/mason/jar/mason.19.jar:\
${PWD}/../resources/geotools/*:\
${PWD}/../resources/jts-1.8.0/lib/*:\
${PWD}/../geomason-1.5/geomason.1.5.jar:\
${PWD}/build/

# Run java, passing all command line arguments
java -server simburglar.SimBurglar "$@"
