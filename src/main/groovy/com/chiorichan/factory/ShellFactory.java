/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.factory;

import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import com.chiorichan.framework.WebUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Sits as an interface between GroovyShell and Interpreters
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class ShellFactory
{
	Map<String, EvalMetaData> scriptHistory = Maps.newLinkedHashMap();
	GroovyShell shell = null;
	
	ShellFactory setShell( GroovyShell shell )
	{
		this.shell = shell;
		return this;
	}
	
	/**
	 * This method is provided purely for convenience
	 * It is highly discouraged to use the parse, run or evaluate methods within
	 * as it will bypass the servers script stack tracing mechanism
	 * 
	 * @return The GroovyShell backing this ShellFactory, which changes with each script execute
	 */
	public GroovyShell getGroovyShell()
	{
		return shell;
	}
	
	public List<ScriptTraceElement> examineStackTrace( StackTraceElement[] stackTrace )
	{
		Validate.notNull( stackTrace );
		
		List<ScriptTraceElement> scriptTrace = Lists.newLinkedList();
		
		for ( StackTraceElement ste : stackTrace )
		{
			if ( ste.getFileName() != null && ste.getFileName().matches( "GroovyScript\\d*\\.chi" ) )
			{
				scriptTrace.add( new ScriptTraceElement( ste, scriptHistory.get( ste.getFileName() ) ) );
			}
		}
		
		return scriptTrace;
	}
	
	public Script makeScript( String scriptText, EvalMetaData metaData )
	{
		String scriptName = "GroovyScript" + WebUtils.randomNum( 8 ) + ".chi";
		metaData.scriptName = scriptName;
		
		Script script = shell.parse( scriptText, scriptName );
		
		metaData.script = script;
		
		scriptHistory.put( scriptName, metaData );
		
		return script;
	}
	
	void onFinished()
	{
		scriptHistory.clear();
	}
}
