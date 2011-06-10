package net.simcop.serverautostoponly;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Calendar;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Properties;
import java.io.*;

import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoStopOnlyPlugin extends JavaPlugin
{
    public Logger Log = Logger.getLogger("Minecraft");
    static String mainDirectory = "plugins" + File.separator + "AutoStopOnly"; //sets the main directory for easy reference
    static File Config = new File(mainDirectory + File.separator + "autostoponly.properties"); //the file separator is the / sign, this will create a new Zones.dat files in the mainDirectory variable listed above, if no Zones directory exists then it will automatically be made along with the file.
    static Properties prop = new Properties(); //creates a new properties file
    public AutoStopOnlyLoop LoopThread;

    public void onEnable()
    {
        PluginManager pluginManager = getServer().getPluginManager();
        BufferedWriter Writer = null;

        new File(mainDirectory).mkdir(); //makes the Config directory/folder in the plugins directory

        if(!Config.exists()) { //Checks to see if the config file exists, defined above, if it doesn't exist then it will do the following. the&nbsp;! turns the whole statement around, checking that the file doesn't exist instead of if it exists.
          try {
            Config.createNewFile(); //creates the file zones.dat
            FileOutputStream out = new FileOutputStream(Config); //creates a new output steam needed to write to the file
            prop.put("stoptime", "12:00:00");
            prop.put("warntime", "30");
            prop.put("warnmsg", "Shutting down the server");
            prop.store(out, "'stoptime' uses 24-hour time. Separate times with a space. [Hour:Minute:Second]\r\n"+
                            "'warntime' is how many seconds before shutdown/restart to show warning.\r\n" +
                            "'warnmsg' is what to tell users.");
            out.flush();  //Explained below in tutorial
            out.close(); //Closes the output stream as it is not needed anymore.
            Log.log(Level.INFO, "[AutoStopOnly] autostoponly.properties created");
          } catch (IOException ex) {
            Log.log(Level.WARNING, "[AutoStopOnly] Exception while creating autostoponly.properties\n");
            ex.printStackTrace(); //explained below.
          }
        }

        String stoptime, warntime, warnmsg;
        //stoptime = warntime = warnmsg = "";
        stoptime = "12:00:00";
        warntime = "30";
        warnmsg = "Shutting down server...";

        try
        {
          FileInputStream in = new FileInputStream(Config); //Creates the input stream
          prop.load(in); //loads the file contents of zones ("in" which references to the zones file) from the input stream.
          stoptime = prop.getProperty("stoptime");
          warntime = prop.getProperty("warntime");
          warnmsg = prop.getProperty("warnmsg");
        }
        catch(Exception e)
        {
            Log.log(Level.WARNING, "[AutoStopOnly] Exception while reading autostoponly.properties, using defaults");
            e.printStackTrace();
        }

        LoopThread = new AutoStopOnlyLoop(stoptime, warntime, warnmsg, this.getServer(), Log);
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

class AutoStopOnlyLoop implements Runnable
{

    public Calendar Cal;
    public ArrayList<StopTime> StopTimes, WarnTimes;
    public int WarnTime;
    public String WarnMessage;
    public Boolean Warned = false, Running = true;
    public org.bukkit.Server MCServer;
    public Logger Log;
    public FakeOp fakeperson;

    public AutoStopOnlyLoop(String stoptime, String warntime, String warnmsg, org.bukkit.Server MCServer, Logger Log)
    {
        this.MCServer = MCServer;
        this.StopTimes = new ArrayList<StopTime>();
        this.WarnTimes = new ArrayList<StopTime>();
        this.fakeperson = new FakeOp(MCServer);
        
        String[] t;
        for(String s : stoptime.split(" "))
        {
            try{
                t = s.split(":");
                StopTimes.add(new StopTime(Integer.parseInt(t[0]), Integer.parseInt(t[1]), Integer.parseInt(t[2])));
            }catch(Exception e){} // I don't like this, i may remove it or make it do a stack trace
        }

        WarnTimes.add(new StopTime(0, 0, Integer.parseInt(warntime)));

        this.WarnMessage = warnmsg;
        if(this.WarnMessage.trim().equals(""))
        {
            this.WarnMessage = "Scheduled shutdown started.";
        }

        this.Log = Log;
    }

    public void forceShutdown()
    {
        MCServer.broadcastMessage(WarnMessage);

        try{
            MCServer.savePlayers(); // make sure players are saved
            for(org.bukkit.World w : MCServer.getWorlds())
            {
                w.save(); // make sure worlds are saved
            }

            MCServer.dispatchCommand(fakeperson, "save-all");
            MCServer.dispatchCommand(fakeperson, "stop");
        }catch(Exception e){}
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

            for(StopTime t : StopTimes){

                for(StopTime w : WarnTimes){
                    if(t.doWarn(w))
                    {
                        MCServer.broadcastMessage(org.bukkit.ChatColor.RED + WarnMessage);
                    }
                }

                if(t.isNow())
                {
                  forceShutdown(); // call the existing code
                }
            }

            try
            {
                Thread.sleep(750);
            }
            catch(Exception e){}
        }
    }
}

class FakeOp implements org.bukkit.command.CommandSender {
  org.bukkit.Server server;
  FakeOp(org.bukkit.Server server) {this.server = server;}
  public void sendMessage(String message) {}
  public boolean isOp() {return true;}
  public org.bukkit.Server getServer() {return server;}
}
