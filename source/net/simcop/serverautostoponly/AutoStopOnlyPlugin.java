package net.simcop.serverautostoponly;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Calendar;
import java.util.Scanner;
import java.util.ArrayList;
import java.io.*;

import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoStopOnlyPlugin extends JavaPlugin
{
    public Logger Log = Logger.getLogger("Minecraft");

    public void onEnable()
    {
        PluginManager pluginManager = getServer().getPluginManager();
        BufferedWriter Writer = null;
        pListener = new AutoStopOnlyPlayerListener(this);

        new File("plugins/AutoStopOnly/").mkdir();

        if(!new File("plugins/AutoStopOnly/autostoponly.properties").exists())
        {
            try
            {
                new File("plugins/AutoStopOnly/autostoponly.properties").createNewFile();
                Writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("plugins/AutoStopOnly/autostoponly.properties")));
                Writer.write("stoptime=12:00:00\r\n# Use 24-hour time. Separate times with a space. [Hour:Minute:Second]\r\n");
                Writer.write("warntime=0:30\r\n# How many seconds before shutdown/restart to show warning. Separate times with a space.[Seconds]\r\n");
                Writer.write("warnmsg=Shutting down server...\r\n# Warning message to display.\r\n");
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
            Scanner scan = new Scanner(new FileInputStream("plugins/AutoStopOnly/autostoponly.properties"));
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
            Log.log(Level.WARNING, "[AutoStopOnly] Exception while reading autostoponly.properties");
            e.printStackTrace();
        }


        LoopThread = new AutoStopOnlyLoop(stoptime, warntime, warnmsg, enablerestart, path, this.getServer(), Log);
        new Thread(LoopThread).start();

        Log.log(Level.INFO, "AutoStopOnly Enabled. " + System.getProperty("os.name"));
        Log.log(Level.INFO, "[AutoStopOnly] Scheduled for shutdown at time(s): " + stoptime);
    }

    public void onDisable()
    {
        LoopThread.Running = false;
        Log.log(Level.INFO, "[AutoStopOnly] Disabled.");
    }

}
