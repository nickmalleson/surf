# Run the model a number of times, one after the other. Takes the number of times to run as the only
# argument. No command-line options to MASON are allowed, these need to be set in the commands
# below. Runs in batch jobs to CORES cores (will only do CORES models at once, as per second
# suggestion here: https://unix.stackexchange.com/questions/103920/parallelize-a-bash-for-loop)

CORES=8 # Number of cores to use
N=$1 # total number of times to run the model (command-line argument)
echo "Will run $1 times in $CORES cores"

for i in `seq 1 1 $1`; do
    ((x=x%CORES)); ((x++==0)) && wait
    echo "RUNNING $i"
env JAVA_OPTS="-Xmx4G" \
scala -cp ../resources/mason/mason/build/:\
../resources/mason/contrib/geomason/build/:\
../resources/mason_resources/*:\
./lib/*:\
./build/:\
../resources/config/classes/\
 surf.abm.main.SurfABM -for 4032 -time 500 -seed $i &
    sleep 5;
done
