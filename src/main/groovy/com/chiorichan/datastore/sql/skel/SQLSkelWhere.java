/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.sql.skel;


/**
 * Provides the Skeleton Interface for SQL Queries implementing the Where Methods
 */
public interface SQLSkelWhere<B, P>
{
	B or();
	
	B and();
	
	SQLWhereKeyValue<B> where( String key );
	
	SQLWhereGroup<B, P> group();
}
