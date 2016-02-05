
# Runs through some tweets, expands short URLs, and if they point to youtube it tries to find out
# what category the video was posted to.
#
# To run the file:
#    python twitter_youtube.py
#
# It expects to find a file called 'tweets.csv' in the same directory.
# That file should be comma-separated and have the links to be followed in the first column.
# See the example tweets.csv file
#
# What we're looking for is a short section of html that lists the video categorym e,g,:
#    <h4 class="title">
#      Category
#    </h4>
#    <ul class="content watch-info-tag-list">
#        <li><a href="/channel/UC-9-kyTW8ZkZNDHQJ6FgpwQ" class="yt-uix-sessionlink g-hovercard      spf-link " data-ytid="UC-9-kyTW8ZkZNDHQJ6FgpwQ" data-sessionlink="ei=1rRhVvePNeX7iAbbhbfYBQ" >Music</a></li>
#    </ul>
#

from bs4 import BeautifulSoup
from urllib import urlopen
import traceback # To print a traceback if there is an unexpected error
import sys

def get_category(url):
    """Assumes that the url points to youtube. Follows it and tries to find the categoty. Returns
    the category on None if it can't find one"""

    print("Following url", url,)

    page = urlopen(url)

    html = page.read() # Read the html and save it as a string

    soup = BeautifulSoup(html, 'html.parser')

    # print(soup.prettify())
    # FInd the category by: finding the header with 'Category', moving up one to the parent tag,
    # then finding the first li tag which has the link and actual category
    # (parent.li.a.contents[0]). See the html snipped at the top to see why this works.
    
    try:

        for h in soup.find_all('h4'):
            for content in h.contents:
                if "Category" in content:
                    category = h.parent.li.a.contents[0].strip()
                    # This is the h4 tag that we're after.
                    print("\tI found the category:",category)
                    return category

    except AttributeError as e:
        print ("There was an error trying to get the category for the url {}. \n The error is '{}'".format(url, str(e)))

    print("\tI can't find a category, maybe not a youtube page?")
    return None




# ************ MAIN PROGRAM STARTS HERE *************

successes = 0 # Count the number of successful categories retrieved
total = 0 # Count the total number of urls checked

# Read a csv file with tweets in it
try:
	with open('tweets.csv', 'r') as inf:

		count = 0

		broken_urls = []

		for line in inf: # Iterate over every line in the file

			count += 1

			if (count % 100 == 0):
				print ("Read line {}".format(count))
	
		
			total += 1

			cols = line.strip().split(',') # Split the line into separate columns
			url = cols[0].strip() # The url is in the first column

			try:    
				cat = get_category(url) # Call the get_category function to get the category

				with open('output.csv', 'a') as outf:

					if cat == None: # I didn't find a category
						outf.write( line.strip() + "," + "-" + "\n" ) # Write out '-' to show that no category was found

					else: # I did find a category
						successes += 1
						outf.write ( line.strip() + "," + cat + "\n")

			except IOError:
				broken_urls.append(url)
				print("I couldnt' retrieve the URL: {}".format(url) )
				
except Exception as e: # Catch *everything* so that at least some output will be written, then we don't have to go over the same urls again.
    print ("An unexpected error has been generated: {}".format(str(e)))
    print ("The traceback is:")
    print (traceback.format_exc())
except: # For any other exception (including Keyboard interrupt etc)
    print "FAIL! Unexpected error:", sys.exc_info()[0]
    

print ("Finished! I found {} / {} pages that had a category".format(successes, total) )

print ("I also found {} broken urls. They are {}".format(len(broken_urls), broken_urls) )
