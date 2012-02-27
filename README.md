Simple Java Plugin Manager
==========================

This is a very simple implementation of a plugin system for Java - it only has **two** classes!

The 'trick' is to leverage the little-used Java artifact
<tt>package-info.class</tt>. This gives us a consistent pattern to search for while scanning the classpath (both directories and jar files).

Once a <tt>package-info.class</tt> file is found, the <tt>java.lang.Package</tt> for it can be loaded which in turn forces the resolution of any imports and annotations in the <tt>package-info</tt> class. In the following example, three different 'helper' classes are declared to be plugins:

    @Plugin({HelperClass1.class, HelperClass2.class, HelperClass3.class})
    package simple.test;

    import org.simple.pluginspi.Plugin;

    import simple.test.HelperClass1;
    import simple.test.HelperClass2;
    import simple.test.HelperClass3;



