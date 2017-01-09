/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.site;

import io.netty.handler.ssl.SslContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.net.ssl.SSLException;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.text.WordUtils;

import com.chiorichan.AppConfig;
import com.chiorichan.account.AccountLocation;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.configuration.apache.ApacheConfiguration;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.datastore.DatastoreManager;
import com.chiorichan.datastore.sql.bases.H2SQLDatastore;
import com.chiorichan.datastore.sql.bases.MySQLDatastore;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.datastore.sql.bases.SQLiteDatastore;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventException;
import com.chiorichan.event.site.SiteLoadEvent;
import com.chiorichan.factory.ScriptBinding;
import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.factory.ScriptingFactory;
import com.chiorichan.factory.ScriptingResult;
import com.chiorichan.http.Routes;
import com.chiorichan.http.ssl.CertificateWrapper;
import com.chiorichan.lang.ApplicationException;
import com.chiorichan.lang.EnumColor;
import com.chiorichan.lang.ExceptionReport;
import com.chiorichan.lang.SiteException;
import com.chiorichan.logger.Log;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.session.SessionManager;
import com.chiorichan.session.SessionPersistenceMethod;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.FileFunc;
import com.chiorichan.util.NetworkFunc;
import com.chiorichan.util.SecureFunc;
import com.chiorichan.util.Versioning;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Implements loading sites from file
 */
public class Site implements AccountLocation
{
	private final File file;
	final YamlConfiguration yaml;
	private SQLDatastore datastore;

	private final String siteId;
	private String siteTitle;
	private final List<String> ips;

	/**
	 * Holds the enabled domains and subdomains
	 */
	final Map<String, Set<String>> domains = Maps.newHashMap();

	private SslContext defaultSslContext = null;

	private final List<String> cachePatterns = Lists.newArrayList();
	private SessionPersistenceMethod sessionPersistence = SessionPersistenceMethod.COOKIE;
	private final String encryptionKey;
	private final Routes routes = new Routes( this );

	private File directory;

	// Deprecated
	private final List<String> metatags = Lists.newCopyOnWriteArrayList();

	private final ScriptBinding binding = new ScriptBinding();
	private final ScriptingFactory factory = ScriptingFactory.create( binding );

