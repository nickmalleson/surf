u# For visualising the model.
# https://mesa.readthedocs.io/en/latest/tutorials/adv_tutorial.html

import model
from model import DDAModel

from mesa.visualization.modules import CanvasGrid
from mesa.visualization.ModularVisualization import ModularServer
from mesa.visualization.modules import ChartModule


def agent_portrayal(agent):
    """A portrayal that defines how agents should be drawn"""
    return {"Shape": "circle",
            "Color": agent.colour,
            "Filled": "true",
            "Layer": 0,
            "r": 0.5}


# A 10x10 grid with 500*50 pixels
grid = CanvasGrid(agent_portrayal, model._WIDTH, model._HEIGHT, 500, 50)

# A graph. For more info:
# https://mesa.readthedocs.io/en/latest/tutorials/adv_tutorial.html#building-your-own-visualization-component
bleedout_chart = ChartModule([{"Label": "Bleedout rate", "Color": "Grey"}],
                             data_collector_name='datacollector')

active_agents_chart = ChartModule([{"Label": "Number of active agents", "Color": "Black"}],
                                  data_collector_name='datacollector')

camera_counts_chart_a = ChartModule([{"Label": "Camera A counts", "Color": "Red"}],
                                    data_collector_name='datacollector')

camera_counts_chart_b = ChartModule([{"Label": "Camera B counts", "Color": "Blue"}],
                                    data_collector_name='datacollector')

camera_counts_chart_m = ChartModule([{"Label": "Camera M counts", "Color": "Green"}],
                                    data_collector_name='datacollector')

server = ModularServer(DDAModel,
                       [grid, bleedout_chart, active_agents_chart, camera_counts_chart_a, camera_counts_chart_b, camera_counts_chart_m],
                       "DDA Model",
                       model.NUM_AGENTS,  # Agents
                       model.NUM_ITERATIONS,  # Iterations
                       0.5)  # Bleedout rate

server.port = 8889
server.launch()
