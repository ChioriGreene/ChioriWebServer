/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.factory;

import groovy.lang.Script;

import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.ConsoleLogger;
import com.chiorichan.Loader;
import com.chiorichan.event.EventBus;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.util.Common;
import com.chiorichan.util.ObjectUtil;
import com.chiorichan.util.StringUtil;
import com.chiorichan.util.Versioning;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public abstract class ScriptingBaseJava extends Script
{
	@SuppressWarnings( "unchecked" )
	String var_export( Object... objs )
	{
		StringBuilder sb = new StringBuilder();
		
		for ( Object obj : objs )
		{
			if ( obj != null )
			{
				Map<String, Object> children = Maps.newLinkedHashMap();
				
				if ( obj instanceof Map )
				{
					for ( Entry<Object, Object> e : ( ( Map<Object, Object> ) obj ).entrySet() )
					{
						String key = ObjectUtil.castToString( e.getKey() );
						if ( key == null )
							key = e.getKey().toString();
						children.put( key, e.getValue() );
					}
				}
				else if ( obj instanceof List )
				{
					for ( int i = 0; i < ( ( List<Object> ) obj ).size(); i++ )
						children.put( "" + i, ( ( List<Object> ) obj ).get( i ) );
				}
				else if ( obj instanceof Object[] )
				{
					for ( int i = 0; i < ( ( Object[] ) obj ).length; i++ )
						children.put( "" + i, ( ( Object[] ) obj )[i] );
				}
				
				// boolean[], byte[], short[], char[], int[], long[], float[], double[], Object[]
				
				Object value = ObjectUtil.castToString( obj );
				if ( value == null )
					value = obj.toString();
				
				if ( !children.isEmpty() )
					value = children.size();
				
				sb.append( "\n" + obj.getClass().getName() + "(" + value + ")" );
				
				if ( !children.isEmpty() )
				{
					sb.append( " {" );
					for ( Entry<String, Object> c : children.entrySet() )
					{
						sb.append( "\n\t[" + c.getKey() + "]=>" );
						for ( String s : var_export( c.getValue() ).split( "\n" ) )
							sb.append( "\n\t" + s );
					}
					sb.append( "\n}" );
				}
			}
			else
			{
				sb.append( "\nnull" );
			}
		}
		
		return sb.substring( 1 ).toString();
	}
	
	String trim( String str )
	{
		return str.trim();
	}
	
	String strtoupper( String str )
	{
		return str.toUpperCase();
	}
	
	String strtolower( String str )
	{
		return str.toLowerCase();
	}
	
	int count( Map<Object, Object> maps )
	{
		return maps.size();
	}
	
	int count( List<Object> list )
	{
		return list.size();
	}
	
	int count( String var )
	{
		return var.length();
	}
	
	int count( String[] var )
	{
		return var.length;
	}
	
	int strlen( String var )
	{
		return var.length();
	}
	
	@SuppressWarnings( "unchecked" )
	boolean empty( Object o )
	{
		if ( o == null )
			return true;
		else if ( o instanceof List<?> )
			return empty( ( List<Object> ) o );
		else if ( o instanceof Map<?, ?> )
			return empty( ( Map<Object, Object> ) o );
		else if ( o instanceof String )
			return empty( ( String ) o );
		
		return false;
	}
	
	boolean empty( List<Object> list )
	{
		return ( list == null || list.size() < 1 );
	}
	
	boolean empty( Map<Object, Object> maps )
	{
		return ( maps == null || maps.size() < 1 );
	}
	
	boolean empty( String var )
	{
		if ( var == null )
			return true;
		
		if ( var.isEmpty() )
			return true;
		
		return false;
	}
	
	int time()
	{
		return Common.getEpoch();
	}
	
	/**
	 * Default format is M/d/yyyy
	 * 
	 * @param Date
	 *            you wish to convert
	 * @return Long containing the epoch of provided date
	 */
	Long dateToEpoch( String date )
	{
		return dateToEpoch( date, null );
	}
	
	Long dateToEpoch( String date, String format )
	{
		try
		{
			if ( format == null )
				format = "M/d/yyyy";
			
			SimpleDateFormat sdf = new SimpleDateFormat( format );
			
			return sdf.parse( date ).getTime() / 1000;
		}
		catch ( ParseException e )
		{
			return 0L;
		}
	}
	
	String date()
	{
		return date( "" );
	}
	
	String date( String format )
	{
		return date( format, "" );
	}
	
	String date( String format, Object data )
	{
		return date( format, data, null );
	}
	
	String date( String format, Object data, String def )
	{
		return date( format, ObjectUtil.castToString( data ), def );
	}
	
	String date( String format, Date date )
	{
		return date( format, date, null );
	}
	
	String date( String format, Date date, String def )
	{
		return date( format, ObjectUtil.castToString( date.getTime() / 1000 ), def );
	}
	
	String date( String format, String data )
	{
		return date( format, data, null );
	}
	
	String date( String format, String data, String def )
	{
		Date date = new Date();
		
		if ( data != null && !data.isEmpty() )
		{
			data = data.trim();
			
			if ( data.length() > 10 )
				data = data.substring( 0, 10 );
			
			try
			{
				date = new Date( Long.parseLong( data ) * 1000 );
			}
			catch ( NumberFormatException e )
			{
				if ( def != null )
					return def;
			}
		}
		
		if ( format == null || format.isEmpty() )
			format = "MMM dx YYYY";
		
		if ( format.contains( "U" ) )
		{
			format = format.replaceAll( "U", ( date.getTime() / 1000 ) + "" );
		}
		
		if ( format.contains( "x" ) )
		{
			Calendar var1 = Calendar.getInstance();
			var1.setTime( date );
			int day = var1.get( Calendar.DAY_OF_MONTH );
			String suffix = "";
			
			if ( day >= 11 && day <= 13 )
			{
				suffix = "'th'";
			}
			else
				switch ( day % 10 )
				{
					case 1:
						suffix = "'st'";
						break;
					case 2:
						suffix = "'nd'";
						break;
					case 3:
						suffix = "'rd'";
						break;
					default:
						suffix = "'th'";
						break;
				}
			
			format = format.replaceAll( "x", suffix );
		}
		
		return new SimpleDateFormat( format ).format( date );
	}
	
	String md5( String str )
	{
		return StringUtil.md5( str );
	}
	
	Integer strpos( String haystack, String needle )
	{
		return strpos( haystack, needle, 0 );
	}
	
	Integer strpos( String haystack, String needle, int offset )
	{
		if ( offset > 0 )
			haystack = haystack.substring( offset );
		
		if ( !haystack.contains( needle ) )
			return null;
		
		return haystack.indexOf( needle );
	}
	
	String str_replace( String needle, String replacement, String haystack )
	{
		return haystack.replaceAll( needle, replacement );
	}
	
	String money_format( String amt )
	{
		if ( amt.isEmpty() )
			return "$0.00";
		
		return money_format( Integer.parseInt( amt ) );
	}
	
	String money_format( Integer amt )
	{
		if ( amt == 0 )
			return "$0.00";
		
		DecimalFormat df = new DecimalFormat( "$###,###,###.00" );
		return df.format( amt );
	}
	
	int getEpoch()
	{
		return Common.getEpoch();
	}
	
	List<String> explode( String limiter, String data )
	{
		if ( data == null || data.isEmpty() )
			return Lists.newArrayList();
		
		return Splitter.on( limiter ).splitToList( data );
	}
	
	Map<String, String> explode( String limiter, String separator, String data )
	{
		if ( data == null || data.isEmpty() )
			return Maps.newHashMap();
		
		return Splitter.on( limiter ).withKeyValueSeparator( separator ).split( data );
	}
	
	String implode( String joiner, Map<String, String> data )
	{
		return implode( joiner, "=", data );
	}
	
	String implode( String joiner, String separator, Map<String, String> data )
	{
		return Joiner.on( joiner ).withKeyValueSeparator( separator ).join( data );
	}
	
	String implode( String joiner, String[] data )
	{
		return Joiner.on( joiner ).join( data );
	}
	
	String implode( String joiner, Iterator<String> data )
	{
		return Joiner.on( joiner ).join( data );
	}
	
	String implode( String joiner, Iterable<String> data )
	{
		return Joiner.on( joiner ).join( data );
	}
	
	/**
	 * Determines if the color hex is darker then 50%
	 * 
	 * @param hexdec
	 *            A hexdec color, e.g., #fff, #f3f3f3
	 * @return True if color is darker then 50%
	 */
	boolean isDarkColor( String hexdec )
	{
		return Integer.parseInt( hexdec, 16 ) > ( 0xffffff / 2 );
	}
	
	String money_format( Double amt )
	{
		if ( amt == 0 )
			return "$0.00";
		
		DecimalFormat df = new DecimalFormat( "$###,###,###.00" );
		return df.format( amt );
	}
	
	public boolean isNumeric( String str )
	{
		NumberFormat formatter = NumberFormat.getInstance();
		ParsePosition pos = new ParsePosition( 0 );
		formatter.parse( str, pos );
		return str.length() == pos.getIndex();
	}
	
	boolean is_null( Object obj )
	{
		return ( obj == null );
	}
	
	boolean file_exists( File file )
	{
		return file.exists();
	}
	
	boolean file_exists( String file )
	{
		return new File( file ).exists();
	}
	
	String dirname( File path )
	{
		return path.getParent();
	}
	
	String dirname( String path )
	{
		return new File( path ).getParent();
	}
	
	BigDecimal round( BigDecimal amt, int dec )
	{
		return amt.round( new MathContext( dec, RoundingMode.HALF_DOWN ) );
	}
	
	PluginManager getPluginManager()
	{
		return Loader.getPluginManager();
	}
	
	Plugin getPluginByName( String search )
	{
		return Loader.getPluginManager().getPluginbyName( search );
	}
	
	EventBus getEventBus()
	{
		return Loader.getEventBus();
	}
	
	Loader getLoader()
	{
		return Loader.getInstance();
	}
	
	ConsoleLogger getLogger()
	{
		return Loader.getLogger( getClass().getSimpleName() );
	}
	
	String apache_get_version()
	{
		return "THIS IS NOT APACHE YOU DUMMY!!!";
	}
	
	String getProduct()
	{
		return Versioning.getProduct();
	}
	
	String getVersion()
	{
		return Versioning.getVersion();
	}
}