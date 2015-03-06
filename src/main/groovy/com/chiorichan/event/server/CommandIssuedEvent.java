/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.event.server;

import com.chiorichan.account.InteractivePermissible;
import com.chiorichan.event.Cancellable;

/**
 * Command Issued Event
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class CommandIssuedEvent extends ServerEvent implements Cancellable
{
	private final InteractivePermissible handler;
	private String command;
	private boolean cancelled;
	
	public CommandIssuedEvent( final String command, final InteractivePermissible handler )
	{
		this.command = command;
		this.handler = handler;
	}
	
	/**
	 * Gets the command that the user is attempting to execute from the console
	 * 
	 * @return Command the user is attempting to execute
	 */
	public String getCommand()
	{
		return command;
	}
	
	/**
	 * Sets the command that the server will execute
	 * 
	 * @param message
	 *            New message that the server will execute
	 */
	public void setCommand( String message )
	{
		this.command = message;
	}
	
	/**
	 * Get the command sender.
	 * 
	 * @return The sender
	 */
	public InteractivePermissible getPermissible()
	{
		return handler;
	}
	
	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}
	
	@Override
	public void setCancelled( boolean cancel )
	{
		cancelled = cancel;
	}
}
