/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * <p>
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 * <p>
 * All Rights Reserved.
 */
package com.chiorichan.site;

import com.chiorichan.AppConfig;
import com.chiorichan.helpers.Namespace;
import com.chiorichan.logger.Log;
import com.chiorichan.zutils.ZHttp;
import com.chiorichan.zutils.ZObjects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DomainParser
{
	private static final List<String> tldMaps = new ArrayList<>();

	static
	{
		try
		{
			tldMaps.addAll( AppConfig.get().initializeResourceStream( "com/chiorichan/tld.txt", "tld.txt" ).collect( Collectors.toList() ) );
		}
		catch ( IOException e )
		{
			Log.get().severe( "Could not load the TLD file", e );
		}
	}

	public static boolean isTld( String domain )
	{
		domain = ZHttp.normalize( domain );
		for ( String tld : tldMaps )
			if ( domain.matches( tld ) )
				return true;
		return false;
	}

	private final Namespace tld;
	private final Namespace sub;

	public DomainParser( String fullDomain )
	{
		fullDomain = ZHttp.normalize( fullDomain );

		if ( ZObjects.isEmpty( fullDomain ) )
		{
			tld = new Namespace();
			sub = new Namespace();
			return;
		}

		Namespace ns = Namespace.parseString( fullDomain );
		int parentNodePos = -1;

		for ( int n = 0; n < ns.getNodeCount(); n++ )
		{
			String sns = ns.subNamespace( n ).getString();
			if ( isTld( sns ) )
			{
				parentNodePos = n;
				break;
			}
		}

		if ( parentNodePos > 0 )
		{
			tld = ns.subNamespace( parentNodePos );
			sub = ns.subNamespace( 0, parentNodePos );
		}
		else
		{
			tld = new Namespace();
			sub = ns;
		}
	}

	public Namespace getTld()
	{
		return tld;
	}

	public Namespace getSub()
	{
		return sub;
	}

	public Namespace getFullDomain()
	{
		return sub.merge( tld );
	}

	public Namespace getRootDomain()
	{
		return Namespace.parseString( sub.getLast() + "." + tld.getString() );
	}

	public Namespace getChildDomain()
	{
		return sub.getNodeCount() <= 1 ? new Namespace() : sub.subNamespace( 1 );
	}
}
