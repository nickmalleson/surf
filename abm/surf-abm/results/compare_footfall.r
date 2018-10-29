#library(sf)
#library(spData)
#library(rgdal)
#library(tidyverse)
#library(GISTools)
library(stats)      # For a-spatial aggregation (aggregate)
library(dplyr)      # for summary statistics
library(lubridate)  # For playing with dates
library(feather)    # To allow python and R to share data

# When running this script directly in R(Studio) we need to specify the current working directory
# These are the working directories of the surf model output and of the footfall data

#wd_agents = "C:/Code/surf/abm/surf-abm/results"
wd_agents <- '/Users/nick/research_not_syncd/git_projects/surf/abm/surf-abm/results'

#wd_footfall = "C:/Footfall/noggin_data/noggin_data"
wd_footfall <- '/Users/nick/research_not_syncd/git_projects/surf/abm/surf-abm/results'
#wd_footfall = ("/Users/nick/mapping/projects/frl/otley/time_use/UKDA-8128-tab/tab")







#wd_agents = '/Users/nick/research_not_syncd/git_projects/surf/abm/surf-abm/results'
setwd(wd_agents) # This is only required when running directly in R(Studio)

# Look for the right folder of the most recent model run
SCENARIO = "Commuters1" # The name of the scenario to analyse
#scenario.dir <- paste("~/research_not_syncd/git_projects/surf/abm/surf-abm/results/out/",SCENARIO,"/",sep="")
scenario.dir = paste("./out/",SCENARIO,sep="") # This is the relative directory of the scenario 
runs = sort(list.files(scenario.dir)) # All the different runs (directories)
run = tail(runs, n=1) # Most recent run/scenario
wd = paste(scenario.dir, "/", run, sep="") # The new working directory
print(paste("Using working directory of most recent model of type",SCENARIO,":",wd))

# Read model data
modelDf = read.csv(paste(wd,'/camera-counts.csv',sep=""))

# Process model dates
date = as.Date(modelDf$Date)
modelDf$datetime = as.POSIXct(date) + 3600 * modelDf$Hour
# Round the dates (useful later)
#modelDf$Day = as.POSIXct(round(modelDf$datetime, units="days" ))
modelDf$Day = as.integer(substr(modelDf$Date,9,10))
#hour = as.POSIXct(round(modelDf$datetime, units="hours"))
#modelDf$Week = floor_date(modelDf$datetime, "week") # (from lubridate library)
#modelDf$Month = floor_date(modelDf$datetime, "month")
modelDf$Weekday = weekdays(modelDf$datetime)
#modelDf$hourOfDay <- as.integer(format(modelDf$datetime, "%H")) # (https://stackoverflow.com/questions/10683224/obtain-hour-from-datetime-vector)
modelDf$HourOfDay = modelDf$Hour

# Only analyse some days
modelDf = modelDf[which(modelDf$Day == 14),]

# Summarise per camera and hour
aggModelDf = aggregate(Count~Camera + HourOfDay, data=modelDf, FUN=sum)
aggModelDf$RelCount = aggModelDf$Count / sum(aggModelDf$Count)

par(mfrow=c(2,1))
ylab="Total Count"
plot(aggregate(Count~HourOfDay, data=aggModelDf, FUN=sum), 
     main="Footfall per hour of day (model)", ylab=ylab, col="black", type="l"
)

# Read observations
setwd(wd_footfall)
obsDf = read_feather("noggin_data.feather")

# Process obs. dates
obsDf$datetime <- as.POSIXct(obsDf$Timestamp)
# Round the dates (useful later)
obsDf$Day   <-   as.POSIXct(round(obsDf$datetime, units="days" ))
obsDf$Hour  <-   as.POSIXct(round(obsDf$datetime, units="hours"))
obsDf$Week  <-   floor_date(obsDf$datetime, "week") # (from lubridate library)
obsDf$Month <-   floor_date(obsDf$datetime, "month")
obsDf$Weekday <- weekdays(obsDf$datetime)
obsDf$HourOfDay <- as.integer(format(obsDf$datetime, "%H")) # (https://stackoverflow.com/questions/10683224/obtain-hour-from-datetime-vector)

# Summarise per camera and hour
#obsDf = obsDf[which( !(obsDf$Location %in% c(16,19)) ),]
#obsDf[which(obsDf$Location==20),]$Location = 17
#aggObsDf = aggregate(Count~Location + HourOfDay, data=obsDf, FUN=sum)
#aggObsDf$RelCount = aggObsDf$Count / sum(aggObsDf$Count)

# Summarise per camera and hour (middle of the week only)
obsDf = obsDf[which( !(obsDf$Location %in% c(16,19)) & obsDf$Weekday %in% c("Tuesday","wednesday","Thursday") ),]
obsDf[which(obsDf$Location==20),]$Location = 17
aggObsDf = aggregate(Count~Location + HourOfDay, data=obsDf, FUN=sum)
aggObsDf$RelCount = aggObsDf$Count / sum(aggObsDf$Count)

ylab="Total Count"
plot(aggregate(Count~HourOfDay, data=aggObsDf, FUN=sum), 
     main="Footfall per hour of day (observations middle of week)", ylab=ylab, col="black", type="l"
)







## Create separate "sensor" (camera) data:
sensor.ids = unique(obsDf$Location)

