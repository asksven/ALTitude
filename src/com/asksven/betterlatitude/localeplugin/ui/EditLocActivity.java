/*
 * Copyright (C) 2012 asksven
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
 * 
 * This file was contributed by two forty four a.m. LLC <http://www.twofortyfouram.com>
 * unter the terms of the Apache License, Version 2.0
 */

package com.asksven.betterlatitude.localeplugin.ui;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.asksven.betterlatitude.R;
import com.asksven.betterlatitude.localeplugin.Constants;
import com.asksven.betterlatitude.localeplugin.bundle.BundleScrubber;
import com.asksven.betterlatitude.localeplugin.bundle.PluginBundleManager;
import com.twofortyfouram.locale.BreadCrumber;

/**
 * This is the "Edit" activity for a Locale Plug-in.
 */
public final class EditLocActivity extends AbstractPluginActivity implements AdapterView.OnItemSelectedListener
{

	static final String TAG = "CommandCenterStatsLocalePlugin:EditLocActivity"; 
    /**
     * Help URL, used for the {@link com.twofortyfouram.locale.platform.R.id#twofortyfouram_locale_menu_help} menu item.
     */
    // TODO: Place a real help URL here
    private static final String HELP_URL = "http://blog.asksven.org"; //$NON-NLS-1$

    /**
     * Flag boolean that can only be set to true via the "Don't Save"
     * {@link com.twofortyfouram.locale.platform.R.id#twofortyfouram_locale_menu_dontsave} menu item in
     * {@link #onMenuItemSelected(int, MenuItem)}.
     * <p>
     * If true, then this {@code Activity} should return {@link Activity#RESULT_CANCELED} in {@link #finish()}.
     * <p>
     * If false, then this {@code Activity} should generally return {@link Activity#RESULT_OK} with extras
     * {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} and {@link com.twofortyfouram.locale.Intent#EXTRA_STRING_BLURB}.
     * <p>
     * There is no need to save/restore this field's state when the {@code Activity} is paused.
     */
    private boolean mIsCancelled = false;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        /*
         * A hack to prevent a private serializable classloader attack
         */
        BundleScrubber.scrub(getIntent());
        BundleScrubber.scrub(getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE));

        setContentView(R.layout.locale_plugin_location);

        if (Build.VERSION.SDK_INT >= 11)
        {
            CharSequence callingApplicationLabel = null;
            try
            {
                callingApplicationLabel = getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(getCallingPackage(), 0));
            }
            catch (final NameNotFoundException e)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.e(Constants.LOG_TAG, "Calling package couldn't be found", e); //$NON-NLS-1$
                }
            }
            if (null != callingApplicationLabel)
            {
                setTitle(callingApplicationLabel);
            }
        }
        else
        {
            setTitle(BreadCrumber.generateBreadcrumb(getApplicationContext(), getIntent(), getString(R.string.plugin_name)));
        }

        // populate the widgets
		TextView tvLatitude 	= (TextView) findViewById(R.id.EditLatitude);
		TextView tvLongitude 	= (TextView) findViewById(R.id.EditLongitude);
		
		tvLatitude.setText("");
		tvLongitude.setText("");

        /*
         * if savedInstanceState is null, then then this is a new Activity instance and a check for EXTRA_BUNDLE is needed
         */
        if (null == savedInstanceState)
        {
            final Bundle forwardedBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);

            if (PluginBundleManager.isBundleValid(forwardedBundle))
            {
            	// PluginBundleManager.isBundleValid must be changed if elements are added to the bundle
            	// 
            	tvLatitude.setText(String.valueOf(forwardedBundle.getFloat(PluginBundleManager.BUNDLE_EXTRA_FLOAT_LATITUDE)));
            	tvLongitude.setText(String.valueOf(forwardedBundle.getFloat(PluginBundleManager.BUNDLE_EXTRA_FLOAT_LONGITUDE)));

                Log.i(TAG, "Retrieved from Bundle: Lat=" + tvLatitude.getText() + " Long=" + tvLongitude.getText());
            }
        }        

        /*
         * if savedInstanceState isn't null, there is no need to restore any Activity state directly via onSaveInstanceState(), as
         * the EditText object handles that automatically
         */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finish()
    {
        if (mIsCancelled)
        {
            setResult(RESULT_CANCELED);
        }
        else
        {
            final String latitude = String.valueOf(((TextView) findViewById(R.id.EditLatitude)).getText());
            final String longitude = String.valueOf(((TextView) findViewById(R.id.EditLongitude)).getText());
            
            float fLat = 0f;
            float fLong = 0f;
            try
            {
            	fLat = Float.valueOf(latitude);
            	fLong = Float.valueOf(longitude);
            }
            catch (Exception e)
            {
            	// defaults
                fLat = 0f;
                fLong = 0f;
            }
            
            /*
             * This is the result Intent to Locale
             */
            final Intent resultIntent = new Intent();

            /*
             * This extra is the data to ourselves: either for the Activity or the BroadcastReceiver. Note that anything
             * placed in this Bundle must be available to Locale's class loader. So storing String, int, and other standard
             * objects will work just fine. However Parcelable objects must also be Serializable. And Serializable objects
             * must be standard Java objects (e.g. a private subclass to this plug-in cannot be stored in the Bundle, as
             * Locale's classloader will not recognize it).
             */
            final Bundle resultBundle = new Bundle();
            resultBundle.putInt(PluginBundleManager.BUNDLE_EXTRA_INT_VERSION_CODE, Constants.getVersionCode(this));
            resultBundle.putFloat(PluginBundleManager.BUNDLE_EXTRA_FLOAT_LATITUDE, fLat);
            resultBundle.putFloat(PluginBundleManager.BUNDLE_EXTRA_FLOAT_LONGITUDE, fLong);
            
            Log.i(TAG, "Saved Bundle: Lat=" + fLat + ", Long=" + fLong);

            resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle);
            
           	resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, "Set location");

            setResult(RESULT_OK, resultIntent);
        }

        super.finish();
    }


    
	/**
	 * Take the change of selection from the spinners into account and refresh the ListView
	 * with the right data
	 */
	public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
	{
		// Depending on the action enable/disable the other spinners
        String selectedAction = ((Spinner) findViewById(R.id.spinnerAction)).getSelectedItem().toString();
        
        boolean bEnabled = false;
        
        if (selectedAction.equals("0"))
        {
        	bEnabled = false;
        }
        else
        {
        	bEnabled = true;
        }
        
		Spinner spinnerInterval = (Spinner) findViewById(R.id.spinnerInterval);
		spinnerInterval.setEnabled(bEnabled);
		
		Spinner spinnerAccuracy = (Spinner) findViewById(R.id.spinnerAccuracy);
		spinnerAccuracy.setEnabled(bEnabled);
		
		Spinner spinnerDuration = (Spinner) findViewById(R.id.spinnerDuration);
		spinnerDuration.setEnabled(bEnabled);
        
	}
	
	public void onNothingSelected(AdapterView<?> parent)
	{
	}


}