---
layout: post
title: "Validating ABMs (SIMSOC discussion)"
category: announce
tags : [abm, validation]
---

<figure style="float:right; width:50%">
  <img src="../figures/attribution/people-time-square-small.jpg" alt="people on time square"/>
  <figcaption> Photo by <a href="https://unsplash.com/photos/slapbg2mejw?utm_source=unsplash&utm_medium=referral&utm_content=creditCopyText">Meriç
Dağlı</a> on <a href="https://unsplash.com/search/photos/people-crowd?utm_source=unsplash&utm_medium=referral&utm_content=creditCopyText">Unsplash</a>.
</figcaption>
</figure>

On 12th September 2018, I asked the [SIMSOC](https://www.jiscmail.ac.uk/cgi-bin/webadmin?A0=SimSoc) email list for advice on how to validate an empirical agent-based model in the absence of good validation data. You can read the original message and the replies on the SIMSOC archives [here](https://www.jiscmail.ac.uk/cgi-bin/webadmin?A2=SimSoc;d4e85e43.1809) and at the end of this post. The question lead to an interesting discussion, with around 20 messages posted to the list or to me personally. My colleague [Tomas Crols](https://environment.leeds.ac.uk/geography/staff/1022/dr-tomas-crols) and [I](http://www.nickmalleson.co.uk/) have collated the responses and summarised them here. The response on SIMSOC was extremely useful for us; hopefully this summary is useful to others. Thank you to everyone who replied!

## Context (briefly)

Colleagues and I have built a spatially-realistic agent-based model with agents who move around. It’s based on a real urban area. We use a national survey to calibrate the agents' behaviours and then simulate behaviour at a local level. We have _no data on the spatial behaviour of the real individuals in our study area_, which is the motivation for the work in the first place. If we had good data for model validation, then we wouldn't need this model. **So how do we validate the model?**

## Suggestions from SIMSOC

### Use the work to motivate further data collection

We could reframe the purpose of the work. Rather than trying to use the model to explore a case study area, we could use it to discover what kind of empirical research is needed to gain more insight into the system.

Using the simulations we might discover that some variables make no difference, whereas others have a large influence.

### Use alternative data or other types of validation

Here people suggested alternative ways of validating the model, or innovative uses of alternative data sets.

 - Use alternative, national-level surveys. E.g. in the UK there is the National Transport Survey. This would not be as reliable as a direct validation of the case study area, but would give more evidence that the model is producing sensible patterns.
 - Use an alternative case study area as a proof of the generalisability of the model. 
 - Compare the model outcomes to a different measure, e.g. using space syntax measures as a proxy estimate of pedestrian flows.
 - Look at 'interesting events' to see whether the model (with suitable parameterisation) is able to capture these.
 - Validate the _model logic_ through peer review and discussions with unbiased experts (or by wrapping hot towels round our heads :-) ). This could help to validate the formalisation of our knowledge that we have implemented.
 - Good parameter sweeps / monte-carlo testing of the model might help to give evidence that the model is relatively stable and, therefore, the results are less likely to have emerged by chance. Similarly, we could use _docking_ (re-implementation of the model) to show that the logic is 'correct' (at in terms of implementation).

Broadly, these might be summarised as a _pattern oriented modelling_ approach. We could do a many levelled, qualitative validation. We can check that the outcome distributions are the right shape (or other known facts about people) to simultaneously constrain the simulation in many aspects/dimensions/scales at once.

### Argue against the need for validation