# Use those IDs to create a new dataframe for each sensor, and add them into a list.
# This could be done with a 'for' loop over each sensor ID, or using the 'lapply' function
sensors.list = lapply(X = sensor.ids, FUN = function(id) obsDf[which(obsDf$Location==id),]   )
names(sensors.list) = c(sensor.ids) # Give each list entry the name of it's sensor (probably not necessary)

# Now average footfall for individual cameras...
sensors.data.avgweek = list() # Create a new list that will have the footfall per hour for each sensor on an average weekday
for (index in seq_along(sensors.list)) { # index is the number in the list, this isn't the same as the sensor ID
  sensor.data <- sensors.list[[index]] # Get the dataframe for the sensor out of the list
  # Create an average weekday for that dataframe and add it to the list
  avg.weekday <- aggregate(
    Count~HourOfDay, data=sensor.data[which(sensor.data$Weekday %in% c("Tuesday","Wednesday", "Thursday")),], 
    FUN=mean # (Note: mean used rather than sum to get average not total footfall)
  )
  # Some sensors might have 0s. These need fixing or removing
  if (nrow(avg.weekday)!=24) {
    stop(paste("Sensor",names(sensors.list[index]),"(list index",index,") has only",nrow(avg.weekday),"hours") )
  }
  sensors.data.avgweek[[index]] <- avg.weekday
}
rm(sensor.data,avg.weekday) # To stop getting confused later, these should only exist in the for loop
names(sensors.data.avgweek) <- c(sensor.ids) 

# For info plot the average weekday counts for each sensor
par(mfrow=c(3,1))
colours <- sample(colours(), length(sensor.ids))
for (index in seq_along(sensors.list)) {
  if (index==1) {
    plot (
      sensors.data.avgweek[[index]]$Count,
      main=paste0("Mean 'weekday' observed footfall by sensor"), 
      ylab="Mean footfall", xlab="Hour of day", col=colours[index], type="l", ylim=c(0,500), lwd=2
    )
  } else {
    lines( sensors.data.avgweek[[index]]$Count, col=colours[index], lwd=2)
  }
}
legend("topright", legend = sensor.ids, col=colours, lty=1, lwd=2)



## Now for the model results

# Use the sensor IDs to create a new dataframe for each sensor in the model, and add them into a list.
# This could be done with a 'for' loop over each sensor ID, or using the 'lapply' function
modelSensors.list = lapply(X = sensor.ids, FUN = function(id) modelDf[which(modelDf$Camera==id),]   )
names(modelSensors.list) = c(sensor.ids) # Give each list entry the name of it's sensor (probably not necessary)

# Now average footfall for individual cameras...
modelSensors.data.avgweek = list() # Create a new list that will have the footfall per hour for each sensor on an average weekday
for (index in seq_along(modelSensors.list)) { # index is the number in the list, this isn't the same as the sensor ID
  sensor.data <- modelSensors.list[[index]] # Get the dataframe for the sensor out of the list
  # Create an average weekday for that dataframe and add it to the list
  
  # Next remark is not important anymore now that subset of dates is selected in beginning of code.
    # remark: currently we're using 01/01/2011 and following days, which are Saturday and following
    # first day (Saturday) could be left out
  avg.weekday <- aggregate(
    #Count~HourOfDay, data=sensor.data[which(sensor.data$Weekday %in% c("Sunday", "Monday","Tuesday","Wednesday","Thursday","Friday")),],
    #Count~HourOfDay, data=sensor.data[which(sensor.data$Day == 14),],
    Count~HourOfDay, data=sensor.data, 
    FUN=mean # (Note: mean used rather than sum to get average not total footfall)
  )
  # with all days
  #avg.weekday <- aggregate(
  #  Count~HourOfDay, data=sensor.data, 
  #  FUN=mean # (Note: mean used rather than sum to get average not total footfall)
  #)
  
  # Some sensors might have 0s. These need fixing or removing
  if (nrow(avg.weekday)!=24) {
    stop(paste("Sensor",names(modelSensors.list[index]),"(list index",index,") has only",nrow(avg.weekday),"hours") )
  }
  modelSensors.data.avgweek[[index]] <- avg.weekday
}
rm(sensor.data,avg.weekday) # To stop getting confused later, these should only exist in the for loop
names(modelSensors.data.avgweek) <- c(sensor.ids) 

# For info plot the average weekday counts for each sensor
for (index in seq_along(modelSensors.list)) {
  if (index==1) {
    plot (
      modelSensors.data.avgweek[[index]]$Count,
      main=paste0("Mean model footfall by sensor"), 
      ylab="Mean footfall", xlab="Hour of day", col=colours[index], type="l", ylim=c(0,1000), lwd=2
    )
  } else {
    lines( modelSensors.data.avgweek[[index]]$Count, col=colours[index], lwd=2)
  }
}
legend("topright", legend = sensor.ids, col=colours, lty=1, lwd=2)

# For plot the difference between the observations and the model
for (index in seq_along(modelSensors.list)) {
  if (index==1) {
    plot (
      sensors.data.avgweek[[index]]$Count - modelSensors.data.avgweek[[index]]$Count,
      main=paste0("Difference between observations and model by sensor"), 
      ylab="Footfall difference", xlab="Hour of day", col=colours[index], type="l", ylim=c(-800,300), lwd=2
    )
  } else {
    lines( sensors.data.avgweek[[index]]$Count - modelSensors.data.avgweek[[index]]$Count, col=colours[index], lwd=2)
  }
}
legend("topright", legend = sensor.ids, col=colours, lty=1, lwd=2)
