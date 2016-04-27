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


## Note: Installing the `config` library

I have now included the `config` lubrary with the main project, so there is no need for separate installation. This is how to install the [config](https://github.com/typesafehub/config) library (for reading configuration files).

To get and build config you need the scala build tool (sbt). Then, from the root project directory (surf) do:

```
cd abm/resources/
git clone https://github.com/typesafehub/config
sbt
 > compile
```

(Note: the last command is executed from within the scala build tool)
