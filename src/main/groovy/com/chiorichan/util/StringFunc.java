/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.util;

import io.netty.buffer.ByteBuf;

import java.awt.Color;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Provides Chiori-chan Web Server specific helper methods
 */
public class StringFunc
{
	// private static final String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
	
	// public static boolean validateIpAddress( final String ipAddress )
	// {
	// Pattern pattern = Pattern.compile( IPADDRESS_PATTERN );
	// Matcher matcher = pattern.matcher( ipAddress );
	// return matcher.matches();
	// }
	
	public static byte[] byteBuf2Bytes( ByteBuf buf )
	{
		byte[] bytes = new byte[buf.readableBytes()];
		int readerIndex = buf.readerIndex();
		buf.getBytes( readerIndex, bytes );
		return bytes;
	}
	
	public static String byteBuf2String( ByteBuf buf, Charset charset )
	{
		return new String( byteBuf2Bytes( buf ), charset );
	}
	
	public static String bytesToStringUTFNIO( byte[] bytes )
	{
		if ( bytes == null )
			return null;
		
		CharBuffer cBuffer = ByteBuffer.wrap( bytes ).asCharBuffer();
		return cBuffer.toString();
	}
	
	/**
	 * Returns true if either array contains elements from the other
	 */
	public static boolean comparable( Object[] array1, Object[] array2 )
	{
		if ( array1.length == 0 && array2.length == 0 )
			return true;
		for ( Object obj : array1 )
			if ( ArrayUtils.contains( array2, obj ) )
				return true;
		return false;
	}
	
	public static boolean containsValidChars( String ref )
	{
		return ref.matches( "[a-z0-9_]*" );
	}
	
	/**
	 * Copies all elements from the iterable collection of originals to the collection provided.
	 * 
	 * @param token
	 *            String to search for
	 * @param originals
	 *            An iterable collection of strings to filter.
	 * @param collection
	 *            The collection to add matches to
	 * @return the collection provided that would have the elements copied into
	 * @throws UnsupportedOperationException
	 *             if the collection is immutable and originals contains a string which starts with the specified search
	 *             string.
	 * @throws IllegalArgumentException
	 *             if any parameter is is null
	 * @throws IllegalArgumentException
	 *             if originals contains a null element. <b>Note: the collection may be modified before this is thrown</b>
	 */
	public static <T extends Collection<String>> T copyPartialMatches( final String token, final Iterable<String> originals, final T collection ) throws UnsupportedOperationException, IllegalArgumentException
	{
		Validate.notNull( token, "Search token cannot be null" );
		Validate.notNull( collection, "Collection cannot be null" );
		Validate.notNull( originals, "Originals cannot be null" );
		
		for ( String string : originals )
			if ( startsWithIgnoreCase( string, token ) )
				collection.add( string );
		
		return collection;
	}
	
	public static byte[] decodeBase64( byte[] bytes )
	{
		return Base64.decodeBase64( bytes );
	}
	
	public static byte[] decodeBase64( String var )
	{
		return Base64.decodeBase64( var );
	}
	
	public static String encodeBase64( byte[] bytes )
	{
		return Base64.encodeBase64String( bytes );
	}
	
	public static String encodeBase64( String var )
	{
		return encodeBase64( var.getBytes() );
	}
	
	public static String escapeFormat( String msg )
	{
		return msg.replaceAll( "%", "%%" );
	}
	
	/**
	 * Determines if a string is all lowercase using the toLowerCase() method.
	 * 
	 * @param str
	 *            The string to check
	 * @return Is it all lowercase?
	 */
	public static boolean isLowercase( String str )
	{
		return str.toLowerCase().equals( str );
	}
	
	public static boolean isTrue( String arg )
	{
		if ( arg == null )
			return false;
		
		return ( arg.equalsIgnoreCase( "true" ) || arg.equalsIgnoreCase( "1" ) );
	}
	
	/**
	 * Determines if a string is all uppercase using the toUpperCase() method.
	 * 
	 * @param str
	 *            The string to check
	 * @return Is it all uppercase?
	 */
	public static boolean isUppercase( String str )
	{
		return str.toUpperCase().equals( str );
	}
	
	public static boolean isValidMD5( String s )
	{
		return s.matches( "[a-fA-F0-9]{32}" );
	}
	
	public static String join( String[] args )
	{
		return Joiner.on( " " ).join( args );
	}
	
	/**
	 * See {@link Arrays#copyOfRange(Object[], int, int)}
	 */
	public static String join( String[] args, int start )
	{
		return join( Arrays.copyOfRange( args, start, args.length ) );
	}
	
