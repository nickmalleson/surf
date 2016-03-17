# Simulating Urban Flows (surf) Agent-Based Model

_README under development_

This is the main ABM for [surf](http://surf.leeds.ac.uk/) project.

## Installing 

### Installing the `config` library

The model uses a library for reading configuration files called [config](https://github.com/typesafehub/config)

To get and build config you need the scala build tool (sbt). Then, from the root project directory (surf) do:

```
cd abm/resources/
git clone https://github.com/typesafehub/config
sbt
 > compile
```

(Note: the last command is executed from within the scala build tool)

## Configuration

The main configuration file is:  `abm/surf-abm/src/surf-abm.conf`

To configure how the log works, edit: `abm/surf-abm/src/log4j.properties`
