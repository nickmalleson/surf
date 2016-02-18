/**
 ** Entrance.java
 **
 ** Copyright 2011 by Sarah Wise, Mark Coletti, Andrew Crooks, and
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id: Entrance.java 842 2012-12-18 01:09:18Z mcoletti $
 **/
package sim.app.geo.sillypeds;



public class Entrance
{
    Space space;
    Tile entrance;

    public Entrance(Space s, Tile t)
    {
        space = s;
        entrance = t;
    }
}