	Site( File file, YamlConfiguration yaml ) throws ApplicationException
	{
		Validate.notNull( file );
		Validate.notNull( yaml );

		this.file = file;
		this.yaml = yaml;

		if ( !yaml.has( "site.id" ) )
			throw new SiteException( "Site id is missing!" );

		siteId = yaml.getString( "site.id" ).toLowerCase();
		siteTitle = yaml.getString( "site.title", AppConfig.get().getString( "framework.sites.defaultTitle", "Unnamed Site" ) );

		ips = yaml.getAsList( "site.listen", Lists.newArrayList() );

		for ( String ip : ips )
			if ( !NetworkFunc.isValidIPv4( ip ) && !NetworkFunc.isValidIPv6( ip ) )
				SiteManager.getLogger().warning( String.format( "The site '%s' is set to listen on ip '%s', but the ip does not match the valid IPv4 or IPv6 regex formula.", siteId, ip ) );

		List<String> listeningIps = NetworkManager.getListeningIps();

		if ( !listeningIps.containsAll( ips ) )
			SiteManager.getLogger().warning( String.format( "The site '%s' is set to listen on ips '%s', but the server is currently not on one or more of those ips. '%s'", siteId, Joiner.on( "," ).useForNull( "null" ).join( ips ), Joiner.on( "," ).useForNull( "null" ).join( listeningIps ) ) );

		if ( ips.contains( "localhost" ) )
			throw new SiteException( "Sites are not permitted to listen on hostname 'localhost', this hostname is reserved for the default site." );

		if ( yaml.has( "site.encryptionKey" ) )
			encryptionKey = yaml.getString( "site.encryptionKey" );
		else
		{
			encryptionKey = SecureFunc.randomize( "0x0000X" );
			yaml.set( "site.encryptionKey", encryptionKey );
		}

		if ( SiteManager.instance().getSiteById( siteId ) != null )
			throw new SiteException( String.format( "There already exists a site by the provided site id '%s'", siteId ) );

		DatastoreManager.getLogger().info( String.format( "Loading site '%s' with title '%s' from YAML file.", siteId, siteTitle ) );

		directory = SiteManager.checkSiteRoot( siteId );

		if ( !yaml.has( "site.web-allowed-origin" ) )
			yaml.set( "site.web-allowed-origin", "*" );

		// Load enabled domains and subdomains
		ConfigurationSection ds = yaml.getConfigurationSection( "site.domains", true );

		// TODO Make it so 'root' can be used as a subdomain and not just a mapping to the root of the domain

		for ( String key : ds.getKeys() )
		{
			ConfigurationSection subds = ds.getConfigurationSection( key );

			String domain = key.replace( "_", "." );

			SiteMapping.put( domain, this );

			domains.put( domain, Sets.newHashSet() );

			for ( String subdomain : subds.getKeys() )
				if ( !"root".equalsIgnoreCase( subdomain ) )
				{
					SiteManager.getLogger().info( String.format( "Initalized subdomain '%s' for site '%s'", subdomain, siteId ) );
					FileFunc.setDirectoryAccessWithException( getSubdomain( subdomain ).directory() );
					domains.get( domain ).add( subdomain );
				}
		}


		File ssl = directory( "ssl" );
		FileFunc.setDirectoryAccessWithException( ssl );

		String sslCertFile = yaml.getString( "site.sslCert" );
		String sslKeyFile = yaml.getString( "site.sslKey" );
		String sslSecret = yaml.getString( "site.sslSecret" );

		if ( sslCertFile != null && sslKeyFile != null )
		{
			File sslCert = new File( ssl.getAbsolutePath(), sslCertFile );
			File sslKey = new File( ssl.getAbsolutePath(), sslKeyFile );

			try
			{
				defaultSslContext = new CertificateWrapper( sslCert, sslKey, sslSecret ).context();
			}
			catch ( SSLException | FileNotFoundException | CertificateException e )
			{
				SiteManager.getLogger().severe( String.format( "Failed to load SslContext for site '%s' using cert '%s', key '%s', and hasSecret? %s", siteId, FileFunc.relPath( sslCert ), FileFunc.relPath( sslKey ), sslSecret != null && !sslSecret.isEmpty() ), e );
			}
		}


		try
		{
			if ( EventBus.instance().callEventWithException( new SiteLoadEvent( this ) ).isCancelled() )
				throw new SiteException( String.format( "Loading of site '%s' was cancelled by an internal event.", siteId ) );
		}
		catch ( EventException e )
		{
			throw new SiteException( e );
		}


		if ( yaml.has( "database" ) && yaml.isConfigurationSection( "database" ) )
			switch ( yaml.getString( "database.type", "sqlite" ).toLowerCase() )
			{
				case "sqlite":
				{
					datastore = new SQLiteDatastore( yaml.getString( "database.dbfile", yaml.getString( "database.filename", "server.db" ) ) );
					break;
				}
				case "mysql":
				{
					String host = yaml.getString( "database.host", "localhost" );
					String port = yaml.getString( "database.port", "3306" );
					String database = yaml.getString( "database.database", "chiorifw" );
					String username = yaml.getString( "database.username", "fwuser" );
					String password = yaml.getString( "database.password", "fwpass" );

					datastore = new MySQLDatastore( database, username, password, host, port );
					break;
				}
				case "h2":
				{
					datastore = new H2SQLDatastore( yaml.getString( "database.dbfile", yaml.getString( "database.filename", "server.db" ) ) );
					break;
				}
				case "none":
				case "":
					DatastoreManager.getLogger().warning( String.format( "The Database for site '%s' is unconfigured, some features maybe not function as expected. See config option 'database.type' in the site config and set the connection params.", siteId ) );
					break;
				default:
					DatastoreManager.getLogger().severe( String.format( "We are sorry, the datastore subsystem currently only supports mysql, sqlite, and h2 databases but we found '%s', please change 'database.type' to 'mysql', 'sqlite', or 'h2' in the site config and set the connection params", yaml.getString( "server.database.type", "sqlite" ).toLowerCase() ) );
			}

		if ( yaml.has( "sessions.persistenceMethod" ) )
			for ( SessionPersistenceMethod method : SessionPersistenceMethod.values() )
				if ( method.name().equalsIgnoreCase( yaml.getString( "sessions.persistenceMethod" ) ) )
					sessionPersistence = method;

		List<String> onLoadScripts = yaml.getStringList( "scripts.on-load" );

		if ( onLoadScripts != null )
			for ( String script : onLoadScripts )
			{
				ScriptingResult result = factory.eval( ScriptingContext.fromFile( this, script ).shell( "groovy" ).site( this ) );

				if ( result.hasExceptions() )
				{
					if ( result.hasException( FileNotFoundException.class ) )
						SiteManager.getLogger().severe( String.format( "Failed to eval onLoadScript '%s' for site '%s' because the file was not found.", script, siteId ) );
					else
					{
						SiteManager.getLogger().severe( String.format( "Exception caught while evaling onLoadScript '%s' for site '%s'", script, siteId ) );
						ExceptionReport.printExceptions( result.getExceptions() );
					}
				}
				else
					SiteManager.getLogger().info( String.format( "Finished evaling onLoadScript '%s' for site '%s' with result: %s", script, siteId, result.getString( true ) ) );
			}

		/**
		 * Warn the user that files can not be served from the `wisp`, a.k.a. Web Interface and Server Point, folder since the server uses it for internal requests.
		 */
		if ( getRootdomain().directory( "~wisp" ).exists() )
			SiteManager.getLogger().warning( String.format( "It would appear that site '%s' contains a subfolder by the name of '~wisp', since we use the uri '/~wisp' for internal access, you will be unable to serve files from this directory!", siteId ) );

		ConfigurationSection archive = yaml.getConfigurationSection( "archive", true );

		if ( !archive.has( "enable" ) )
			archive.set( "enable", false );

		if ( !archive.has( "interval" ) )
			archive.set( "interval", "24h" );

		if ( !archive.has( "keep" ) )
			archive.set( "keep", "3" );

		if ( !archive.has( "lastRun" ) )
			archive.set( "lastRun", "0" );

		if ( archive.getBoolean( "enable" ) )
		{
			String interval = archive.getString( "interval", "24h" ).trim();
			if ( interval.matches( "[0-9]+[dhmsDHMS]?" ) )
			{
				interval = interval.toLowerCase();
				int multiply = 1;

				if ( interval.endsWith( "d" ) || interval.endsWith( "h" ) || interval.endsWith( "m" ) || interval.endsWith( "s" ) )
				{
					switch ( interval.substring( interval.length() - 1 ) )
					{
						case "d":
							multiply = 1728000;
							break;
						case "h":
							multiply = 72000;
							break;
						case "m":
							multiply = 1200;
							break;
						case "s":
							multiply = 20;
							break;
					}
					interval = interval.substring( 0, interval.length() - 1 );
				}

				long timer = Long.parseLong( interval ) * multiply;
				long lastRun = Timings.epoch() - archive.getLong( "lastRun" );
				long nextRun = archive.getLong( "lastRun" ) < 1L ? 600L : lastRun > timer ? 600L : timer - lastRun;
				final Site site = this;

				SiteManager.getLogger().info( String.format( "%s%sScheduled site archive for %s {nextRun: %s, interval: %s}", EnumColor.AQUA, EnumColor.NEGATIVE, siteId, nextRun, timer ) );

				TaskManager.instance().scheduleSyncRepeatingTask( SiteManager.instance(), nextRun, timer, new Runnable()
				{
					@Override
					public void run()
					{
						Log l = SiteManager.getLogger();
						l.info( String.format( "%s%sRunning archive for site %s...", EnumColor.AQUA, EnumColor.NEGATIVE, siteId ) );

						SiteManager.cleanupBackups( siteId, ".zip", archive.getInt( "keep", 3 ) );
						archive.set( "lastRun", Timings.epoch() );

						File dir = AppConfig.get().getDirectory( "archive", "archive" );
						dir = new File( dir, siteId );
						dir.mkdirs();

						File zip = new File( dir, new SimpleDateFormat( "yyyy-MM-dd_HH-mm-ss" ).format( new Date() ) + "-" + siteId + ".zip" );

						try
						{
							FileFunc.zipDir( site.directory(), zip );
						}
						catch ( IOException e )
						{
							l.severe( String.format( "%s%sFailed archiving site %s to %s", EnumColor.RED, EnumColor.NEGATIVE, siteId, zip.getAbsolutePath() ), e );
							return;
						}

						l.info( String.format( "%s%sFinished archiving site %s to %s", EnumColor.AQUA, EnumColor.NEGATIVE, siteId, zip.getAbsolutePath() ) );
					}
				} );
			}
			else
				SiteManager.getLogger().warning( String.format( "Failed to initalize site backup for site %s, interval did not match regex '[0-9]+[dhmsDHMS]?'.", siteId ) );
		}
	}

