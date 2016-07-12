#!/usr/bin/env node

// Use the togpx library to convert all geojson to gpx files.
// https://github.com/tyrasd/togpx

var fs = require('fs'); // For reading/writing files
var togpx = require('togpx'); // togpx library does most of the work


// Options for togpx. Need to define a function to get the times associated with each coordinate
// (these are in an odd place in the geojson). It needs to return an array of UTC ISO 8601 
// timestamp strings (one for each feature).
// See documentation: https://github.com/tyrasd/togpx
// 
// NOTE: Time zone will probably be wrong. I think that Date() assumes local time zone, but data are
// in GMT. The json have a utc_offset field so this is an easy fix if important.

var options = new Object();
options.featureCoordTimes = function(feature) {
    //console.log(feature) // Useful for debugging
    times_secs = feature.properties.coordinateProperties.times;
    var times_array = []; // to return
    for ( i=0; i<times_secs.length; i++) {
        var time_sec = Math.round(parseFloat(times_secs[i]*1000)); // *1000 for miliseconds 
        var time = new Date(time_sec); // Convert to a Date with required format
        times_array.push(time);
        console.log(i+" - "+time_sec +" - "+ time);
        console.log(time.toISOString());
    }
    return times_array;
}


// The function that reads the json file and writes a gpx file
jsontogpx = function(filename, index) {

    var gpx_filename = filename.substring(0, filename.length - 4)+"gpx";
    // Get the data
    var geojson_str = fs.readFileSync(filename , 'utf8'); // Use synchronous file read
    console.log("Have read file "+index+": "+filename);

    // Turn it into a JSON object
    var geojson_data = JSON.parse(geojson_str);

    // Call the library, passing the callback function defined earlier that specifies how to get
    // the timestamps for each point.
    var gpx_str = togpx(geojson_data, options);

    fs.writeFileSync(gpx_filename, gpx_str);
    console.log("Created GPX file: "+gpx_filename);
}

/* ******************** PROGRAM ENTRY POINT ******************** */

// Read all files in the directory (https://stackoverflow.com/questions/32511789/looping-through-files-in-a-folder-node-js)

fs.readdir( "./", function( err, files ) {

    if( err ) {
        console.error( "Could not list the directory.", err );
        process.exit( 1 );
    }

    // Iterate over each file, calling the jsontogpx function
    files.forEach( function(file, index) {
        if (index > 10) { // Temporarily
          return;
        }
        // Check the file is a json
        if (file.substring(file.length - 5, file.length) == ".json") {
            jsontogpx(file,index);
        }
        else {
            console.log("Ignoring file "+file);
        }

    } ) ; // files.foreach


} ) ; // readdir 

