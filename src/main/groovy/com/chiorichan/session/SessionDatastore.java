/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
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