We could argue against the necessity for validation in this case. (_I should note that this is a tricky one for this work: if we had good validation data then we wouldn't need the model in the first place, but we are making assumptions about the accuracy of the model which can only really be quantified through some validation_). 

 - Argue that it is OK to use un-validated exploratory models as a way to explore social dynamics. 
 - Admit that there is no way to validate the model or the mechanisms driving it, but point out that this is common practice with existing 'thought models' 
 - Argue that this is one of the only ways to get any insight into the real dynamics of the system in the absence of full real data.
 - Validation (in the social sciences at least) is subjective anyway, so there is no single 'correct' way to validate.
 - Validation is less important when not trying to make forecasts (which would be difficult without validation because there would be too much speculation), but rather to shed some light on the present situation. 

### Calibrate to different spatial levels

Some contributors suggested trying to model at a larger spatial level first. For example a spatial interaction model could be used to estimate commuting flows, and these could be compared to the ABM. 

Similarly, we could use spatial microsimulation / synthetic population generation to compare the demographics of the study area to those of the national survey and re-weight the behaviours as appropriate. This isn't strictly 'validation', but provides some more evidence that the behaviours are reliable. 



## Useful References

Many authors suggsted some useful reading:

 - Edmonds, Bruce. 2017. “Different Modelling Purposes.” In _Simulating Social Complexity: A Handbook_, edited by Bruce Edmonds and Ruth Meyer, 39–58. Cham: Springer International Publishing. [https://doi.org/10.1007/978-3-319-66948-9_4](https://doi.org/10.1007/978-3-319-66948-9_4).

 - Filatova, Tatiana, J. Gary Polhill, and Stijn van Ewijk. 2016. “Regime Shifts in Coupled Socio-Environmental Systems: Review of Modelling Challenges and Approaches.” _Environmental Modelling & Software_ 75 (January): 333–47. [https://doi.org/10.1016/j.envsoft.2015.04.003](https://doi.org/10.1016/j.envsoft.2015.04.003).

 - Grimm, Volker, Eloy Revilla, Uta Berger, Florian Jeltsch, Wolf M. Mooij, Steven F. Railsback, Hans-Hermann Thulke, Jacob Weiner, Thorsten Wiegand, and Donald L. DeAngelis. 2005. “Pattern-Oriented Modeling of Agent-Based Complex Systems: Lessons from Ecology.” _Science_ 310 (5750): 987–91. [https://doi.org/10.1126/science.1116681](https://doi.org/10.1126/science.1116681).

 - Hassan, Samer, Javier Arroyo, José Manuel Galán, Luis Antunes, and Juan Pavón. 2013. “Asking the Oracle: Introducing Forecasting Principles into Agent-Based Modelling.” _Journal of Artificial Societies and Social Simulation_ 16 (3). [https://doi.org/10.18564/jasss.2241](https://doi.org/10.18564/jasss.2241).

 - Oreskes, Naomi, Kristin Shrader-Frechette, and Kenneth Belitz. 1994. “Verification, Validation, and Confirmation of Numerical Models in the Earth Sciences.” _Science_ 263 (5147): 641–46.

 - Polhill, Gary, and Doug Salt. 2017. “The Importance of Ontological Structure: Why Validation by ‘Fit-to-Data’ Is Insufficient.” In _Simulating Social Complexity_, edited by Bruce Edmonds and Ruth Meyer, 141–72. Springer International Publishing. [https://doi.org/10.1007/978-3-319-66948-9_8](https://doi.org/10.1007/978-3-319-66948-9_8).

<hr/>

## Original Message on SIMSOC (available [here](https://www.jiscmail.ac.uk/cgi-bin/webadmin?A2=SimSoc;d4e85e43.1809))

Hi SIMSOC, 
 
I was wondering if anyone has any thoughts/advice about a difficulty that I’m having with validating a model. This is in response to (very fair) comments by reviewers on a paper that is under review, so I will talk about the problem in general terms. I think the discussion should be of interest to others on the list. 
 
Colleagues and I have built a spatially-realistic agent-based model with agents who move around. It’s based on a real urban area. We have used an a-spatial survey to calibrate the behavioural parameters, such that the agents behave in a way that is consistent with the results of the survey. The survey is national, so not specific to our study area. We put the agents into a virtual environment, let them go, and see what happens. 
 
The reason for creating this model in the first place is that we have no data on the spatial behaviour of the real individuals in our study area. So we’re hoping that by implement behaviour that is consistent with the results of the survey, the agents will give us some insight into the real dynamics of the case study area. 
 
But how do we validate the model? Assume that there are no empirical data available for our study area (it is possible to try to stand on the road and talk to people, but this is probably out of scope). What should an aent-based modeller do when they have an empirical model but no empirical validation data?? 
 
All the best, 

Nick 
