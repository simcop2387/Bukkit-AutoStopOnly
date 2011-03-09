package net.hailxenu.serverautostop;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.io.*;
import java.lang.Thread;


import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

public class AutoStopPlugin extends JavaPlugin
{
    public Logger Log = Logger.getLogger("Minecraft");
    public AutoStopLoop LoopThread;
    public AutoStopPlayerListener pListener;
    public Permissions perms;
    public PermissionHandler pHandler = null;

    public static void main(String[] args) throws Exception
    {
        BufferedWriter Writer = null;
        
        new File("plugins/AutoStop/").mkdir();
        if(!new File("plugins/AutoStop/autostop.properties").exists())
        {
            try
            {
                new File("plugins/AutoStop/autostop.properties").createNewFile();
                Writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("plugins/AutoStop/autostop.properties")));
                Writer.write("stoptime=12:00:00\r\n# Use 24-hour time. [Hour:Minute:Second]\r\n");
                Writer.write("warntime=11:59:00\r\n# Displays warning message at this time. [Hour:Minute:Second]\r\n");
                Writer.write("warnmsg=\r\n# Warning message to display.\r\n");
                Writer.write("enablerestart=false\r\n# Enables automatic server restarts. If this is true, path must not be blank.\r\n");
                Writer.write("path=\r\n# Path to server file (including any arguments). This can also be a command if you are using crontab/screen/etc.\r\n");
                Writer.flush();
                Writer.close();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        System.exit(0);
    }

    public void onEnable()
    {
        PluginManager pluginManager = getServer().getPluginManager();
        BufferedWriter Writer = null;
        pListener = new AutoStopPlayerListener(this);

        try
        {
            if(pluginManager.getPlugin("Permissions").isEnabled())
            {
                perms = ((Permissions)pluginManager.getPlugin("Permissions"));
                perms.setupPermissions();
                pHandler = perms.getHandler();
                Log.log(Level.INFO, "[AutoStop] Permissions " + perms.getDescription().getVersion() + " enabled for use.");
            }
        } catch(NullPointerException npe)
        {
            perms = null;
            Log.log(Level.INFO, "[AutoStop] Permissions not enabled.");
        }

        new File("AutoRestart.jar").delete();
        new File("plugins/AutoStop/").mkdir();
        InputStream f1 = this.getClass().getResourceAsStream("/AutoRestart.jar");
        FileOutputStream f2 = null;
        try {
             f2 = new FileOutputStream(new File("AutoRestart.jar")); // Has to be main bukkit dir
             byte[] buf = new byte[1024];
             int len;
             while ((len = f1.read(buf)) > 0){
               f2.write(buf, 0, len);
             }
             f2.flush();
             f1.close();
             f2.close();
        } catch (Exception ex) { ex.printStackTrace(); }

        if(!new File("plugins/AutoStop/autostop.properties").exists())
        {
            try
            {
                new File("plugins/AutoStop/autostop.properties").createNewFile();
                Writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("plugins/AutoStop/autostop.properties")));
                Writer.write("stoptime=12:00:00\r\n# Use 24-hour time. [Hour:Minute:Second]\r\n");
                Writer.write("warntime=11:59:00\r\n# Displays warning message at this time. [Hour:Minute:Second]\r\n");
                Writer.write("warnmsg=\r\n# Warning message to display.\r\n");
                Writer.write("enablerestart=false\r\n# Enables automatic server restarts. If this is true, path must not be blank.\r\n");
                Writer.write("path=\r\n# Path to server file (including any arguments). This can also be a command if you are using crontab/screen/etc.\r\n");
                Writer.flush();
                Writer.close();
                Log.log(Level.INFO, "[AutoStop] autostop.properties created");
            }
            catch(Exception e)
            {
                Log.log(Level.WARNING, "[AutoStop] Exception while creating autostop.properties\n");
                e.printStackTrace();
            }
        }

        pluginManager.registerEvent(Type.PLAYER_COMMAND_PREPROCESS, pListener, Priority.Normal, this);

        String stoptime, warntime, warnmsg, path;
        stoptime = warntime = warnmsg = path = "";
        Boolean enablerestart = false;

        try
        {
            Scanner scan = new Scanner(new FileInputStream("plugins/AutoStop/autostop.properties"));
            String input, var, val;
            while(scan.hasNextLine())
            {
                input = scan.nextLine();

                if(input.startsWith("#")) continue;

                var = input.substring(0, input.indexOf("=")).toLowerCase();
                val = input.substring(input.indexOf("=") + 1);

                if(var.equals("stoptime"))
                {
                    stoptime = val;
                }
                else if(var.equals("warntime"))
                {
                    warntime = val;
                }
                else if(var.equals("warnmsg"))
                {
                    warnmsg = val;
                }
                else if(var.equals("enablerestart"))
                {
                    enablerestart = Boolean.parseBoolean(val);
                }
                else if(var.equals("path"))
                {
                    path = val;
                }
            }
            
            scan.close();
        }
        catch(Exception e)
        {
            Log.log(Level.WARNING, "[AutoStop] Exception while reading autostop.properties");
            e.printStackTrace();
        }



        LoopThread = new AutoStopLoop(stoptime, warntime, warnmsg, enablerestart, path, this.getServer(), Log);
        new Thread(LoopThread).start();

        Log.log(Level.INFO, "[AutoStop] Started. Schedule for shutdown at " + stoptime);
    }

    public void onDisable()
    {
        LoopThread.Running = false;
        Log.log(Level.INFO, "[AutoStop] Disabled.");
    }

}

