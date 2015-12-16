# Twitter Youtube Category Scraper

## Information

This is a simple little python program that reads in some tweets, expands short URLs, and if they point to youtube it tries to find out what category the video was posted to.

## Running

 1. Download the [twitter_youtube.py](https://raw.githubusercontent.com/nickmalleson/surf/master/projects/twitter_youtube/twitter_youtube.py) and save it somewhere.
 
 2. Prepare your data. The program expects to find a file called 'tweets.csv' in the same directory as the .py file. That file should be comma-separated and have the links to be followed in the first column. You can have other columns if you want, the program will ignore them. It should not have a header row at the top. See the example [tweets.csv](tweets.csv) file.

 3. Run the program simply by double-clicking on the twitter_youtube.py file that you downloaded. I think it needs python version 2 (3 is the latest one, but I've not tested it with version 3). You might need to install python first from [here](https://www.python.org/download/releases/2.7.2/). 

 4. Have a look at the output: [output.csv](./output.csv). If it worked, this file will have all the original columns from tweets.csv with a new column added that stores the category.