	Site( String siteId )
	{
		this.siteId = siteId;

		file = null;
		yaml = new YamlConfiguration();
		encryptionKey = SecureFunc.randomize( "0x0000X" );
		ips = Lists.newArrayList();
		siteTitle = Versioning.getProduct();
		datastore = AppConfig.get().getDatabase();

		directory = SiteManager.checkSiteRoot( siteId );
	}

	public void addToCachePatterns( String pattern )
	{
		if ( !cachePatterns.contains( pattern.toLowerCase() ) )
			cachePatterns.add( pattern.toLowerCase() );
	}

	/**
	 * @return The site main directory
	 */
	public File directory()
	{
		return directory;
	}

	/**
	 * @param subdir
	 *             The subdirectory name
	 * @return The subdirectory of the site main directory
	 */
	public File directory( String subdir )
	{
		return new File( directory, subdir );
	}

	public File directoryPublic()
	{
		return directory( "public" );
	}

	public File directoryResource()
	{
		return directory( "resource" );
	}

	public File directoryTemp()
	{
		return AppConfig.get().getDirectoryCache( getId() );
	}

	public File directoryTemp( String append )
	{
		return AppConfig.get().getDirectoryCache( getId() + File.pathSeparator + append );
	}

	public ApacheConfiguration getApacheConfig()
	{
		return new ApacheConfiguration();
	}

