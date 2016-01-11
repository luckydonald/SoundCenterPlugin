package com.soundcenter.soundcenter.plugin.network.tcp;

import com.soundcenter.soundcenter.plugin.SoundCenter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.soundcenter.soundcenter.lib.tcp.TcpOpcodes;
import com.soundcenter.soundcenter.plugin.data.ServerUser;
import com.soundcenter.soundcenter.plugin.messages.Messages;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ConnectionManager {

	public static boolean initialize(Player player) {
		ServerUser tcpUser = SoundCenter.userList.getAcceptedUserByName(player.getName());

		if (tcpUser != null) {
			if (player.hasPermission("sc.init") && tcpUser.isAccepted() // initialize
					&& (tcpUser.getIp().equals(player.getAddress().getAddress()) || !SoundCenter.config.verifyIp())) {

				tcpUser.setInitialized(true);
				SoundCenter.userList.incrementInitializedUserCount();

				SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_INFO_INITIALIZED, null, null, tcpUser);
				player.sendMessage(Messages.INFO_INIT_SUCCESS);

				return true;

			} else if (!player.hasPermission("sc.init")) { // no permission
				player.sendMessage(Messages.ERR_PERMISSION_INIT);
				SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_DENY_PERMISSION_INIT, null, null, tcpUser);
				
				tcpUser.setQuitReason("Missing required permission: 'sc.init'");
				tcpUser.disconnect();

				return false;

			} else if (tcpUser.isAccepted()) { // ip-verification failed
				SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_DENY_IP, null, null, tcpUser);
				player.sendMessage(Messages.ERR_IP_VERIFICATION);
				
				tcpUser.setQuitReason("IP-Verification failed.");
				tcpUser.disconnect();


				return false;

			} else { // user not accepted
				player.sendMessage(Messages.ERR_NOT_ACCEPTED);
				return true;
			}

		} else { // audioclient not connected
			sendStartClientMessage(player);
			return true;
		}
	}

	public static void sendStartClientMessage(Player player) {

		String ip = SoundCenter.config.serverIp();
		if (ip.isEmpty()) {
			ip = Bukkit.getServer().getIp();
		}
		String f = "" + Messages.INFO_START_AUDIOCLIENT;
		/*
		Minecraft Server
			{mcurl}		minecraft host (or ip)
			{mcip}		minecraft ip
			{mcport}	minecraft port
			{mcversion}	minecraft version (not bukkit version?)

			{url}		alias to {mcurl}
			{ip}		alias to {mcip}

		SoundCenter Server
			{scurl}		sound center url (actually alias for {scip})
			{scip}		sound center ip (will be same as {mcip} if nothing is in SoundCenter config)
			{scport}	sound center port
			{scversion}	sound center version

			{port} 		alias to {scport}

		Game
			{player}	player name
			{version}	bukkit version
		*/
		String mcip = Bukkit.getServer().getIp();
		String mcurl = mcip;
		if (mcip.length() == 0) {
			try {
				InetAddress addr = InetAddress.getLocalHost();

				// Get IP Address
				byte[] ipAddr = addr.getAddress();

				// Get hostname
				mcip = addr.getHostAddress();
				mcurl = addr.getHostName();
			} catch (UnknownHostException ignored) {
				//do nothing.
			}
		}
		String mcport = "" + Bukkit.getServer().getPort();
		String scip = mcip;
		if (SoundCenter.config.serverIp() != null && SoundCenter.config.serverIp().length() > 0) {
			// if config has ip
			scip = SoundCenter.config.serverIp();
		}
		String scurl = scip;
		if (SoundCenter.config.serverBindAddr() != null && SoundCenter.config.serverBindAddr().length() > 0) {
			// if config has hostname
			scurl = SoundCenter.config.serverBindAddr();
		}
		String scport = "" + SoundCenter.config.port();
		String mcversion = "" + Bukkit.getServer().getVersion();
		String bktversion = ""+ Bukkit.getServer().getBukkitVersion();
		//Bukkit.getServer().getI.getAddress
		//player.getAddress().()

		// Minecraft server
		f = f.replace("{mcip}", mcip);
		f = f.replace("{mcurl}", mcurl);
		f = f.replace("{mcport}", mcport);
		f = f.replace("{mcversion}", mcversion);

		// Minecraft server aliases
		f = f.replace("{url}", mcurl);
		f = f.replace("{ip}", mcip);

		// SoundCenter server
		f = f.replace("{scip}", scip);		// can be {mcip} if nothing is in SCs config.
		f = f.replace("{scurl}", scurl);	// actually {scip} alias
		f = f.replace("{scport}", scport);
		f = f.replace("{scversion}", "" + SoundCenter.MIN_CL_VERSION);
		// SoundCenter server aliases
		f = f.replace("{port}", scport);

		//Game
		f = f.replace("{player}", player.getName());
		f = f.replace("{version}", bktversion);
		player.sendMessage(f);
	}

}
