/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.factory.event;

import com.chiorichan.event.Cancellable;
import com.chiorichan.event.Event;
import com.chiorichan.event.HandlerList;
import com.chiorichan.factory.EvalExecutionContext;

/**
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class PostEvalEvent extends Event implements Cancellable
{
	private EvalExecutionContext context;
	private boolean cancelled;
	
	public PostEvalEvent( EvalExecutionContext context )
	{
		this.context = context;
	}
	
	public EvalExecutionContext context()
	{
		return context;
	}
	
	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}
	
	@Override
	public void setCancelled( boolean cancelled )
	{
		this.cancelled = cancelled;
	}
	
	public static HandlerList getHandlerList()
	{
		return handlers;
	}
}