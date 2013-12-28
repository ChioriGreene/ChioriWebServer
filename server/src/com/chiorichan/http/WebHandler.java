package com.chiorichan.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLConnection;

import org.apache.poi.util.IOUtils;

import com.chiorichan.Loader;
import com.chiorichan.event.server.RequestEvent;
import com.chiorichan.framework.Framework;
import com.chiorichan.framework.Site;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class WebHandler implements HttpHandler
{
	public void handle( HttpExchange t ) throws IOException
	{
		try
		{
			HttpRequest request = new HttpRequest( t );
			
			String uri = request.getURI();
			String domain = request.getDomain();
			String site = "";
			
			if ( uri.startsWith( "/" ) )
				uri = uri.substring( 1 );
			
			if ( domain.equalsIgnoreCase( "localhost" ) || domain.equalsIgnoreCase( "127.0.0.1" ) | domain.equalsIgnoreCase( request.getLocalAddr() ) )
				domain = "";
			
			if ( domain.split( "\\." ).length > 2 )
			{
				String[] var1 = domain.split( "\\.", 2 );
				site = var1[0];
				domain = var1[1];
			}
			
			Site currentSite = Loader.getPersistenceManager().getSiteManager().getSiteByDomain( domain );
			
			if ( currentSite == null )
				currentSite = new Site( "default", Loader.getConfig().getString( "framework.sites.defaultTitle", "Unnamed Chiori Framework Site" ), domain );
			
			currentSite.setSubDomain( site );
			request.setSite( currentSite );
			
			request.initSession();
			
			HttpResponse response = request.getResponse();
			
			RequestEvent event = new RequestEvent( request );
			
			Loader.getPluginManager().callEvent( event );
			
			if ( event.isCancelled() )
			{
				Loader.getLogger().warning( "Navigation was cancelled by a plugin" );
				
				int status = event.getStatus();
				String reason = event.getReason();
				
				if ( status < 400 && status > 599 )
				{
					status = 502;
					reason = "Navigation Cancelled by Internal Event";
				}
				
				response.sendError( status, reason );
				return;
			}
			
			// Handle page output from here
			Framework fw = request.getFramework();
			
			if ( !fw.rewriteVirtual( domain, site, uri ) )
			{
				File siteRoot = new File( Loader.webroot, currentSite.getWebRoot( site ) );
				
				if ( !siteRoot.exists() )
					siteRoot.mkdirs();
				
				if ( uri.isEmpty() )
					uri = "/";
				
				File dest = new File( siteRoot, uri );
				
				if ( uri.endsWith( ".groovy" ) || uri.endsWith( ".chi" ) || ( dest.isDirectory() && new File( dest, "index.groovy" ).exists() ) || ( dest.isDirectory() && new File( dest, "index.chi" ).exists() ) )
				{
					Loader.getLogger().info( "Requesting uri (" + currentSite.siteId + ") '" + uri + "'" );
					
					fw.loadPageInternal( "", "", "", uri, "", "-1" );
				}
				else
				{
					Loader.getLogger().info( "Requesting uri (" + currentSite.siteId + ") '" + uri + " from " + dest.getAbsolutePath() + "'" );
					
					if ( dest.isDirectory() )
					{
						if ( new File( dest, "index.html" ).exists() )
							uri = uri + "/index.html";
						else if ( new File( dest, "index.htm" ).exists() )
							uri = uri + "/index.htm";
						else if ( new File( dest, "index.groovy" ).exists() )
							uri = uri + "/index.groovy";
						else if ( new File( dest, "index.chi" ).exists() )
							uri = uri + "/index.chi";
						else if ( Loader.getConfig().getBoolean( "server.allowDirectoryListing" ) )
						{
							// TODO: Implement Directory Listings
							
							response.sendError( 403, "Sorry, Directory Listing has not been implemented on this Server!" );
							return;
						}
						else
						{
							response.sendError( 403, "Directory Listing is Denied on this Server!" );
							return;
						}
					}
					
					dest = new File( siteRoot, uri );
					
					String target = dest.getAbsolutePath();
					
					FileInputStream is;
					try
					{
						is = new FileInputStream( target );
					}
					catch ( FileNotFoundException e )
					{
						response.sendError( 404 );
						return;
					}
					
					Loader.getLogger().fine( "Detected file to be of " + ContentTypes.getContentType( dest ) + " type." );
					response.setContentType( ContentTypes.getContentType( dest ) );
					
					try
					{
						ByteArrayOutputStream buffer = response.getOutput();
						
						int nRead;
						byte[] data = new byte[16384];
						
						while ( ( nRead = is.read( data, 0, data.length ) ) != -1 )
						{
							buffer.write( data, 0, nRead );
						}
						
						buffer.flush();
						
						is.close();
					}
					catch ( IOException e )
					{
						e.printStackTrace();
						response.sendError( 500, e.getMessage() );
						return;
					}
				}
			}
			
			response.sendResponse();
		}
		catch ( Exception e )
		{
			if ( e.getMessage().equals( "Broken pipe" ) )
				Loader.getLogger().warning( "Broken Pipe: The browser closed the connection before data could be written to it.", e );
			else
				e.printStackTrace();
		}
	}
}