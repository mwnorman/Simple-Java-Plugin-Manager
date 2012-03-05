package simple.test;

import javax.annotation.Resource;

public class Helper2 implements Helper {

	@Resource(description="some random unique resource name", type = Integer.class)
	int someValue;
	
    public Helper2() {
        //public default constructor required
    }
    
    public void help() {
        System.out.printf("(some value=%d)some more help\n", someValue);
    }
}