	/**
	 * See {@link Arrays#copyOfRange(Object[], int, int)}
	 */
	public static String join( String[] args, int start, int end )
	{
		return join( Arrays.copyOfRange( args, start, end ) );
	}
	
	public static String limitLength( String text, int max )
	{
		if ( text.length() <= max )
			return text;
		return text.substring( 0, max ) + "...";
	}
	
	public static String md5( byte[] bytes )
	{
		return DigestUtils.md5Hex( bytes );
	}
	
	public static String md5( String str )
	{
		if ( str == null )
			return null;
		
		return DigestUtils.md5Hex( str );
	}
	
	public static Color parseColor( String color )
	{
		Pattern c = Pattern.compile( "rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)" );
		Matcher m = c.matcher( color );
		
		// First try to parse RGB(0,0,0);
		if ( m.matches() )
			return new Color( Integer.valueOf( m.group( 1 ) ), // r
			Integer.valueOf( m.group( 2 ) ), // g
			Integer.valueOf( m.group( 3 ) ) ); // b
			
		try
		{
			Field field = Class.forName( "java.awt.Color" ).getField( color.trim().toUpperCase() );
			return ( Color ) field.get( null );
		}
		catch ( Exception e )
		{
		}
		
		try
		{
			return Color.decode( color );
		}
		catch ( Exception e )
		{
		}
		
		return null;
	}
	
	public static String removeInvalidChars( String ref )
	{
		return ref.replaceAll( "[^a-z0-9_]", "" );
	}
	
	public static List<String> repeatToList( String chr, int length )
	{
		List<String> list = Lists.newArrayList();
		for ( int i = 0; i < length; i++ )
			list.add( chr );
		return list;
	}
	
	public static String replaceAt( String par, int at, String rep )
	{
		StringBuilder sb = new StringBuilder( par );
		sb.setCharAt( at, rep.toCharArray()[0] );
		return sb.toString();
	}
	
	/**
	 * This method uses a substring to check case-insensitive equality. This means the internal array does not need to be
	 * copied like a toLowerCase() call would.
	 * 
	 * @param string
	 *            String to check
	 * @param prefix
	 *            Prefix of string to compare
	 * @return true if provided string starts with, ignoring case, the prefix provided
	 * @throws NullPointerException
	 *             if prefix is null
	 * @throws IllegalArgumentException
	 *             if string is null
	 */
	public static boolean startsWithIgnoreCase( final String string, final String prefix ) throws IllegalArgumentException, NullPointerException
	{
		Validate.notNull( string, "Cannot check a null string for a match" );
		if ( string.length() < prefix.length() )
			return false;
		return string.substring( 0, prefix.length() ).equalsIgnoreCase( prefix );
	}
	
	public static byte[] stringToBytesASCII( String str )
	{
		byte[] b = new byte[str.length()];
		for ( int i = 0; i < b.length; i++ )
			b[i] = ( byte ) str.charAt( i );
		return b;
	}
	
	public static byte[] stringToBytesUTF( String str )
	{
		byte[] b = new byte[str.length() << 1];
		for ( int i = 0; i < str.length(); i++ )
		{
			char strChar = str.charAt( i );
			int bpos = i << 1;
			b[bpos] = ( byte ) ( ( strChar & 0xFF00 ) >> 8 );
			b[bpos + 1] = ( byte ) ( strChar & 0x00FF );
		}
		return b;
	}
	
	/**
	 * Scans a string list for entries that are not lower case.
	 * 
	 * @param strings
	 *            The original list to check.
	 * @return Lowercase string array.
	 */
	public static String[] toLowerCase( Collection<String> strings )
	{
		List<String> result = Lists.newArrayList();
		for ( String string : strings )
			if ( string != null )
				result.add( string.toLowerCase() );
		return result.toArray( new String[0] );
	}
	
	/**
	 * Scans a string array for entries that are not lower case.
	 * 
	 * @param stringList
	 *            The original array to check.
	 * @return The corrected string array.
	 */
	public static String[] toLowerCase( String... array )
	{
		return toLowerCase( Arrays.asList( array ) );
	}
	
	public static Collection<String> toLowerCaseList( Collection<String> strings )
	{
		List<String> result = Lists.newArrayList();
		
		for ( String string : strings )
			result.add( string.toLowerCase() );
		
		return result;
	}
	
	/**
	 * Scans a string array for entries that are not lower case.
	 * 
	 * @param stringList
	 *            The original array to check.
	 * @return The corrected string array.
	 */
	public static Collection<String> toLowerCaseList( String... array )
	{
		return toLowerCaseList( Arrays.asList( array ) );
	}
	