	protected ScriptBinding getBinding()
	{
		return binding;
	}

	public List<String> getCachePatterns()
	{
		return cachePatterns;
	}

	public YamlConfiguration getConfig()
	{
		return yaml;
	}

	public SQLDatastore getDatastore()
	{
		return datastore;
	}

	public SslContext getDefaultSslContext()
	{
		return defaultSslContext;
	}

	public Map<String, Set<String>> getDomains()
	{
		return Collections.unmodifiableMap( domains );
	}

	/**
	 * Compiles a map of domains and subdomains with valid SslContext
	 * Main domain is under the subdomain root
	 *
	 * @return Map of domains and subdomains with valid SslContext
	 */
	public Map<String, Map<String, SslContext>> getDomainsWithSslContext()
	{
		return new HashMap<String, Map<String, SslContext>>()
		{
			{
				for ( Entry<String, Set<String>> e : domains.entrySet() )
					put( e.getKey(), new HashMap<String, SslContext>()
					{
						{
							SslContext sslRoot = getSslContext( e.getKey() );
							if ( sslRoot != null )
								put( "root", sslRoot );
							for ( String s : e.getValue() )
							{
								SslContext ssl = getSslContext( e.getKey(), s );
								if ( ssl != null )
									put( s, ssl );
							}
						}
					} );
			}
		};
	}

	public String getEncryptionKey()
	{
		return encryptionKey;
	}

	public ScriptingFactory getEvalFactory()
	{
		return factory;
	}

	public File getFile()
	{
		return file == null ? yaml.loadedFrom() == null ? null : new File( yaml.loadedFrom() ) : file;
	}

	public Object getGlobal( String key )
	{
		return binding.getVariable( key );
	}

	public Map<String, Object> getGlobals()
	{
		return binding.getVariables();
	}

	@Override
	public String getId()
	{
		return siteId;
	}

	public List<String> getIps()
	{
		return ips;
	}

	public String getLoginForm()
	{
		return getConfig().getString( "accounts.loginForm", "/wisp/login" );
	}

	public String getLoginPost()
	{
		return getConfig().getString( "accounts.loginPost", "/" );
	}

	@Deprecated
	public List<String> getMetatags()
	{
		if ( metatags == null )
			return new CopyOnWriteArrayList<String>();

		return metatags;
	}

	public SiteDomain getRootdomain() throws SiteException
	{
		return getSubdomain( "root" );
	}

	public Routes getRoutes()
	{
		return routes;
	}

	/**
	 * Gets the site configured Session Key from configuration.
	 *
	 * @return The Session Key
	 */
	public String getSessionKey()
	{
		String key = yaml.getString( "sessions.keyName" );
		if ( key == null )
			return SessionManager.getDefaultSessionName();
		return "_ws" + WordUtils.capitalize( key );
	}

	public SessionPersistenceMethod getSessionPersistenceMethod()
	{
		return sessionPersistence;
	}

	public SslContext getSslContext( String domain )
	{
		return getSslContext( domain, "root" );
	}

	public SslContext getSslContext( String domain, String subdomain )
	{
		Validate.notEmpty( domain );
		Validate.notEmpty( subdomain );

		File ssl = directory( "ssl" );
		FileFunc.setDirectoryAccessWithException( ssl );

		ConfigurationSection section = yaml.getConfigurationSection( "site.domains." + domain.replace( ".", "_" ) + "." + subdomain.replace( ".", "_" ), true );
		String sslCertFile = section.getString( "sslCert" );
		String sslKeyFile = section.getString( "sslKey" );
		String sslSecret = section.getString( "sslSecret" );

		try
		{
			if ( sslCertFile != null && sslKeyFile != null )
			{
				File sslCert = new File( ssl, sslCertFile );
				File sslKey = new File( ssl, sslKeyFile );

				return new CertificateWrapper( sslCert, sslKey, sslSecret ).context();
			}
		}
		catch ( SSLException | FileNotFoundException | CertificateException e )
		{
			SiteManager.getLogger().severe( String.format( "Failed to load SslContext for site '%s' and subdomain '%s' using cert '%s', key '%s', and hasSecret? %s", siteId, subdomain, sslCertFile, sslKeyFile, sslSecret != null && !sslSecret.isEmpty() ), e );
		}

		return null;
	}

