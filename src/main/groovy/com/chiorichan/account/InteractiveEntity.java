/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account;

/**
 * 
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public interface InteractiveEntity
{
	void sendMessage( String... msgs );
	
	void sendMessage( String string );
	
	boolean isBanned();
	
	boolean isWhitelisted();
	
	boolean isAdmin();
	
	boolean isOp();
	
	boolean kick( String kickMessage );
	
	boolean isValid();
}
