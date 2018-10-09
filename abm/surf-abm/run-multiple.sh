# Run the model a number of times, one after the other. Takes the number of times to run as the only
# argument. No command-line options to MASON are allowed, these need to be set in the commands
# below.

N=$1
echo "Will run $1 times"
for i in `seq 1 1 $N`
do
echo "RUNNING $i"
env JAVA_OPTS="-Xmx4G" \
scala -cp ../resources/mason/mason/build/:\
../resources/mason/contrib/geomason/build/:\
../resources/mason_resources/*:\
./lib/*:\
./build/:\
../resources/config/classes/\
 surf.abm.main.SurfABM -for 2000 -time 500 -seed 1 &
sleep 5;
done

