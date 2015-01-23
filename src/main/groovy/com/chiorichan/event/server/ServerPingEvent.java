/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.event.server;

import java.net.InetAddress;

import com.chiorichan.event.HandlerList;

/**
 * Called when a server list ping is coming in.
 */
public class ServerPingEvent extends ServerEvent
{
	private static final HandlerList handlers = new HandlerList();
	private final InetAddress address;
	private String motd;
	private final int numPlayers;
	private int maxPlayers;
	
	public ServerPingEvent(final InetAddress address, final String motd, final int numPlayers, final int maxPlayers)
	{
		this.address = address;
		this.motd = motd;
		this.numPlayers = numPlayers;
		this.maxPlayers = maxPlayers;
	}
	
	/**
	 * Get the address the ping is coming from.
	 * 
	 * @return the address
	 */
	public InetAddress getAddress()
	{
		return address;
	}
	
	/**
	 * Get the message of the day message.
	 * 
	 * @return the message of the day
	 */
	public String getMotd()
	{
		return motd;
	}
	
	/**
	 * Change the message of the day message.
	 * 
	 * @param motd
	 *           the message of the day
	 */
	public void setMotd( String motd )
	{
		this.motd = motd;
	}
	
	/**
	 * Get the number of players sent.
	 * 
	 * @return the number of players
	 */
	public int getNumPlayers()
	{
		return numPlayers;
	}
	
	/**
	 * Get the maximum number of players sent.
	 * 
	 * @return the maximum number of players
	 */
	public int getMaxPlayers()
	{
		return maxPlayers;
	}
	
	/**
	 * Set the maximum number of players sent.
	 * 
	 * @param maxPlayers
	 *           the maximum number of player
	 */
	public void setMaxPlayers( int maxPlayers )
	{
		this.maxPlayers = maxPlayers;
	}
	
	@Override
	public HandlerList getHandlers()
	{
		return handlers;
	}
	
	public static HandlerList getHandlerList()
	{
		return handlers;
	}
}