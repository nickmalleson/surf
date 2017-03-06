from mesa import Agent

import random

class DDAAgent(Agent):
    """Default agent"""
    def __init__(self, unique_id, model):
        super().__init__(unique_id, model)
        print("\tCreated agent {}".format(unique_id) )


    def step(self):
        """Step the agent"""
        # Note: to get the other agents
        #print(self.unique_id," have found the agents:",self.model.schedule.agents)
        pass

    def __repr__(self):
        return "DDAAgent {}".format(self.unique_id)
