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

/**
 * Handles the link between an Account and it's Handler
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public abstract class AccountHandler extends InteractivePermissible
{
	public Account currentAccount = null;
	
	/**
	 * Attachs an account to this handler
	 * 
	 * @param acct
	 *            The account to be attached
	 */
	public final void attachAccount( Account acct )
	{
		acct.putHandler( this );
		this.currentAccount = acct;
	}
	
	/**
	 * Returns the account current associated with this handler
	 * 
	 * @return The account
	 */
	public final Account getAccount()
	{
		return currentAccount;
	}
	
	/**
	 * Removes the current account from the handler
	 */
	public final void reset()
	{
		if ( currentAccount != null )
		{
			currentAccount.removeHandler( this );
			currentAccount = null;
		}
	}
	
	@Override
	public final boolean isValid()
	{
		return currentAccount != null;
	}
	
	@Override
	public final String getEntityId()
	{
		return ( currentAccount == null ) ? null : currentAccount.getAcctId();
	}
}
