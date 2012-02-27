Simple Java Plugin Manager
==========================

This is a very simple implementation of a plugin system for Java - it only has
<b>2</b> classes!

The 'trick' is to leverage the little-used Java artifact
<tt>package-info.class</tt>. This gives us a consistent pattern to search for
while scanning the classpath (both directories and jar files).