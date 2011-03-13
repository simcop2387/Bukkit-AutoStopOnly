/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.hailxenu.serverautostop;

import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;

public class AutoStopPlayerListener extends PlayerListener {

    private AutoStopPlugin plugin;

    public AutoStopPlayerListener(AutoStopPlugin p)
    {
            this.plugin = p;
    }

    @Override
    public void onPlayerCommandPreprocess(PlayerChatEvent event)
    {
        String[] split = event.getMessage().split(" ");
        String command = split[0];

        if(command.equalsIgnoreCase("/reboot") || command.equalsIgnoreCase("/restart"))
        {
            boolean canUse = false;
            if(plugin.perms != null)
            {
                canUse = plugin.pHandler.has(event.getPlayer(), "autostop.use");
            } else {
                canUse = event.getPlayer().isOp();
            }

            if(canUse)
            {
                event.getPlayer().getServer().broadcastMessage(ChatColor.RED + "Retarting server...");
                plugin.LoopThread.Running = false;
                plugin.LoopThread.forceShutdown();
            } else {
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot use this command.");
            }

        }
    }

}
