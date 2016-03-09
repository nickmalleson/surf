package hello;

import java.util.ArrayList;

public class HelloWorld {

    public ArrayList<Integer> l;

    public HelloWorld() {

        l = new ArrayList<Integer>();
        l.add(2);
        l.add(10);

    }

    public static void main(String[] args) {
        // Prints "Hello, World" to the terminal window.
        System.out.println("Hello, World");
        HelloWorld h = new HelloWorld();
        System.out.println(h.l.toString());
    }

}

