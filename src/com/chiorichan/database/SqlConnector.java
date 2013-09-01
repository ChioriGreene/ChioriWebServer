package com.chiorichan.database;

import java.net.ConnectException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.chiorichan.Loader;
import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;
import com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException;

public class SqlConnector
{
	public Connection con;
	
	private String saved_db, saved_user, saved_pass, saved_host, saved_port;
	
	public SqlConnector()
	{
		
	}
	
	public SqlConnector(String db, String user, String pass) throws SQLException, ClassNotFoundException, ConnectException
	{
		init( db, user, pass, null, null );
	}
	
	public SqlConnector(String db, String user, String pass, String host) throws SQLException, ClassNotFoundException, ConnectException
	{
		init( db, user, pass, host, null );
	}
	
	public SqlConnector(String db, String user, String pass, String host, String port) throws SQLException, ClassNotFoundException, ConnectException
	{
		init( db, user, pass, host, port );
	}
	
	public void init( String db, String user, String pass, String host, String port ) throws SQLException, ClassNotFoundException, ConnectException
	{
		if ( host == null )
			host = "localhost";
		
		if ( port == null )
			port = "3306";
		
		Class.forName( "com.mysql.jdbc.Driver" );
		
		saved_db = db;
		saved_user = user;
		saved_pass = pass;
		saved_host = host;
		saved_port = port;
		
		con = DriverManager.getConnection( "jdbc:mysql://" + host + ":" + port + "/" + db, user, pass );
		
		Loader.getLogger().info( "We succesully connected to the sql database using 'jdbc:mysql://" + host + ":" + port + "/" + db + "'." );
	}
	
