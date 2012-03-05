package simple.test;

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