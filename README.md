Simple Java Plugin Manager
==========================

This is a very simple implementation of a plugin system for Java - it only has **one** class!

The 'trick' is to leverage the little-used Java artifact
<tt>package-info.class</tt>. This gives us a consistent pattern to search for while scanning the classpath (both directories and jar files).

Once a <tt>package-info.class</tt> file is found, the <tt>java.lang.Package</tt> for it can be loaded which in turn forces the resolution of any imports and annotations in the <tt>package-info</tt> class. In the following example, three different 'helper' classes are declared to be plugins:

```java
@Plugin({Helper1.class, Helper2.class, Helper3.class})
package simple.test;

import org.simple.pluginspi.PluginManager.Plugin;

import simple.test.Helper1;
import simple.test.Helper2;
import simple.test.Helper3;
```
Each helper class implements a common <tt>Helper</tt> interface:

```java
public interface Helper {
    public void help();
}

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

import javax.annotation.Resource;

public class Helper2 implements Helper {

    @Resource(description="some random unique resource name", type = Integer.class)
    int someValue;
    
    public Helper2() {
        //public default constructor required
    }
    
    public void help() {
        System.out.println("some more help");
    }
}

public class Helper3 implements Helper {

    public Helper3() {
        //public default constructor required
    }
    
    public void help() {
        System.out.println("that's the last bit of help yer gonna get outa me!");
    }
}
```

Now it is possible to ask the <tt>PluginManager</tt> to find all <tt>Helper</tt>'s without having to know exactly where they are on the classpath - or even what package they are in:

```java
//javase imports
import java.util.List;

//simple plugin imports
import org.simple.pluginspi.PluginManager;

//domain imports
import simple.test.Helper;

public class Main {

    public static void main(String ...args) {
        PluginManager pluginManager = PluginManager.getPluginManager();
        pluginManager.addResource("some random unique resource name", Integer.class, 3);
        List<Helper> helpers = pluginManager.findPlugins(Helper.class);
        for (Helper h : helpers) {
            h.help();
        }
    }
}
```
    with a little help from my plugin friends
    some more help
    that's the last bit of help yer gonna get outa me!
