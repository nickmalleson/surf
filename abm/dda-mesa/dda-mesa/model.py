ltfrom agent import DDAAgent
from agent import AgentStates

from mesa import Model
from mesa.time import RandomActivation  # To randomise the order of agent step() methods
from mesa.space import MultiGrid  # The environment
from mesa.datacollection import DataCollector  # For collecting model data

import matplotlib.pyplot as plt
from scipy.stats import norm
import pandas  # The model data are collected as DataFrames

import sys
import traceback
import numpy as np
import random
from multiprocessing import Pool

from typing import List

# These parameters are used when the model is executed from this class
# (e.g. by the code in if__name__=="__main__" at the end of the file)
# and are also read by the visualisation class.

NUM_AGENTS = 600
# NUM_ITERATIONS = 7200
NUM_ITERATIONS = 1000
MULTIPROCESS = False
_WIDTH = 24  # Don't change the width and the height
_HEIGHT = 1


class DDAModel(Model):
    """A simple DDA model"""

    _width = _WIDTH  # width and height of the world. These shouldn't be changed
    _height = _HEIGHT

    def __init__(self, N, iterations, bleedout_rate=np.random.normal(0.5, scale=0.1), mp=False):
        """
        Create a new instance of the DDA model.
        
        Parameters:
            N - the number of agents
            iterations - the number of iterations to run the model for
            blr - the bleedout rate (the probability that agents leave at the midpoint) (default normal distribution
            with mean=0.5 and sd=0.1)
            mp - whether to use multiprocess (agents call step() method at same time) (doesn't work!) (default False)
        """
        self.num_agents = N
        self._bleedout_rate = bleedout_rate
        self.iterations = iterations
        self.mp = mp

        # Locations of important parts of the environment. These shouldn't be changed
        self.graveyard = (0, 0)  # x,y locations of the graveyard
        self.loc_a = (1, 0)  # Location a (on left side of street)
        self.loc_b = (23, 0)  # Location b (on the right side)
        self.loc_mid = (12, 0)  # The midpoint

        # 'Cameras' that store the number of agents who pass them over the course of an hour. The historical counts
        # are saved by mesa using the DataCollector
        self._camera_a = 0  # Camera A
        self._camera_b = 0  # Camera B
        self._camera_m = 0  # The midpoint

        # Set up the scheduler. Note that this isn't actually used (see below re. agent's stepping)
        self.schedule = RandomActivation(self)  # Random order for calling agent's step methods

        # For multiprocess step method
        self.pool = Pool()

        # Create the environment
        self.grid = MultiGrid(DDAModel._width, DDAModel._height, False)

        # Define a variable that can be used to indicate whether the model has finished
        self.running = True

        # Create a distribution that tells us the number of agents to be added to the world at each
        self._agent_dist = DDAModel._make_agent_distribution(N)

        # Create all the agents
        for i in range(self.num_agents):
            a = DDAAgent(i, self)
            self.schedule.add(a)  # Add the agents to the schedule
            # All agents start as 'retired' in the graveyard
            a.state = AgentStates.RETIRED
            self.grid.place_agent(a, self.graveyard)  # All agents start in the graveyard

        print("Created {} agents".format(len(self.schedule.agents)))

        # Define a collector for model data
        self.datacollector = DataCollector(
            model_reporters={"Bleedout rate": lambda m: m.bleedout_rate,
                             "Number of active agents": lambda m: len(m.active_agents()),
                             "Camera A counts": lambda m: m.camera_a,
                             "Camera B counts": lambda m: m.camera_b,
                             "Camera M counts": lambda m: m.camera_m
                             },
            agent_reporters={"Location (x)": lambda agent: agent.pos[0],
                             "State": lambda agent: agent.state
                             }
        )

    def step(self):
        """Advance the model by one step."""
        print("Iteration {}".format(self.schedule.steps))

        self.datacollector.collect(self)  # Collect data about the model

        # See if the model has finished running.
        if self.schedule.steps >= self.iterations:
            self.running = False
            return

        # Things to do every hour.
        #  - 1 - reset the camera counters
        #  - 2 - activate some agents

        num_to_activate = -1
        s = self.schedule.steps  # Number of steps (for convenience)
        if s % 60 == 0:  # On the hour
            # Reset the cameras
            self._reset_cameras()
            # Calculate the number of agents to activate
            num_to_activate = int(self._agent_dist[int((s / 60) % 24)])
            print("\tActivating {} agents on hour {}".format(num_to_activate, s % 60))

        else:
            num_to_activate = 0
            
        assert num_to_activate >= 0, \
            "The number of agents to activate should be greater or equal to 0, not {}".format(num_to_activate)
            
        if num_to_activate > 0:
            # Choose some agents that are currently retired to activate.
            retired_agents = [a for a in self.schedule.agents if a.state == AgentStates.RETIRED]
            assert len(retired_agents) >= num_to_activate, \
                "Too few agents to activate (have {}, need {})".format(len(retired_agents), num_to_activate)
    
            to_activate = np.random.choice(retired_agents, size=num_to_activate, replace=False)
            print("\t\tActivating agents: {}".format(to_activate))
    
            for a in to_activate:
                a.activate()


        #        XXXX HERE - see line 477 om wprlomgca,eras/py

        # Call all agents' 'step' method.

        if not self.mp:  # Not using multiprocess. Do it the mesa way:
            self.schedule.step()
        else:
            # Better to do it a different way to take advantage of multicore processors and to ignore agents who are not
            # active (no need for them to step at all)
            # NOTE: Doesn't work! The problem is that the DDAAgent needs the DDAModel class, which means
            # that this class needs to be pickled and copied to the child processes. The first problem (which can be
            # fixed by creating functions rather than using lambda, although this is messy) is that DDAModel uses
            # lambda functions, that can't be pickled. Second and more difficult problem is that the Pool object itself
            # cannot be shared. Possible solution here:
            # https://stackoverflow.com/questions/25382455/python-notimplementederror-pool-objects-cannot-be-passed-between-processes
            # but for the meantime I'm not going to try to fix this.
            active_agents = self.active_agents()  # Get all of the active agents
            random.shuffle(active_agents)

            if active_agents is None:
                print("\tNo agents are active")  # Nothing to do
            else:
                p = Pool()
                p.map(DDAAgent._step_agent, active_agents)  # Calls step() for all agents

            # As not using the proper schedule method, need to update time manually.
            self.schedule.steps += 1
            self.schedule.time += 1

    def increment_camera_a(self):
        """Used by agents to tell the model that they have just passed the camera at location A. It would be neater
        to have the cameras detect the agents, but I think that this would be quite expensive."""
        self._camera_a += 1  # Increment the count of the current hour (most recent)

    def increment_camera_b(self):
        """Used by agents to tell the model that they have just passed the camera at location B. It would be neater
        to have the cameras detect the agents, but I think that this would be quite expensive."""
        self._camera_b += 1  # Increment the count of the current hour (most recent)

    def increment_camera_m(self):
        """Used by agents to tell the model that they have just passed the camera at the midpoint. This is only for
        information really, in this scenario there is no camera at the midpoint"""
        self._camera_m += 1  # Increment the count of the current hour (most recent)

    @property
    def camera_a(self) -> int:
        """Getter for the count of the camera at location A"""
        return self._camera_a

    @property
    def camera_b(self) -> int:
        """Getter for the count of the camera at location B"""
        return self._camera_b

    @property
    def camera_m(self) -> int:
        """Getter for the count of the camera at the midpoint"""
        return self._camera_m

    def _reset_cameras(self):
        """Reset the cameras to zero. Done on the hour"""
        self._camera_a = 0
        self._camera_b = 0
        self._camera_m = 0

    @staticmethod
    def _step_agent(a):
        """Call the given agent's step method. Only required because Pool.map doesn't take lambda functions."""
        a.step()

    # bleedout rate is defined as a property: http://www.python-course.eu/python3_properties.php
    @property
    def bleedout_rate(self):
        """Get the current bleedout rate"""
        return self._bleedout_rate

    @bleedout_rate.setter
    def bleedout_rate(self, blr: float) -> None:
        """Set the bleedout rate. It must be between 0 and 1 (inclusive). Failure
        to do that raises a ValueError."""
        if blr < 0 or blr > 1:
            raise ValueError("The bleedout rate must be between 0 and 1, not '{}'".format(blr))
        self._bleedout_rate = blr

    def active_agents(self) -> List[DDAAgent]:
        """Return a list of the active agents (i.e. those who are not retired)"""
        return [a for a in self.schedule.agents if a.state != AgentStates.RETIRED]

    @classmethod
    def _make_agent_distribution(cls, N):
        """Create a distribution that tells us the number of agents to be created at each hour"""
        a = np.arange(0, 24, 1)  # Create an array with one item for each hour
        rv1 = norm(loc=12., scale=6.0)  # A continuous, normal random variable with a peak at 12
        dist = rv1.pdf(a)  # Draw from the random variable pdf, taking values from a
        return [round(item * N, ndigits=0) for item in dist]  # Return a rounded list (the number of agents at each hour)


