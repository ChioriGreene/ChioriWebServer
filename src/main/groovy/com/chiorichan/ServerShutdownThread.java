/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan;


public class ServerShutdownThread extends Thread
{
	@Override
	public void run()
	{
		try
		{
			if ( Loader.isRunning() )
				Loader.stop( "Stopping the server... Goodbye!" );
		}
		catch ( Exception ex )
		{
			ex.printStackTrace();
		}
	}
}
