package de.themoep.simpleteamchat;

import de.themoep.servertags.bukkit.ServerInfo;
import de.themoep.servertags.bukkit.ServerTags;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SimpleTeamChat
 * Copyright (C) 2015 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class SimpleTeamChat extends JavaPlugin implements CommandExecutor, Listener {

    private ServerTags serverTags = null;
    private PermissionsEx permissionsEx = null;

    private Map<UUID, ChatDestination> playerChannels = new HashMap<UUID, ChatDestination>();

    private String globalChannel = "G";
    private String globalColor = ChatColor.GRAY + "";
    private String globalFormat = "%prefix%&f%username%%suffix%%serverinfo%&f: &7%msg%";
    private String serverInfoFormat = " &7(%tag%)";
    private String channelTagFormat = "%color%%tag% | ";
    private String teamFormat = "%tag%%prefix%&f%username%%suffix%%serverinfo%&f: &7%msg%";

    public void onEnable() {
        loadConfig();
        serverTags = (ServerTags) getServer().getPluginManager().getPlugin("ServerTags");
        permissionsEx = (PermissionsEx) getServer().getPluginManager().getPlugin("PermissionsEx");
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadConfig() {
        saveDefaultConfig();
        reloadConfig();
        globalChannel = getConfig().getString("globalchannel", globalChannel);
        try {
            globalColor = ChatColor.valueOf(getConfig().getString("globalcolor").toUpperCase()) + "";
        } catch(IllegalArgumentException e) {
            String configColorString = getConfig().getString("globalcolor");
            if(configColorString.length() == 1) {
                globalColor = ChatColor.getByChar(getConfig().getString("globalcolor").charAt(0)) + "";
            } else {
                globalColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("globalcolor"));
            }
        }
        globalFormat = ChatColor.translateAlternateColorCodes('&', getConfig().getString("globalformat", globalFormat));
        globalFormat = globalFormat.replace("%username%", "%s").replace("%msg%", "%s");

        teamFormat = ChatColor.translateAlternateColorCodes('&', getConfig().getString("teamformat", teamFormat));
        teamFormat = teamFormat.replace("%username%", "%s").replace("%msg%", "%s");

        serverInfoFormat = ChatColor.translateAlternateColorCodes('&', getConfig().getString("serverinfo", serverInfoFormat));
        channelTagFormat = ChatColor.translateAlternateColorCodes('&', getConfig().getString("channeltag", channelTagFormat));
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(cmd.getName().equalsIgnoreCase("simpleteamchat") && sender.hasPermission("simpleteamchat.command") && args.length > 0) {
            if(args[0].equalsIgnoreCase("reload") && sender.hasPermission("simpleteamchat.command.reload")) {
                loadConfig();
                sender.sendMessage(ChatColor.GREEN + "Reloaded config!");
            } else if(args[0].equalsIgnoreCase("team") && sender.hasPermission("simpleteamchat.command.team")) {
                if(args.length > 2) {
                    Team team = getServer().getScoreboardManager().getMainScoreboard().getTeam(args[1]);
                    if(team != null) {
                        String msg = args[2];
                        for(int i = 3; i < args.length; i++) {
                            msg += " " + args[i];
                        }
                        sendMessage(sender, team, msg);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Kein Team mit dem Namen " + ChatColor.YELLOW + args[1] + ChatColor.RED + " gefunden!");
                    }
                } else {
                    sender.sendMessage("Usage: /" + label.toLowerCase() + " team <teamname> <msg>");
                }
            } else {
                return false;
            }
        } else if(sender.hasPermission("SimpleTeamChat.chat")){
            if(args.length == 0) {
                if(sender instanceof Player) {
                    if(cmd.getName().equalsIgnoreCase("teamchat")) {
                        Team senderTeam = getServer().getScoreboardManager().getMainScoreboard().getEntryTeam(sender.getName());
                        if(senderTeam != null) {
                            sender.sendMessage(ChatColor.GREEN + "Du sprichst nun in deinem Team-Kanal! Nutze /global bzw. /eglobal um im normalen Chat zu sprechen!");
                            playerChannels.put(((Player) sender).getUniqueId(), ChatDestination.TEAM);
                        } else {
                            sender.sendMessage(ChatColor.RED + "Du bist in keinem Team!");
                        }
                    } else if(cmd.getName().equalsIgnoreCase("chat")) {
                        playerChannels.put(((Player) sender).getUniqueId(), ChatDestination.GLOBAL);
                        sender.sendMessage(ChatColor.GREEN + "Du sprichst nun im globalen Chat-Kanal!");
                    }
                } else {
                    sender.sendMessage("Die Konsole kann nur im globalen Channel sprechen ;)");
                }
            } else {
                String msg = args[0];
                for(int i = 1; i < args.length; i++) {
                    msg += " " + args[i];
                }
                if(cmd.getName().equalsIgnoreCase("teamchat")) {
                    Team senderTeam = getServer().getScoreboardManager().getMainScoreboard().getEntryTeam(sender.getName());
                    if(senderTeam != null) {
                        sendMessage(sender, senderTeam, msg);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Du bist in keinem Team!");
                    }
                } else if(cmd.getName().equalsIgnoreCase("chat")) {
                    sendGlobalMessage(sender, msg);
                }
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Du hast nicht genügend Rechte um zu chatten!");
        }
        return true;
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if(!event.isCancelled()) {
            ArrayList<String> args = new ArrayList<String>(Arrays.asList(event.getMessage().split(" ")));
            if(args.size() > 0 && args.get(0).startsWith("/") && args.get(0).equalsIgnoreCase(globalChannel)) {
                event.setCancelled(true);
                args.remove(0);
                onCommand(event.getPlayer(), getServer().getPluginCommand("chat"), globalChannel, args.toArray(new String[args.size()]));
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        //if(event.isCancelled())
        //    return;
        if(event.getPlayer().hasPermission("simpleteamchat.chat")) {
            ChatDestination destination = playerChannels.get(event.getPlayer().getUniqueId());
            if(event.getPlayer().hasPermission("simpleteamchat.chat.color")) {
                event.setMessage(ChatColor.translateAlternateColorCodes('&', event.getMessage()));
            }
            if(destination == null || destination == ChatDestination.GLOBAL) {
                event.setFormat(getGlobalFormat(event.getPlayer()));
            } else {
                Team senderTeam = getServer().getScoreboardManager().getMainScoreboard().getEntryTeam(event.getPlayer().getName());
                if(senderTeam != null) {
                    sendMessage(event.getPlayer(), senderTeam, event.getMessage());
                    event.setCancelled(true);
                } else {
                    event.setFormat(getGlobalFormat(event.getPlayer()));
                    playerChannels.remove(event.getPlayer().getUniqueId());
                }
            }
        } else {
            event.getPlayer().sendMessage(ChatColor.RED + "Du hast nicht genügend Rechte um zu chatten!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        playerChannels.remove(event.getPlayer().getUniqueId());
    }

    private void sendGlobalMessage(CommandSender sender, String msg) {
        if(sender.hasPermission("simpleteamchat.chat.color")) {
            msg = ChatColor.translateAlternateColorCodes('&', msg);
        }
        String formattedMessage = String.format(getGlobalFormat(sender), new String[]{sender.getName(), msg});
        for(Player player : getServer().getOnlinePlayers()) {
            player.sendMessage(formattedMessage);
        }
        getServer().getConsoleSender().sendMessage(formattedMessage);
    }

    private void sendMessage(CommandSender sender, Team team, String msg) {
        Set<CommandSender> receivers = new HashSet<CommandSender>();
        for(String entry : team.getEntries()) {
            if(entry.equalsIgnoreCase(sender.getName()))
                continue;
            Player player = getServer().getPlayer(entry);
            if(player != null && player.isOnline()) {
                receivers.add(player);
            }
        }
        String teamColor = "";
        if(team.getPrefix().length() > 1) {
            ChatColor color = ChatColor.getByChar(team.getPrefix().charAt(1));
            if(color != null) {
                teamColor += color;
            }
        }

        receivers.add(sender);
        receivers.add(getServer().getConsoleSender());
        for(Player player : getServer().getOnlinePlayers()) {
            if(player.hasPermission("simpleteamchat.chat.spy")) {
                receivers.add(player);
            }
        }

        if(sender.hasPermission("simpleteamchat.chat.color")) {
            msg = ChatColor.translateAlternateColorCodes('&', msg);
        }

        String formattedMessage = String.format(getFormat(sender, ChatDestination.TEAM, teamColor, team.getDisplayName()), new String[]{sender.getName(), msg});
        for(CommandSender receiver : receivers) {
            receiver.sendMessage(formattedMessage);
        }

    }

    private String getGlobalFormat(CommandSender sender) {
        return getFormat(sender, ChatDestination.GLOBAL, globalColor, globalChannel);
    }

    private String getFormat(CommandSender sender, ChatDestination destination, String channelColor, String channelTag) {
        String format = destination == ChatDestination.GLOBAL ? globalFormat : teamFormat;
        String tag = channelTagFormat.replace("%color%", channelColor).replace("%tag%", channelTag);
        format = format.replace("%tag%", tag);
        String info = "";
        if(serverTags != null && sender instanceof Player) {
            ServerInfo serverInfo = serverTags.getPlayerServer((Player) sender);
            if(serverInfo != null) {
                info = serverInfoFormat.replace("%tag%", serverInfo.getTag()).replace("%name%", serverInfo.getName());
            }
        }
        format = format.replace("%serverinfo%", info);
        String prefix = "";
        String suffix = "";
        if(permissionsEx != null && sender instanceof Player) {
            PermissionUser permUser = permissionsEx.getPermissionsManager().getUser((Player) sender);
            if(permUser != null) {
                prefix = ChatColor.translateAlternateColorCodes('&', permUser.getPrefix());
                suffix = ChatColor.translateAlternateColorCodes('&', permUser.getSuffix());
            }
        }
        format = format.replace("%prefix%", prefix);
        format = format.replace("%suffix%", suffix);
        return format;
    }

    private enum ChatDestination {
        GLOBAL,
        TEAM;
    }

}
