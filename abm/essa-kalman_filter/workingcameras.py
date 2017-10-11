import random
import collections
import shortestpath
from scipy import misc
from scipy.ndimage import imread
import numpy as np
import pandas as pd
import time
import matplotlib.pyplot as plt


from scipy.stats import norm



#%%

''' class to set up the filereader - reads files in and out
    reads Paint map in, reads origin-destination matrix in, saves simulated
    camera counts... ultimately needs to read simulated camera counts in and
    write optimised origin-destination matrix out'''


class IO:
    
    def readMap(self):    
        arr = imread('streetinleeds.png')

        data =[[arr[j, i, 0] for i in range(arr.shape[1])] for j in range(arr.shape[0])]

        #print(np.unique(data))

        for i in range(len(data)):
            for j in range(len(data[0])):
                if data[i][j] == 255:
                    data[i][j] = 'B'  # B for bleed out spot
                if data[i][j] == 195:
                    data[i][j] = 'R' # R for roads (pavements etc.)
                if data[i][j] == 237:
                    data[i][j] = 'E' # E for entrance/exits
                if data[i][j] == 34:
                    data[i][j] = 'G' # G for graveyard
                    
        return data

    def writeCounts(self, dataIn, columnsIn):
        datatranspose = list(map(list, zip(*dataIn)))
        df = pd.DataFrame(data = datatranspose, columns = columnsIn)
        df.to_csv('out.csv')
        pass





#%%

## base class Agent

class Agent(object):

    def run(self):
        pass

#%%

## Person inherits Agent

class Person(Agent):
    
    def __init__(self, worldIn, agentsIn):
        self.x = 50
        self.y = 150
        self.world = worldIn
        self.agents = agentsIn
        
    def run(self):
        pass
        
    
#%%
    
''' Worker inherits Person    
##
## Each worker has (by default) a random home and random work location.
## However, adjustTimes can be called in the main program to match the number
## of workers going to and from each point to an origin-destination matrix.
## They have a corresponding 'home->work' journey that is calculated
## using the shortestpath.shortestjourney method (which currently
## minimises distances while avoiding any boxes not marked with 'R' for road).
## Each iteration the time is assessed and based on how long their journey
## is the workers will set off at the right time to arrive for 9am.
## Workers all leave home at 5pm, arriving back home at a time dependent
## on their journey time.
## Their individual 'journey counters' keep track of how far along their
## commute they are, resetting to 1 when they arrive at their destination.
##    
## Workers have four states: at home, at work, journeying to work and 
## journeying home. '''