if __name__ == "__main__":
    model = DDAModel(NUM_AGENTS, NUM_ITERATIONS, mp=MULTIPROCESS)

    try:  # Do it all in a try so that I can have the console afterwards to see what's going on
        while model.running:
            model.step()

        # Lets see a graph of the agent ids:
        # plt.hist([a.unique_id for a in model.schedule.agents] )
        # plt.show()

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

        # Graph all of the agent variables (at the moment this is just the distribution of x values)
        model.datacollector.get_agent_vars_dataframe().hist()

        # Graph the model variables. All of them to start
        model.datacollector.get_model_vars_dataframe().hist()

        # A few model variables separately
        df: pandas.DataFrame = model.datacollector.get_model_vars_dataframe()

        camera_a = df['Camera A counts']
        camera_b = df['Camera B counts']
        #XXXX NEED TO AGGREGATE THE CAMERAS OVER EACH HOUR


        xaxis = [i for i in range(len(camera_a))]
        plt.plot(xaxis, camera_a, 'r--', xaxis, camera_b, 'b--')
        plt.ylabel("Camera Counts")


        print("Finished")

    except Exception as e:
        exc_type, exc_value, exc_traceback = sys.exc_info()
        traceback.print_exc()
        print(e)
        # traceback.print_tb(exc_traceback)