	public SiteDomain getSubdomain( String subdomain )
	{
		if ( subdomain == null || subdomain.length() == 0 )
			subdomain = "root";
		subdomain = subdomain.toLowerCase();
		return new SiteDomain( this, subdomain );
	}

	public Set<String> getSubdomains( String domain )
	{
		if ( domains.containsKey( domain ) )
			return Collections.unmodifiableSet( domains.get( domain ) );
		else
			return Sets.newHashSet();
	}

	public String getTitle()
	{
		return siteTitle;
	}

	public boolean hasDefaultSslContext()
	{
		return defaultSslContext != null;
	}

	public File resourceFile( String file ) throws FileNotFoundException
	{
		Validate.notNull( file, "File can't be null" );

		if ( file.length() == 0 )
			throw new FileNotFoundException( "File can't be empty!" );

		File root = directoryResource();

		File packFile = new File( root, file );

		if ( packFile.exists() )
			return packFile;

		root = packFile.getParentFile();

		if ( root.exists() && root.isDirectory() )
		{
			File[] files = root.listFiles();
			Map<String, File> found = Maps.newLinkedHashMap();
			List<String> preferred = ScriptingContext.getPreferredExtensions();

			for ( File child : files )
				if ( child.getName().startsWith( packFile.getName() + "." ) )
					found.put( child.getName().substring( packFile.getName().length() + 1 ).toLowerCase(), child );

			if ( found.size() > 0 )
			{
				if ( preferred.size() > 0 )
					for ( String ext : preferred )
						if ( found.containsKey( ext.toLowerCase() ) )
							return found.get( ext.toLowerCase() );

				return found.values().toArray( new File[0] )[0];
			}
		}

		throw new FileNotFoundException( String.format( "Could not find the file '%s' file in site '%s' resource directory '%s'.", file, getId(), root.getAbsolutePath() ) );
	}

	public File resourcePackage( String pack ) throws FileNotFoundException
	{
		Validate.notNull( pack, "Package can't be null" );

		if ( pack.length() == 0 )
			throw new FileNotFoundException( "Package can't be empty!" );

		pack = pack.replace( ".", System.getProperty( "file.separator" ) );

		File root = directoryResource();

		File packFile = new File( root, pack );

		if ( packFile.exists() )
			return packFile;

		root = packFile.getParentFile();

		if ( root.exists() && root.isDirectory() )
		{
			File[] files = root.listFiles();
			Map<String, File> found = Maps.newLinkedHashMap();
			List<String> preferred = ScriptingContext.getPreferredExtensions();

			for ( File child : files )
				if ( child.getName().startsWith( packFile.getName() + "." ) )
					found.put( child.getName().substring( packFile.getName().length() + 1 ).toLowerCase(), child );

			if ( found.size() > 0 )
			{
				if ( preferred.size() > 0 )
					for ( String ext : preferred )
						if ( found.containsKey( ext.toLowerCase() ) )
							return found.get( ext.toLowerCase() );

				return found.values().toArray( new File[0] )[0];
			}
		}

		throw new FileNotFoundException( String.format( "Could not find the package '%s' file in site '%s'.", pack, getId() ) );
	}

	public void save() throws IOException
	{
		save( false );
	}

	public void save( boolean force ) throws IOException
	{
		File file = getFile();
		if ( file != null && ( file.exists() || force ) )
			yaml.save( file );
	}

	public void setGlobal( String key, Object val )
	{
		binding.setVariable( key, val );
	}

	public void setTitle( String title )
	{
		siteTitle = title;
		yaml.set( "site.title", title );
	}

	@Override
	public String toString()
	{
		return "Site{id=" + getId() + ",title=" + getTitle() + ",domains=" + Joiner.on( "," ).withKeyValueSeparator( "=" ).join( domains ) + "ips=" + Joiner.on( "," ).join( ips ) + ",siteDir=" + directory.getAbsolutePath() + "}";
	}

	public void unload()
	{
		// Do Nothing
	}
}
