package com.chiorichan.framework;

import groovy.lang.Script;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.chiorichan.Loader;
import com.chiorichan.util.StringUtil;

abstract public class scriptingBaseJava extends Script
{
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
	
	String time()
	{
		return date( "U" );
	}
	
	String date( String format )
	{
		return date( format, null );
	}
	
	String date( String format, String data )
	{
		Date date = new Date();
		
		if ( data != null && !data.isEmpty() )
		{
			data = data.trim();
			
			if ( data.length() > 10 )
				data = data.substring( 0, 10 );
			
			date = new Date( Long.parseLong( data ) * 1000 );
		}
		
		if ( format.equals( "U" ) )
			return Loader.getEpoch() + "";
		
		if ( format == null || format.isEmpty() )
			format = "MMM d YYYY";
		
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
	
	String money_format( String amt )
	{
		if ( amt == "" )
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
	
	String money_format( Double amt )
	{
		if ( amt == 0 )
			return "$0.00";
		
		DecimalFormat df = new DecimalFormat( "$###,###,###.00" );
		return df.format( amt );
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
	
	String apache_get_version()
	{
		return "THIS IS NOT APACHE YOU DUMMY!!!";
	}
}