/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.lang;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.Loader;
import com.chiorichan.factory.EvalCallback;
import com.chiorichan.factory.EvalContext;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.factory.EvalResult;
import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.factory.StackFactory;
import com.google.common.collect.Maps;

/**
 * Carries extra information for debugging when an {@link Exception} is thrown by the {@link EvalFactory}
 */
public class EvalException extends Exception
{
	private static final long serialVersionUID = -1611181613618341914L;
	private static final Map<Class<? extends Throwable>, EvalCallback> registered = Maps.newConcurrentMap();
	
	private List<ScriptTraceElement> scriptTrace = null;
	private final ErrorReporting level;
	
	public EvalException( ErrorReporting level )
	{
		this.level = level;
	}
	
	public EvalException( ErrorReporting level, String message )
	{
		super( message );
		this.level = level;
	}
	
	public EvalException( ErrorReporting level, String message, Throwable cause )
	{
		super( message, cause );
		if ( cause instanceof EvalException )
			throw new IllegalArgumentException( "The cause argument for EvalException can't be of it's own type." );
		this.level = level;
	}
	
	public EvalException( ErrorReporting level, Throwable cause )
	{
		super( cause );
		if ( cause instanceof EvalException )
			throw new IllegalArgumentException( "The cause argument for EvalException can't be of it's own type." );
		this.level = level;
	}
	
	/**
	 * Processes and appends the input exception thrown to the context provided.
	 * 
	 * @param cause
	 *            The exception thrown
	 * @param context
	 *            The EvalContext associated with the eval request
	 * @return True if we should abort any further execution of code
	 */
	public static boolean exceptionHandler( Throwable cause, EvalContext context )
	{
		if ( cause == null )
			return false;
		
		EvalResult result = context.result();
		
		/**
		 * We just forward {@link EvalException}
		 */
		if ( cause instanceof EvalException )
		{
			result.addException( ( EvalException ) cause );
			if ( ! ( ( EvalException ) cause ).isIgnorable() )
				return true;
		}
		else if ( cause instanceof EvalMultipleException )
		{
			boolean abort = false;
			for ( EvalException e : ( ( EvalMultipleException ) cause ).getExceptions() )
				if ( EvalException.exceptionHandler( e, context ) )
					abort = true;
			return abort;
		}
		else if ( cause instanceof NullPointerException || cause instanceof ArrayIndexOutOfBoundsException )
		{
			result.addException( new EvalException( ErrorReporting.E_ERROR, cause ) );
			return true;
		}
		else
		{
			boolean handled = false;
			
			Map<Class<? extends Throwable>, EvalCallback> assignable = Maps.newHashMap();
			
			for ( Entry<Class<? extends Throwable>, EvalCallback> entry : registered.entrySet() )
				if ( cause.getClass().equals( entry.getKey() ) )
				{
					ErrorReporting e = entry.getValue().callback( cause, context );
					if ( e == null )
					{
						handled = true;
						break;
					}
					else
						return !e.isIgnorable();
				}
				else if ( entry.getKey().isAssignableFrom( cause.getClass() ) )
					assignable.put( entry.getKey(), entry.getValue() );
			
			if ( !handled )
				if ( assignable.size() == 0 )
				{
					Loader.getLogger().severe( "Uncaught exception in EvalFactory for exception " + cause.getClass().getName(), cause );
					result.addException( new EvalException( ErrorReporting.E_ERROR, "Uncaught exception in EvalFactory", cause ) );
				}
				else if ( assignable.size() == 1 )
				{
					ErrorReporting e = assignable.values().toArray( new EvalCallback[0] )[0].callback( cause, context );
					if ( e == null )
					{
						result.addException( new EvalException( ErrorReporting.E_ERROR, cause ) );
						return true;
					}
					else if ( !e.isIgnorable() )
						return true;
				}
				else
					for ( Entry<Class<? extends Throwable>, EvalCallback> entry : assignable.entrySet() )
					{
						boolean noAssignment = true;
						for ( Class<? extends Throwable> sub : assignable.keySet() )
							if ( sub != entry.getKey() )
								if ( sub.isAssignableFrom( entry.getKey() ) )
									noAssignment = false;
						if ( noAssignment )
						{
							ErrorReporting e = entry.getValue().callback( cause, context );
							return e != null && !e.isIgnorable();
						}
					}
		}
		
		return false;
	}
	
	/**
	 * Registers an expected exception to be thrown by any subsystem of {@link EvalFactory}
	 * 
	 * @param callback
	 *            The Callback to call when such exception is thrown
	 * @param clzs
	 *            Classes to be registered
	 */
	@SafeVarargs
	public static void registerException( EvalCallback callback, Class<? extends Throwable>... clzs )
	{
		for ( Class<? extends Throwable> clz : clzs )
			registered.put( clz, callback );
	}
	
	public ErrorReporting errorLevel()
	{
		return level;
	}
	
	public ScriptTraceElement[] getScriptTrace()
	{
		return scriptTrace.toArray( new ScriptTraceElement[0] );
	}
	
	public boolean hasScriptTrace()
	{
		return scriptTrace != null && scriptTrace.size() > 0;
	}
	
	public boolean isIgnorable()
	{
		return level.isIgnorable();
	}
	
	public boolean isScriptingException()
	{
		return getCause() != null && getCause().getStackTrace().length > 0 && getCause().getStackTrace()[0].getClassName().startsWith( "org.codehaus.groovy.runtime" );
	}
	
	public EvalException populateScriptTrace( StackFactory factory )
	{
		scriptTrace = factory.examineStackTrace( getCause() == null ? getStackTrace() : getCause().getStackTrace() );
		return this;
	}
}
