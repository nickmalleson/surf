from mesa import Agent

import random

class DDAAgent(Agent):
    """Default DDA agent"""

    def __init__(self, unique_id, model):
        """Initialise an agent with a unique_id and a reference to the model"""
        super().__init__(unique_id, model)
        # Agents need a state - this will be set by the model when the agent is created
        self.state = None
        # Each should have a colour for display (doesn't affect the analysis)
        self.colour = random.choice(["red", "blue", "green", "yellow", "orange", "pink", "green", "purple"])

        print("\tCreated agent {}".format(unique_id))


    def step(self):
        """Step the agent"""
        # Note: to get the other agents
        #print(self.unique_id," have found the agents:",self.model.schedule.agents)
        
        # Randomly move cell. This is to demo how to do it the 'proper way' (computationally expensive
        # and unnecessary in this simple model
        # self.model.grid.move_agent(self, DDAAgent._get_rand_neighbour_cell(self))

        if self.state == AgentStates.RETIRED:  # Don't do anything if the agent is retired.
            return

        x,y = self.pos # The agent's position
        a, b, g = self.model.loc_a, self.model.loc_b, self.model.graveyard # The locations; for convenience

        # If the agent has reached their destination then they can retire to the graveyard
        if (self.state == AgentStates.TRAVELLING_FROM_A and self.pos == b) or \
            (self.state == AgentStates.TRAVELLING_FROM_B and self.pos == a):
            self.retire()
            return

        # See if they should leave through the midpoint
        if self.pos == g:
            if random.random > self.model.bleedout_rate():
                self.retire()
                return

        # Otherwise move
        if self.state == AgentStates.TRAVELLING_FROM_A:
            self.model.grid.move_agent(self, ((x+1), 0) ) # Move 1 to right (away from point A)
        elif self.state == AgentStates.TRAVELLING_FROM_B:
            self.model.grid.move_agent(self, ((x-1), 0) ) # Move 1 to left (away from point A)




        # XXXX WHAT ABOUT LEAVING FROM THE GRAVEYARD

        # # Randomly move
        # if x == 0: # If far left, move right (or not)
        #     self.model.grid.move_agent(self, random.choice([ (x,0), (1,0) ] ) )
        # elif x == self.model._width-1: # If far right, move left (or not)
        #     self.model.grid.move_agent(self, random.choice([(x, 0), (self.model._width-2, 0)]))
        # else: # Otherwise chose to move left or right, or not
        #     self.model.grid.move_agent(self, random.choice([(x, 0), (x-1, 0), (x+1,0) ]))



    def activate(self):
        """Take this agent from a RETIRED state into an ACTIVE state (i.e. moving in the street)"""

        # Choose a location (either endpoint A or B) and move the agent there
        x = random.choice([self.model.loc_a, self.model.loc_b])
        self.model.grid.move_agent(self, x)

        # Change their state
        if x == self.model.loc_a:
            self.state = AgentStates.TRAVELLING_FROM_A
        else:
            self.state = AgentStates.TRAVELLING_FROM_B

    def retire(self):
        """Make this agent RETIRE"""
        self.model.grid.move_agent(self, self.model.graveyard)
        self.state = AgentStates.RETIRED

    def __repr__(self):
        return "DDAAgent {} (state {})".format(self.unique_id, self.state)




    
    #  Other useful functions, for reference more than anything else

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


class AgentStates:
    RETIRED, TRAVELLING_FROM_A, TRAVELLING_FROM_B = range(3)
