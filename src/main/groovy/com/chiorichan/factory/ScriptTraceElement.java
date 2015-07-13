/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Similar to StackTraceElement except only used for Groovy Scripts
 */
public class ScriptTraceElement
{
	private final String fileName;
	private final String methodName;
	private final String className;
	private final int lineNum;
	private final int colNum;
	private final EvalContext context;
	
	public ScriptTraceElement( EvalContext context, int lineNum, int colNum )
	{
		this( context, lineNum, colNum, "", "" );
	}
	
	public ScriptTraceElement( EvalContext context, int lineNum, int colNum, String methodName, String className )
	{
		this.context = context;
		fileName = context.name();
		
		this.lineNum = lineNum;
		this.colNum = colNum;
		
		if ( ( className == null || className.isEmpty() ) && context.name() != null )
			if ( context.name().contains( "." ) )
				className = context.name().substring( 0, context.name().indexOf( "." ) );
			else
				className = context.name();
		
		this.methodName = methodName;
		this.className = className;
	}
	
	public ScriptTraceElement( EvalContext context, StackTraceElement ste )
	{
		this.context = context;
		fileName = ste.getFileName();
		methodName = ste.getMethodName();
		className = ste.getClassName();
		lineNum = ste.getLineNumber();
		colNum = -1;
	}
	
	public ScriptTraceElement( EvalContext context, String msg )
	{
		this.context = context;
		fileName = context.name();
		methodName = "run";
		className = ( context.name() == null || context.name().isEmpty() ) ? "<Unknown Class>" : context.name().substring( 0, context.name().lastIndexOf( "." ) );
		
		msg = msg.replaceAll( "\n", "" );
		
		// org.codehaus.groovy.control.MultipleCompilationErrorsException: startup failed: GroovyScript44898378.chi: 69: expecting '}', found ':' @ line 69, column 20. instream.close(): ^ 1 error
		// startup failed:GroovyScript26427446.chi: 7: unable to resolve class BASE64Decoder @ line 7, column 14. def data = new BASE64Decoder().decodeBuffer( file.data ); ^1 error
		
		// Pattern p1 = Pattern.compile( "line[: ]?([0-9]*), column[: ]?([0-9]*)\\. (.*)\\^" );
		Pattern p1 = Pattern.compile( "line[: ]?([0-9]*), column[: ]?([0-9]*)" );
		Matcher m1 = p1.matcher( msg );
		
		// Loader.getLogger().debug( msg );
		
		if ( m1.find() )
		{
			lineNum = Integer.parseInt( m1.group( 1 ) );
			colNum = Integer.parseInt( m1.group( 2 ) );
			// methodName = m1.group( 3 ).trim();
		}
		else
		{
			lineNum = -1;
			colNum = -1;
		}
	}
	
	public EvalContext context()
	{
		return context;
	}
	
	public String getClassName()
	{
		return className;
	}
	
	public int getColumnNumber()
	{
		return colNum;
	}
	
	public String getFileName()
	{
		return fileName;
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
	public String toString()
	{
		return String.format( "%s.%s(%s:%s)", className, methodName, fileName, lineNum );
	}
}
