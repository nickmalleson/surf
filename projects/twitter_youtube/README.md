# Twitter Youtube Category Scraper

## Information

This is a simple little python program that reads in some tweets, expands short URLs, and if they point to youtube it tries to find out what category the video was posted to.

## Preparation

You'll need to install python, if it isn't already installed. On a mac, the easiest way to do this is to use a package manager called [Homebrew](http://brew.sh/). Once you have homebrew, you can install python by typing the following at the Terminal:

<pre><code>brew install python</code></pre>

Google for how to install it on Windows. If you're using Linux, I'm sure you don't need any instructions :-)

Also, you need to install a python library called 'Beautiful Soup'. This is the thing that reads the youtube webpages to try to find the video category. To install it on a Mac or Linux, try typing the following into the Terminal:

<pre><code>sudo pip install beautifulsoup4</code></pre>

And if that doesn't work, try:

<pre><code>sudo easy_install beautifulsoup4</code></pre>

If that doesn't work, or if you're using Windows, have a look at the install instructions on the [beautiful soup website](http://www.crummy.com/software/BeautifulSoup/#Download).


## Running

Once Python and Beautiful Soup are ready, follow these instructions to run the program:

 1. Download the [twitter_youtube.py](https://raw.githubusercontent.com/nickmalleson/surf/master/projects/twitter_youtube/twitter_youtube.py) and save it somewhere.
 
 2. Prepare your data. The program expects to find a file called 'tweets.csv' in the same directory as the .py file. That file should be comma-separated and have the links to be followed in the first column. You can have other columns if you want, the program will ignore them. It should not have a header row at the top. See the example [tweets.csv](tweets.csv) file.

 3. Run the program simply by double-clicking on the twitter_youtube.py file that you downloaded. I think it needs python version 2 (3 is the latest one, but I've not tested it with version 3). You might need to install python first from [here](https://www.python.org/download/releases/2.7.2/). 

 4. Have a look at the output: [output.csv](./output.csv). If it worked, this file will have all the original columns from tweets.csv with a new column added that stores the category. 

**Important:** the file _appends_ new lines to the output. So if you have an old output.csv file there already and you re-run the program, it will add the new results to that existing file. To get completely new results you need to delete the output.csv file.

## Other Useful Things

I've also written a little python script ([find_urls.py](https://raw.githubusercontent.com/nickmalleson/surf/master/projects/twitter_youtube/find_urls.py)) that goes through a csv file, looks for urls, and outputs only lines in the file that have a url in them. To run it do the following:

<pre><code>python find_urls.py infil.csv outfile.csv</code></pre>

where <code>infile.csv</code> and <code>outfile.csv</code> are the names of your input and output files respectively.
