package com.chiorichan.factory.parsers;

import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.Loader;
import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.factory.HTMLCommentParser;
import com.chiorichan.framework.Site;

public class LinksParser extends HTMLCommentParser
{
	Site site;
	
	public LinksParser()
	{
		super( "url_to" );
	}
	
	public String runParser( String source, Site _site ) throws ShellExecuteException
	{
		site = _site;
		Map<String, String> aliases = site.getAliases();
		
		if ( source.isEmpty() )
			return "";
		
		if ( aliases == null || aliases.size() < 1 )
			return source;
		
		for ( Entry<String, String> entry : aliases.entrySet() )
		{
			source = source.replace( "%" + entry.getKey() + "%", entry.getValue() );
		}
		
		return runParser( source );
	}
	
	/**
	 * args: [] = http://example.com/
	 * args: [subdomain] = http://subdomain.example.com/
	 * TODO: Expand this function.
	 */
	@Override
	public String resolveMethod( String... args ) throws ShellExecuteException
	{
		String url = "http://";
		
		if ( args.length >= 1 && !args[0].isEmpty() )
			url += args[0] + ".";
		
		if ( site != null )
			url += site.getDomain() + "/";
		else
			url += Loader.getSiteManager().getFrameworkSite().getDomain() + "/";
		
		return url;
	}
	
}
