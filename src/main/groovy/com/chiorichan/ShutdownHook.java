/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan;

public class ShutdownHook extends Thread
{
	ShutdownHook()
	{
		Runtime.getRuntime().addShutdownHook( this );
	}
	
	@Override
	public void run()
	{
		try
		{
			if ( Loader.isRunning() )
				Loader.stop( null );
		}
		catch ( Exception ex )
		{
			ex.printStackTrace();
		}
	}
}