#!/usr/bin/env node

// Use the togpx library to convert all geojson to gpx files.

// Read the togpx library
var togpx = require('togpx');

// Get the data
var filename = "./example.json"
var fs = require('fs')
var geojson_str = fs.readFileSync(filename , 'utf8'); // Use synchronous file read
// Turn it into a JSON object
var geojson_data = JSON.parse(geojson_str);

console.log("Have read file "+filename);
//console.log(geojson_str);

// Options for togpx. Need to define a function to get the times associated with each coordinate
// (these are in an odd place in the geojson). It needs to return an array of UTC ISO 8601 
// timestamp strings (one for each feature).
// https://github.com/tyrasd/togpx
// 
// NOTE: Time zone will probably be wrong. I think that Date() assumes local time zone, but data are
// in GMT. The json have a utc_offset field so this is an easy fix if important.

var options = new Object();
options.featureCoordTimes = function(feature) {
    //console.log(feature) // Useful for debugging
    times_secs = feature.properties.coordinateProperties.times;
    var times_array = []; // to return
    for ( i=0; i<times_secs.length; i++) {
        var time_sec = Math.round(parseFloat(times_secs[i])); 
        var time = new Date(time_sec*1000); // Convert to a Date with required format
        times_array.push(time);
        console.log(i+" - "+time_sec +" - "+ time);
    }
    return times_array;
}

// Call the library, passing a callback that specifies how to get the timestamps for each point
//gpx_str = togpx(geojson_data, featureCoordTimes = function(feature) {
//    console.log(feature)
//}
//gpx_str = togpx(geojson_data, featureCoordTimes = "coordinateProperties.times");
var gpx_str = togpx(geojson_data, options);

fs.writeFileSync(filename.substring(0, filename.length - 4)+"gpx", gpx_str);

console.log("Created GPX:\n"+gpx_str);