class Worker(Person):
  
    
    def __init__(self, worldIn, agentsIn):
        self.type = 'Worker'
        self.world = worldIn   
        self.agents = agentsIn
        self.walkingspeed = 1
        #self.walkingspeed = int(random.random()*3) # so far unused
                
        
        ## calls world.getSpawns() to feed in a list of spawn points, 
        ## then assigns a 'home' and 'work' spawn randomly (making sure they're
        ## different)
        spawns = self.world.getSpawns()
        #print(spawns)
        self.entrancespawn = spawns[0]
        #print(self.exitspawn)
        
        # sets their initial position to their home, and the journey counter
        # to 0
        self.x = self.entrancespawn[0]
        self.y = self.entrancespawn[1]
        
        
        self.journeycounter = 0
        self.state = 'A'
        

        


        
        
    def adjustLocations(self, entryIn, exitIn, journeyIn):
        
        ## function used to adjust times to fit an origin-destination matrix
        ## decides new journey, and corresponding journey length, set off time,
        ## etc.
        
        self.entrancespawn = entryIn
        self.exitspawn = exitIn
        self.journey = journeyIn
        self.journeylength = len(self.journey) - 1
        self.journeytime = self.journeylength
        #print(self.setofftime)
        #print(self.journeytime)
        
        self.x = self.entrancespawn[0]
        self.y = self.entrancespawn[1]

        
    ## each workers' run function evaluates first the time input: if it's 
    ## their set off time they begin their commute to work, and if it's 5pm
    ## they begin their journey home
    ## if it's neither of those times their state is evaluated: at home remains
    ## at home, at work remains at work, whereas each time step they are 
    ## 'Journeying' they move another point along their journey and their journey
    ## counter is updated to keep track of their progress
    ## if their journey counter hits their journey time they arrive at their
    ## destination (this isn't necessary with 1 move per time step, but is
    ## necessary for e.g 4 moves per time step)

    def run(self, hourIn):

        if hourIn%self.walkingspeed == 0:
            self.makeJourney(self.journeycounter)
            self.journeycounter = self.journeycounter + 1
        else: 
            pass
        
        

            
    ## the goToWork function monitors the progress of the commute, 
    ## and updates the position of the worker along their journey            
    def makeJourney(self, counterIn):
        if counterIn < self.journeytime:
            #print(counterIn)
            self.x = self.journey[(counterIn)][0]
            self.y = self.journey[(counterIn)][1]         
            self.state = 'Journeying'
        if counterIn == self.journeytime:
            self.x = self.exitspawn[0]
            self.y = self.exitspawn[1]
            self.state = 'B'
            
            
    def retire(self):
    
    
        ''' function to move an agent into the agent graveyard
        used when killing an agent at the bleedout spot, and
        also used when agents arrive at their destination  '''
        self.x = graveyardloc[0][0]
        self.y = graveyardloc[0][1]
        self.state = 'G'
        self.journeycounter = 1000
        #print('agent retired')
        
    def activate(self):

    
        ''' function to activate an agent out of the agent
        graveyard
        used when introducing agents to the system '''


        self.entrancespawn = spawns[0]
        self.x = self.entrancespawn[0]
        self.y = self.entrancespawn[1]
        self.journeycounter = 0
        self.state = 'A'
        #print('agent activated')
    



#%%

class Camera(object):
    
    ## each camera has a random location (only allowed on the building walls against
    ## the road) and a line of sight (one square of road in whatever direction
    ## the map dictates - if there's a choice of more than one square one is
    ## randomly selected)
    ## the cameras also all have a list of all the agents in the system, and
    ## they keep track of which of those agents is in their line of sight
    ## ('agentsenclosed')
    ## if an agent which was in 'agentenclosed' LAST time step is now no longer
    ## in the lineofsight THIS time step, then the counter is increased by 1
    ## every hour the counter is stored in hourlycount, and reset to 0
    
    def __init__(self, agentsIn, worldIn):
        
        self.world = worldIn
    
        self.agents = agentsIn
        self.agentsenclosed = []
        self.countsum = 0
        self.hourlycount = []
        
        #print(locations)
        self.location = []
        self.lineofsight = self.location
    

    def setLocation(self, locationIn):
        self.location = locationIn
        self.lineofsight = [self.location]  
    
    
        

    ## checks for agents in line of sight, adds them to agentsenclosed  
    def updateAgentsEnclosed(self):
        for agent in self.agents:
            if [agent.x, agent.y] in self.lineofsight:
                #print("An agent is in sight!")
                self.agentsenclosed.append(agent)
                
    ## checks to see if any of the agents that were in the line of sight last
    ## time tick are now not there
    ## i.e goes through agents in agentsenclosed, checks to see if their x and
    ## y are still contained in line of sight, and if not it removes them from
    ## the agentsenclosed list and adds one to the counter
    def countAgentsLeft(self):
        for i in reversed(range(len(self.agentsenclosed))):
            if [self.agentsenclosed[i].x, self.agentsenclosed[i].y] not in self.lineofsight:
                #print("An agent has left the line of sight!")
                self.countsum = self.countsum + 1
                self.agentsenclosed.pop(i)
            
    ## stores counter in hourlycounts every hour
    def run(self, timeIn):
        self.updateAgentsEnclosed()
        self.countAgentsLeft()
        if timeIn%60 == 0:
            self.hourlycount.append(self.countsum)
            self.countsum = 0
            

        
#%%

## Environment class
## initially a map filled with R for road
## but using setMap in the main program we can adjust it to match that of a 
## map picture (R still represents road, E for spawn points, B for buildings
## and C for building 'walls', i.e potential camera spots - see IO class for
## details)

