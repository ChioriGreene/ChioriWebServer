/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.site;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.chiorichan.ConsoleLogger;
import com.chiorichan.Loader;
import com.chiorichan.ServerManager;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.lang.SiteException;
import com.chiorichan.lang.StartupException;
import com.chiorichan.util.FileFunc;
import com.google.common.collect.Lists;

public class SiteManager implements ServerManager
{
	Map<String, Site> siteMap;
	
	public SiteManager()
	{
		siteMap = new LinkedHashMap<String, Site>();
	}
	
	public void init() throws StartupException
	{
		if ( siteMap.size() > 0 )
			throw new StartupException( "Site manager already has sites loaded. Please unload the existing sites first." );
		
		DatabaseEngine sql = Loader.getDatabase();
		
		// Load sites from YAML Filebase.
		File siteFileBase = new File( "sites" );
		
		FileFunc.directoryHealthCheck( siteFileBase );
		
		File defaultSite = new File( siteFileBase, "000-default.yaml" );
		
		// We make sure the default framework YAML FileBase exists and if not we copy it from the Jar.
		if ( !defaultSite.exists() )
		{
			try
			{
				defaultSite.getParentFile().mkdirs();
				FileFunc.putResource( "com/chiorichan/default-site.yaml", defaultSite );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
		
		FileFilter fileFilter = new WildcardFileFilter( "*.yaml" );
		File[] files = siteFileBase.listFiles( fileFilter );
		
		if ( files != null && files.length > 0 )
			for ( File f : files )
			{
				try
				{
					Site site = new Site( f );
					
					if ( site != null )
					{
						siteMap.put( site.siteId, site );
					}
				}
				catch ( SiteException e )
				{
					getLogger().warning( "Exception encountered while loading a site from YAML FileBase, Reason: " + e.getMessage() );
					if ( e.getCause() != null )
						e.getCause().printStackTrace();
				}
			}
		
		// Load sites from Framework Database.
		if ( sql != null && sql.isConnected() )
			try
			{
				if ( !sql.tableExist( "sites" ) )
				{
					DatabaseEngine.getLogger().info( "We detected the non-existence of table 'sites' in the server database, we will attempt to create it now." );
					
					String table = "CREATE TABLE `sites` (";
					table += "`siteId` varchar(255) NOT NULL,";
					table += " `title` varchar(255) NOT NULL DEFAULT 'Unnamed Chiori Framework Site',";
					table += " `domain` varchar(255) NOT NULL,";
					table += " `source` varchar(255) NOT NULL DEFAULT 'pages',";
					table += " `resource` varchar(255) NOT NULL DEFAULT 'resources',";
					table += " `subdomains` text NOT NULL,";
					table += " `protected` text NOT NULL,";
					table += " `metatags` text NOT NULL,";
					table += " `aliases` text NOT NULL,";
					table += " `configYaml` text NOT NULL";
					table += ");";
					
					sql.queryUpdate( table );
				}
				
				// Load sites from the database
				ResultSet rs = sql.query( "SELECT * FROM `sites`;" );
				
				if ( sql.getRowCount( rs ) > 0 )
				{
					do
					{
						try
						{
							Site site = new Site( rs );
							
							if ( site != null )
							{
								siteMap.put( site.siteId, site );
							}
						}
						catch ( SiteException e )
						{
							getLogger().severe( "Exception encountered while loading a site from SQL DataBase, Reason: " + e.getMessage() );
							if ( e.getCause() != null )
								e.getCause().printStackTrace();
						}
						
					}
					while ( rs.next() );
				}
			}
			catch ( SQLException e )
			{
				getLogger().warning( "Exception encountered while loading a sites from Database", e );
			}
	}
	
	public Site getSiteById( String siteId )
	{
		if ( siteId == null )
			return null;
		
		return siteMap.get( siteId );
	}
	
	public Site getSiteByDomain( String domain )
	{
		for ( Site site : siteMap.values() )
			if ( site != null )
				if ( site.getDomain().equalsIgnoreCase( domain.trim() ) )
					return site;
		
		return null;
	}
	
	public List<Site> getSites()
	{
		return new ArrayList<Site>( siteMap.values() );
	}
	
	public Site getFrameworkSite()
	{
		return getSiteById( "framework" );
	}
	
	public void reload()
	{
		siteMap = new LinkedHashMap<String, Site>();
		init();
	}
	
	public String remove( String siteId )
	{
		if ( siteId.equals( "framework" ) )
			return "You can not delete the framework site.";
		
		if ( siteMap.containsKey( siteId ) )
		{
			Site site = siteMap.get( siteId );
			
			// TODO Either confirm that someone want to delete a site or make it so they can be restored.
			
			switch ( site.siteType )
			{
				case SQL:
					DatabaseEngine sql = Loader.getDatabase();
					
					if ( sql != null && sql.isConnected() )
					{
						if ( !sql.delete( "sites", "`siteId` = '" + siteId + "'" ) )
							return "There was an unknown reason the site could be deleted.";
					}
					else
						return "There was an unknown reason the site could be deleted.";
					
					break;
				case FILE:
					File f = site.getFile();
					f.delete();
					
					break;
				default:
					return "There was an unknown reason the site could be deleted.";
			}
			
			siteMap.remove( siteId );
			
			return "The specified site was successfully removed.";
		}
		else
			return "The specified site was not found.";
	}
	
	public String add( String siteId, String type )
	{
		if ( siteMap.containsKey( siteId ) )
			return "There already exists a site by the id you provided.";
		
		if ( !type.equalsIgnoreCase( "sql" ) && !type.equalsIgnoreCase( "file" ) )
			return "The only available site types are 'sql' and 'file'. '" + type + "' was not a valid option.";
		
		return "";
	}
	
	public List<Site> parseSites( String sites )
	{
		return parseSites( sites, "|" );
	}
	
	public List<Site> parseSites( String sites, String regExSplit )
	{
		List<Site> siteList = Lists.newArrayList();
		String[] sitesArray = sites.split( regExSplit );
		
		for ( String siteId : sitesArray )
		{
			Site site = getSiteById( siteId );
			if ( site != null )
				siteList.add( site );
		}
		
		return siteList;
	}
	
	public static ConsoleLogger getLogger()
	{
		return Loader.getLogger( "SiteMgr" );
	}
}
