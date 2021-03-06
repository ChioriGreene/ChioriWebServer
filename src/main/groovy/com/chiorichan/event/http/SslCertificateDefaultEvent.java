/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.event.http;

import io.netty.handler.ssl.SslContext;

import com.chiorichan.event.AbstractEvent;

public class SslCertificateDefaultEvent extends AbstractEvent
{
	private final String hostname;
	private SslContext context = null;

	public SslCertificateDefaultEvent( String hostname )
	{
		this.hostname = hostname;
	}

	public String getHostname()
	{
		return hostname;
	}

	public SslContext getSslContext()
	{
		return context;
	}

	public void setContext( SslContext context )
	{
		this.context = context;
	}
}
