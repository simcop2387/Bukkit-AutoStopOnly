package net.hailxenu.serverautostop;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Calendar;
import java.util.Date;
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
    public String StopTime;

    public void onEnable()
    {
        PluginManager pluginManager = getServer().getPluginManager();
        BufferedReader Reader;
        BufferedWriter Writer;

        if(!new File("autostop.properties").exists())
        {
            try
            {
                new File("autostop.properties").createNewFile();
                Writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("autostop.properties")));
                Writer.write("stoptime=12:00\r\n# Use 24-hour time.\r\n");
                Writer.flush();
                Log.log(Level.INFO, "[AutoStop] autostop.properties created");
            }
            catch(Exception e)
            {
                Log.log(Level.WARNING, "[AutoStop] Exception while creating autostop.properties\n");
                e.printStackTrace();
            }
        }

        try
        {
            Reader = new BufferedReader(new InputStreamReader(new FileInputStream("autostop.properties")));
            StopTime = Reader.readLine().toLowerCase().replace("stoptime=", "");
        }
        catch(Exception e)
        {
            Log.log(Level.WARNING, "[AutoStop] Exception while reading autostop.properties");
            e.printStackTrace();
        }

        LoopThread = new Thread(new AutoStopLoop(StopTime, this.getServer(), Log));
        LoopThread.start();

        Log.log(Level.INFO, "[AutoStop] Started. Schedule for shutdown at " + StopTime);
    }

    public void onDisable()
    {
        Log.log(Level.INFO, "[AutoStop] Disabled. Server will not shutdown at scheduled time.");
    }

}

class AutoStopLoop implements Runnable
{

    public Calendar Cal;
    public int StopHour, StopMinute;
    public org.bukkit.Server MCServer;
    public Logger Log;

    public AutoStopLoop(String StopTime, org.bukkit.Server MCServer, Logger Log)
    {
        this.MCServer = MCServer;
        this.StopHour = Integer.parseInt(StopTime.split(":")[0]);
        this.StopMinute = Integer.parseInt(StopTime.split(":")[1]);
        this.Log = Log;
    }

    public void run()
    {
        int hour, minute;

        while(true)
        {
            Cal = Calendar.getInstance();
            hour = Cal.get(Calendar.HOUR_OF_DAY);
            minute = Cal.get(Calendar.MINUTE);

            if(StopHour == hour && StopMinute == minute)
            {
                Log.log(Level.INFO, "[AutoStop] Scheduled shutdown started. Server will terminate in 5 seconds.");
                MCServer.broadcastMessage(org.bukkit.ChatColor.RED + "Scheduled shutdown started. Server will terminate in 5 seconds.");

                try
                {
                    Thread.sleep(5000);
                }
                catch(Exception e){}

                MCServer.savePlayers();
                for(org.bukkit.World w : MCServer.getWorlds())
                {
                    w.save();
                }
                
                System.exit(0);
            }

            try
            {
                Thread.sleep(10000); // 10 Seconds
            }
            catch(Exception e){}
        }
    }
}
