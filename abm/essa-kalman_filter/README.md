# Kalman Filter Data Assimilation Example

This is the example presented at the ESSA [Social Simulation Conference (SSC)](http://www.sim2017.com/) on 26th September 2017 in Dublin. It shows how to use an Ensemble Kalman Filter (EnKF) to assimilate data into a simple
agent-based model. I can't take any credit for the code, it was written entirely by Alice Tapper and [Jon Ward](http://www1.maths.leeds.ac.uk/~jaward/); I've only adapted and commented it.

The slides explaining the work are available on the [Simulating Urban Flows
website](http://surf.leeds.ac.uk/p/2017-09-26-essa-da.html) and the full paper is available <a href="http://surf.leeds.ac.uk/p/2017-09-26-essa-da.pdf">here</a>.

The easiest way to work out what is happenning is to read through the [Kalman Example notebook](./Kalman_Example.ipynb) which presents the overall process but doesn't run the model in full. The graphs etc. that were generated for the conference paper were produced by [kalman.py](./kalman.py) - that file iterates the whole process a sufficient number of times to get some results. The model itself (which is less important) is [workingcameras.py](./workingcameras.py).