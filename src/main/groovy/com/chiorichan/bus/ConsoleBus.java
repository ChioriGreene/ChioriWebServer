/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.bus;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.logging.Level;

import joptsimple.OptionSet;

import org.joda.time.DateTime;

import com.chiorichan.ChatColor;
import com.chiorichan.ConsoleLogManager;
import com.chiorichan.Loader;
import com.chiorichan.LoggerOutputStream;
import com.chiorichan.ServerShutdownThread;
import com.chiorichan.http.session.SessionManager;
import com.chiorichan.util.FileUtil;
import com.chiorichan.util.Versioning;

public class ConsoleBus implements Runnable
{
	private ConsoleLogManager logManager;
	
	private OptionSet options;
	public static int lastFiveTick = -1;
	public static int currentTick = (int) ( System.currentTimeMillis() / 50 );
	public Thread primaryThread;
	
	public boolean useColors = true;
	
	public Loader loader;
	
	public void init( Loader _loader, OptionSet _options )
	{
		loader = _loader;
		options = _options;
		
		if ( options.has( "nocolor" ) )
			useColors = false;
			
		logManager = new ConsoleLogManager( "" );
		logManager.init();
		
		// TODO: Add alt color handling for System OUT and ERR.
		System.setOut( new PrintStream( new LoggerOutputStream( getLogger().getLogger(), Level.INFO ), true ) );
		System.setErr( new PrintStream( new LoggerOutputStream( getLogger().getLogger(), Level.SEVERE ), true ) );
		
		Runtime.getRuntime().addShutdownHook( new ServerShutdownThread() );
		
		primaryThread = new Thread( this, "Server Thread" );
	}
	
	@Override
	public void run()
	{
		try
		{
			long i = System.currentTimeMillis();
			
			@SuppressWarnings( "unused" )
			boolean g = false;
			long Q = 0;
			
			for ( long j = 0L; Loader.isRunning(); g = true )
			{
				long k = System.currentTimeMillis();
				long l = k - i;
				
				if ( l > 2000L && i - Q >= 15000L )
				{
					if ( loader.getWarnOnOverload() )
						getLogger().warning( "Can\'t keep up! Did the system time change, or is the server overloaded?" );
					l = 2000L;
					Q = i;
				}
				
				if ( l < 0L )
				{
					getLogger().warning( "Time ran backwards! Did the system time change?" );
					l = 0L;
				}
				
				j += l;
				i = k;
				while ( j > 50L )
				{
					currentTick = (int) ( System.currentTimeMillis() / 50 );
					j -= 50L;
					loopTick( currentTick );
				}
				
				Thread.sleep( 1L );
			}
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
			// Crash report generate here
		}
		finally
		{
			try
			{
				Loader.shutdown();
			}
			catch ( Throwable throwable1 )
			{
				throwable1.printStackTrace();
			}
		}
	}
	
	private void loopTick( int tick )
	{
		//Loader.getScheduler().mainThreadHeartbeat( tick );
		
		// Execute every five minutes - ex: clean sessions and checking for updates.
		int fiveMinuteTick = new DateTime().getMinuteOfHour();
		if ( fiveMinuteTick % 5 == 0 && lastFiveTick != fiveMinuteTick )
		{
			lastFiveTick = fiveMinuteTick;
			
			if ( fiveMinuteTick % Loader.getConfig().getInt( "sessions.cleanupInterval", 5 ) == 0 )
				SessionManager.mainThreadHeartbeat( tick );
			
			if ( fiveMinuteTick % Loader.getConfig().getInt( "auto-updater.check-interval", 30 ) == 0 )
				Loader.getAutoUpdater().check();
		}
	}
	
	public ConsoleLogManager getLogger()
	{
		return logManager;
	}
	
	public boolean isPrimaryThread()
	{
		return Thread.currentThread().equals( primaryThread );
	}
	
	public void showBanner()
	{
		try
		{
			InputStream is = null;
			try
			{
				is = Loader.class.getClassLoader().getResourceAsStream( "com/chiorichan/banner.txt" );
				
				String[] banner = new String( FileUtil.inputStream2Bytes( is ) ).split( "\\n" );
				
				for ( String l : banner )
				{
					getLogger().info( ChatColor.GOLD + l );
				}
				
				getLogger().info( ChatColor.NEGATIVE + "" + ChatColor.GOLD + "Starting " + Versioning.getProduct() + " Version " + Versioning.getVersion() );
				getLogger().info( ChatColor.NEGATIVE + "" + ChatColor.GOLD + Versioning.getCopyright() );
			}
			finally
			{
				if ( is != null )
					is.close();
			}
		}
		catch ( IOException e )
		{	
			
		}
	}
}
