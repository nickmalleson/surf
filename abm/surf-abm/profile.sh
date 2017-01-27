# Might need this first: 
#ulimit -c unlimited
env JAVA_OPTS="-Xmx4G" \
scala -cp ../resources/mason/mason/build/:\
../resources/mason/contrib/geomason/build/:\
../resources/mason_resources/*:\
./lib/*:\
./build/:\
../resources/config/classes/ \
surf.abm.main.SurfABM \
-Xrunhprof:heap=sites,cpu=samples \
"$@"
