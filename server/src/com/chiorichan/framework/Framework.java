package com.chiorichan.framework;

import com.chiorichan.ConsoleLogManager;
import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.http.HttpCode;
import com.chiorichan.http.HttpRequest;
import com.chiorichan.http.HttpResponse;
import com.chiorichan.http.PersistenceManager;
import com.chiorichan.http.PersistentSession;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.util.Versioning;

public class Framework
{
	protected final PersistentSession sess;
	
	public Framework(PersistentSession _sess)
	{
		sess = _sess;
	}
	
	public AccountServiceWrapper getAccountManager()
	{
		return new AccountServiceWrapper( sess.getCurrentAccount() );
	}
	
	public ConfigurationManagerWrapper getConfigurationManager()
	{
		return new ConfigurationManagerWrapper( sess );
	}
	
	public HttpUtilsWrapper getHttpUtils()
	{
		return new HttpUtilsWrapper( sess );
	}
	
	public DatabaseEngine getServerDatabase()
	{
		return new DatabaseEngine( Loader.getPersistenceManager().getDatabase() );
	}
	
	public DatabaseEngine getSiteDatabase()
	{
		return new DatabaseEngine( sess.getRequest().getSite().getDatabase() );
	}
	
	public PersistentSession getSession()
	{
		return sess;
	}
	
	public HttpRequest getRequest()
	{
		return sess.getRequest();
	}
	
	public HttpResponse getResponse()
	{
		return sess.getResponse();
	}
	
	public Site getSite()
	{
		return sess.getRequest().getSite();
	}
	
	public String getProduct()
	{
		return Versioning.getProduct();
	}
	
	public String getVersion()
	{
		return Versioning.getVersion();
	}
	
	public String getCopyright()
	{
		return Versioning.getCopyright();
	}
	
	public PluginManager getPluginManager()
	{
		return Loader.getPluginManager();
	}
	
	public ConsoleLogManager getLogger()
	{
		return Loader.getLogger();
	}
	
	public String getStatusDescription( int errNo )
	{
		return HttpCode.msg( errNo );
	}
	
	public PersistenceManager getPersistenceManager()
	{
		return Loader.getPersistenceManager();
	}
}
