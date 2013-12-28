package com.chiorichan.framework;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.event.EventException;
import com.chiorichan.event.server.RenderEvent;
import com.chiorichan.event.server.ServerVars;
import com.chiorichan.http.HttpRequest;
import com.chiorichan.http.HttpResponse;
import com.chiorichan.http.PersistenceManager;
import com.chiorichan.http.PersistentSession;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.util.Versioning;

public class Framework
{
	protected HttpRequest request;
	protected HttpResponse response;
	
	protected FrameworkServer _server;
	protected FrameworkConfigurationManager _config;
	protected FrameworkDatabaseEngine _db;
	protected FrameworkUserService _usr;
	protected FrameworkFunctions _fun;
	protected FrameworkImageUtils _img;
	
	protected Enviro env = null;
	protected ByteArrayOutputStream out = new ByteArrayOutputStream();
	protected boolean continueNormally = true;
	protected String alternateOutput = "An Unknown Error Has Risen!";
	protected int httpStatus = 200;
	
	protected Map<String, String> rewriteVars = new HashMap<String, String>();
	protected Map<ServerVars, Object> serverVars = new HashMap<ServerVars, Object>();
	
	public Evaling eval;
	
	protected String uid;
	
	public String getUid()
	{
		return uid;
	}
	
	public Framework(HttpRequest _request, HttpResponse _response)
	{
		request = _request;
		response = _response;
		
		uid = request.getSession().getId();
		
		env = new Enviro( this );
		loadVars();
	}
	
