# For visualising the model.
# https://mesa.readthedocs.io/en/latest/tutorials/adv_tutorial.html

import model
from model import DDAModel

from mesa.visualization.modules import CanvasGrid
from mesa.visualization.ModularVisualization import ModularServer
from mesa.visualization.modules import ChartModule


def agent_portrayal(agent):
    """A portrayal that defines how agents should be drawn"""
    return {    "Shape": "circle",
                "Color": "red",
                 "Filled": "true",
                 "Layer": 0,
                 "r": 0.5}

# A 10x10 grid with 500*500 pixels
grid = CanvasGrid(agent_portrayal, model._WIDTH, model._HEIGHT, 500, 500)

# A graph. For more info: https://mesa.readthedocs.io/en/latest/tutorials/adv_tutorial.html#building-your-own-visualization-component
chart = ChartModule([{"Label": "Bleedout Rate",
                      "Color": "Black"}],
                    data_collector_name='datacollector')

server = ModularServer(DDAModel,
                       [grid, chart],
                       "DDA Model",
                       5,  # Agents
                       1000,   # Iterations
                       0.5)   # Bleedout rate
server.port = 8889
server.launch()
