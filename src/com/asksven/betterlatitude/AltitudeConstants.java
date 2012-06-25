/*
 * Copyright (C) 2011-2012 asksven
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
package com.asksven.betterlatitude;


import android.content.Context;
import android.content.res.Resources;

/**
 * Impements a singleton for accessing internationalized status strings
 * @author sven
 *
 */
public class AltitudeConstants
{

	/** constants for the connection status */
	public String STATUS_UPDATE_PENDING 		= "";
	public String STATUS_UPDATE_BUFFERED 		= "";
	public String STATUS_NOTIFICATION_ON 		= "";
	public String STATUS_NOT_LOGGED_IN	 		= "";
	public String STATUS_SERVICE_NOT_STARTED 	= "";
	public String STATUS_SERVICE_UNAVAILABLE 	= "";
	public String STATUS_SERVICE_RUNNING	 	= "";
	public String STATUS_FG_SERVICE_STARTED 	= "";
	public String STATUS_LOCATION_UPDATED 		= "";
	public String BROADCAST_STATUS_CHANGED 		= "";

	static AltitudeConstants m_instance = null;
	private AltitudeConstants()
	{
	}
	
	public static AltitudeConstants getInstance(Context ctx)
	{		
		if (m_instance == null)
		{
			m_instance = new AltitudeConstants();
			m_instance.STATUS_UPDATE_PENDING 		= ctx.getString(R.string.status_update_pending);
			m_instance.STATUS_UPDATE_BUFFERED 		= ctx.getString(R.string.status_update_buffered);
			m_instance.STATUS_NOTIFICATION_ON 		= ctx.getString(R.string.status_notification_on);
			m_instance.STATUS_NOT_LOGGED_IN	 		= ctx.getString(R.string.status_not_logged_in);
			m_instance.STATUS_SERVICE_NOT_STARTED 	= ctx.getString(R.string.status_service_not_started);
			m_instance.STATUS_SERVICE_RUNNING	 	= ctx.getString(R.string.status_service_running);
			m_instance.STATUS_SERVICE_UNAVAILABLE 	= ctx.getString(R.string.status_service_unavailable);
			m_instance.STATUS_FG_SERVICE_STARTED 	= ctx.getString(R.string.status_fg_service_started);
			m_instance.STATUS_LOCATION_UPDATED 		= ctx.getString(R.string.status_location_updated);
			m_instance.BROADCAST_STATUS_CHANGED 	= ctx.getString(R.string.status_connection_changed);

		}
		return m_instance;
	}
}
