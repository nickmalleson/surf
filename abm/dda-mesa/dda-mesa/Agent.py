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
        
        # Randomly move cell. This is to demo how to do it the 'proper way' (computationally expensive
        # and unnecessary in this simple model
        #self.model.grid.move_agent(self, DDAAgent._get_rand_neighbour_cell(self))

        # Randomly move
        x,y = self.pos
        if x == 0: # If far left, move right (or not)
            self.model.grid.move_agent(self, random.choice([ (x,0), (1,0) ] ) )
        elif x == self.model._width-1: # If far right, move left (or not)
            self.model.grid.move_agent(self, random.choice([(x, 0), (self.model._width-2, 0)]))
        else: # Otherwise chose to move left or right, or not
            self.model.grid.move_agent(self, random.choice([(x, 0), (x-1, 0), (x+1,0) ]))

        

    def __repr__(self):
        return "DDAAgent {}".format(self.unique_id)
    








    
    # Other useful functions, for reference more than anything else

    @classmethod
    def _get_rand_neighbour_cell(cls, agent):
        """Get a neighbouring cell at random. Don't use this, it's very expensive.
        Included here for reference (it's the 'proper' mesa way of doing it"""
        x, y = agent.pos  # (pos is updated by grid.place_agent method when they are initialised in Model)
        possible_steps = agent.model.grid.get_neighborhood(
            agent.pos,
            moore=True,
            include_center=True)
        return random.choice(possible_steps)

    
    
    def _agents_with_me(self):
        """Get the agents on the same cell as me (as a list)"""
        self.model.grid.get_cell_list_contents([self.pos])        
        
    def _agents_near_me(self):
        """Get agents near me (as a list)"""
        self.model.grid.get_neighbors(self.pos, moore=True, include_center=False, radius=1)
        