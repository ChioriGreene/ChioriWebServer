/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account;

import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.Loader;
import com.chiorichan.account.adapter.AccountLookupAdapter;
import com.chiorichan.permission.PermissionResult;
import com.chiorichan.permission.structure.Permission;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

public abstract class Account<T extends AccountLookupAdapter> implements InteractiveEntity
{
	public Account( String userId, T adapter ) throws LoginException
	{
		if ( userId.isEmpty() )
			throw new LoginException( LoginExceptionReason.emptyUsername );
		
		if ( adapter == null )
			throw new LoginException( LoginExceptionReason.unknownError );
		
		lookupAdapter = adapter;
		
		metaData = adapter.readAccount( userId );
		acctId = metaData.getAcctId();
	}
	
	public Account( AccountMetaData meta, T adapter ) throws LoginException
	{
		if ( meta == null )
			throw new LoginException( LoginExceptionReason.unknownError );
		
		if ( adapter == null )
			throw new LoginException( LoginExceptionReason.unknownError );
		
		lookupAdapter = adapter;
		
		metaData = meta;
		acctId = meta.getAcctId();
	}
	
	/**
	 * Cached Account lookup adapter.
	 */
	protected final T lookupAdapter;
	
	/**
	 * Set of AccountHandlers
	 */
	protected volatile Set<AccountHandler> handlers = Sets.newHashSet();
	
	/**
	 * Account MetaData
	 */
	protected final AccountMetaData metaData;
	
	/**
	 * Account Id
	 */
	protected final String acctId;
	
	/**
	 * Get the baked registered listeners associated with this handler list
	 * 
	 * @return the array of registered listeners
	 */
	public final AccountHandler[] getHandlers()
	{
		checkHandlers();
		return handlers.toArray( new AccountHandler[0] );
	}
	
	public final void putHandler( AccountHandler handler )
	{
		if ( !handlers.contains( handler ) )
			handlers.add( handler );
	}
	
	public final void removeHandler( AccountHandler handler )
	{
		checkHandlers();
		handlers.remove( handler );
	}
	
	public final void clearHandlers()
	{
		checkHandlers();
		handlers.clear();
	}
	
	public final boolean hasHandler()
	{
		checkHandlers();
		return !handlers.isEmpty();
	}
	
	public final int countHandlers()
	{
		checkHandlers();
		return handlers.size();
	}
	
	private void checkHandlers()
	{
		// for ( AccountHandler h : handlers )
		// if ( !h.isValid() )
		// handlers.remove( h );
	}
	
	public final AccountMetaData getMetaData()
	{
		return metaData;
	}
	
	public final boolean validatePassword( String pass )
	{
		String password = getPassword();
		return ( password.equals( pass ) || password.equals( DigestUtils.md5Hex( pass ) ) || DigestUtils.md5Hex( password ).equals( pass ) );
	}
	
	@Override
	public boolean isBanned()
	{
		return Loader.getPermissionManager().getEntity( getAcctId() ).isBanned();
	}
	
	@Override
	public boolean isWhitelisted()
	{
		return Loader.getPermissionManager().getEntity( getAcctId() ).isWhitelisted();
	}
	
	@Override
	public boolean isAdmin()
	{
		return Loader.getPermissionManager().getEntity( getAcctId() ).isAdmin();
	}
	
	@Override
	public boolean isOp()
	{
		return Loader.getPermissionManager().getEntity( getAcctId() ).isOp();
	}
	
	public final PermissionResult checkPermission( String perm )
	{
		return Loader.getPermissionManager().getEntity( getAcctId() ).checkPermission( perm );
	}
	
	public final PermissionResult checkPermission( Permission perm )
	{
		return Loader.getPermissionManager().getEntity( getAcctId() ).checkPermission( perm );
	}
	
	/**
	 * @deprecated
	 *             {@link #checkPermission(String)} is to replace this method since it provides more options to the requester
	 * @param perm
	 *            The permission node, e.g., com.chiorichan.permission.node
	 * @return
	 *         The result of said check. Will always return false if permission value is not of type boolean.
	 */
	@Deprecated
	public final boolean hasPermission( String perm )
	{
		return checkPermission( perm ).isTrue();
	}
	
	public abstract String getPassword();
	
	public abstract String getDisplayName();
	
	public abstract String getUsername();
	
	public final boolean kick( final String kickMessage )
	{
		for ( AccountHandler h : handlers )
			if ( !h.kick( kickMessage ) )
				return false;
		return true;
		
	}
	
	public final void save()
	{
		lookupAdapter.saveAccount( metaData );
	}
	
	public String getAcctId()
	{
		return metaData.getAcctId();
	}
	
	public String toString()
	{
		return "User{" + metaData.toString() + ",Handlers{" + Joiner.on( "," ).join( handlers ) + "}}";
	}
	
	/**
	 * 
	 * @param key
	 *            Metadata key.
	 * @return String
	 *         Returns an empty string if no result.
	 */
	public String getString( String key )
	{
		return getString( key, "" );
	}
	
	/**
	 * Get a string from the Metadata with a default value
	 * 
	 * @param key
	 *            Metadata key.
	 * @param def
	 *            Default value to return if no result.
	 * @return String
	 */
	public String getString( String key, String def )
	{
		if ( !metaData.containsKey( key ) )
			return def;
		
		return metaData.getString( key );
	}
	
	@Override
	public final void sendMessage( String... msgs )
	{
		for ( String msg : msgs )
			sendMessage( msg );
	}
	
	@Override
	public final void sendMessage( String msg )
	{
		for ( AccountHandler h : handlers )
			h.sendMessage( msg );
	}
	
	public final void reloadAndValidate() throws LoginException
	{
		metaData.mergeData( lookupAdapter.reloadAccount( metaData ) );
	}
	
	public final AccountLookupAdapter getLookupAdapter()
	{
		return lookupAdapter;
	}
	
	/**
	 * Called before the AccountManager makes the login offical.
	 * 
	 * @throws LoginException
	 *             Throw this exception if you wish to interrupt the login
	 */
	public abstract void preLoginCheck() throws LoginException;
	
	/**
	 * Called as the last line before account is returned.
	 * 
	 * @throws LoginException
	 *             Throw this exception if you wish to interrupt the login
	 */
	public abstract void postLoginCheck() throws LoginException;
	
	/**
	 * Called from AccountManager to match the account using an array of fields, e.g., email, phone, name, acctId
	 * 
	 * @param id
	 *            The identifier to match against.
	 * @return Boolean
	 *         true if it matches.
	 */
	public abstract boolean isYou( String id );
}
