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

public class AutoStopPlugin extends JavaPlugin
{
    public Logger Log = Logger.getLogger("Minecraft");
    public Thread LoopThread;

    public void onEnable()
    {
        PluginManager pluginManager = getServer().getPluginManager();
        BufferedWriter Writer = null;

        /*
        new File("plugins/AutoStop/AutoRestart.jar").delete();
        new File("plugins/AutoStop/").mkdir();
        InputStream f1 = this.getClass().getResourceAsStream("/AutoRestart.jar");
        FileOutputStream f2 = null;
        try {
             f2 = new FileOutputStream(new File("plugins/AutoStop/AutoRestart.jar"));
             byte[] buf = new byte[1024];
             int len;
             while ((len = f1.read(buf)) > 0){
               f2.write(buf, 0, len);
             }
             f2.flush();
             f1.close();
             f2.close();
        } catch (Exception ex) { ex.printStackTrace(); }
        */

        if(!new File("plugins/AutoStop/autostop.properties").exists())
        {
            try
            {
                new File("plugins/AutoStop/autostop.properties").createNewFile();
                Writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("plugins/AutoStop/autostop.properties")));
                Writer.write("stoptime=12:00\r\n# Use 24-hour time. [Hour:Minute]\r\n");
                Writer.write("warntime=11:59\r\n# Displays warning message at this time. [Hour:Minute]\r\n");
                Writer.write("warnmsg=\r\n# Warning message to display.\r\n");
                //Writer.write("enablerestart=false\r\n# Enables automatic server restarts. If this is true, path must not be blank.\r\n");
                //Writer.write("path=\r\n# Path to server file (including any arguments). This can also be a command if you are using crontab/screen/etc.\r\n");
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



        LoopThread = new Thread(new AutoStopLoop(stoptime, warntime, warnmsg, enablerestart, path, this.getServer(), Log));
        LoopThread.start();

        Log.log(Level.INFO, "[AutoStop] Started. Schedule for shutdown at " + stoptime);
    }

    public void onDisable()
    {
        LoopThread.stop();
        Log.log(Level.INFO, "[AutoStop] Disabled. Server will not shutdown at scheduled time.");
    }

}

class AutoStopLoop implements Runnable
{

    public Calendar Cal;
    public int StopHour, StopMinute, WarnMinute, WarnHour, WarnSecond;
    public String WarnMessage, Path;
    public Boolean EnableRestart = false, Warned = false;
    public org.bukkit.Server MCServer;
    public Logger Log;

    public AutoStopLoop(String stoptime, String warntime, String warnmsg,
            Boolean enablerestart, String path, org.bukkit.Server MCServer, Logger Log)
    {
        this.MCServer = MCServer;
        this.StopHour = Integer.parseInt(stoptime.split(":")[0]);
        this.StopMinute = Integer.parseInt(stoptime.split(":")[1]);
        this.WarnHour = Integer.parseInt(warntime.split(":")[0]);
        this.WarnMinute = Integer.parseInt(warntime.split(":")[1]);
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

        while(true)
        {
            Cal = Calendar.getInstance();
            hour = Cal.get(Calendar.HOUR_OF_DAY);
            minute = Cal.get(Calendar.MINUTE);
            second = Cal.get(Calendar.SECOND);

            if(WarnHour == hour && WarnMinute == minute && !Warned)
            {
                MCServer.broadcastMessage(WarnMessage);
                Warned = true;
            }

            if(StopHour == hour && StopMinute == minute)
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
                        Process p = Runtime.getRuntime().exec("java -jar plugins/AutoStop/AutoRestart.jar " + Path);
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
                Thread.sleep(5000); // 5 Seconds
            }
            catch(Exception e){}
        }
    }
}
