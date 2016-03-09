package hello

/**
  * Created by nick on 09/03/2016.
  */
object Hello extends TestInterface() {
    def main(args: Array[String]): Unit = {
        println("Hello, world!");
        var h = new HelloWorld();
        println(h.l.toString());

    }

    def someMethod(): Integer= {
        print("Method called");
        return 1;
    }
}