class Environment(object):
    
    # sets up a world with a given height and width
    # initially all points of the world are 'N', null
    def __init__(self, height, width):
        self.height = height
        self.width = width
        self.data =[['R' for i in range(self.width)] for j in range(self.height)]

    
    def setMap(self, mapIn):
        self.data = mapIn
        self.height = len(mapIn)
        self.width = len(mapIn[0])
    
    
    ## returns locations of spawn points
    def getSpawns(self):

        indices = []
        for k in range(len(self.data)):
    
           for i, j in enumerate(self.data[k]):
               if j == 'E':
                   indices.append([k, i])
        #print(indices)
        return indices
     
    ## returns locations of potential camera spots
    def getCameraSpots(self):
        
        indices = []
        for k in range(len(self.data)):
    
           for i, j in enumerate(self.data[k]):
               if j == 'E':
                   indices.append([k, i])
        return indices
        
        
    def getGraveyardLoc(self):
        
        indices = []
        for k in range(len(self.data)):
    
           for i, j in enumerate(self.data[k]):
               if j == 'G':
                   indices.append([k, i])
        return indices
        
    def getBleedoutSpot(self):
        
        indices = []
        for k in range(len(self.data)):
    
           for i, j in enumerate(self.data[k]):
               if j == 'B':
                   indices.append([k, i])
        return indices
      
        
#%%
#-----------------------------------------------------------------------------#
#-------------------------------actual program--------------------------------#
#-----------------------------------------------------------------------------#
    
    
## set defaults
numberOfAgents = 8000 # overwritten in the case that origin-destination matrix is used
numberOfCameras = 2

## set underlying distribution that defines how many agents are released

x = np.arange(0, 24, 1)

rv1 = norm(loc = 12., scale = 6.0)

unroundeddist = rv1.pdf(x)

dist = [round(item*numberOfAgents) for item in unroundeddist]


## set bleedout rate

fs = 24 # sample rate 
f = 0.5 # the frequency of the signal

x = np.arange(fs) # the points on the x axis for plotting
# compute the value (amplitude) of the sin wave at the for each sample
y = [ np.cos(2*np.pi*f * (i/fs)) for i in np.arange(fs)]
bleedoutdist = [(abs(value)*0.8)+0.1 for value in y]



## sets up values to use so that the timescale can be adjusted
## ticksToAnHour sets base time (i.e 60 -> one time tick is one minute)
## numberOfIterations is then 'numberofhours' * ticksToAnHour 
ticksToAnHour = 60
numberOfIterations = 620


## prepares IO for file reading/writing
io = IO()

## sets up the world, reading in the paint map file
world = Environment(10, 10)
mapIn = io.readMap()
world.setMap(mapIn)


## code to set up an array of agents
agents = []

## the origin-destination matrix is read in, and used to determine how many
## agents to build with each origin as their home spawn and which destination
## as their work spawn

spawns = world.getSpawns()
cameraplaces = world.getCameraSpots()
graveyardloc = world.getGraveyardLoc()
bleedoutspot = world.getBleedoutSpot()

journeysarray = [shortestpath.shortestjourney([spawns1[0], spawns1[1]], [spawns2[0], spawns2[1]], world.data)\
 for spawns1 in spawns for spawns2 in spawns if spawns1 != spawns2]



## builds a number of agents dependent on TIME now
def buildAgents(numberIn):
    
    # do something with the time to work out how many agents to build!!!    
    
    for i in range(numberIn):    
    
        agents.append(Worker(world,agents))    
    
        agents[-1].adjustLocations(spawns[0], spawns[1], journeysarray[0])
    
                
    #for i in range(len(agents)):
        #print("Worker " + repr(i) + " works at " + repr(agents[i].exitspawn[0]) + " " + repr(agents[i].exitspawn[1]))
        #print("Worker " + repr(i) + " lives at " + repr(agents[i].entrancespawn[0]) + " " + repr(agents[i].entrancespawn[1]))
    
 