	public HashMap<String, Object> selectOne( String table, List<String> keys, List<? extends Object> values ) throws SQLException
	{
		if ( isNull( keys ) || isNull( values ) )
		{
			Loader.getLogger().warning( "[DB ERROR] Either keys array or values array equals null!\n" );
			return null;
		}
		
		if ( keys.size() != values.size() )
		{
			System.err.print( "[DB ERROR] Keys array and values array must match in length!\n" );
			return null;
		}
		
		HashMap<String, Object> result = new HashMap<String, Object>();
		
		String where = "";
		
		if ( keys.size() > 0 && values.size() > 0 )
		{
			int x = 0;
			String prefix = "";
			for ( String s : keys )
			{
				where += prefix + "`" + s + "` = '" + values.get( x ) + "'";
				x++;
				prefix = " AND ";
			}
		}
		
		ResultSet rs = query( "SELECT * FROM `" + table + "` WHERE " + where + " LIMIT 1;" );
		
		if ( rs != null && getRowCount( rs ) > 0 )
		{
			try
			{
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();
				
				do
				{
					for ( int i = 1; i < columnCount + 1; i++ )
					{
						result.put( rsmd.getColumnName( i ), rs.getObject( i ) );
					}
				}
				while ( rs.next() );
				
				return result;
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	public Boolean isNull( Object o )
	{
		if ( o == null )
			return true;
		
		return false;
	}
	
	public int getRowCount( ResultSet rs )
	{
		try
		{
			// int curRow = rs.getRow();
			rs.last();
			int lastRow = rs.getRow();
			rs.first(); // TODO: Set the row???
			return lastRow;
		}
		catch ( Exception e )
		{
			return 0;
		}
	}
	
	public Boolean isConnected()
	{
		try
		{
			return !con.isClosed();
		}
		catch ( SQLException e )
		{
			return false;
		}
	}
	
	public Boolean reconnect()
	{
		try
		{
			con = DriverManager.getConnection( "jdbc:mysql://" + saved_host + ":" + saved_port + "/" + saved_db, saved_user, saved_pass );
			System.out.print( "We succesully connected to the sql database.\n" );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		
		return true;
	}
	
	public int queryUpdate( String query )
	{
		int cnt = 0;
		
		try
		{
			PreparedStatement statement = con.prepareStatement( query );
			statement.execute();
			cnt = statement.getUpdateCount();
		}
		catch ( MySQLNonTransientConnectionException e )
		{
			if ( reconnect() )
				return queryUpdate( query );
		}
		catch ( CommunicationsException e )
		{
			if ( reconnect() )
				return queryUpdate( query );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		
		Loader.getLogger().fine( "Update Query: \"" + query + "\" which affected " + cnt + " row(s)." );
		return cnt;
	}
	
	public ResultSet query( String query ) throws SQLException
	{
		try
		{
			PreparedStatement statement = con.prepareStatement( query );
			ResultSet result = statement.executeQuery();
			
			// System.out.print( "Query: \"" + query + "\" which returned " + getRowCount( result ) + " row(s).\n" );
			
			return result;
		}
		catch ( MySQLNonTransientConnectionException e )
		{
			if ( reconnect() )
				return query( query );
		}
		catch ( CommunicationsException e )
		{
			if ( reconnect() )
				return query( query );
		}
		
		return null;
	}
	
	public Boolean update( String table, List<? extends Object> keys, List<? extends Object> values )
	{
		return update( table, keys, values, null, null );
	}
	
	public Boolean update( String table, List<? extends Object> keys, List<? extends Object> list, List<? extends Object> keysW, List<? extends Object> valuesW )
	{
		if ( isNull( keys ) || isNull( list ) )
		{
			System.err.print( "[DB ERROR] Either keys array or values array equals null!\n" );
			return false;
		}
		
		if ( keys.size() != list.size() )
		{
			System.err.print( "[DB ERROR] Keys array and values array must match in length!\n" );
			return false;
		}
		
		if ( keysW.size() != valuesW.size() )
		{
			System.err.print( "[DB ERROR] Where keys array and where values array must match in length!\n" );
			return false;
		}
		
		if ( keys.size() < 1 )
		{
			System.err.print( "[DB ERROR] The keys to be updated can not be empty!\n" );
			return false;
		}
		
		String where = "";
		String update = "";
		
		int x = 0;
		String prefix = "";
		
		for ( Object s : keys )
		{
			update += prefix + "`" + s + "` = '" + list.get( x ) + "'";
			x++;
			prefix = ", ";
		}
		
		if ( !isNull( keysW ) && !isNull( valuesW ) && keysW.size() > 0 && valuesW.size() > 0 )
		{
			x = 0;
			prefix = "";
			
			for ( Object s : keysW )
			{
				where += prefix + "`" + s + "` = '" + valuesW.get( x ) + "'";
				x++;
				prefix = " AND ";
			}
			
			if ( where.length() > 0 )
				where = " WHERE " + where;
		}
		
		int cnt = queryUpdate( "UPDATE " + table + " SET " + update + where + ";" );
		
		return ( cnt > 0 );
	}
	
	public HashMap<String, Object> selectOne( String table, String key, String val ) throws SQLException
	{
		return selectOne( table, Arrays.asList( key ), Arrays.asList( val ) );
	}
	
	public static JSONObject convert( ResultSet rs ) throws SQLException, JSONException
	{
		JSONObject json = new JSONObject();
		ResultSetMetaData rsmd = rs.getMetaData();
		int x = 0;
		
		do
		{
			int numColumns = rsmd.getColumnCount();
			JSONObject obj = new JSONObject();
			
			for ( int i = 1; i < numColumns + 1; i++ )
			{
				String column_name = rsmd.getColumnName( i );
				
				if ( rsmd.getColumnType( i ) == java.sql.Types.ARRAY )
				{
					obj.put( column_name, rs.getArray( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.BIGINT )
				{
					obj.put( column_name, rs.getInt( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.TINYINT )
				{
					obj.put( column_name, rs.getInt( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.BIT ) // Sometimes tinyints are read as being bits
				{
					obj.put( column_name, rs.getInt( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.BOOLEAN )
				{
					obj.put( column_name, rs.getBoolean( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.BLOB )
				{
					obj.put( column_name, rs.getBlob( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.DOUBLE )
				{
					obj.put( column_name, rs.getDouble( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.FLOAT )
				{
					obj.put( column_name, rs.getFloat( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.INTEGER )
				{
					obj.put( column_name, rs.getInt( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.NVARCHAR )
				{
					obj.put( column_name, rs.getNString( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.VARCHAR )
				{
					obj.put( column_name, rs.getString( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.SMALLINT )
				{
					obj.put( column_name, rs.getInt( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.DATE )
				{
					obj.put( column_name, rs.getDate( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.TIMESTAMP )
				{
					obj.put( column_name, rs.getTimestamp( column_name ) );
				}
				else
				{
					obj.put( column_name, rs.getObject( column_name ) );
				}
			}
			
			json.put( "" + x, obj );
			x++;
		}
		while ( rs.next() );
		
		return json;
	}
}
