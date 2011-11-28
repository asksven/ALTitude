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

package com.asksven.betterlatitude;

import com.asksven.betterlatitude.utils.Configuration;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Activity for managing preferences using Android's preferences framework
 * @see http://www.javacodegeeks.com/2011/01/android-quick-preferences-tutorial.html
 * 
 * Access prefs goes like this:
 *   SharedPreferences sharedPrefs = 
 *   	PreferenceManager.getDefaultSharedPreferences(this);
 *   sharedPrefs.getBoolean("perform_updates", false));
 *   
 * @author sven
 *
 */
public class PreferencesActivity extends PreferenceActivity
{
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		// disable all LocationListener prefs in free version
		if (!Configuration.isFullVersion(this))
		{
			findPreference("start_on_boot").setEnabled(false);
			findPreference("loc_provider").setEnabled(false);
			findPreference("loc_provider").setSummary("Latitude location provider: cell network");
			findPreference("update_interval").setEnabled(false);
			findPreference("update_interval").setSummary("Latitude update interval: 15 minutes");
			findPreference("update_accuracy").setEnabled(false);
			findPreference("update_accuracy").setSummary("Latitude update accuracy: 2 Km");
			findPreference("map_loc_provider").setEnabled(false);
			findPreference("map_locProvider").setSummary("Map location provider: cell network");
			findPreference("map_update_interval").setEnabled(false);
			findPreference("map_update_interval").setSummary("Map update interval: 15 minutes");
			findPreference("map_update_accuracy").setEnabled(false);
			findPreference("map_update_accuracy").setSummary("Map update accuracy: 2 Km");
			
		}
	}
}
