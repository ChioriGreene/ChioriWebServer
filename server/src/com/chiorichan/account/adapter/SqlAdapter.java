package com.chiorichan.account.adapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.json.JSONException;

import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.helpers.AccountMetaData;
import com.chiorichan.account.helpers.LoginException;
import com.chiorichan.account.helpers.LoginExceptionReasons;
import com.chiorichan.account.helpers.LookupAdapterException;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.framework.Site;
import com.chiorichan.util.Common;
import com.google.common.collect.Lists;

public class SqlAdapter implements AccountLookupAdapter
{
	SqlConnector sql;
	String table;
	List<String> accountFields;
	
	public SqlAdapter() throws LookupAdapterException
	{
		sql = Loader.getPersistenceManager().getDatabase();
		table = Loader.getConfig().getString( "accounts.lookupAdapter.table", "accounts" );
		accountFields = Loader.getConfig().getStringList( "accounts.lookupAdapter.fields", new ArrayList<String>() );
	}
	
	public SqlAdapter(Site site) throws LookupAdapterException
	{
		Validate.notNull( site );
		
		sql = site.getDatabase();
		table = site.getYaml().getString( "accounts.table", "accounts" );
		accountFields = site.getYaml().getStringList( "accounts.fields", new ArrayList<String>() );
	}
	
	public ResultSet getResultSet( String uid ) throws SQLException
	{
		if ( uid == null || uid.isEmpty() )
			return null;
		
		ResultSet rs = sql.query( "SELECT * FROM `accounts` WHERE `acctID` = '" + uid + "' LIMIT 1;" );
		
		if ( rs == null || sql.getRowCount( rs ) < 1 )
			return null;
		
		return rs;
	}
	
	@Override
	public List<AccountMetaData> getAccounts()
	{
		List<AccountMetaData> metas = Lists.newArrayList();
		
		try
		{
			ResultSet rs = sql.query( "SELECT * FROM `" + table + "`;" );
			
			if ( rs == null || sql.getRowCount( rs ) < 1 )
				return Lists.newArrayList();
			
			do
			{
				AccountMetaData meta = new AccountMetaData();
				meta.setAll( SqlConnector.convertRow( rs ) );
				meta.set( "displayName", ( rs.getString( "fname" ).isEmpty() ) ? rs.getString( "name" ) : rs.getString( "fname" ) + " " + rs.getString( "name" ) );
				metas.add( meta );
			}
			while ( rs.next() );
		}
		catch ( SQLException | JSONException e )
		{
			return metas;
		}
		
		return metas;
	}
	
	@Override
	public void saveAccount( AccountMetaData account )
	{
		
	}
	
	@Override
	public AccountMetaData reloadAccount( AccountMetaData account )
	{
		return null;
	}
	
	@Override
	public AccountMetaData loadAccount( String accountname ) throws LoginException
	{
		try
		{
			AccountMetaData meta = new AccountMetaData();
			
			if ( accountname == null || accountname.isEmpty() )
				throw new LoginException( LoginExceptionReasons.emptyUsername );
			
			String additionalAccountFields = "";
			for ( String f : accountFields )
			{
				additionalAccountFields += " OR `" + f + "` = '" + accountname + "'";
			}
			
			ResultSet rs = sql.query( "SELECT * FROM `" + table + "` WHERE `accountname` = '" + accountname + "' OR `accountID` = '" + accountname + "'" + additionalAccountFields + ";" );
			
			if ( rs == null || sql.getRowCount( rs ) < 1 )
				throw new LoginException( LoginExceptionReasons.incorrectLogin );
			
			meta.setAll( SqlConnector.convertRow( rs ) );
			
			meta.set( "displayName", ( rs.getString( "fname" ).isEmpty() ) ? rs.getString( "name" ) : rs.getString( "fname" ) + " " + rs.getString( "name" ) );
			
			return meta;
		}
		catch ( SQLException | JSONException e )
		{
			throw new LoginException( e );
		}
	}
	
	@Override
	public void preLoginCheck( Account account ) throws LoginException
	{
		AccountMetaData meta = account.getMetaData();
		
		if ( meta.getInteger( "numloginfail" ) > 5 )
			if ( meta.getInteger( "lastloginfail" ) > ( Common.getEpoch() - 1800 ) )
				throw new LoginException( LoginExceptionReasons.underAttackPleaseWait );
		
		if ( !meta.getString( "actnum" ).equals( "0" ) )
			throw new LoginException( LoginExceptionReasons.accountNotActivated );
	}
	
	@Override
	public void postLoginCheck( Account account ) throws LoginException
	{
		try
		{
			sql.queryUpdate( "UPDATE `accounts` SET `lastactive` = '" + Common.getEpoch() + "', `lastlogin` = '" + Common.getEpoch() + "', `lastloginfail` = 0, `numloginfail` = 0 WHERE `accountID` = '" + account.getAccountId() + "'" );
		}
		catch ( SQLException e )
		{
			throw new LoginException( e );
		}
	}
	
	@Override
	public void failedLoginUpdate( Account account )
	{
		// TODO Update use as top reflect this failure.
		// sql.queryUpdate( "UPDATE `accounts` SET `lastactive` = '" + Common.getEpoch() + "', `lastloginfail` = 0, `numloginfail` = 0 WHERE `accountID` = '" + account.getAccountId() + "'" );
	}
	
	@Override
	public boolean matchAccount( Account account, String accountname )
	{
		AccountMetaData meta = account.getMetaData();
		
		for ( String f : accountFields )
		{
			if ( meta.getString( f ).equals( accountname ) )
				return true;
		}
		
		return false;
	}
}