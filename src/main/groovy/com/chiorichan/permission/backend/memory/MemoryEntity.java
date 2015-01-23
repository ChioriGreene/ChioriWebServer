package com.chiorichan.permission.backend.memory;

import java.util.Map;

import com.chiorichan.permission.backend.MemoryBackend;
import com.chiorichan.permission.backend.PermissibleProxy;
import com.chiorichan.permission.structure.Permission;

public class MemoryEntity extends PermissibleProxy
{
	public MemoryEntity( String userName, MemoryBackend backend )
	{
		super( userName, backend );
	}

	@Override
	public boolean isPermissionSet( String req )
	{
		return false;
	}

	@Override
	public boolean isPermissionSet( Permission req )
	{
		return false;
	}

	@Override
	public boolean hasPermission( String req )
	{
		return false;
	}

	@Override
	public boolean hasPermission( Permission req )
	{
		return false;
	}

	@Override
	public boolean isOp()
	{
		return false;
	}

	@Override
	public String getPrefix( String siteName )
	{
		return null;
	}

	@Override
	public void setPrefix( String prefix, String siteName )
	{
		
	}

	@Override
	public String getSuffix( String siteName )
	{
		return null;
	}

	@Override
	public void setSuffix( String suffix, String siteName )
	{
		
	}

	@Override
	public String[] getPermissions( String site )
	{
		return null;
	}

	@Override
	public Map<String, String[]> getAllPermissions()
	{
		return null;
	}

	@Override
	public void save()
	{
		
	}

	@Override
	public void remove()
	{
		
	}

	@Override
	public String[] getSites()
	{
		return null;
	}
}