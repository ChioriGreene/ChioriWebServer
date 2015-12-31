package com.chiorichan.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;

import java.io.IOException;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

import com.chiorichan.net.NetworkManager;
import com.chiorichan.util.StringFunc;

public class HttpsHandler extends SslHandler
{
	public HttpsHandler( SSLEngine engine )
	{
		super( engine );
	}

	public HttpsHandler( SSLEngine engine, boolean startTls )
	{
		super( engine, startTls );
	}

	@Override
	public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) throws Exception
	{
		/*
		 * Common SSL exception such as those below are thrown thru this exceptionCaught() method first
		 *
		 * javax.net.ssl.SSLHandshakeException: Client requested protocol SSLv3 not enabled or not supported
		 * javax.net.ssl.SSLHandshakeException: no cipher suites in common
		 */

		// io.netty.handler.ssl.NotSslRecordException;

		if ( cause instanceof SSLException || ! ( cause instanceof IOException ) )
		{
			String protocol = StringFunc.regexCapture( cause.getMessage(), "Client requested protocol (.*) not enabled or not supported" );

			if ( protocol != null )
				NetworkManager.getLogger().severe( String.format( "Client tried to negotiate a SSL connection using protocol version %s, which is currently disabled or not supported", protocol ) );
			else if ( cause.getMessage().contains( "SSLv2Hello is disabled" ) )
				NetworkManager.getLogger().severe( "Client tried to negotiate a SSL connection using protocol version SSLv2Hello, which is currently disabled" );
			else if ( cause.getMessage().contains( "no cipher suites in common" ) )
				// Expand on this exception and see if it would be possible to discover which cipher suites the client was expecting
				NetworkManager.getLogger().severe( "Client tried to negotiate a SSL connection but the server and client had no cipher suites in common" );
			else
				NetworkManager.getLogger().severe( "Caught an unexpected yet severe exception while trying to negotiate the SSL connection", cause );
		}
		else
			super.exceptionCaught( ctx, cause );
	}
}
