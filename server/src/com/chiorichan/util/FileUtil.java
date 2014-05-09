package com.chiorichan.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

/**
 * Class containing file utilities
 */
public class FileUtil
{
	public static byte[] inputStream2Bytes( InputStream is ) throws IOException
	{
		return inputStream2ByteArray( is ).toByteArray();
	}

	public static ByteArrayOutputStream inputStream2ByteArray( InputStream is ) throws IOException
	{
		int nRead;
		byte[] data = new byte[16384];
		ByteArrayOutputStream bs = new ByteArrayOutputStream();

		while ( (nRead = is.read( data, 0, data.length )) != -1 )
		{
			bs.write( data, 0, nRead );
		}

		bs.flush();

		return bs;
	}

	/**
	 * This method copies one file to another location
	 *
	 * @param inFile
	 * the source filename
	 * @param outFile
	 * the target filename
	 * @return true on success
	 */
	@SuppressWarnings( "resource" )
	public static boolean copy( File inFile, File outFile )
	{
		if ( !inFile.exists() )
			return false;

		FileChannel in = null;
		FileChannel out = null;

		try
		{
			in = new FileInputStream( inFile ).getChannel();
			out = new FileOutputStream( outFile ).getChannel();

			long pos = 0;
			long size = in.size();

			while ( pos < size )
			{
				pos += in.transferTo( pos, 10 * 1024 * 1024, out );
			}
		}
		catch ( IOException ioe )
		{
			return false;
		}
		finally
		{
			try
			{
				if ( in != null )
					in.close();
				if ( out != null )
					out.close();
			}
			catch ( IOException ioe )
			{
				return false;
			}
		}

		return true;
	}
}