	/**
	 * Trim specified character from both ends of a String
	 * 
	 * @param text
	 *            Text
	 * @param character
	 *            Character to remove
	 * @return Trimmed text
	 */
	public static String trimAll( String text, char character )
	{
		String normalizedText = trimFront( text, character );
		
		return trimEnd( normalizedText, character );
	}
	
	/**
	 * Trim specified character from end of string
	 * 
	 * @param text
	 *            Text
	 * @param character
	 *            Character to remove
	 * @return Trimmed text
	 */
	public static String trimEnd( String text, char character )
	{
		String normalizedText;
		int index;
		
		if ( text == null || text.isEmpty() )
			return text;
		
		normalizedText = text.trim();
		index = normalizedText.length() - 1;
		
		while ( normalizedText.charAt( index ) == character )
			if ( --index < 0 )
				return "";
		return normalizedText.substring( 0, index + 1 ).trim();
	}
	
	/**
	 * Trim specified character from front of string
	 * 
	 * @param text
	 *            Text
	 * @param character
	 *            Character to remove
	 * @return Trimmed text
	 */
	public static String trimFront( String text, char character )
	{
		String normalizedText;
		int index;
		
		if ( text == null || text.isEmpty() )
			return text;
		
		normalizedText = text.trim();
		index = -1;
		
		do
			index++;
		while ( index < normalizedText.length() && normalizedText.charAt( index ) == character );
		
		return normalizedText.substring( index ).trim();
	}
	
	public static Collection<String> wrap( Collection<String> col )
	{
		return wrap( col, '`' );
	}
	
	public static Collection<String> wrap( Collection<String> col, char wrap )
	{
		synchronized ( col )
		{
			String[] strs = col.toArray( new String[0] );
			col.clear();
			for ( int i = 0; i < strs.length; i++ )
				col.add( wrap( strs[i], wrap ) );
		}
		
		return col;
	}
	
	public static List<String> wrap( List<String> list )
	{
		return wrap( list, '`' );
	}
	
	public static List<String> wrap( List<String> list, char wrap )
	{
		synchronized ( list )
		{
			String[] strs = list.toArray( new String[0] );
			
			try
			{
				list.add( "DUMMY ADD!" );
			}
			catch ( UnsupportedOperationException e )
			{
				list = Lists.newLinkedList();
			}
			
			list.clear();
			for ( int i = 0; i < strs.length; i++ )
				list.add( wrap( strs[i], wrap ) );
		}
		
		return list;
	}
	
	public static Map<String, String> wrap( Map<String, String> map )
	{
		return wrap( map, '`', '\'' );
	}
	
	public static Map<String, String> wrap( Map<String, String> map, char keyWrap, char valueWrap )
	{
		synchronized ( map )
		{
			String[] keys = map.keySet().toArray( new String[0] );
			String[] values = map.values().toArray( new String[0] );
			
			try
			{
				map.put( "DUMMY KEY", "DUMMY VALUE" );
			}
			catch ( UnsupportedOperationException e )
			{
				map = Maps.newHashMap();
			}
			
			map.clear();
			for ( int i = 0; i < keys.length; i++ )
				map.put( wrap( keys[i], keyWrap ), wrap( values[i], valueWrap ) );
		}
		
		return map;
		
		/*
		 * Map<String, String> newMap = Maps.newHashMap();
		 * 
		 * for ( Entry<String, String> e : map.entrySet() )
		 * if ( e.getKey() != null && !e.getKey().isEmpty() )
		 * newMap.put( keyWrap + e.getKey() + keyWrap, valueWrap + ( e.getValue() == null ? "" : e.getValue() ) + valueWrap );
		 * 
		 * return newMap;
		 */
	}
	
	public static Set<String> wrap( Set<String> set )
	{
		return wrap( set, '`' );
	}
	
	public static Set<String> wrap( Set<String> set, char wrap )
	{
		synchronized ( set )
		{
			String[] strs = set.toArray( new String[0] );
			
			try
			{
				set.add( "DUMMY ADD!" );
			}
			catch ( UnsupportedOperationException e )
			{
				set = Sets.newLinkedHashSet();
			}
			
			set.clear();
			for ( int i = 0; i < strs.length; i++ )
				set.add( wrap( strs[i], wrap ) );
		}
		
		return set;
	}
	
	public static String wrap( String str, char wrap )
	{
		return String.format( "%s%s%s", wrap, str, wrap );
	}
}
