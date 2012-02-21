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

package com.asksven.betterlatitude.credentialstore;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;

/**
 * @author sven
 *
 */
public class LatitudeApi
{
	private static final String TAG = "LatitudeApi";
	
	/** The OAuth2 token */
	private String m_authToken;
	
	/** the singleton */
	private static LatitudeApi m_instance = null;
	
	/** the context */
	private static Context m_context = null;
	
	
	/** request for AccountManager */
	private static final int REQUEST_AUTHENTICATE = 0;



	private LatitudeApi()
	{
		
	}
	
	public static LatitudeApi getInstance(Context ctx)
	{
		if (m_instance == null)
		{
			m_instance = new LatitudeApi();
			m_context = ctx;
		}
		
		return m_instance;
	}
    /**
	 * Clears our credentials (token and token secret) from the shared preferences.
	 * We also setup the authorizer (without the token).
	 * After this, no more authorized API calls will be possible.
	 */
    public void clearCredentials()
    {
    	// Clear cookie credentials
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(m_context);
    	new SharedPreferencesCredentialStore(prefs).clearCredentials();
    	
    	// also clear account manager credentials if existing
		AccountManager manager = AccountManager.get(m_context);
		manager.invalidateAuthToken("com.google", m_authToken);

	    SharedPreferences.Editor editor = prefs.edit();
	    editor.remove("accountName");
	    editor.commit();

    }

    /**
     * Check if credentials exist
     * @return true is credentials were stored
     */
    public static boolean hasCredentials(Context ctx)
    {
    	boolean bRet = false;
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		CredentialStore credentialStore = new SharedPreferencesCredentialStore(prefs);
		AccessTokenResponse accessTokenResponse = credentialStore.read();

		// distinguish between "classical" and "account manager" credentials
		if (!useAccountManager(ctx))
		{
			// check if token is valid (exists)
			if (accessTokenResponse.accessToken.equals(""))
			{
				bRet = false;
			}
			else
			{
				bRet = true;
			}
			
		}
		else
		{
			String accountName = prefs.getString("accountName", null);
	
			// we know the account to use
			if (accountName == null)
			{
				bRet = false;
			}
			else
			{
				bRet = true;
			}
		}
		
		return bRet;

    }
//
//    /**
//     * returns the cached auth token
//     * @return
//     */
//    public String getAuthToken()
//    {
//    	if (useAccountManager(m_context))
//    	{
//    		return m_authToken;
//    	}
//    	else
//    	{
//	    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(m_context);
//			CredentialStore credentialStore = new SharedPreferencesCredentialStore(prefs);
//			AccessTokenResponse accessTokenResponse = credentialStore.read();
//
//			return accessTokenResponse.accessToken;
//    	}
//    }
//    
    public static boolean useAccountManager(Context ctx)
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	return prefs.getBoolean("use_account_manager", false);

    }
