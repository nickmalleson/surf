from mesa import Model
from mesa.time import RandomActivation
from Agent import DDAAgent

import matplotlib.pyplot as plt

class DDAModel(Model):
    """A simple DDA model"""
    def __init__(self, N):
        self.num_agents = N
        self.schedule = RandomActivation(self)
        # Create agents
        for i in range(self.num_agents):
            a = DDAAgent(i, self)
            self.schedule.add(a) # Add the agents to the schedule

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
