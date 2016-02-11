# MASON ABM

Here are the files etc. for the version of the surf ABM implemented in [MASON](https://cs.gmu.edu/~eclab/projects/mason/)


## Instructions to prepare external resources

### Getting and Building MASON

It is necessary to download some stuff first and combile the MASON source. This should all really be in a Makefile, but I can't be bothered at the moment.

Unless otherwise stated, the following commands assume you are in the <code>surf/abm/surf-mason/</code> directory

 - get the MASON source <pre><code>git clone https://github.com/eclab/mason.git</code></pre> This will make a new directory called <code>mason</code>.
 
 - Get some external libraries that MASON needs, store them in a new directory called <code>resources</code>, and extract the zip file  <pre>
<code>mkdir resources
wget -P resources/ https://cs.gmu.edu/~eclab/projects/mason/libraries.zip && unzip resources/libraries.zip </code>
</pre> 

 - Add those libraries to the CLASSPATH 
<pre><code>export CLASSPATH=$CLASSPATH:\
${PWD}/resources/libraries/bsh-2.0b4.jar:\
${PWD}/resources/libraries/itext-1.2.jar:\
${PWD}/resources/libraries/jcommon-1.0.21.jar:\
${PWD}/resources/libraries/jfreechart-1.0.17.jar:\
${PWD}/resources/libraries/jmf.jar:\
${PWD}/resources/libraries/portfolio.jar</code></pre>

 - Go into the directory that has the MASON source and build everything <pre><code>cd mason/mason/
make jar</code></pre>

 - While you're there, the docs might be useful as well: <code>make docs</code>

To test that everything works, you can either use one of the scripts in the <code>mason/start/</code> directory, or just run: <pre><code>java -Xmx200M -jar jar/mason.19.jar sim.display.Console</code></pre> (from the <code>surf/abm/surf-mason/mason/mason/</code> directory).

### Getting GeoMason






  
   <pre><code></code></pre>
    <pre><code></code></pre>

 