//	public void useAccount(Activity parent, boolean tokenExpired)
//	{
//		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(m_context);
//		
//		String accountName = settings.getString("accountName", null);
//
//		// we know the account to use
//		if (accountName != null)
//		{
//			AccountManager manager = AccountManager.get(m_context);
//			Account[] accounts = manager.getAccountsByType("com.google");
//			int size = accounts.length;
//			for (int i = 0; i < size; i++)
//			{
//				Account account = accounts[i];
//				if (accountName.equals(account.name))
//				{
//					if (tokenExpired)
//					{
//						manager.invalidateAuthToken("com.google", this.m_authToken);
//					    SharedPreferences.Editor editor = settings.edit();
//					    editor.remove("accountName");
//					    editor.commit();
//					    m_authToken = null;
//
//					}
//					else
//					{	
//						if (parent != null)
//						{
//							useAccount(parent, manager, account);
//						}
//						else
//						{
//							useAccount(manager, account);
//						}
//					}
//					return;
//				}
//			}
//		}
//		
//		// show dialog if not headless
//		if (parent != null)
//		{
//			parent.showDialog(MainActivity.DIALOG_ACCOUNTS);
//		}
//	}
//	
//	/**
//	 * @param manager
//	 * @param account
//	 */
//	public void useAccount(final AccountManager manager, final Account account) 
//	{
//		// Save the account that will be used to connect
//		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(m_context);
//	    SharedPreferences.Editor editor = settings.edit();
//	    editor.putString("accountName", account.name);
//	    editor.commit();
//	    
//	    // if token is already instanciated just use it
//	    if ( (m_authToken != null) && (!m_authToken.equals("")) )
//	    {
//	    	setLocationApiCall(m_authToken);
//	    	return;
//	    }
//	    
//		// see http://code.google.com/apis/gdata/faq.html#clientlogin
//		// for authTokenType
//		manager.getAuthToken(account, OAuth2ClientConstants.SCOPE, true,
//			  new AccountManagerCallback<Bundle>()
//			  {
//				    public void run(AccountManagerFuture<Bundle> bundle)
//				    {
//				    try
//				    {
//						// If the user has authorized your application to use the tasks API
//						// a token is available.
//						setAuthenticated(bundle.getResult().getString(AccountManager.KEY_AUTHTOKEN));
//						setLocationApiCall(m_authToken);
//				    }
//				    catch (OperationCanceledException e)
//				    {
//				      // TODO: The user has denied you access to the API, you
//				      // should handle that
//				    }
//				    catch (Exception e)
//				    {
//				      handleException(null, e);
//				    }
//				  }
//			}, null);
//    }
//
//	/**
//	 * @param manager
//	 * @param account
//	 */
//	public void useAccount(final Activity parent, final AccountManager manager, final Account account) 
//	{
//		// Save the account that will be used to connect
//		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(m_context);
//	    SharedPreferences.Editor editor = settings.edit();
//	    editor.putString("accountName", account.name);
//	    editor.commit();
//	    
//	    new Thread()
//	    {
//	        @Override
//	        public void run()
//	        {
//	        	try
//	        	{
//	        		// see http://code.google.com/apis/gdata/faq.html#clientlogin
//	        		// for authTokenType
//					final Bundle bundle =
//					    manager.getAuthToken(account, "oauth2:https://www.googleapis.com/auth/latitude.all.best", true, null, null).getResult();
//					parent.runOnUiThread(new Runnable()
//					{
//						public void run()
//						{
//							try
//							{
//								if (bundle.containsKey(AccountManager.KEY_INTENT))
//								{
//									// show dialog if not headless
//									if (parent != null)
//									{
//										Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
//										int flags = intent.getFlags();
//										flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
//										intent.setFlags(flags);
//										parent.startActivityForResult(intent, REQUEST_AUTHENTICATE);
//									}
//								}
//								else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN))
//								{
//									setAuthenticated(bundle.getString(AccountManager.KEY_AUTHTOKEN));
//								}
//							}
//							catch (Exception e)
//							{
//								handleException(parent, e);
//							}
//						}
//					});
//				}
//	        	catch (Exception e)
//	        	{
//	        		handleException(parent, e);
//	        	}
//	        }
//        }.start();
//    }
//	
//	
//	/**
//	 * Store the authToken to use for further roundtrips
//	 * @param authToken
//	 */
//	void setAuthenticated(String authToken)
//	{
//	    this.m_authToken = authToken;
////	    clientLogin.auth = authToken;
////	    authenticated();
//	}
//	
//	void handleException(Activity parent, Exception e)
//	{
//		e.printStackTrace();
//		Log.e(TAG, "An error occured in handleException: " + e.getMessage());
//		if (e instanceof HttpResponseException)
//		{
//			HttpResponse response = ((HttpResponseException) e).getResponse();
//			int statusCode = response.getStatusCode();
//			try
//			{
//				response.ignore();
//			}
//			catch (IOException e1)
//			{
//				e1.printStackTrace();
//			}
//			if (statusCode == 401 || statusCode == 403)
//			{
//				// invalidate token and redo auth
//				useAccount(parent, true);
//				return;
//			}
//			try
//			{
//				Log.e(TAG, response.parseAsString());
//			}
//			catch (IOException parseException)
//			{
//				parseException.printStackTrace();
//			}
//		}
//		Log.e(TAG, e.getMessage(), e);
//	}
//	
//	private boolean setLocationApiCall(String token)
//	{
//		boolean bRet = true;
//
//		try
//		{
//			if (!DataNetwork.hasDataConnection(m_context))
//			{
//				
//				return false;
//			}
//			
//			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(m_context);
//	    	boolean bLogLoc 		= prefs.getBoolean("log_location", false);
//	    	
//			JsonFactory jsonFactory = new JacksonFactory();
//			HttpTransport transport = new NetHttpTransport();
//			
//			CredentialStore credentialStore = new SharedPreferencesCredentialStore(prefs);
//			AccessTokenResponse accessTokenResponse = credentialStore.read();
//			
//			// check if token is valid (not empty)
//			if ( (token == null)||(token.equals("")) )
//			{
//				LocationService.getInstance().notifyError("Access to latitude was not granted. Please log on");
//				bRet = false;
//				LocationService.getInstance().setStatus(LocationService.STATUS_NOT_LOGGED_IN);
//				return bRet;				
//			}
//			
//			GoogleAccessProtectedResource accessProtectedResource = new GoogleAccessProtectedResource(accessTokenResponse.accessToken,
//			        transport,
//			        jsonFactory,
//			        OAuth2ClientConstants.CLIENT_ID,
//			        OAuth2ClientConstants.CLIENT_SECRET,
//			        accessTokenResponse.refreshToken);
//			
////		    final Latitude latitude = new Latitude(transport, accessProtectedResource, jsonFactory);
//		    
//		    final Latitude.Builder builder = Latitude.builder(transport, jsonFactory);
//		    builder.setHttpRequestInitializer(accessProtectedResource);
//		    builder.setApplicationName("ALTitude");
//			final Latitude latitude = builder.build();
//		    latitude.setKey(OAuth2ClientConstants.API_KEY);
//		    
//		    // empty the stack and update all locations with the right timestamp
//		    ArrayList<Location> locationStack = LocationService.getInstance().getLocationStack();
//		    if (locationStack != null)
//		    {
//		    	for (int i=0; i < locationStack.size(); i++)
//		    	{
//		    		Location location = locationStack.get(i);
//		    		
//			    	if (bLogLoc)
//			    	{
//						Logger.i(TAG, " Service Updating Latitude with position Lat: "
//								+ String.valueOf(location.getLatitude())
//								+ " Long: " + String.valueOf(location.getLongitude()));
//			    	}
//			    	else
//			    	{
//			    		Logger.i(TAG, " Service Updating Latitude");
//			    	}
//
////				    LatitudeCurrentlocationResourceJson currentLocation = new LatitudeCurrentlocationResourceJson();
//			    	com.google.api.services.latitude.model.Location currentLocation = new com.google.api.services.latitude.model.Location();
//				    currentLocation.set("latitude", location.getLatitude());
//				    currentLocation.set("longitude", location.getLongitude());
//				    currentLocation.set("timespampMs", location.getTime());
//				    
//				    Insert myInsert = latitude.currentLocation().insert(currentLocation);
//
//				    String now = DateUtils.now("HH:mm:ss");
//				    LocationService.getInstance().setStatus(LocationService.STATUS_LOCATION_UPDATED + ": " + now);
//
//				    if (myInsert != null)
//				    {
//				    	//myInsert.execute();
//				    	new UpdateLatitudeTask().execute(myInsert);
//				    }
//				    else
//				    {
//				    	LocationService.getInstance().setStatus(LocationService.STATUS_UPDATE_BUFFERED + "(" + locationStack.size() + ")");
//				    	throw new IOException("CurrentLocation.Insert failed");
//				    	
//				    }
//		    	}
//		    	
//		    	// if we got here all updates were OK, delete the stack
//		    	LocationService.getInstance().notifyCurrentLocation();
//		    	locationStack.clear();
//		    }
//		}
//		catch (IOException ex)
//		{
//			bRet = false;
//			Logger.i(TAG, "An error occured in setLocationApiCall() '" +  ex.getMessage() + "'");
//			if (ex.getMessage().equals("401 Unauthorized"))
//			{
//				LocationService.getInstance().setStatus(LocationService.STATUS_NOT_LOGGED_IN);
//			}
//			LocationService.getInstance().notifyError("Updating Latitude failed with error '" + ex.getMessage() + "'");
////			Logger.i(TAG, ex.getStackTrace());
//			
//		}
//		
//		return bRet;
//	}
//
//	class UpdateLatitudeTask extends AsyncTask<Insert, Void, Void>
//	{
//
//	    private Exception exception;
//
//	    protected Void doInBackground(Insert... inserts)
//	    {
//	    	try
//	        {
//	    		inserts[0].execute();
//	        	return null;
//	        }
//	        catch (Exception e)
//	        {
//	        	
//	            this.exception = e;
//	            Log.e(TAG, "An error occured in UpdateLatitude.doInBackground(): " + e.getMessage());
//	            return null;
//	        }
//	    }
//
//	    protected void onPostExecute()
//	    {
//
//	    }
//	 }
//
}
