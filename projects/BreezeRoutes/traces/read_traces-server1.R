# Used to read in the traces on the server. This is better done as four separate processes.
# A separate script 'collate_traces.R' collates the outputs from this script and the others.

# Important: as this runs as 4 different processes, the script needs to know which one
# it is so that it knows which data files to read in.
SCRIPT_NUMBER = 1

library(rgdal) 
library(GISTools)
library(parallel)
library(plotKML)   # For reading GPX files
library(gsubfn)    # For some slightly more advanced grepping

#setwd("/Users/nick/mapping/projects/runkeeper/mitmount/runkeeper/mapmatching-traces/")

# Get the USER ID from an original gpx file
read.userid <- function(filename) {
  text <- tryCatch( 
    { readLines(filename) },
    error=function(cond) {
      message(paste("Could not read file for username: ", filename, '. Message is:', cond))
      return(-1)
    }
  ) # tryCatch
  if (text==-1) {
    return(-1)
  }
  result <- grep('userId=[A-z0-9]+', text, value=TRUE) # Find the bit with the User ID
  if (length(result)==0) {
    warning(paste("No match for user im file",filename))
    return (-1)
  }
  # Match looks ok, return the userid part.
  uid <- substring(result,8)
  #print(paste("Got UID: ",uid))
  return(uid)
}

# Get the start datetime from the original gpx file
read.datetime <- function(filename) {
  text <- tryCatch( 
    { readLines(filename) },
    error=function(cond) {
      message(paste("Could not read file for username: ", filename, '. Message is:', cond))
      return(-1)
    }
  ) # tryCatch
  if (text==-1) {
    return(-1)
  }
  # Get the text in between all of the time tags
  all.times <- strapplyc(text, "<time>(.*?)</time>", simplify = c)
  
  if (length(all.times)<2) {
    warning(paste("Got fewer than two times for the trip in file:",filename))
    return (-1)
  }

  # First and last are the beginning and end. Convert these to datetime (see notes in raw_data_analysis/2-breeze-analyse_data.html)
  # NOTE: I don't know the UTC offeset, which needs to be added to times. 
  # The problem is that when the data are converted from json to gpx, the offset doesn't come through
  start <- as.POSIXct(all.times[1],                 origin="1970-01-01")
  end   <- as.POSIXct(all.times[length(all.times)], origin="1970-01-01")

  # Return the start and end times in a list
  return( list (start, end) )
}




# Paths to the original files, shortest paths, and matched paths
path.org =      "./gpx/"
path.matched =  "./gpx-matched/"
path.shortest = "./gpx-shortest/"

TRACES_FILE <- paste("./traces-server",SCRIPT_NUMBER,".RData",sep="")


orig <- list() 
matched <- list()
shortest <- list()
orig.ma <- list()# Projected versions
matched.ma <- list()
shortest.ma <- list()
userid <- list()
starttime <- list() # The start and end times of the trip 
endtime <- list()

# Split the file names into 4 lists to run on four different servers
file.names <- dir(path.matched, pattern =".gpx")
file.names.split <- split(file.names,c(1,2,3,4))
file.names <- file.names.split[[SCRIPT_NUMBER]]


for(i in seq(length(file.names))){
    #if ( i %% 5000 == 0) {
    #   print(paste("Temporarily saving traces,",i))
    #    save(orig, matched, shortest, orig.ma, matched.ma, shortest.ma, userid, file=TRACES_FILE)
    #}
    start.time <- proc.time()[['elapsed']]


    f <- substr(file.names[i], 1, nchar(file.names[i])-12) # The name of the file without the '-matched.gpx' extension.
    f.orig <-    paste(path.org,      f, ".gpx", sep="")
    f.matched <- paste(path.matched,  f, "-matched.gpx", sep="") # The matched file (f531b5395-matched.gpx)
    f.shortest<- paste(path.shortest, f, "-shortest.gpx", sep="") # The shortest path (f531b5395-shortest.gpx)
    print(paste("Reading file (",i,")",f))
    
    # Create SpatialLinesDataFrames for each track, project them to good projection for MA (Albers) add them to the lists
    # (https://www.arcgis.com/home/item.html?id=d075ba0b6b5e4d71b596e882493f7789)
    # Read the three required files simultaneously in parallel
    read.data <- mclapply(
      list("orig"=f.orig, "matched"=f.matched, "shortest"=f.shortest), 
      FUN=function(x){
        # Read the GPX and convert the $tracks to a dataframe. All wrapped in a try-catch in case file can't be read
        gpx <- as.data.frame(
          tryCatch(
            {
              readGPX(x, metadata = TRUE, bounds = TRUE,waypoints = FALSE, tracks = TRUE, routes = FALSE)$tracks
            },
            error=function(cond) {
              message(paste("Could not read file: ", x, '. Message is:', cond))
              return(NULL)
            },
            warning=function(cond) {
              message(paste("Could not read file: ", x, '. Message is:', cond))
              return(NULL)
            }
          ) #trycatch
        ) #as.data.frame
        # Work out where x,y coords are
        if ('GraphHopper.lon' %in% colnames(gpx) ) {
          xcor <- gpx$GraphHopper.lon
          ycor <- gpx$GraphHopper.lat
        } else {
          xcor <- gpx$lon
          ycor <- gpx$lat
        }
        
        lines <- SpatialLines(
          list(Lines(lapply(list(cbind(xcor, ycor)), Line), ID="a")),
          proj4string = CRS("+init=epsg:4326"))
        return(lines)
      }
    )
    orig[[i]]    =  read.data[['orig']]
    matched[[i]] =  read.data[['matched']]
    shortest[[i]] = read.data[['shortest']]
    
    transformed <- mclapply(
      list("orig.ma"=orig[[i]], "matched.ma"=matched[[i]], "shortest.ma"=shortest[[i]]),
      FUN = function(x) { return(spTransform(x, CRS("+init=epsg:5070")) ) }
    )
    orig.ma[[i]] =    transformed[['orig.ma']]
    matched.ma[[i]] = transformed[['matched.ma']]
    shortest.ma[[i]] =transformed[['shortest.ma']]
   
    # To get the user id we need to re-read the original gpx file and parse it manually. The
    # readGPX function doesn't keep the meta-data, and readOGR is too slow.
    uid <- read.userid(f.orig)
    if (uid == -1) {
      warning(paste("Not able to get the user ID for route (",i,")", f ) )
      uid = "NA"
    }
    userid[[i]] = uid

    # Same for the start date time
    times <- read.datetime(f.orig)
    if (typeof(times) != typeof(list()) )  { # If we didn't get a list back, then something went wrong.
      warning(paste("Not able to get the user start datetime for route (",i,")", f ) )
      dt = c(NA,NA)
    }
    starttime[[i]] <- times[[1]]
    endtime[[i]] <- times[[2]]


    print(paste("\t.. finished in",round(proc.time()[['elapsed']]-start.time,digits=2),"secs"))
    
    
} # for files

# Save the traces
save(orig, matched, shortest, orig.ma, matched.ma, shortest.ma, userid, starttime, endtime, file=TRACES_FILE)

if (length(matched) != length(orig) || length(matched) != length(shortest) || length(matched) != length(userid) || 
    length(matched) != length(starttime) || length(matched) != length(endtime)) {
    warning("For some reason there are different numbers of original, matched, and shortest paths.")
}

print("FINISHED!")
