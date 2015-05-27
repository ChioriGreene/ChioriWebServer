/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.ConsoleLogger;
import com.chiorichan.Loader;
import com.chiorichan.ServerManager;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.event.EventBus;
import com.chiorichan.plugin.PluginDescriptionFile;
import com.chiorichan.scheduler.TaskCreator;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.util.RandomFunc;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

/**
 * Provides Account Management to the Server
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public final class AccountManager extends AccountEvents implements ServerManager, TaskCreator
{
	/**
	 * Holds an instance of this Account Manager
	 */
	public static final AccountManager INSTANCE = new AccountManager();
	
	/**
	 * Has this manager already been initialized?
	 */
	private static boolean isInitialized = false;
	
	/**
	 * References accounts meta data. We try and populate this list at load with all available accounts but this is not always guaranteed.
	 */
	final AccountList accounts = new AccountList();
	
	boolean isDebug = false;
	int maxAccounts = -1;
	
	public static void init()
	{
		if ( isInitialized )
			throw new IllegalStateException( "The Account Manager has already been initialized." );
		
		assert INSTANCE != null;
		
		INSTANCE.init0();
		
		isInitialized = true;
	}
	
	private AccountManager()
	{
		
	}
	
	private void init0()
	{
		isDebug = Loader.getConfig().getBoolean( "accounts.debug" );
		maxAccounts = Loader.getConfig().getInt( "accounts.maxLogins", -1 );
		
		EventBus.INSTANCE.registerEvents( AccountType.MEMORY.getCreator(), this );
		EventBus.INSTANCE.registerEvents( AccountType.SQL.getCreator(), this );
		EventBus.INSTANCE.registerEvents( AccountType.FILE.getCreator(), this );
	}
	
	public AccountMeta createAccount( String acctId, String siteId )
	{
		return createAccount( acctId, siteId, AccountType.getDefaultType() );
	}
	
	public AccountMeta createAccount( String acctId, String siteId, AccountType type )
	{
		if ( !type.isEnabled() )
			throw AccountResult.FEATURE_DISABLED.exception();
		
		AccountContext context = type.getCreator().createAccount( acctId, siteId );
		
		return new AccountMeta( context );
	}
	
	public String generateAcctId( String seed )
	{
		String acctId = "";
		
		if ( seed == null || seed.isEmpty() )
			acctId = RandomFunc.randomize( "ab123C" );
		else
		{
			if ( seed.contains( " " ) || seed.contains( "|" ) )
			{
				String[] split = seed.split( " |\\|" );
				acctId += ( split.length < 1 || split[0].isEmpty() ? "" + RandomFunc.randomize( 'a' ) : split[0].substring( 0, 1 ) ).toLowerCase();
				acctId += ( split.length < 2 || split[1].isEmpty() ? "" + RandomFunc.randomize( 'b' ) : split[1].substring( 0, 1 ) ).toLowerCase();
				acctId += "123";
				acctId += ( split.length < 3 || split[2].isEmpty() ? "" + RandomFunc.randomize( 'C' ) : split[2].substring( 0, 1 ) ).toUpperCase();
			}
		}
		
		assert acctId.length() == 6;
		assert acctId.matches( "[a-z]{2}[0-9]{3}[A-Z]" );
		
		int tries = 1;
		
		do
		{
			// When our tries are divisible by 25 we attempt to randomize the last letter for more chances.
			if ( tries % 25 == 0 )
				acctId = acctId.substring( 0, 5 ) + RandomFunc.randomize( acctId.substring( 5 ) );
			
			acctId = acctId.substring( 0, 2 ) + RandomFunc.randomize( "123" ) + acctId.substring( acctId.length() - 1 );
			
			tries++;
		}
		while ( exists( acctId ) );
		
		return acctId;
	}
	
	private boolean exists( String acctId )
	{
		if ( accounts.keySet().contains( acctId ) )
			return true;
		
		for ( AccountType type : AccountType.getAccountTypes() )
		{
			if ( type.getCreator().exists( acctId ) )
			{
				return true;
			}
		}
		return false;
	}
	
	public AccountMeta getAccount( String acctId )
	{
		AccountMeta acct = accounts.get( acctId );
		
		if ( acct == null )
		{
			acct = fireAccountLookup( acctId );
			
			if ( acct == null )
				return null;
			
			accounts.put( acct );
		}
		
		return acct;
	}
	
	public AccountMeta getAccountWithException( String acctId ) throws AccountException
	{
		AccountMeta acct = accounts.get( acctId );
		
		if ( acct == null )
		{
			acct = fireAccountLookupWithException( acctId );
			
			if ( acct == null )
				return null;
			
			accounts.put( acct );
		}
		
		return acct;
	}
	
	public AccountMeta getAccountPartial( String partial ) throws AccountException
	{
		Validate.notNull( partial );
		
		AccountMeta found = null;
		String lowerName = partial.toLowerCase();
		int delta = Integer.MAX_VALUE;
		for ( AccountMeta meta : getAccounts() )
		{
			if ( meta.getAcctId().toLowerCase().startsWith( lowerName ) )
			{
				int curDelta = meta.getAcctId().length() - lowerName.length();
				if ( curDelta < delta )
				{
					found = meta;
					delta = curDelta;
				}
				if ( curDelta == 0 )
					break;
			}
		}
		return found;
	}
	
	public Set<Account> getInitializedAccounts()
	{
		Set<Account> accts = Sets.newHashSet();
		for ( AccountMeta meta : accounts )
			if ( meta.isInitialized() )
				accts.add( meta );
		return accts;
	}
	
	/**
	 * Gets all Account Permissibles by crawling the {@link AccountMeta} and {@link AccountInstance}
	 * 
	 * @return
	 *         A set of AccountPermissibles
	 */
	public Set<AccountPermissible> getAccountPermissibles()
	{
		Set<AccountPermissible> accts = Sets.newHashSet();
		for ( AccountMeta meta : accounts )
			if ( meta.isInitialized() )
				accts.addAll( Arrays.asList( meta.instance().getPermissibles() ) );
		return accts;
	}
	
	Set<AccountMeta> getAccounts0()
	{
		return accounts.toSet();
	}
	
	public Set<AccountMeta> getAccounts()
	{
		return Collections.unmodifiableSet( getAccounts0() );
	}
	
	public Set<AccountMeta> getAccounts( String key, String value )
	{
		Validate.notNull( key );
		Validate.notNull( value );
		
		Set<AccountMeta> results = Sets.newHashSet();
		
		if ( value.contains( "|" ) )
		{
			for ( String s : Splitter.on( "|" ).split( value ) )
				if ( s != null && !s.isEmpty() )
					results.addAll( getAccounts( key, s ) );
			
			return results;
		}
		
		boolean isLower = value.toLowerCase().equals( value ); // Is query string all lower case?
		
		for ( AccountMeta meta : accounts.toSet() )
		{
			String str = ( isLower ) ? meta.getString( key ).toLowerCase() : meta.getString( key );
			
			if ( str != null && !str.isEmpty() && str.contains( value ) )
			{
				results.add( meta );
				continue;
			}
		}
		
		return results;
	}
	
	public Set<AccountMeta> getAccountsBySite( String site )
	{
		return getAccountsBySite( SiteManager.INSTANCE.getSiteById( site ) );
	}
	
	public Set<AccountMeta> getAccountsBySite( Site site )
	{
		Validate.notNull( site );
		
		Set<AccountMeta> results = Sets.newHashSet();
		
		for ( AccountMeta meta : accounts.toSet() )
			if ( meta.getSite() == site )
				results.add( meta );
		
		return results;
	}
	
	public Set<AccountMeta> getAccounts( String query )
	{
		Validate.notNull( query );
		
		Set<AccountMeta> results = Sets.newHashSet();
		
		if ( query.contains( "|" ) )
		{
			for ( String s : Splitter.on( "|" ).split( query ) )
				if ( s != null && !s.isEmpty() )
					results.addAll( getAccounts( s ) );
			
			return results;
		}
		
		boolean isLower = query.toLowerCase().equals( query ); // Is query string all lower case?
		
		for ( AccountMeta meta : accounts.toSet() )
		{
			String id = ( isLower ) ? meta.getAcctId().toLowerCase() : meta.getAcctId();
			
			if ( !id.isEmpty() && id.contains( query ) )
			{
				results.add( meta );
				continue;
			}
			
			id = ( isLower ) ? meta.getDisplayName().toLowerCase() : meta.getDisplayName();
			
			if ( !id.isEmpty() && id.contains( query ) )
			{
				results.add( meta );
				continue;
			}
			
			// TODO Figure out how to further check these values.
			// Maybe send the check into the Account Creator
		}
		
		return results;
	}
	
	public Set<Account> getBanned()
	{
		Set<Account> accts = Sets.newHashSet();
		for ( AccountMeta meta : accounts )
			if ( meta.isBanned() )
				accts.add( meta );
		return accts;
	}
	
	public Set<Account> getWhitelisted()
	{
		Set<Account> accts = Sets.newHashSet();
		for ( AccountMeta meta : accounts )
			if ( meta.isWhitelisted() )
				accts.add( meta );
		return accts;
	}
	
	public Set<Account> getOperators()
	{
		Set<Account> accts = Sets.newHashSet();
		for ( AccountMeta meta : accounts )
			if ( meta.isOp() )
				accts.add( meta );
		return accts;
	}
	
	public void save()
	{
		for ( AccountMeta meta : accounts )
			meta.save();
	}
	
	public void reload()
	{
		save();
		accounts.clear();
	}
	
	@Override
	public boolean isEnabled()
	{
		return true;
	}
	
	@Override
	public String getName()
	{
		return "AccountManager";
	}
	
	@Override
	public PluginDescriptionFile getDescription()
	{
		return null;
	}
	
	public static ConsoleLogger getLogger()
	{
		return Loader.getLogger( "AcctMgr" );
	}
	
	public boolean isDebug()
	{
		return isDebug;
	}
	
	/**
	 * Attempts to kick all logins of account
	 * 
	 * @param acct
	 *            The Account to kick
	 * @param msg
	 *            The reason for kick
	 * @return Was the kick successful
	 */
	public boolean kick( AccountInstance acct, String msg )
	{
		Validate.notNull( acct );
		
		return fireKick( acct, msg );
	}
	
	/**
	 * See {@link #kick(AccountInstance, String)}
	 */
	public boolean kick( AccountMeta acct, String msg )
	{
		Validate.notNull( acct );
		
		if ( !acct.isInitialized() )
			throw AccountResult.ACCOUNT_NOT_INITIALIZED.exception( acct.getDisplayName() );
		
		return kick( acct.instance(), msg );
	}
	
	/**
	 * Attempts to only kick the provided instance of login
	 * 
	 * @param acct
	 *            The instance to kick
	 * @param msg
	 *            The reason to kick
	 * @return Was the kick successful
	 */
	public boolean kick( AccountPermissible acct, String msg )
	{
		Validate.notNull( acct );
		
		return fireKick( acct, msg );
	}
}
