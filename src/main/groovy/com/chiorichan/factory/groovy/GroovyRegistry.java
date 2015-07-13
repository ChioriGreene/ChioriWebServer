/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.groovy;

import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import groovy.lang.Script;
import groovy.transform.TimedInterrupt;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;

import com.chiorichan.Loader;
import com.chiorichan.account.Account;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountType;
import com.chiorichan.account.auth.AccountAuthenticator;
import com.chiorichan.event.EventBus;
import com.chiorichan.factory.EvalCallback;
import com.chiorichan.factory.EvalContext;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.factory.processors.EmbeddedGroovyScriptProcessor;
import com.chiorichan.factory.processors.GroovyScriptProcessor;
import com.chiorichan.lang.ErrorReporting;
import com.chiorichan.lang.EvalException;
import com.chiorichan.lang.SandboxSecurityException;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.session.SessionManager;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Timings;
import com.google.common.collect.Maps;

/**
 * Handles the registry of the Groovy related scripting language
 */
public class GroovyRegistry
{
	/*
	 * Groovy Imports :P
	 */
	private static final GroovyImportCustomizer imports = new GroovyImportCustomizer();
	private static final Class<?>[] classImports = new Class<?>[] {Loader.class, AccountManager.class, AccountType.class, Account.class, AccountAuthenticator.class, EventBus.class, PermissionManager.class, PluginManager.class, TaskManager.class, Timings.class, SessionManager.class, SiteManager.class, Site.class, EvalContext.class};
	private static final String[] starImports = new String[] {"com.chiorichan.lang", "com.chiorichan.util", "org.apache.commons.lang3.text", "org.ocpsoft.prettytime", "java.util", "java.net", "com.google.common.base"};
	private static final String[] staticImports = new String[] {"com.chiorichan.util.Looper"};
	
	private static final GroovySandbox secure = new GroovySandbox();
	
	/*
	 * Groovy Sandbox Customization
	 */
	private static final ASTTransformationCustomizer timedInterrupt = new ASTTransformationCustomizer( TimedInterrupt.class );
	
	static
	{
		imports.addImports( classImports );
		imports.addStarImports( starImports );
		imports.addStaticStars( staticImports );
		
		// Transforms scripts to limit their execution to 30 seconds.
		long timeout = Loader.getConfig().getLong( "advanced.security.defaultScriptTimeout", 30L );
		if ( timeout > 0 )
		{
			Map<String, Object> timedInterruptParams = Maps.newHashMap();
			timedInterruptParams.put( "value", timeout );
			timedInterrupt.setAnnotationParameters( timedInterruptParams );
		}
	}
	
	public GroovyRegistry()
	{
		/**
		 * Register Script-Processors
		 */
		if ( Loader.getConfig().getBoolean( "advanced.scripting.gspEnabled", true ) )
			EvalFactory.register( new EmbeddedGroovyScriptProcessor() );
		if ( Loader.getConfig().getBoolean( "advanced.scripting.groovyEnabled", true ) )
			EvalFactory.register( new GroovyScriptProcessor() );
		
		EvalException.registerException( new EvalCallback()
		{
			@Override
			public ErrorReporting callback( Throwable cause, EvalContext context )
			{
				MultipleCompilationErrorsException exp = ( MultipleCompilationErrorsException ) cause;
				ErrorCollector e = exp.getErrorCollector();
				boolean abort = false;
				
				for ( Object err : e.getErrors() )
					if ( err instanceof Throwable )
					{
						if ( EvalException.exceptionHandler( ( Throwable ) err, context ) )
							abort = true;
					}
					else if ( err instanceof ExceptionMessage )
					{
						if ( EvalException.exceptionHandler( ( ( ExceptionMessage ) err ).getCause(), context ) )
							abort = true;
					}
					else if ( err instanceof SyntaxErrorMessage )
					{
						EvalException.exceptionHandler( ( ( SyntaxErrorMessage ) err ).getCause(), context );
						abort = true;
					}
					else if ( err instanceof Message )
					{
						StringWriter writer = new StringWriter();
						( ( Message ) err ).write( new PrintWriter( writer, true ) );
						EvalException.exceptionHandler( new EvalException( ErrorReporting.E_NOTICE, writer.toString() ), context );
					}
				return abort ? ErrorReporting.E_ERROR : ErrorReporting.E_IGNORABLE;
			}
		}, MultipleCompilationErrorsException.class );
		
		EvalException.registerException( new EvalCallback()
		{
			@Override
			public ErrorReporting callback( Throwable cause, EvalContext context )
			{
				context.result().addException( new EvalException( ErrorReporting.E_ERROR, cause ) );
				return ErrorReporting.E_ERROR;
			}
		}, TimeoutException.class, MissingMethodException.class, CompilationFailedException.class, SandboxSecurityException.class, GroovyRuntimeException.class );
		
		EvalException.registerException( new EvalCallback()
		{
			@Override
			public ErrorReporting callback( Throwable cause, EvalContext context )
			{
				context.result().addException( new EvalException( ErrorReporting.E_PARSE, cause ) );
				return ErrorReporting.E_PARSE;
			}
		}, SyntaxException.class );
		
		/**
		 * {@link TimeoutException} is thrown when a script does not exit within an alloted amount of time.<br>
		 * {@link MissingMethodException} is thrown when a groovy script tries to call a non-existent method<br>
		 * {@link SyntaxException} is for when the user makes a syntax coding error<br>
		 * {@link CompilationFailedException} is for when compilation fails from source errors<br>
		 * {@link SandboxSecurityException} thrown when script attempts to access a blacklisted API<br>
		 * {@link GroovyRuntimeException} thrown for basically all remaining Groovy exceptions not caught above
		 */
	}
	
	/**
	 * Attempts to create a new GroovyShell instance using our own CompilerConfigurations
	 * 
	 * @return
	 *         new instance of GroovyShell
	 */
	public static GroovyShell getNewShell( EvalContext context )
	{
		CompilerConfiguration configuration = new CompilerConfiguration();
		
		/*
		 * Finalize Imports and implement Sandbox
		 */
		configuration.addCompilationCustomizers( imports, timedInterrupt, secure );
		
		/*
		 * Set Groovy Base Script Class
		 */
		configuration.setScriptBaseClass( ScriptingBaseGroovy.class.getName() );
		
		/*
		 * Set default encoding
		 */
		configuration.setSourceEncoding( context.factory().charset().name() );
		
		return new GroovyShell( Loader.class.getClassLoader(), context.factory().binding(), configuration );
	}
	
	public static Script makeScript( GroovyShell shell, EvalContext context )
	{
		return makeScript( shell, context.readString(), context );
	}
	
	public static Script makeScript( GroovyShell shell, String source, EvalContext context )
	{
		return shell.parse( source, context.name() );
	}
}
