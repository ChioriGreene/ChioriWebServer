/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.event.http;

import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;

public class HttpExceptionEvent extends HttpEvent
{
	private final Throwable cause;
	private int httpCode = -1;
	private final HttpRequestWrapper request;
	private String errorHtml = "";
	private boolean isDevelopmentMode;
	
	public HttpExceptionEvent( HttpRequestWrapper request, Throwable cause, boolean isDevelopmentMode )
	{
		this.cause = cause;
		this.request = request;
		this.isDevelopmentMode = isDevelopmentMode;
	}
	
	public boolean isDevelopmentMode()
	{
		return isDevelopmentMode;
	}
	
	public HttpRequestWrapper getRequest()
	{
		return request;
	}
	
	public HttpResponseWrapper getResponse()
	{
		return request.getResponse();
	}
	
	public String getErrorHtml()
	{
		if ( errorHtml.isEmpty() )
			return null;
		
		return errorHtml;
	}
	
	public void setErrorHtml( String errorHtml )
	{
		this.errorHtml = errorHtml;
	}
	
	public int getHttpCode()
	{
		return httpCode;
	}
	
	public Throwable getThrowable()
	{
		return cause;
	}
}
