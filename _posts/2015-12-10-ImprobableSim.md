---
layout: post
title: "surf Simulation with SpatialOS"
tagline: "Building the first Agent-Based Model with Improbable's SpatialOS"
category: announce
tags : [research, agent-based modelling, simulation, improbable]
---

One of the main aims of the _surf_ project is to build an agent-based model of flows around a city. The earliest agent-based models (e.g. [Schelling's (1969) segregation model](http://www.jstor.org/stable/1823701#.Vmla3wxCeGM)) were run by hand which obviously limited their complexity. It would be realistically impossible to create a simulation of more than a handful of agents. 

<figure>
<a href="https://en.wikipedia.org/wiki/Moore's_law"><img style="float:left; height:auto%; width:50%" src="https://upload.wikimedia.org/wikipedia/commons/0/00/Transistor_Count_and_Moore's_Law_-_2011.svg"></a>
<figcaption>
[Moore's law](https://en.wikipedia.org/wiki/Moore's_law): CPU speed has doubled approximately every two years. Source: [Wgsimon](https://commons.wikimedia.org/wiki/User:Wgsimon) ([CC-BY-SA](http://creativecommons.org/licenses/by-sa/3.0/))
</figcaption>
</figure>

Fortunately, computer power has increased so dramatically in recent years that it is now possible to simulate huge numbers of individual agents on a normal desktop computer. However, to simulate a system as large and complicated as a city is still beyond the capabilities of a single computer. It is possible to build a model that can be spread over a number of different computers that all work together, but this is technically very difficult. You'd need to be a much better programmer than me to make it work! So I have started looking for other tools that can automate the process of splitting a model across a collection of computers. 

<figure>
<a href="http://improbable.io/learn-more"><img style="float:right; height:auto%; width:50%" src="http://improbable.io/wp-content/uploads/2015/11/1.png"></a>
<figcaption>
Examples of entities that could be included in a SpatialOS model.
</figcaption>
</figure>

For this reason, I recently spent a week working with [improbable](http://improbable.io/), a tech startup in London. Improbable are in the process of creating a simulation engine called [SpatialOS](http://improbable.io/learn-more). SpatialOS handles the distribution of a single model over a number of individual compute nodes, which makes it possible to make models that are larger than those that are able to run on a single computer. The software is still under development, but the intention is to run models in the cloud. So if a model needs 50 separate computers to run it, the system just asks a cloud provider for 50 computers. If it needs 10,000 it asks for that many. And the entire process should be invisible to the model developer.

The software is still under development, but it’s easy to use. After a couple of days I had learned the basics and was able to implement a simple agent-based model of people commuting in Leeds (see the video below). The model reads in information about the number of people who live in each Lower Super Output Area and where they commute to. It has a 24-hour clock and the individual agents go to work in the morning, coming home again in the afternoon. At the moment it only simulates 1,350 agents, but when the model is scaled-up on a larger computer system it should be possible to model to the 750,000 people in the city.

<iframe width="560" height="315" src="https://www.youtube.com/embed/f2TqsVr7IzU" frameborder="0" allowfullscreen></iframe>

Another nice feature of the software is that it has been integrated with the [Unity](https://unity3d.com/) game engine. This not only provides a good 3D interface to view a running model, but also gives the developer access to the Unity physics engine. Using complicated physics in a model is usually beyond the capabilities of most modellers.

On the whole it's very exciting, and I'm looking forward to continuing to develop my agent-based model of urban flows with it.