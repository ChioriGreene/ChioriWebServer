package com.chiorichan.user;

import java.util.LinkedHashMap;
import java.util.Map;

import com.chiorichan.util.ObjectUtil;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

public class UserMetaData
{
	Map<String, Object> metaData = Maps.newLinkedHashMap();
	
	public boolean hasMinimumData()
	{
		return metaData.containsKey( "username" ) && metaData.containsKey( "password" ) && metaData.containsKey( "userId" );
	}
	
	public String getUsername()
	{
		return getString( "username" );
	}
	
	public String getPassword()
	{
		return getString( "password" );
	}
	
	public Object getObject( String key )
	{
		return metaData.get( key );
	}
	
	public void set( String key, Object obj )
	{
		metaData.put( key, obj );
	}
	
	public String getString( String key )
	{
		return ObjectUtil.castToString( metaData.get( key ) );
	}
	
	public Integer getInteger( String key )
	{
		Object obj = metaData.get( key );
		
		if ( obj instanceof String )
			return Integer.parseInt( (String) obj );
		else
			return (Integer) obj;
	}
	
	public Boolean getBoolean( String key )
	{
		Object obj = metaData.get( key );
		
		if ( obj instanceof String )
			return Boolean.parseBoolean( (String) obj );
		else
			return (Boolean) obj;
	}
	
	public void setAll( LinkedHashMap<String, Object> data )
	{
		metaData.putAll( data );
	}
	
	public String toString()
	{
		return Joiner.on( "," ).withKeyValueSeparator( "=" ).join( metaData );
	}
	
	public boolean containsKey( String key )
	{
		return metaData.containsKey( key );
	}

	public String getUserId()
	{
		return getString( "userID" );
	}
}