	public void loadVars()
	{
		try
		{
			// serverVars.put( ServerVars.PHP_SELF, requestFile.getPath() );
			serverVars.put( ServerVars.DOCUMENT_ROOT, Loader.getConfig().getString( "settings.webroot", "webroot" ) + request.getSite().getWebRoot( null ) );
			serverVars.put( ServerVars.HTTP_ACCEPT, request.getHeader( "Accept" ) );
			serverVars.put( ServerVars.HTTP_USER_AGENT, request.getUserAgent() );
			serverVars.put( ServerVars.HTTP_CONNECTION, request.getHeader( "Connection" ) );
			serverVars.put( ServerVars.HTTP_HOST, request.getLocalHost() );
			serverVars.put( ServerVars.HTTP_ACCEPT_ENCODING, request.getHeader( "Accept-Encoding" ) );
			serverVars.put( ServerVars.HTTP_ACCEPT_LANGUAGE, request.getHeader( "Accept-Language" ) );
			serverVars.put( ServerVars.REMOTE_HOST, request.getRemoteHost() );
			serverVars.put( ServerVars.REMOTE_ADDR, request.getRemoteAddr() );
			serverVars.put( ServerVars.REMOTE_PORT, request.getRemotePort() );
			serverVars.put( ServerVars.REQUEST_TIME, request.getRequestTime() );
			serverVars.put( ServerVars.REQUEST_URI, request.getURI() );
			serverVars.put( ServerVars.CONTENT_LENGTH, request.getContentLength() );
			serverVars.put( ServerVars.AUTH_TYPE, request.getAuthType() );
			serverVars.put( ServerVars.SERVER_NAME, request.getServerName() );
			serverVars.put( ServerVars.SERVER_PORT, request.getServerPort() );
			serverVars.put( ServerVars.HTTPS, request.isSecure() );
			serverVars.put( ServerVars.SESSION, request.getSession() );
			serverVars.put( ServerVars.SERVER_SOFTWARE, Versioning.getProduct() );
			serverVars.put( ServerVars.SERVER_ADMIN, Loader.getConfig().getString( "server.admin", "webmaster@" + request.getDomain() ) );
			serverVars.put( ServerVars.SERVER_SIGNATURE, Versioning.getProduct() + " Version " + Versioning.getVersion() );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
	}
	
	public String replaceAt( String par, int at, String rep )
	{
		StringBuilder sb = new StringBuilder( par );
		sb.setCharAt( at, rep.toCharArray()[0] );
		return sb.toString();
	}
	
	public boolean rewriteVirtual( String domain, String subdomain, String uri )
	{
		SqlConnector sql = Loader.getPersistenceManager().getSql();
		
		try
		{
			// TODO: Fix the select issue with blank subdomains. It's not suppose to be 1111 but it is to prevent the
			// redirect loop.
			ResultSet rs = sql.query( "SELECT * FROM `pages` WHERE (site = '" + subdomain + "' OR site = '1111') AND domain = '" + domain + "' UNION SELECT * FROM `pages` WHERE (site = '" + subdomain + "' OR site = '1111') AND domain = '';" );
			
			if ( sql.getRowCount( rs ) > 0 )
			{
				Map<String, HashMap<String, Object>> candy = new TreeMap<String, HashMap<String, Object>>();
				
				do
				{
					HashMap<String, Object> data = new HashMap<String, Object>();
					
					String prop = rs.getString( "page" );
					
					if ( prop.startsWith( "/" ) )
						prop = prop.substring( 1 );
					
					data.put( "page", prop );
					
					String[] props = prop.split( "[.//]" );
					String[] uris = uri.split( "[.//]" );
					
					String weight = StringUtils.repeat( "?", Math.max( props.length, uris.length ) );
					
					boolean whole_match = true;
					for ( int i = 0; i < Math.max( props.length, uris.length ); i++ )
					{
						try
						{
							Loader.getLogger().fine( prop + " --> " + props[i] + " == " + uris[i] );
							
							if ( props[i].matches( "\\[([a-zA-Z0-9]+)=\\]" ) )
							{
								weight = replaceAt( weight, i, "Z" );
								
								String key = props[i].replaceAll( "[\\[\\]=]", "" );
								String value = uris[i];
								
								rewriteVars.put( key, value );
								
								// PREP MATCH
								
								Loader.getLogger().fine( "Found a PREG match to " + rs.getString( "page" ) );
							}
							else if ( props[i].equals( uris[i] ) )
							{
								weight = replaceAt( weight, i, "A" );
								
								Loader.getLogger().fine( "Found a match to " + rs.getString( "page" ) );
								// MATCH
							}
							else
							{
								whole_match = false;
								Loader.getLogger().fine( "Found no match to " + rs.getString( "page" ) );
								break;
								// NO MATCH
							}
						}
						catch ( ArrayIndexOutOfBoundsException e )
						{
							whole_match = false;
							break;
						}
						catch ( Exception e )
						{
							e.printStackTrace();
							whole_match = false;
							break;
						}
					}
					
					if ( whole_match )
					{
						data.put( "site", rs.getString( "site" ) );
						data.put( "domain", rs.getString( "domain" ) );
						data.put( "page", rs.getString( "page" ) );
						data.put( "title", rs.getString( "title" ) );
						data.put( "reqlevel", rs.getString( "reqlevel" ) );
						data.put( "theme", rs.getString( "theme" ) );
						data.put( "view", rs.getString( "view" ) );
						data.put( "html", rs.getString( "html" ) );
						data.put( "file", rs.getString( "file" ) );
						
						candy.put( weight, data );
					}
				}
				while ( rs.next() );
				
				if ( candy.size() > 0 )
				{
					@SuppressWarnings( "unchecked" )
					HashMap<String, Object> data = (HashMap<String, Object>) candy.values().toArray()[0];
					
					Loader.getLogger().info( "Rewriting page request to " + data );
					
					try
					{
						loadPageInternal( (String) data.get( "theme" ), (String) data.get( "view" ), (String) data.get( "title" ), (String) data.get( "file" ), (String) data.get( "html" ), (String) data.get( "reqlevel" ) );
					}
					catch ( IOException e )
					{
						e.printStackTrace();
					}
					
					return true;
				}
				
				Loader.getLogger().fine( "Failed to find a page redirect for Framework Rewrite... '" + subdomain + "." + domain + "' '" + uri + "'" );
				return false;
			}
			else
			{
				Loader.getLogger().fine( "Failed to find a page redirect for Framework Rewrite... '" + subdomain + "." + domain + "' '" + uri + "'" );
				return false;
			}
		}
		catch ( SQLException e )
		{
			e.printStackTrace(); // TODO: Better this catch
		}
		
		return true;
	}
	
	public void loadPageInternal( String theme, String view, String title, String file, String html, String reqPerm ) throws IOException
	{
		try
		{
			Site currentSite = request.getSite();
			
			if ( currentSite == null )
				return;
			
			if ( html.isEmpty() && file.isEmpty() )
			{
				response.sendError( 500, "There was a problem handling your request. Try again later." );
				return;
			}
			
			File requestFile = null;
			if ( !file.isEmpty() )
			{
				if ( file.startsWith( "/" ) )
					file = file.substring( 1 );
				
				if ( currentSite.protectCheck( file ) )
				{
					Loader.getLogger().warning( "Loading of page '" + file + "' is not allowed since its hard protected in the site configs." );
					response.sendError( 401, "Loading of this page is not allowed since its hard protected in the site configs." );
					return;
				}
				
				if ( Loader.webroot.isEmpty() )
					Loader.webroot = "webroot";
				
				File siteRoot = new File( Loader.webroot, currentSite.siteId );
				
				if ( !siteRoot.exists() )
					siteRoot.mkdirs();
				
				requestFile = new File( siteRoot, file );
				
				Loader.getLogger().info( "Requesting file: " + requestFile.getAbsolutePath() );
				
				if ( !requestFile.exists() )
				{
					Loader.getLogger().warning( "Could not load file '" + requestFile.getAbsolutePath() + "'" );
					response.sendError( 404 );
					return;
				}
				
				if ( requestFile.isDirectory() )
				{
					if ( new File( requestFile, "index.groovy" ).exists() )
					{
						requestFile = new File( requestFile, "index.groovy" );
					}
					else if ( new File( requestFile, "index.html" ).exists() )
					{
						requestFile = new File( requestFile, "index.html" );
					}
					else
					{
						response.sendError( 500, "There was a problem finding the index page." );
						return;
					}
				}
				
				if ( !requestFile.exists() )
				{
					Loader.getLogger().warning( "Could not load file '" + requestFile.getAbsolutePath() + "'" );
					response.sendError( HttpServletResponse.SC_NOT_FOUND );
					return;
				}
			}
			
			eval = env.newEval();
			
			serverVars.put( ServerVars.DOCUMENT_ROOT, new File( Loader.getConfig().getString( "settings.webroot", "webroot" ), currentSite.getWebRoot( currentSite.getSubDomain() ) ).getAbsolutePath() );
			
			Map<String, Object> $server = new HashMap<String, Object>();
			
			for ( Entry<ServerVars, Object> en : serverVars.entrySet() )
			{
				$server.put( en.getKey().getName().toLowerCase(), en.getValue() );
			}
			
			env.set( "_SERVER", $server );
			env.set( "_REQUEST", request.getRequestMap() );
			env.set( "_POST", request.getPostMap() );
			env.set( "_GET", request.getGetMap() );
			env.set( "_REWRITE", rewriteVars );
			
			if ( getUserService().initalize( reqPerm ) )
			{
				if ( !html.isEmpty() )
					eval.evalCode( html );
				
				if ( requestFile != null )
					eval.evalFile( requestFile );
			}
			
			String source = eval.reset();
			
			/*
			String source;
			if ( continueNormally )
			{
				source = eval.reset();
			}
			else
			{
				currentSite = Loader.getPersistenceManager().getSiteManager().getSiteById( "framework" );
				
				if ( currentSite instanceof FrameworkSite )
					( (FrameworkSite) currentSite ).setDatabase( Loader.getPersistenceManager().getSql() );
				
				source = alternateOutput;
				theme = "com.chiorichan.themes.error";
				view = "";
			}
			*/
			
			RenderEvent event = new RenderEvent( this, source );
			
			event.theme = theme;
			event.view = view;
			event.title = title;
			
			try
			{
				Loader.getPluginManager().callEventWithException( event );
				
				if ( event.sourceChanged() )
					source = event.getSource();
				
				response.getOutput().write( source.getBytes() );
			}
			catch ( EventException ex )
			{
				ex.printStackTrace();
				response.getOutput().write( errorPage( ex.getCause() ).getBytes() );
			}
		}
		catch ( Throwable e )
		{
			e.printStackTrace();
			response.getOutput().write( errorPage( e ).getBytes() );
		}
	}
	
	public String getRequestVar( String key )
	{
		return request.getParameter( key );
	}
	
	public String getRewriteVar( String key )
	{
		return getRewriteVar( key, null );
	}
	
	public String getRewriteVar( String key, String def )
	{
		if ( rewriteVars.containsKey( key ) )
			return rewriteVars.get( key );
		
		return def;
	}
	
	public static String escapeHTML( String l )
	{
		return StringUtils.replaceEach( l, new String[] { "&", "\"", "<", ">" }, new String[] { "&amp;", "&quot;", "&lt;", "&gt;" } );
	}
	
	public String codeSampleEval( String code, int line )
	{
		return codeSampleEval( code, line, -1 );
	}
	
	public String codeSampleEval( String code, int line, int col )
	{
		try
		{
			StringBuilder sb = new StringBuilder();
			
			if ( code.isEmpty() )
				return "";
			
			int cLine = 0;
			for ( String l : code.split( "\n" ) )
			{
				cLine++;
				
				if ( cLine > line - 5 && cLine < line + 5 )
				{
					if ( cLine == line )
					{
						if ( l.length() >= col )
						{
							if ( col > -1 )
							{
								if ( col - 1 > -1 )
									col--;
								
								l = escapeHTML( l.substring( 0, col ) ) + "<span style=\"background-color: red; font-weight: bolder;\">" + l.substring( col, col + 1 ) + "</span>" + escapeHTML( l.substring( col + 1 ) );
							}
							else
								l = escapeHTML( l );
							
							sb.append( "<span class=\"error\"><span class=\"ln error-ln\">" + cLine + "</span> " + l + "</span>" );
						}
						else
							sb.append( "<span class=\"ln\">" + cLine + "</span> " + escapeHTML( l ) + "\n" ); // XXX: Fix It.
																																			// Why does
																																			// this happen?
					}
					else
					{
						sb.append( "<span class=\"ln\">" + cLine + "</span> " + escapeHTML( l ) + "\n" );
					}
				}
			}
			
			return sb.toString().substring( 0, sb.toString().length() - 1 );
		}
		catch ( Exception e )
		{
			return "Am exception was thrown while preparing code preview: " + e.getMessage();
		}
	}
	
	public String codeSample( String file, int line )
	{
		return codeSample( file, line, -1 );
	}
	
	public String codeSample( String file, int line, int col )
	{
		if ( !file.isEmpty() )
		{
			FileInputStream is;
			try
			{
				is = new FileInputStream( file );
			}
			catch ( FileNotFoundException e )
			{
				return e.getMessage();
			}
			
			StringBuilder sb = new StringBuilder();
			try
			{
				BufferedReader br = new BufferedReader( new InputStreamReader( is, "ISO-8859-1" ) );
				
				int cLine = 0;
				String l;
				while ( ( l = br.readLine() ) != null )
				{
					l = escapeHTML( l ) + "\n";
					
					cLine++;
					
					if ( cLine > line - 5 && cLine < line + 5 )
					{
						if ( cLine == line )
						{
							sb.append( "<span class=\"error\"><span class=\"ln error-ln\">" + cLine + "</span> " + l + "</span>" );
						}
						else
						{
							sb.append( "<span class=\"ln\">" + cLine + "</span> " + l );
						}
					}
				}
				
				is.close();
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
			
			return sb.toString().substring( 0, sb.toString().length() - 1 );
		}
		return "";
	}
	
	public String javaException( StackTraceElement[] ste )
	{
		if ( ste == null || ste.length < 1 )
			return "";
		
		int l = 0;
		
		StringBuilder sb = new StringBuilder();
		
		for ( StackTraceElement e : ste )
		{
			String file = ( e.getFileName() == null ) ? "eval()" : e.getFileName() + "(" + e.getLineNumber() + ")";
			
			sb.append( "<tr class=\"trace " + ( ( e.getClassName().startsWith( "com.chiori" ) || e.getClassName().startsWith( "org.eclipse" ) || e.getClassName().startsWith( "java" ) ) ? "core" : "app" ) + " collapsed\">\n" );
			sb.append( "	<td class=\"number\">#" + l + "</td>\n" );
			sb.append( "	<td class=\"content\">\n" );
			sb.append( "		<div class=\"trace-file\">\n" );
			sb.append( "			<div class=\"plus\">+</div>\n" );
			sb.append( "			<div class=\"minus\">–</div>\n" );
			sb.append( "			" + file + ": <strong>" + e.getClassName() + "." + e.getMethodName() + "</strong>\n" );
			sb.append( "		</div>\n" );
			sb.append( "		<div class=\"code\">\n" );
			sb.append( "			<pre>Sorry, Code previews are not currently available.</pre>\n" );
			// sb.append( "			<pre><? codeSamp($trc["file"], $trc["line"]); ?></pre>\n" );
			sb.append( "		</div>\n" );
			sb.append( "	</td>\n" );
			sb.append( "</tr>\n" );
			
			l++;
		}
		
		return sb.toString();
	}
	
	public String errorPage( Throwable t )
	{
		// TODO: Make this whole thing better. Optional fancy error pages.
		
		if ( !response.isCommitted() )
		{
			Site currentSite = Loader.getPersistenceManager().getSiteManager().getSiteById( "framework" );
			
			if ( currentSite instanceof FrameworkSite )
				( (FrameworkSite) currentSite ).setDatabase( Loader.getPersistenceManager().getSql() );
			
			try
			{
				env.set( "stackTrace", t );
				env.set( "codeSample", "Sorry, No code preview is available at this time." );
				
				if ( t instanceof CodeParsingException )
				{
					if ( ( (CodeParsingException) t ).getLineNumber() > -1 )
						env.set( "codeSample", codeSampleEval( ( (CodeParsingException) t ).getSourceCode(), ( (CodeParsingException) t ).getLineNumber(), ( (CodeParsingException) t ).getColumnNumber() ) );
				}
				
				env.set( "stackTraceHTML", javaException( t.getStackTrace() ) );
				
				// String page = ? "/panic.php" : "/notfound.php";
				return loadPage( "com.chiorichan.themes.error", "", "Critical Server Exception", "/panic.php", "", "-1" );
			}
			catch ( Exception e1 )
			{
				e1.printStackTrace();
				response.sendError( 500, "Critical Server Exception while loading the Framework Fancy Error Page: " + e1.getMessage() );
			}
		}
		
		return t.getMessage();
	}
	
	public String loadPage( String theme, String view, String title, String file, String html, String reqPerm ) throws IOException, CodeParsingException
	{
		if ( html == null )
			html = "";
		
		if ( file == null )
			file = "";
		
		// TODO: Check reqlevel
		
		String source = html;
		Site currentSite = request.getSite();
		
		if ( !file.isEmpty() )
		{
			if ( file.startsWith( "/" ) )
				file = file.substring( 1 );
			
			if ( currentSite.protectCheck( file ) )
				throw new IOException( "Loading of this page is not allowed since its hard protected in the site configs." );
			
			if ( Loader.webroot.isEmpty() )
				Loader.webroot = "webroot";
			
			File siteRoot = new File( Loader.webroot, currentSite.siteId );
			
			if ( !siteRoot.exists() )
				siteRoot.mkdirs();
			
			File requestFile = new File( siteRoot, file );
			
			Loader.getLogger().info( "Requesting file: " + requestFile.getAbsolutePath() );
			
			if ( !requestFile.exists() )
				throw new IOException( "Could not load file '" + requestFile.getAbsolutePath() + "'" );
			
			if ( requestFile.isDirectory() )
				if ( new File( requestFile, "index.php" ).exists() )
					requestFile = new File( requestFile, "index.php" );
				else if ( new File( requestFile, "index.html" ).exists() )
					requestFile = new File( requestFile, "index.html" );
				else
					throw new IOException( "There was a problem finding the index page." );
			
			if ( !requestFile.exists() )
			{
				throw new IOException( "Could not load file '" + requestFile.getAbsolutePath() + "'" );
			}
			
			source = getFileContents( requestFile.getAbsolutePath() );
			source = getServer().executeCode( source );
		}
		
		RenderEvent event = new RenderEvent( this, source );
		
		event.theme = theme;
		event.view = view;
		event.title = title;
		
		Loader.getPluginManager().callEvent( event );
		
		if ( event.sourceChanged() )
			source = event.getSource();
		
		return source;
	}
	
	private String getFileContents( String path )
	{
		FileInputStream is;
		try
		{
			is = new FileInputStream( path );
		}
		catch ( FileNotFoundException e )
		{
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		
		try
		{
			BufferedReader br = new BufferedReader( new InputStreamReader( is, "ISO-8859-1" ) );
			
			String l;
			while ( ( l = br.readLine() ) != null )
			{
				sb.append( l );
				sb.append( '\n' );
			}
			
			is.close();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		
		return sb.toString();
	}
	
	public FrameworkServer getServer()
	{
		if ( _server == null )
			_server = new FrameworkServer( this );
		
		return _server;
	}
	
	public PluginManager getPluginManager()
	{
		return Loader.getPluginManager();
	}
	
	public FrameworkConfigurationManager getConfigurationManager()
	{
		if ( _config == null )
			_config = new FrameworkConfigurationManager( this );
		
		return _config;
	}
	
	public FrameworkDatabaseEngine getDatabaseEngine()
	{
		if ( _db == null )
			_db = new FrameworkDatabaseEngine( this );
		
		return _db;
	}
	
	public FrameworkUserService getUserService()
	{
		if ( _usr == null )
			_usr = new FrameworkUserService( this );
		
		return _usr;
	}
	
	public FrameworkFunctions getFunctions()
	{
		if ( _fun == null )
			_fun = new FrameworkFunctions( this );
		
		return _fun;
	}
	
	public FrameworkImageUtils getImageUtils()
	{
		if ( _img == null )
			_img = new FrameworkImageUtils( this );
		
		return _img;
	}
	
	public HttpResponse getResponse()
	{
		return response;
	}
	
	public HttpRequest getRequest()
	{
		return request;
	}
	
	public String getProduct()
	{
		return "Chiori Web Server (implementing Chiori Framework API)";
	}
	
	public String getVersion()
	{
		return Loader.getVersion();
	}
	
	public Enviro getEnv()
	{
		return env;
	}
	
	public ByteArrayOutputStream getOutputStream()
	{
		return out;
	}
	
	public static SqlConnector getDatabase()
	{
		return Loader.getPersistenceManager().getSql();
	}
	
	public PersistenceManager getPersistenceManager()
	{
		return Loader.getPersistenceManager();
	}
	
	public void generateError( String errStr )
	{
		generateError( 500, errStr );
	}
	
	public void generateError( Throwable t )
	{
		StringBuilder sb = new StringBuilder();
		
		for ( StackTraceElement s : t.getStackTrace() )
		{
			sb.append( s + "\n" );
		}
		
		env.set( "stackTrace", sb.toString() );
		
		Loader.getLogger().warning( t.getMessage() );
		
		generateError( 500, t.getMessage() );
	}
	
	public void setStatus( int status )
	{
		response.setStatus( status );
	}
	
	public void generateError( int errNo, String reason )
	{
		Loader.getLogger().warning( "Generating Error (" + errNo + "): " + reason );
		
		continueNormally = false;
		
		StringBuilder op = new StringBuilder();
		
		response.setStatus( errNo );
		
		op.append( "<h1>" + getServer().getStatusDescription( errNo ) + "</h1>\n" );
		op.append( "<p class=\"warning show\">" + reason + "</p>\n" );
		
		alternateOutput = op.toString();
		httpStatus = errNo;
	}
	
	public Map<ServerVars, Object> getServerVars()
	{
		return serverVars;
	}
	
	public PersistentSession getSession()
	{
		return request.getSession();
	}
	
	public void setRequest( HttpRequest var1 )
	{
		request = var1;
	}
	
	public void setResponse( HttpResponse var1 )
	{
		response = var1;
	}
}