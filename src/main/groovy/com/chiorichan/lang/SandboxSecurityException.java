/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.lang;

import org.apache.commons.lang3.Validate;

/**
 * Used to track line and column numbers for SecurityExceptions thrown from GroovySandbox
 */
public class SandboxSecurityException extends SecurityException implements IException
{
	private static final long serialVersionUID = -3520264898037710187L;

	int lineNum = -1;
	int colNum = -1;
	String methodName = "";
	String className = "";

	public SandboxSecurityException( String msg )
	{
		super( msg );
	}

	public SandboxSecurityException( String msg, Throwable cause )
	{
		super( msg, cause );
	}

	public String getClassName()
	{
		return className;
	}

	public int getLineColumnNumber()
	{
		return colNum;
	}

	public int getLineNumber()
	{
		return lineNum;
	}

	public String getMethodName()
	{
		return methodName;
	}

	@Override
	public boolean handle( ExceptionReport report, ExceptionContext context )
	{
		return false;
	}

	@Override
	public boolean isIgnorable()
	{
		return false;
	}

	@Override
	public ReportingLevel reportingLevel()
	{
		return ReportingLevel.E_ERROR;
	}

	public void setClassName( String className )
	{
		Validate.notNull( className );
		this.className = className;
	}

	public void setLineColumnNumber( int colNum )
	{
		this.colNum = colNum;
	}

	public void setLineNumber( int lineNum )
	{
		this.lineNum = lineNum;
	}

	public void setLineNumber( int lineNum, int colNum )
	{
		this.lineNum = lineNum;
		this.colNum = colNum;
	}

	public void setMethodName( String methodName )
	{
		Validate.notNull( methodName );
		this.methodName = methodName;
	}
}
