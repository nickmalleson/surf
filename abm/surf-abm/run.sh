env JAVA_OPTS="-Xmx4G" \
scala -cp ../surf-mason/mason/mason/:\
../surf-mason/geomason-1.5/:\
./lib/*:\
./build/:\
../resources/config/config/target/classes/\
 surf.abm.SurfABM "$@"
