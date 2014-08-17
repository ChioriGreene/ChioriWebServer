package com.chiorichan.framework

import com.chiorichan.ConsoleLogManager
import com.chiorichan.Loader
import com.chiorichan.account.bases.Account
import com.chiorichan.database.DatabaseEngine
import com.chiorichan.http.HttpCode
import com.chiorichan.http.HttpRequest
import com.chiorichan.http.HttpResponse
import com.chiorichan.http.PersistenceManager
import com.chiorichan.http.PersistentSession
import com.chiorichan.plugin.PluginManager
import com.chiorichan.util.Versioning

abstract class ScriptingBaseGroovy extends ScriptingBaseJava
{
	void var_dump ( Object obj )
	{
		println var_export( obj )
	}
	
	HttpRequest getRequest()
	{
		return request;
	}
	
	HttpResponse getResponse()
	{
		return response;
	}
	
	PersistentSession getSession()
	{
		return request.getSession();
	}
	
	void echo( String var )
	{
		println var
	}
	
	String getVersion()
	{
		return Versioning.getVersion();
	}
	
	String getProduct()
	{
		return Versioning.getProduct();
	}
	
	String getCopyright()
	{
		return Versioning.getCopyright();
	}
	
	Account getAccount()
	{
		return request.getSession().getCurrentAccount();
	}
	
	ConfigurationManagerWrapper getConfigurationManager()
	{
		return new ConfigurationManagerWrapper( request.getSession() );
	}
	
	HttpUtilsWrapper getHttpUtils()
	{
		return new HttpUtilsWrapper( request.getSession() );
	}
	
	DatabaseEngine getServerDatabase()
	{
		return Loader.getPersistenceManager().getDatabase();
	}
	
	DatabaseEngine getSiteDatabase()
	{
		return request.getSite().getDatabase();
	}
	
	Site getSite()
	{
		return getRequest().getSite();
	}
	
	String getStatusDescription( int errNo )
	{
		return HttpCode.msg( errNo );
	}
	
	PersistenceManager getPersistenceManager()
	{
		return Loader.getPersistenceManager();
	}
	
	String url_to()
	{
		return url_to( null );
	}
	
	String url_to( String subdomain )
	{
		String url = "http://";
		
		if ( subdomain != null && !subdomain.isEmpty() )
			url += subdomain + ".";
		
		if ( request.getSite() != null )
			url += request.getSite().getDomain() + "/";
		else
			url += Loader.getSiteManager().getFrameworkSite().getDomain() + "/";
		
		return url;
	}
	
	String url_to_login()
	{
		if ( request.getSite() == null )
			return "/login";
		
		return request.getSite().getYaml().getString( "scripts.login-form", "/login" );;
	}
	
	String url_to_logout()
	{
		return url_to_login + "?logout";
	}
}