class AutoStopLoop implements Runnable
{

    public Calendar Cal;
    public int StopHour, StopMinute, StopSecond, WarnMinute, WarnHour, WarnSecond;
    public String WarnMessage, Path;
    public Boolean EnableRestart = false, Warned = false, Running = true;
    public org.bukkit.Server MCServer;
    public Logger Log;

    public AutoStopLoop(String stoptime, String warntime, String warnmsg,
            Boolean enablerestart, String path, org.bukkit.Server MCServer, Logger Log)
    {
        this.MCServer = MCServer;
        this.StopHour = Integer.parseInt(stoptime.split(":")[0]);
        this.StopMinute = Integer.parseInt(stoptime.split(":")[1]);
        this.StopSecond = Integer.parseInt(stoptime.split(":")[2]);
        this.WarnHour = Integer.parseInt(warntime.split(":")[0]);
        this.WarnMinute = Integer.parseInt(warntime.split(":")[1]);
        this.WarnSecond = Integer.parseInt(warntime.split(":")[2]);
        this.WarnMessage = warnmsg;
        if(this.WarnMessage.trim().equals(""))
        {
            this.WarnMessage = "Scheduled shutdown started.";
        }
        this.EnableRestart = enablerestart;
        this.Path = path;
        this.Log = Log;
    }

    public void run()
    {
        int hour, minute, second;

        while(Running)
        {
            Cal = Calendar.getInstance();
            hour = Cal.get(Calendar.HOUR_OF_DAY);
            minute = Cal.get(Calendar.MINUTE);
            second = Cal.get(Calendar.SECOND);

            if(WarnHour == hour && WarnMinute == minute && WarnSecond == second && !Warned)
            {
                MCServer.broadcastMessage(WarnMessage);
                Warned = true;
            }

            if(StopHour == hour && StopMinute == minute && StopSecond == second)
            {
                Log.log(Level.INFO, "[AutoStop] Shutting down server.");
                
                MCServer.savePlayers();
                for(org.bukkit.World w : MCServer.getWorlds())
                {
                    w.save();
                }

                if(EnableRestart)
                {
                    try
                    {
                        Process p = Runtime.getRuntime().exec("java -jar AutoRestart.jar " + Path);
                        Log.log(Level.INFO, "[AutoStop] Restarted server.");
                        System.exit(0);
                    } catch(Exception e){
                        Log.log(Level.WARNING, "[AutoStop] Exception while restarting server.");
                        e.printStackTrace();
                    }
                }

                System.exit(0);
            }

            try
            {
                Thread.sleep(500);
            }
            catch(Exception e){}
        }
    }
}
