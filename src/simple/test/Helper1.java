package simple.test;

import javax.annotation.PostConstruct;

public class Helper1 implements Helper {

    public Helper1() {
        //public default constructor required
    }
    
    @PostConstruct
    protected void setUp() {
        System.out.println("Helper1 setUp");
    }
    
    public void help() {
        System.out.println("with a little help from my plugin friends");
    }
}