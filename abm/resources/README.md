# Resources for the ABMs

_These steps are no longer necessary as I have included mason, and it's third-party libraries, in the project. But these are useful for reference_

## Instructions to prepare external resources

### 

### Getting and Building MASON


It is necessary to download some stuff first and combile the MASON source. This should all really be in a Makefile, but I can't be bothered at the moment.

Unless otherwise stated, the following commands assume you are in the <code>surf/abm/resources/</code> directory

 - get the MASON source <pre><code>git clone https://github.com/eclab/mason.git</code></pre> This will make a new directory called <code>mason</code>.
 
 - Get some external libraries that MASON needs, store them in a new directory called <code>mason\_resources</code>, and extract the zip file  <pre>
<code>mkdir mason\_resources
wget -P mason\_resources/ https://cs.gmu.edu/~eclab/projects/mason/libraries.zip && unzip -d mason\_resources/ mason\_resources/libraries.zip </code>
</pre> 

 - Add those libraries to the CLASSPATH
<pre><code>export CLASSPATH=$CLASSPATH:\
${PWD}/mason\_resources/bsh-2.0b4.jar:\
${PWD}/mason\_resources/itext-1.2.jar:\
${PWD}/mason\_resources/jcommon-1.0.21.jar:\
${PWD}/mason\_resources/jfreechart-1.0.17.jar:\
${PWD}/mason\_resources/jmf.jar:\
${PWD}/mason\_resources/portfolio.jar</code></pre> 
or just:
<pre><code>export CLASSPATH=$CLASSPATH:${PWD}/resources/mason\_resources/*</code></pre>
(I think the latter works now)


 - Go into the directory that has the MASON source and build everything <pre><code>cd mason/mason/
make 
make jar
make docs</code></pre>
If <code>make jar</code> doesn't work, you probably need to install [Java3D](https://java3d.java.net/binary-builds.html). To do this: 

   1. download for your system from [here](https://java3d.java.net/binary-builds.html)
   2. Unzip the file
   3. Go into the directory that it created and unzip the <code>j3d-jre.zip</code> file.
   4. That will create a <code>lib/ext</code> that contains three files: <code>j3dcore.jar</code>, <code>j3dutils.jar</code>, and <code>vecmath.jar</code>. Copy those three files into the <code>abm/resources/mason\_resources/</code> directory.
   5. Then running <code>make jar</code> should work

 - While you're making MASON, the docs might be useful as well: <code>make docs</code>

 - And because we'll need it later, add the MASON jar file to your path:
<pre><code>export CLASSPATH=$CLASSPATH:$PWD/jar/mason.19.jar</code></pre>
(assuming you're still in the <code>surf/abm/surf-mason/mason/mason/</code> directory).

To test that everything works, you can either use one of the scripts in the <code>mason/start/</code> directory, or just run: <pre><code>java -Xmx200M -jar jar/mason.19.jar sim.display.Console</code></pre> (from the <code>surf/abm/resources/mason/mason/</code> directory).

### Getting GeoMason

[GeoMason](http://cs.gmu.edu/~eclab/projects/mason/extensions/geomason/)  comes with the main mason download in the directory `abm/resources/mason/contrib/geomason/`. It is an extension to provide support for GIS things. This is how to get the third-party resources (now included in the main project)

Again these commands assume that you are in the <code>surf/abm/resources/</code> directory.

First we need some more resources

 - The [Java Topology Suite](http://www.vividsolutions.com/jts/JTSHome.htm) 

  - Download and extract the files:
<pre><code>wget -P mason\_resources/ http://www.vividsolutions.com/jts/bin/jts-1.8.0.zip && unzip -d mason\_resources/jts-1.8.0 mason\_resources/jts-1.8.0.zip </code></pre>

  - We'll add them to the CLASSPATH later.

 - [GeoTools](https://sourceforge.net/projects/geotools/)

  - You'll need to download this manually, I can't find a direct link to the zip file.
  - Save the file in <code>mason\_resources/</code>, unzip it, then rename the directory from <code>geotools-XX</code> (where XX is some version number) to just <code>geotools</code>.

  - Now add the required classes to the classpath:
   - <pre><code>export CLASSPATH=$CLASSPATH:\
${PWD}/mason\_resources/geotools/*:\
${PWD}/mason\_resources/jts-1.8.0/lib/*</code></pre>

 - You might also need OGR from [http://gdal.org/](http://gdal.org/). That's platform specific, so go to the website and download it. I wouldn't bother with this step yet as it might not be a problem.

Finally, GeoMason!

**This is now included in my repository, but here are some instructions anyway in case you want to download and build it.**

Make sure you're in the <code>surf/abm/surf-mason/</code> directory. 

First download and extract GeoMason
<pre><code>wget http://cs.gmu.edu/~eclab/projects/mason/extensions/geomason/geomason.src.1.5.tgz && \
tar -xzvf geomason.src.1.5.tgz && \
rm geomason.src.1.5.tgz && \
wget http://cs.gmu.edu/~eclab/projects/mason/extensions/geomason/geomason.demos.1.5.tgz && \
tar -xvzf geomason.demos.1.5.tgz  && \
rm geomason.demos.1.5.tgz
</code></pre>

Then build it:

<pre><code>cd geomason-1.5
make jar
make demos
export CLASSPATH=$CLASSPATH:./geomason.1.5.jar
</code></pre>


Note: when I tried to do <code>make demos</code> it failed to build two of the demos. To correct this, edit <code>geomason-1.5/Makefile</code> and comment out lines 43 and 44 so that they look like this:

<pre><code>#sim/app/geo/sickStudents/*.java \
#sim/app/geo/turkana/*.java</code></pre>

**That's it!**

Now you can run the GeoMason examples with, e.g.:

<pre><code>java -cp $CLASSPATH:. sim.app.geo.campusworld.CampusWorldWithUI</code></pre>




## (Fixing the CLASSPATH)

The following clears the CLASSPATH and re-initialises it with everything it needs for (Geo)Mason. Make sure you're in the <code>surf/abm/surf-mason/</code> directory

<pre><code>
export CLASSPATH=""
export CLASSPATH=\
${PWD}/resources/libraries/*:\
${PWD}/mason/mason/jar/mason.19.jar:\
${PWD}/resources/geotools/*:\
${PWD}/resources/jts-1.8.0/lib/*:\
${PWD}/geomason-1.5/geomason.1.5.jar

</code></pre>
  

 

