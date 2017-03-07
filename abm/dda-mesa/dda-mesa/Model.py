from agent import DDAAgent

from mesa import Model
from mesa.time import RandomActivation # To randomise the order of agent step() methods
from mesa.space import MultiGrid # The environment
from mesa.datacollection import DataCollector # For collecting model data

import matplotlib.pyplot as plt
import numpy as np

import sys
import traceback


# These parameters are used when the model is executed from this class
# (e.g. by the code in if__name__=="__main__" at the end of the file)
# and are also read by the visualisation class.

NUM_AGENTS = 600
NUM_ITERATIONS = 7200
_WIDTH = 24 # Don't change the width and the height
_HEIGHT = 1


class DDAModel(Model):
    """A simple DDA model"""
    
    _width  = _WIDTH # width and height of the world. These shouldn't be changed
    _height = _HEIGHT
    # Locations of important parts of the environment. These shouldn't be changed
    _graveyard =  ( 0 , 0) # x,y locations of the graveyard
    _loc_a     =  ( 1 , 0) # Location a (on left side of street)
    _loc_b     =  ( 23, 0) # Location b (on the right side)
    _loc_mid   =  ( 12, 0) # The midpoint
    
    def __init__(self, N, iterations,
                 bleedout_rate=np.random.normal(0.5, scale=0.1)):
        """
        Create a new instance of the DDA model.
        
        Parameters:
            N - the number of agents
            iterations - the number of iterations to run the model for
            blr - the bleedout rate (the probability that agents leave at the midpoint)
        """
        self.num_agents = N
        self.bleedout_rate = bleedout_rate
        self.iterations = iterations
        
        # Set up the scheduler
        self.schedule = RandomActivation(self) # Random order for calling agent's step methods
        
        # Create the environment
        self.grid = MultiGrid(DDAModel._width, DDAModel._height, False)
        
        # Define a variable that can be used to indicate whether the model has finished
        self.running = True
        
        
        # Create agents
        for i in range(self.num_agents):
            a = DDAAgent(i, self)
            self.schedule.add(a) # Add the agents to the schedule
            self.grid.place_agent(a, DDAModel._graveyard) # All agents startin the graveyard
        print("Created {} agents".format(len(self.schedule.agents) ) )
        
        # Define a collector for model data
        self.datacollector = DataCollector(
            model_reporters={"Bleedout Rate": lambda m: m.bleedout_rate},
            agent_reporters={"Location (x)":  lambda a: a.pos[0]}
            )
    
        

    def step(self):
        '''Advance the model by one step.'''
        print("Iteration {}".format(self.schedule.steps))
        self.datacollector.collect(self) # Collect data about the model
        if self.schedule.steps >= self.iterations:
            self.running = False
        else:
            self.schedule.step()
        
    
    # bleedout rate is defined as a property: http://www.python-course.eu/python3_properties.php
    @property
    def bleedout_rate(self):
        """Get the current bleedout rate"""
        return self.__bleedout_rate
    @bleedout_rate.setter
    def bleedout_rate(self, blr):
        """Set the bleedout rate. It must be between 0 and 1 (inclusive). Failure
        to do that raises a ValueError."""
        if blr < 0 or blr > 1:
            raise ValueError("The bleedout rate must be between 0 and 1, not '{}'".format(blr) )
        self.__bleedout_rate = blr
        
    
        


if __name__ == "__main__":
    model = DDAModel(NUM_AGENTS, NUM_ITERATIONS)

    try: # Do it all in a try so that I can have the console afterwards to see what's going on
        while model.running:
            model.step()



        # Lets see a graph of the agent ids:
        #plt.hist([a.unique_id for a in model.schedule.agents] )
        #plt.show()

        # Lets see where most of the agents are
        agent_counts = np.zeros((model.grid.width, model.grid.height))
        for cell in model.grid.coord_iter():
            cell_content, x, y = cell
            agent_count = len(cell_content)
            agent_counts[x][y] = agent_count
        plt.imshow(agent_counts, interpolation='nearest')
        plt.title("Locations of all agents")
        plt.colorbar()
        plt.show()

        # Look at the distribution of x values
        model.datacollector.get_agent_vars_dataframe().hist()

        # Look at the change in bleedout rate
        model.datacollector.get_model_vars_dataframe().hist()


        print("Finished")
    except:
        exc_type, exc_value, exc_traceback = sys.exc_info()
        traceback.print_tb(exc_traceback)