#%%


## code to set up an array of cameras
cameras = []

def buildCameras():
    for i in range(numberOfCameras):
        #print('building a camera')
        cameras.append(Camera(agents, world))
        cameras[i].setLocation(cameraplaces[i])
        
        
## code to loop through iterations and agents    
## each agent runs, and then each camera updates its counts       

def runAgents(hourIn, bleedoutdist):

    global agents
    global cameras

    ''' define the bleedoutrate based on the clock '''
    
    #bleedoutrate = bleedoutdist[(hourIn//60)%24]
    bleedoutrate = bleedoutdist

    ''' activate some agents based on the clock '''
        
    
    if hourIn%60 == 0:
        buildindex = int(hourIn/60)
        buildnumber = int(dist[buildindex%24])
        #print('building!')
    else:
        buildnumber = 0

    for i in range(buildnumber):
        selectedworker = next(agent for agent in agents if agent.state == 'G')
        selectedworker.activate()


    for i in reversed(range(len(agents))):
        agent = agents[i]
        if agent.state == 'B':
            #print('journey has ended')
            agent.retire()
        elif agent.y == bleedoutspot[0][1]:
            #print('entered')
            #trialnumber = random.random()
            #print(trialnumber)
            if random.random() > bleedoutrate:
               #print('bleeding out')
                agent.retire()
            else:
                agent.run(hourIn)
            
        else:
            agent.run(hourIn)
        
    for k in range(numberOfCameras):
            cameras[k].run(hourIn)


    #print(agents[0].y)

## function to build program in its most basic

def runProgram(bleedoutdist, numberOfIterations, agentnumberIn):
    
    #random.seed(100)    
    #np.random.seed(100)
    
    t0 = time.time()
    
    ''' build all agents and retire them all to begin with'''
    buildAgents(agentnumberIn)    
    
    global agents    
    
    for agent in agents:
        agent.retire()
        
    ''' build cameras '''
    buildCameras()
    
    ''' loops through the desired number of iterations '''
    for i in range(numberOfIterations):
    ## build agents based on the time
    
        #print(i)    
    
        timeIn = i
      
        
        runAgents(timeIn, bleedoutdist)
        
        activeagents = [agent for agent in agents if agent.state == 'Journeying']
        #print('length of active agents')
        #print(len(activeagents))
        #print(agents[0].y)
        
        
    ''' saves camera counts '''
    counts = saveCameraCounts()
    
        
    t1 = time.time()
    total = t1 - t0
    #print(total)
    
    #global agents
    #print(len(agents))
    
    #for agent in agents:
        #print(agent.state)
        
    plotBothCounts(counts)
     
    
    agentlocations = [agent.y for agent in agents]   
    agentjourneycounters = [agent.journeycounter for agent in agents]
    result = [None]*(len(agentlocations)+len(agentjourneycounters))
    result[::2] = agentlocations
    result[1::2] = agentjourneycounters
    
    result.append(bleedoutdist)
    result.append(counts[0][-1])
    result.append(counts[1][-1])
        
    
    agents = []
    global cameras
    cameras = []
    
    #print(total)
        
    
    return result
    
    

def runProgramTrue(bleedoutdist, numberOfIterations, agentnumberIn):
    
    #random.seed(100)    
    #np.random.seed(100)
    
    t0 = time.time()
    
    ''' build all agents and retire them all to begin with'''
    buildAgents(agentnumberIn)    
    
    global agents    
    
    for agent in agents:
        agent.retire()
        
    ''' build cameras '''
    buildCameras()
    
    ''' loops through the desired number of iterations '''
    for i in range(numberOfIterations):
    ## build agents based on the time
    
        #print(i)    
    
        timeIn = i
      
        
        runAgents(timeIn, bleedoutdist)
        
        activeagents = [agent for agent in agents if agent.state == 'Journeying']
        #print('length of active agents')
        #print(len(activeagents))
        #print(agents[0].y)
        
        
    ''' saves camera counts '''
    counts = saveCameraCounts()
    
        
    t1 = time.time()
    total = t1 - t0
    #print(total)
    
    #global agents
    #print(len(agents))
    
    #for agent in agents:
        #print(agent.state)
        
    plotBothCounts(counts)
     
    
    agentlocations = [agent.y for agent in agents]   
    agentjourneycounters = [agent.journeycounter for agent in agents]
    result = [None]*(len(agentlocations)+len(agentjourneycounters))
    result[::2] = agentlocations
    result[1::2] = agentjourneycounters
    
    result.append(bleedoutdist)
    result.append(counts[0])
    result.append(counts[1])
        
    
    agents = []
    global cameras
    cameras = []
    
    #print(total)
        
    
    return result    
    
    
    
    
