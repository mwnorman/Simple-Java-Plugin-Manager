/*
 * This software is licensed under the terms of the ISC License.
 * (ISCL http://www.opensource.org/licenses/isc-license.txt
 * It is functionally equivalent to the 2-clause BSD licence,
 * with language "made unnecessary by the Berne convention" removed).
 * 
 * Copyright (c) 2012, Mike Norman
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER
 * RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE
 * USE OR PERFORMANCE OF THIS SOFTWARE.
 * 
 */
package org.simple.pluginspi;

//javase imports
import java.io.File;
import java.io.FilenameFilter;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

//java eXtension imports
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.SEVERE;

public class PluginManager {

	public static final String PLUGINMANAGER_LOGNAME = "org.simple.pluginspi";
    static final String DOT_CLASS = ".class";
    static final String PACKAGE_INFO_CLASS = "package-info" + DOT_CLASS;
    static final FilenameFilter PACKAGE_INFO_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(PACKAGE_INFO_CLASS);
        }
    };
    static final FilenameFilter DIRECTORIES_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return new File(dir, name).isDirectory();
        }
    };
    public static PluginManager getPluginManager() {
        return PluginManagerHelper.getPluginManager();
    }
    public static void setPluginManager(PluginManager pluginManager) {
        PluginManagerHelper.setPluginManager(pluginManager);
    }
    
    protected Logger log = null;
    protected List<PackageInfo> foundPackages = new ArrayList<PackageInfo>();
    protected Map<String, ResourceInfo> resources = new HashMap<String, ResourceInfo>();

    //non-public constructor so only access is via static getPluginManager/setPluginManager()
    //however, a sub-class could override
    protected PluginManager() {
    	super();
    	initLog();
    	scanForPluginPackages();
    }

    public Logger getLog() {
        return log;
    }    
    public void setLog(Logger log) {
        this.log = log;
    }
    
    protected void initLog() {
        log = Logger.getLogger( PLUGINMANAGER_LOGNAME);
    }
    
    protected void scanForPluginPackages() {
        String javaClassPath = null;
        try {
            javaClassPath = System.getProperty("java.class.path");
        }
        catch (Exception e) {
        	if (log != null && log.isLoggable(SEVERE)) {
                log.log(SEVERE, "unable to retrieve java.class.path property", e);
            }
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        if (log != null && log.isLoggable(FINE)) {
        	log.log(FINE, "scanning classpath for plugins ...");
        }
        StringTokenizer tokenizer = new StringTokenizer(javaClassPath, File.pathSeparator);
        while (tokenizer.hasMoreTokens()) {
            String possibleStartingDirectoryPath = tokenizer.nextToken();
            File possibleStartingDir = new File(possibleStartingDirectoryPath);
            if (possibleStartingDir.isDirectory()) {
                File startingDir = possibleStartingDir;
                List<File> packageInfoFiles = new ArrayList<File>();
                scanDirForMatchingFiles(packageInfoFiles, startingDir, PACKAGE_INFO_FILTER, log);
                if (packageInfoFiles.size() > 0) {
                    for (File packageInfoFile : packageInfoFiles) {
                        PackageInfo pi = new PackageInfo();
                        pi.rootPath = startingDir;
                        pi.packageInfoClass = packageInfoFile;
                        foundPackages.add(pi);
                    }
                }
            }
            else if (possibleStartingDir.isFile() && possibleStartingDir.getPath().endsWith(".jar")) {
                List<String> packageInfoClassNames = new ArrayList<String>();
                scanJarForMatchingFiles(packageInfoClassNames, possibleStartingDir, log);
                if (packageInfoClassNames.size() > 0) {
                    for (String packageInfoClassName : packageInfoClassNames) {
                        try {
                            PackageInfo pi = new PackageInfo();
                            Package pkg = cl.loadClass(packageInfoClassName).getPackage();
                            pi.pkg = pkg;
                            foundPackages.add(pi);
                        }
                        catch (Exception e) {
                        	if (log != null && log.isLoggable(SEVERE)) {
                                log.log(SEVERE, "problem loading " + packageInfoClassName, e);
                            }
                        }
                    }
                }
            }
        }
        if (foundPackages.size() > 0) {
            for (PackageInfo packageInfo : foundPackages) {
                if (packageInfo.pkg == null) {
                    String rootPath = packageInfo.rootPath.getPath();
                    String packageInfoPath = packageInfo.packageInfoClass.getPath();
                    String className = packageInfoPath.substring(rootPath.length()+1,
                        packageInfoPath.length() - DOT_CLASS.length()).replaceAll("[\\\\,/]", ".");
                    try {
                        Package pkg = cl.loadClass(className).getPackage();
                        packageInfo.pkg = pkg;
                        if (log != null && log.isLoggable(FINE)) {
                            log.log(FINE, "found plugin(s) in package: " + pkg.getName());
                        }
                    }
                    catch (Exception e) {
                    	if (log != null && log.isLoggable(SEVERE)) {
                            log.log(SEVERE, "problem loading " + className, e);
                        }
                    }
                }
            }
        }
    }

    public <T> List<T> findPlugins(Class<T> pluginClass) {
    	if (log != null && log.isLoggable(FINE)) {
            StringBuilder sb = new StringBuilder("scanning for <");
            sb.append(pluginClass.getName());
            sb.append(">plugins ... ");
            log.log(FINE, sb.toString());
    	}
        List<T> plugins = new ArrayList<T>();
        for (PackageInfo packageInfo : foundPackages) {
            Plugin p = packageInfo.pkg.getAnnotation(Plugin.class);
            if (p != null) {
                for (Class<?> c1 : p.value()) {
                    for (Class<?> interfaz : c1.getInterfaces()) {
                        if (interfaz.equals(pluginClass)) {
                            Class<? extends T> c2 = c1.asSubclass(pluginClass);
                            if (log != null && log.isLoggable(FINE)) {
                                StringBuilder sb = new StringBuilder("found <");
                                sb.append(pluginClass.getName());
                                sb.append(">plugin: ");
                                sb.append(c2.getName());
                                log.log(FINE, sb.toString());
                            }
                            T newPlugin = null;
                            try {
                                newPlugin = c2.newInstance();
                            }
                            catch (Exception e) {
                            	if (log != null && log.isLoggable(SEVERE)) {
                                    log.log(SEVERE, "problem creating plugin " + c2.getName() , e);
                                }
                            }
                            if (newPlugin != null) {
                            	plugins.add(newPlugin);
                            	List<ResourceAnnotation> resourceAnnotations = new ArrayList<ResourceAnnotation>();
                            	Field fields[] = c2.getDeclaredFields();
                            	for (Field field : fields) {
                            		if (field.isAnnotationPresent(Resource.class)) {
                            			ResourceAnnotation ra = new ResourceAnnotation();
                            			ra.field = field;
                            			ra.resourceAnnotation = field.getAnnotation(Resource.class);
                            			resourceAnnotations.add(ra);
                            		}
                            	}
                            	for (ResourceAnnotation ra : resourceAnnotations) {
                            		Resource resource = ra.resourceAnnotation;
                            		ResourceInfo resourceInfo = resources.get(resource.description());
                            		if (resourceInfo != null) {
                            			if (resource.type().isAssignableFrom(resourceInfo.resourceType)) {
                            				if (!ra.field.isAccessible()) {
                            					ra.field.setAccessible(true);
                            				}
                            				try {
                                                if (log != null && log.isLoggable(FINEST)) {
                                                	log.log(FINEST, "injecting '" +
                                                		resource.description() + "' into " +
                                                		newPlugin.toString() + "." + 
                                                		ra.field.getName());
                                                }
												ra.field.set(newPlugin, resourceInfo.resource);
											}
                            				catch (Exception e) {
			                                	if (log != null && log.isLoggable(SEVERE)) {
                                                	log.log(SEVERE, "problem injecting '" +
                                                		resource.description() + "' into " +
                                                		newPlugin.toString() + "." + 
                                                		ra.field.getName());
			                                    }
											}
                            			}
                            		}
                            	}
                                try {
                                    Method[] declaredMethods = c2.getDeclaredMethods();
                                    for (Method m : declaredMethods) {
                                        if (m.isAnnotationPresent(PostConstruct.class)) {
                                            if (!m.isAccessible()) {
                                                m.setAccessible(true);
                                            }
                                            if (log != null && log.isLoggable(FINEST)) {
                                                StringBuilder sb = new StringBuilder(
                                                	"invoking @PostConstruct method on ");
                                                sb.append(newPlugin.toString());
                                                log.log(FINEST, sb.toString());
                                            }
                                            m.invoke(newPlugin);
                                        }
                                    }
                                }
                                catch (Exception e) {
                                	if (log != null && log.isLoggable(SEVERE)) {
                                        log.log(SEVERE, "problem invoking plugin " + 
                                        	newPlugin.toString() + "'s @PostConstruct method " , e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return plugins;
    }
    
    public <T> void addResource(String description, Class<T> resourceType, T resource) {
    	ResourceInfo resourceInfo = resources.get(description);
    	if (resourceInfo == null) {
    		resourceInfo = new ResourceInfo();
    		resources.put(description, resourceInfo);
    	}
    	resourceInfo.resourceType = resourceType;
    	resourceInfo.resource = resource;
    }

    static void scanDirForMatchingFiles(List<File> matchingFiles, File currentDir, FilenameFilter filter,
        Logger log) {
        if (currentDir.isDirectory()) {
            File[] childFiles = currentDir.listFiles(filter);
            for (int i = 0; i < childFiles.length; i++) {
                if (log != null && log.isLoggable(Level.FINEST)) {
                    StringBuilder sb = new StringBuilder("scan found ");
                    sb.append(PACKAGE_INFO_CLASS);
                    sb.append(' ');
                    sb.append(childFiles[i].toString());
                    log.log(FINEST, sb.toString());
                }
                matchingFiles.add(childFiles[i]);
            }
            String[] childDirs = currentDir.list(DIRECTORIES_FILTER);
            for (int i = 0; i < childDirs.length; i++) {
                scanDirForMatchingFiles(matchingFiles, new File(currentDir, childDirs[i]), filter, log);
            }
        }
    }

    static void scanJarForMatchingFiles(List<String> matchingFiles, File jarfile, Logger log) {
        try {
            JarFile jar = new JarFile(jarfile);
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().toString();
                if (name.endsWith(PACKAGE_INFO_CLASS)) {
                    String className = name.substring(0, name.length() - DOT_CLASS.length())
                        .replaceAll("[\\\\,/]", ".");
                    if (log != null && log.isLoggable(FINEST)) {
                        StringBuilder sb = new StringBuilder("scan found class ");
                        sb.append(className);
                        sb.append(" in jar ");
                        sb.append(jarfile);
                        log.log(FINEST, sb.toString());
                    }
                    matchingFiles.add(className);
                }
            }
        }
        catch (Exception e) {
        	if (log != null && log.isLoggable(SEVERE)) {
                log.log(SEVERE, "problem scanning jar for matching files", e);
            }
        }
    }
    
    static class PackageInfo {
        File rootPath;
        File packageInfoClass;
        Package pkg;
    }
    
    static class ResourceInfo {
        Class<?> resourceType;
        Object resource;
    }
    
    static class ResourceAnnotation {
        Field field;
        Resource resourceAnnotation;
    }
    
    static class PluginManagerHelper {
        static PluginManager PLUGIN_MANAGER_SINGLETON = new PluginManager();
        static PluginManager getPluginManager() {
            return PluginManagerHelper.PLUGIN_MANAGER_SINGLETON;
        }
        static void setPluginManager(PluginManager pluginManager) {
            PLUGIN_MANAGER_SINGLETON = pluginManager;
        }
    }
    
    @Retention(RUNTIME)
    @Target(PACKAGE)
    public static @interface Plugin {
    	public Class<?>[] value() default {};
    } 
    
}