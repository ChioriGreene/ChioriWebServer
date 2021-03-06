/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.session;

import java.util.List;

/**
 * Base class for Session Storage
 */
public abstract class SessionDatastore
{
	abstract List<SessionData> getSessions() throws SessionException;
	
	abstract SessionData createSession( String sessionId, SessionWrapper wrapper ) throws SessionException;
}