## prints the camera counts
def printCameraCounts():
    data = []
    for camera in cameras:
        print("Hourly count: "+str(camera.hourlycount))
        data.append(camera.hourlycount)
    
## as printCameraCounts, but also saves them using the io
def saveCameraCounts():        
    data = []
    columnheadings = []
    for camera in cameras:
        #print(camera.hourlycount)
        data.append(camera.hourlycount)
        columnheadings.append(str(camera.lineofsight))
    io.writeCounts(data, columnheadings)
    #plotCameraCounts(1)
    #print('finished saving camera counts')
    return data
    
def plotCameraCounts(i):
    plt.plot(cameras[i].hourlycount)
    
def plotBothCounts(countsIn):
    camera_a = countsIn[0]
    camera_b = countsIn[1]
    xaxis = [i for i in range(len(camera_a))]
    plt.plot(xaxis, camera_a, 'r--', xaxis, camera_b, 'bs')
    plt.ylabel("Camera Counts")
    
    

    # T = time, xf0 initial state vector, pf0 initial covariance
def runForecast(T, nagents, xf0, pf0, H, R, steps):

    # builds the right number of agents   

    global agents
    global cameras
    
    cameras = []
    agents = []
 
    buildAgents(nagents)   
    
    # adjusts their y locations accordingly
    
    for i in range(nagents):
        
        agents[i].y = int(xf0[2*i])
        agents[i].journeycounter = int(xf0[2*i + 1])
        if agents[i].y == agents[i].exitspawn[1]:
            agents[i].state = 'B'
        elif agents[i].y == graveyardloc[0][1]:
            agents[i].state = 'G'
        elif agents[i].y == agents[i].entrancespawn[1]:
            agents[i].state = 'A'
        else:
            agents[i].state = 'Journeying'
            
    bleedoutrate = xf0[2*nagents]  
    
    buildCameras()
    
    for camera in cameras:
        camera.updateAgentsEnclosed()
    
    for t in range(T, T + steps):
    ## build agents based on the time

        if t%10 == 0:    
<<<<<<< HEAD
            pass
            #print(t)
=======
             #print(t)
             pass
>>>>>>> 7f970caa29ed55e75b88532bbd2671960efff0c2
        timeIn = t      
        
        runAgents(timeIn, bleedoutrate)
        
        #print(agents[0].y)
        
    counts = []
    ''' saves camera counts '''
    counts = saveCameraCounts()
    plotBothCounts(counts)

    agentlocations = [agent.y for agent in agents]   
    agentjourneycounters = [agent.journeycounter for agent in agents]
    result = [None]*(len(agentlocations)+len(agentjourneycounters))
    result[::2] = agentlocations
    result[1::2] = agentjourneycounters
    
    result.append(bleedoutrate)
    result.append(counts[0][-1])
    result.append(counts[1][-1])
    
    agents = []
    cameras = []
    
    return result
    
    

#%%
'''
tobeaveraged = [runProgram(reshaped, 10, 10) for i in range (0,5)]

basearray = np.array(tobeaveraged[0])

for i in range(1, 5):
    basearray = basearray + np.array(tobeaveraged[i])
    
basearray/len(tobeaveraged) '''

#%%
## genetic algorithm section, work in progress! separate file?
#minimum = 0
#maximum = 4
#dfrand = pd.DataFrame(np.random.randint(minimum, maximum, size = (10,10)))


