# JSON to GPX

The file `breezetogpx.js` uses the [togpx](https://github.com/tyrasd/togpx) library to convert the
geojson files to GPX files. The only tricky thing is that the times associated with each coordinate
are stored in a non-standard place in the json:

```
features:properties:coordinateProperties:times
```

and the library needs to be told (via an optional callback function) where to find them.

See [../README.md](../README.md) for details about how to run the script.
