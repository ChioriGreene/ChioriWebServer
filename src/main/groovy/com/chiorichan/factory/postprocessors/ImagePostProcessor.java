package com.chiorichan.factory.postprocessors;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.chiorichan.ChatColor;
import com.chiorichan.ContentTypes;
import com.chiorichan.Loader;
import com.chiorichan.factory.CodeMetaData;
import com.google.zxing.common.detector.MathUtils;

public class ImagePostProcessor implements PostProcessor
{
	@Override
	public String[] getHandledTypes()
	{
		return ContentTypes.getAllTypes( "image" );
	}
	
	@Override
	public String process( CodeMetaData meta, String code )
	{
		float x = 0;
		float y = 0;
		
		if ( meta.params != null )
		{
			if ( meta.params.get( "serverSideOptions" ) != null )
			{
				String[] params = meta.params.get( "serverSideOptions" ).trim().split( "_" );
				
				for ( String p : params )
				{
					if ( p.toLowerCase().startsWith( "x" ) && p.length() > 1 )
						x = Integer.parseInt( p.substring( 1 ) );
					else if ( p.toLowerCase().startsWith( "y" ) && p.length() > 1 )
						y = Integer.parseInt( p.substring( 1 ) );
				}
			}
			
			if ( meta.params.get( "width" ) != null )
				x = Integer.parseInt( meta.params.get( "width" ) );
			
			if ( meta.params.get( "height" ) != null )
				y = Integer.parseInt( meta.params.get( "height" ) );
			
			if ( meta.params.get( "w" ) != null )
				x = Integer.parseInt( meta.params.get( "w" ) );
			
			if ( meta.params.get( "h" ) != null )
				y = Integer.parseInt( meta.params.get( "h" ) );
		}
		
		try
		{
			BufferedImage buf = ImageIO.read( new ByteArrayInputStream( code.getBytes( "ISO-8859-1" ) ) );
			
			if ( buf != null )
			{
				float w = buf.getWidth();
				float h = buf.getHeight();
				float w1 = w;
				float h1 = h;
				
				if ( x < 1 && y < 1 )
				{
					x = w;
					y = h;
				}
				else if ( x > 0 && y < 1 )
				{
					w1 = x;
					h1 = x * ( h / w );
				}
				else if ( y > 0 && x < 1 )
				{
					w1 = y * ( w / h );
					h1 = y;
				}
				else if ( x > 0 && y > 0 )
				{
					w1 = x;
					h1 = y;
				}
				
				if ( w1 < 1 || h1 < 1 )
					return null;
				
				Image image = buf.getScaledInstance( MathUtils.round( w1 ), MathUtils.round( h1 ), Loader.getConfig().getBoolean( "advanced.processors.useFastGraphics", true ) ? Image.SCALE_FAST : Image.SCALE_SMOOTH );
				
				BufferedImage rtn = new BufferedImage( MathUtils.round( w1 ), MathUtils.round( h1 ), buf.getType() );
				Graphics2D graphics = rtn.createGraphics();
				graphics.drawImage( image, 0, 0, null );
				graphics.dispose();
				
				Loader.getLogger().info( ChatColor.AQUA + "Resized image from " + MathUtils.round( w ) + "px by " + MathUtils.round( h ) + "px to " + MathUtils.round( w1 ) + "px by " + MathUtils.round( h1 ) + "px" );
				
				if ( rtn != null )
				{
					ByteArrayOutputStream bs = new ByteArrayOutputStream();
					ImageIO.write( rtn, "png", bs );
					return new String( bs.toByteArray(), "ISO-8859-1" );
				}
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		
		return null;
	}
}