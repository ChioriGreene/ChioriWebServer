package com.chiorichan.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.caucho.quercus.parser.QuercusParseException;
import com.chiorichan.Main;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.event.server.RenderEvent;
import com.chiorichan.event.server.RequestEvent;
import com.chiorichan.framework.Site;
import com.chiorichan.plugin.java.JavaPlugin;

public class Template extends JavaPlugin implements Listener
{
	String pageTitleOverride;
	String docType = "html";
	String pageMark = "<!-- PAGE DATA -->";
	
	public void onEnable()
	{
		Main.getPluginManager().registerEvents( this, this );
		
		// Main.getServer().registerBean( null, "framework" );
	}
	
	public void onDisable()
	{
		
	}
	
	@EventHandler( priority = EventPriority.HIGHEST )
	public void onRequestEvent( RequestEvent event )
	{
		// event.setStatus( 418, "I'm a teapot!" );
		// event.setCancelled( true );
	}
	
	public File getTemplateRoot( Site site )
	{
		File templateRoot = site.getAbsoluteRoot( null );
		templateRoot = new File( templateRoot.getAbsolutePath() + ".template" );
		
		if ( templateRoot.isFile() )
			templateRoot.delete();
		
		if ( !templateRoot.exists() )
			templateRoot.mkdirs();
		
		return templateRoot;
	}
	
	public String getPackageSource( File root, String pack )
	{
		if ( pack == null || pack.isEmpty() )
			return "";
		
		pack = pack.replace( ".", System.getProperty( "file.separator" ) );
		
		File file = new File( root, pack + ".php" );
		
		if ( !file.exists() )
			file = new File( root, pack + ".inc.php" );
		
		if ( !file.exists() )
			file = new File( root, pack );
		
		if ( !file.exists() )
		{
			Main.getLogger().info( "Could not find the file " + file.getAbsolutePath() );
			return "";
		}
		
		Main.getLogger().info( "Retriving File: " + file.getAbsolutePath() );
		
		FileInputStream is;
		try
		{
			is = new FileInputStream( file );
		}
		catch ( FileNotFoundException e )
		{
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		
		try
		{
			BufferedReader br = new BufferedReader( new InputStreamReader( is, "UTF-8" ) );
			
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
	
	public String applyAlias( String source, Map<String, String> aliases )
	{
		for ( Entry<String, String> entry : aliases.entrySet() )
		{
			source = source.replace( "%" + entry.getKey() + "%", entry.getValue() );
		}
		
		return source;
	}
	
	public String doInclude( File root, String pack, RenderEvent event )
	{
		String source = getPackageSource( root, pack );
		
		try
		{
			source = event.executeCode( source );
		}
		catch ( QuercusParseException | IOException e )
		{
			// TODO: Better this catch
			e.printStackTrace();
		}
		
		return applyAlias( source, event.getSite().getAliases() );
	}
	
	@EventHandler( priority = EventPriority.LOWEST )
	public void onPreRenderEvent( RenderEvent event )
	{
		Site site = event.getSite();
		String theme = event.theme;
		String view = event.view;
		String title = event.title;
		
		File root = getTemplateRoot( site );
		
		if ( theme.isEmpty() )
			theme = "com.chiorichan.themes.default";
		
		StringBuilder ob = new StringBuilder();
		
		ob.append( "<!DOCTYPE " + docType + ">\n" );
		ob.append( "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" );
		ob.append( "<head>\n" );
		ob.append( "<meta charset=\"utf-8\">\n" );
		
		if ( pageTitleOverride != null )
			title = pageTitleOverride;
		
		if ( site.title == null )
			site.title = Main.getConfig().getString( "framework.sites.defaultTitle", "Unnamed Chiori Framework Site" );
		
		if ( title == null || title.isEmpty() )
			ob.append( "<title>" + site.title + "</title>\n" );
		else
			ob.append( "<title>" + title + " - " + site.title + "</title>\n" );
		
		for ( String tag : site.getMetatags() )
			ob.append( tag + "\n" );
		
		// Allow pages to disable the inclusion of common header
		ob.append( doInclude( root, domainToPackage( site.domain ) + ".includes.common", event ) + "\n" );
		ob.append( doInclude( root, domainToPackage( site.domain ) + ".includes." + getPackageName( theme ), event ) + "\n" );
		
		ob.append( "</head>\n" );
		ob.append( "<body>\n" );
		
		String pageData = ( theme.isEmpty() ) ? pageMark : doInclude( root, theme, event );
		String viewData = ( view.isEmpty() ) ? pageMark : doInclude( root, view, event );
		
		if ( pageData.indexOf( pageMark ) < 0 )
			pageData = pageData + viewData;
		else
			pageData = pageData.replace( pageMark, viewData );
		
		if ( pageData.indexOf( pageMark ) < 0 )
			pageData = pageData + event.getSource();
		else
			pageData = pageData.replace( pageMark, event.getSource() );
		
		ob.append( pageData + "\n" );
		
		ob.append( "</body>\n" );
		ob.append( "</html>\n" );
		
		event.setSource( ob.toString() );
	}
	
	// This is going to cause trouble *sigh*
	public void setTitleOverride( String title )
	{
		pageTitleOverride = title;
	}
	
	public String getPackageParent( String pack )
	{
		if ( pack.indexOf( "." ) < 0 )
			return pack;
		
		String[] packs = pack.split( "\\.(?=[^.]*$)" );
		
		return packs[0];
	}
	
	public String getPackageName( String pack )
	{
		if ( pack.indexOf( "." ) < 0 )
			return pack;
		
		String[] packs = pack.split( "\\.(?=[^.]*$)" );
		
		return packs[1];
	}
	
	public String domainToPackage( String domain )
	{
		if ( domain == null || domain.isEmpty() )
			return "";
		
		String[] packs = domain.split( "\\." );
		
		List<String> lst = Arrays.asList( packs );
		Collections.reverse( lst );
		
		String pack = "";
		for ( String s : lst )
		{	
			pack += "." + s;
		}
		
		return pack.substring( 1 );
	}
}