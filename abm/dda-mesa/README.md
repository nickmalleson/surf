# ddd-mesa

A simple ABM, implemented in [mesa](https://github.com/projectmesa/mesa/), used to test dynamic data assimilation methods (specifical an Ensemble Kalman Filter).

Heavily dependent on Alice Tapper's work during her internship at Leeds


## Dependencies

### mesa 'submodule'
	 
I want to use the source version of mesa on github, to both get the latest updates and alter/read the source easily. 

If you clone the `surf` repository, then you need to run the following commands to get mesa (instructions [here](https://git-scm.com/book/en/v2/Git-Tools-Submodules)):

```
git submodule init
git submodule update
```

Then install mesa from that module:

```
cd abm/dda-mesa/mesa/
sudo python setup.py install
```

### (Note: how to add mesa as a submodule)

To do this by adding it as a git submodule:

`git submodule add https://github.com/projectmesa/mesa/`

