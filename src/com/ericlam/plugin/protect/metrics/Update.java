package com.ericlam.plugin.protect.metrics;

import com.ericlam.config.Config;
import com.ericlam.main.ItemAunction;
import com.ericlam.plugin.protect.gate.Commander;
import com.ericlam.plugin.protect.gate.Output;
import com.ericlam.plugin.protect.gate.cipher.Text;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Update extends Commander {

    private String ip;
    private Connection connection;

    protected Update(){
        try {
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
            hikariConfig.setPoolName(Config.getInstance().getConfig().getString("MySQL.Pool.name"));
            HikariDataSource source = new HikariDataSource(hikariConfig);
            this.connection = source.getConnection();
            try(PreparedStatement create = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `Data_Metrics` (`IP` VARCHAR(30) NOT NULL , `RecentOpen` TEXT NOT NULL , `OS` TEXT NOT NULL, `Port` MEDIUMINT NOT NULL )");
                PreparedStatement log = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `Error_Log` (`Time` TEXT NOT NULL, `Log` LONGTEXT NOT NULL, `Notes` LONGTEXT )")) {
                create.execute();
                log.execute();
            }
            check();
            Output.create();
            super.run();
        } catch (SQLException | IOException e) {
            new ScheduledThreadPoolExecutor(1).schedule(this,5,TimeUnit.MINUTES);
        }
    }

    private void logging(String log, String notes, LocalDateTime time){
        if (connection == null) return;
        try(PreparedStatement statement = connection.prepareStatement("INSERT  INTO `Error_Log` VALUES (?,?,?)")){
            statement.setString(1,time.toString());
            statement.setString(2,log);
            statement.setString(3,notes);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

   private void check(){
        if (ip == null || connection == null) return;
       LocalDateTime now = LocalDateTime.now();
       try(PreparedStatement statement = connection.prepareStatement("INSERT INTO `Data_Metrics` VALUES (?,?,?,?)")){
           statement.setString(1,ip);
           statement.setString(2,now.toString());
           statement.setString(3,System.getProperty("os.name"));
           statement.setInt(4,ItemAunction.plugin.getServer().getPort());
           statement.execute();
       } catch (SQLException e) {
           new ScheduledThreadPoolExecutor(1).schedule(this,5,TimeUnit.MINUTES);
           System.out.println("error");
           logging(e.getMessage(),e.getSQLState(),now);
       }

   }
}
