/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.terminal;

import java.io.IOException;
import java.io.InputStream;

import com.chiorichan.AppConfig;
import com.chiorichan.Loader;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.factory.BindingProvider;
import com.chiorichan.factory.ScriptBinding;
import com.chiorichan.factory.ScriptingFactory;
import com.chiorichan.lang.EnumColor;
import com.chiorichan.Versioning;
import com.chiorichan.utils.UtilIO;

public class QueryTerminalEntity extends TerminalEntity implements BindingProvider
{
	private ScriptBinding binding = new ScriptBinding();

	private ScriptingFactory factory;

	public QueryTerminalEntity( TerminalHandler handler )
	{
		super( handler );

		binding = new ScriptBinding();
		factory = ScriptingFactory.create( this );
		binding.setVariable( "context", this );
		binding.setVariable( "__FILE__", AppConfig.get().getDirectory() );
	}

	@Override
	public void displayWelcomeMessage()
	{
		try
		{
			InputStream is = null;
			try
			{
				is = Loader.class.getClassLoader().getResourceAsStream( "com/chiorichan/banner.txt" );

				String[] banner = new String( UtilIO.inputStream2Bytes( is ) ).split( "\\n" );

				for ( String l : banner )
					handler.println( EnumColor.GOLD + l );

				handler.println( String.format( "%s%sWelcome to %s Version %s!", EnumColor.NEGATIVE, EnumColor.GOLD, Versioning.getProduct(), Versioning.getVersion() ) );
				handler.println( String.format( "%s%s%s", EnumColor.NEGATIVE, EnumColor.GOLD, Versioning.getCopyright() ) );
			}
			finally
			{
				if ( is != null )
					is.close();
			}
		}
		catch ( IOException e )
		{
			// Ignore
		}
	}

	@Override
	protected void failedLogin( AccountResult result )
	{
		// Do Nothing!
	}

	@Override
	public void finish()
	{
		// Do Nothing!
	}

	@Override
	public ScriptBinding getBinding()
	{
		return binding;
	}

	@Override
	public ScriptingFactory getScriptingFactory()
	{
		return factory;
	}

	@Override
	public String getVariable( String key, String def )
	{
		if ( !binding.hasVariable( key ) )
			return def;

		// This is suppose to be persistent data, i.e., login. But we will use the metadata until something else can be made
		Object obj = binding.getVariable( key );

		if ( obj == null || ! ( obj instanceof String ) )
			return def;

		return ( String ) obj;
	}

	@Override
	public void setVariable( String key, String val )
	{
		// This is suppose to be persistent data, i.e., login. But we will use the metadata until something else can be made
		binding.setVariable( key, val );
	}
}
