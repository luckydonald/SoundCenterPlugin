package com.soundcenter.soundcenter.plugin;

import java.util.Map.Entry;

import com.soundcenter.soundcenter.plugin.network.tcp.ConnectionManager;
//package com.soundcenter.soundcenter.plugin.network.tcp;


import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.soundcenter.soundcenter.plugin.data.ServerUser;

public class SCPlayerListener implements Listener{
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		ConnectionManager.initialize(player);
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		onPlayerDisconnect(event.getPlayer());
	}
	
	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		onPlayerDisconnect(event.getPlayer());
	}
	
	private void onPlayerDisconnect(Player player) {
		ServerUser user = SoundCenter.userList.getAcceptedUserByName(player.getName());
		if (user != null) {
			//remove user from all voice chat listener lists
			for (Entry<Short, ServerUser> entry : SoundCenter.userList.acceptedUsers.entrySet()) {
				ServerUser onlineUser = entry.getValue();
				onlineUser.removeListener(user);
			}
			//disconnect user
			user.disconnect();
		}
	}

}
