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
        
        # Randomly move cell
        x, y = self.pos # (pos is updated by grid.place_agent method when they are initialised in Model)
        possible_steps = self.model.grid.get_neighborhood(
            self.pos,
            moore=True,
            include_center=True)
        new_position = random.choice(possible_steps)
        self.model.grid.move_agent(self, new_position)
        

    def __repr__(self):
        return "DDAAgent {}".format(self.unique_id)
    
    
    
    
    
    
    # Other useful functions, for reference more than anything else
    
    
    def _agents_with_me(self):
        """Get the agents on the same cell as me (as a list)"""
        self.model.grid.get_cell_list_contents([self.pos])        
        
    def _agents_near_me(self):
        """Get agents near me (as a list)"""
        self.model.grid.get_neighbors(self.pos, moore=True, include_center=False, radius=1)
        