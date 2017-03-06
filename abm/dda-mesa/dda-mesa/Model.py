from Agent import DDAAgent

from mesa import Model
from mesa.time import RandomActivation
from mesa.space import MultiGrid

import matplotlib.pyplot as plt



class DDAModel(Model):
    """A simple DDA model"""
    
    _width  = 24 # width and height of the world. These shouldn't be changed
    _height = 1
    # Locations of important parts of the environment. These shouldn't be changed
    _graveyard =  ( 0 , 1) # x,y locations of the graveyard
    _loc_a     =  ( 1 , 1) # Location a (on left side of street)
    _loc_b     =  ( 23, 1) # Location b (on the right side)
    _loc_mid   =  ( 12, 1) # The midpoint
    
    def __init__(self, N):
        """
        Create a new instance of the DDA model.
        
        Parameters:
            N - the number of agents
        """
        self.num_agents = N
        self.schedule = RandomActivation(self) # Random order for calling agent's step methods
        self.grid = MultiGrid(DDAModel._width, DDAModel._height, False)
        
        # Create agents
        for i in range(self.num_agents):
            a = DDAAgent(i, self)
            self.schedule.add(a) # Add the agents to the schedule
            self.grid.place_agent(DDAModel._graveyard) # All agents startin the graveyard

        print("Created {} agents".format(len(self.schedule.agents) ) )

    def step(self):
        '''Advance the model by one step.'''
        self.schedule.step()


if __name__ == "__main__":
    model = DDAModel(10)
    model.step()

    # Lets see a graph of the agent ids:
    plt.hist([a.unique_id for a in model.schedule.agents] )
    plt.show()


    print("Finished")
