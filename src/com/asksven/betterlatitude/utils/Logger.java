/*
 * Copyright (C) 2011 asksven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asksven.betterlatitude.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.asksven.android.common.utils.DataStorage;
import com.asksven.android.common.utils.GenericLogger;

/**
 * A Logger class with logging to logcat and to own logfile
 * @author sven
 *
 */
public class Logger  
{

	/** The application's own logfile */
	public static String LOGFILE = "betterlatitude.log";

	Boolean bLogToFile = null;
	/**
	 * 
	 */
	public static void d(String strTag, String strMessage, Context ctx)
	{
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	if (prefs.getBoolean("own_logfile", false))
    	{
    		GenericLogger.d(LOGFILE, strTag, strMessage);
    	}
    	else
    	{
    		Log.d(strTag, strMessage);
    	}
	}
	
	public static void e(String strTag, String strMessage, Context ctx)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	if (prefs.getBoolean("own_logfile", false))
    	{
    		GenericLogger.e(LOGFILE, strTag, strMessage);
    	}
    	else
    	{
    		Log.e(strTag, strMessage);
    	}
    		
	}

	public static void i(String strTag, String strMessage, Context ctx)
	{
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	if (prefs.getBoolean("own_logfile", false))
    	{
    		GenericLogger.d(LOGFILE, strTag, strMessage);
    	}
    	else
    	{
    		Log.d(strTag, strMessage);
    	}
	}
	
	public static void e(String strTag, StackTraceElement[] stack, Context ctx)
	{
		Log.e(strTag, "An Exception occured. Stacktrace:");
		for (int i=0; i < stack.length; i++)
		{
			Log.e(strTag, stack[i].toString());
		}
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	if (prefs.getBoolean("own_logfile", false))
    	{
    		DataStorage.LogToFile(LOGFILE, stack);
    	}
	}
	
//	private boolean logToFile()
//	{
//		if (bLogToFile == null)
//		{
//			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences();
//			bLogToFile = prefs.getBoolean("own_logfile", true);
//
//	}
}
