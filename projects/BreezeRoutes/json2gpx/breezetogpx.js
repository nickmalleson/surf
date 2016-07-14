#!/usr/bin/env node

// Parameter: the input and output directories (with trailling slashes!)

// For testing: run on files in the pwd
//var INDIR = "./";
//var OUTDIR = "./";

// On MIT server:
//var INDIR  = "~/runkeeper/runkeeper-data/boston/breeze_geo/";
//var OUTDIR = "~/runkeeper/breeze-gpx/";

// On Nick's laptop (server directories mounted):
var INDIR  = "/Users/nick/mapping/projects/runkeeper/mitmount/runkeeper/runkeeper-data/boston/breeze_geo/";
var OUTDIR = "/Users/nick/mapping/projects/runkeeper/mitmount/runkeeper/breeze-gpx/";

var OVERWRITE = false; // Whether to override output gpx files

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
    
    // First, find the time offset (in seconds)
    // (No longer using offset - times are UTC)
    //var offset = parseInt(feature.properties.utcOffset);
    //console.log("\tOffset:"+offset);

    // The times associated with each coordinate (in seconds since epoch)
    times_secs = feature.properties.coordinateProperties.times;
    var times_array = []; // to return
    for ( i=0; i<times_secs.length; i++) {
        var time_sec = parseFloat(times_secs[i]); // The time in seconds
        //var time_inc_offset = time_sec + offset; // Add or subtract the offset to make GMT  
        var time_ms = Math.round(time_sec * 1000); // Make miliseconds and round to an integer to pass to Date
        var time = new Date(time_ms); // Convert to a Date with required format
        times_array.push(time.toISOString());
        //console.log("****");console.log("\ttime_sec: "+time_sec);console.log("\ttime ms: "+time_ms);console.log("\ttime: " + time);console.log("\tISO string: "+time.toISOString());
    }
    return times_array;
}


// The function that reads the json file and writes a gpx file
jsontogpx = function(filename, index) {

    var gpx_filename = filename.substring(0, filename.length - 7)+"gpx";

    // See if the gox for this file has already been created
    if (!OVERWRITE && fs.existsSync(OUTDIR+gpx_filename) ) {
        console.log("Ignoring file ("+index+") that already has gpx output: "+filename);
        return;
    }

    // Get the data
    var geojson_str = fs.readFileSync(INDIR + filename , 'utf8'); // Use synchronous file read
    console.log("Have read file "+index+": "+filename);

    // Turn it into a JSON object
    var geojson_data = JSON.parse(geojson_str);

    // Call the library, passing the callback function defined earlier that specifies how to get
    // the timestamps for each point.
    var gpx_str = togpx(geojson_data, options);

    fs.writeFileSync(OUTDIR+gpx_filename, gpx_str);
    console.log("Created GPX file: "+OUTDIR+gpx_filename);
}

/* ******************** PROGRAM ENTRY POINT ******************** */

// Read all files in the directory (https://stackoverflow.com/questions/32511789/looping-through-files-in-a-folder-node-js)

fs.readdir( INDIR, function( err, files ) {

    if( err ) {
        console.error( "Could not list the directory.", err );
        process.exit( 1 );
    }

    // Iterate over each file, calling the jsontogpx function
    files.forEach( function(file, index) {
        //if (index > 10) { // Temporarily
        //  return;
        //}
        // Check the file is a geojson
        if (file.substring(file.length - 8, file.length) == ".geojson") {
            jsontogpx(file,index);
        }
        else {
            console.log("Ignoring file ("+index+"): "+file);
        }

    } ) ; // files.foreach

    console.log("FINISHED");

} ) ; // readdir 

