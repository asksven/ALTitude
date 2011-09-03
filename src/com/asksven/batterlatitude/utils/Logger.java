/**
 * 
 */
package com.asksven.batterlatitude.utils;

import com.asksven.android.common.utils.GenericLogger;

/**
 * @author sven
 *
 */
public class Logger  
{

	/** The application's own logfile */
	public static String LOGFILE = "betterlatitude.log";
	
	/**
	 * 
	 */
	public static void d(String strTag, String strMessage)
	{
		GenericLogger.d(LOGFILE, strTag, strMessage);
	}
	
	public static void e(String strTag, String strMessage)
	{
		GenericLogger.e(LOGFILE, strTag, strMessage);
	}

	public static void i(String strTag, String strMessage)
	{
		GenericLogger.d(LOGFILE, strTag, strMessage);
	}

}
