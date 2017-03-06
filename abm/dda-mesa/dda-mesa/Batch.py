# For running multiple versions of the model

from Model import DDAModel

import matplotlib.pyplot as plt


all_wealth = [] # Save a particular agent attribute (actually their IDs for now)

for j in range(100):
    # Run the model
    model = DDAModel(10)
    for i in range(10): # Run 10 iterations
        model.step()

    # Store the results
    for agent in model.schedule.agents:
        all_wealth.append(agent.unique_id)

plt.hist(all_wealth, bins=range(max(all_wealth)+1))
plt.show()
