package com.chiorichan.command.defaults;

import java.util.List;

import org.apache.commons.lang3.Validate;

import com.chiorichan.ChatColor;
import com.chiorichan.Main;
import com.chiorichan.command.CommandSender;
import com.chiorichan.user.User;
import com.google.common.collect.ImmutableList;

public class SayCommand extends VanillaCommand
{
	public SayCommand()
	{
		super( "say" );
		this.description = "Broadcasts the given message as the console";
		this.usageMessage = "/say <message>";
		this.setPermission( "bukkit.command.say" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		if ( args.length == 0 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		StringBuilder message = new StringBuilder();
		if ( args.length > 0 )
		{
			message.append( args[0] );
			for ( int i = 1; i < args.length; i++ )
			{
				message.append( " " );
				message.append( args[i] );
			}
		}
		
		if ( sender instanceof User )
		{
			Main.getInstance().getLogger().info( "[" + sender.getName() + "] " + message );
		}
		
		Main.getInstance().broadcastMessage( ChatColor.LIGHT_PURPLE + "[Server] " + message );
		
		return true;
	}
	
	@Override
	public List<String> tabComplete( CommandSender sender, String alias, String[] args ) throws IllegalArgumentException
	{
		Validate.notNull( sender, "Sender cannot be null" );
		Validate.notNull( args, "Arguments cannot be null" );
		
		if ( args.length >= 1 )
		{
			return super.tabComplete( sender, alias, args );
		}
		return ImmutableList.of();
	}
}