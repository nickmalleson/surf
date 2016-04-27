# Simulating Urban Flows (surf) Agent-Based Model

_README under development_

This is the main ABM for [surf](http://surf.leeds.ac.uk/) project.


## Configuration

The main configuration file is:  `abm/surf-abm/src/surf-abm.conf`

To configure how the log works, edit: `abm/surf-abm/src/log4j.properties`

## Profiling

I have been using the [VisualVM](http://visualvm.java.net/) profiling tool to analyse CPU and memory use. You can install it on macs with homebrew:

```
brew cask install visualvm
```

Once it has been installed, you can run VisualVM, connect to a running process, and click on the 'sampler' and 'profiler' tabs to see how much time the CPU spends on each method, etc.



