/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simburglar.exceptions;

/**
 * Thrown if there is a problem with agent routing.
 * @author Nick Malleson
 */
public class RoutingException extends Exception {

    public RoutingException(String msg) {
        super(msg);
    }
    
}
