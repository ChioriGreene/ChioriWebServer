/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.http;

public enum HttpResponseStage
{
	READING( 0 ), WRITTING( 1 ), WRITTEN( 2 ), CLOSED( 3 ), MULTIPART( 4 );
	
	private final int stageId;
	
	HttpResponseStage( int id )
	{
		stageId = id;
	}
	
	public int getId()
	{
		return stageId;
	}
}
