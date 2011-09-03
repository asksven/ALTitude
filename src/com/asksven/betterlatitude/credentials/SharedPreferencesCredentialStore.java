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
package com.asksven.betterlatitude.credentials;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;

public class SharedPreferencesCredentialStore implements CredentialStore
{

	private static final String ACCESS_TOKEN = "access_token";
	private static final String EXPIRES_IN = "expires_in";
	private static final String REFRESH_TOKEN = "refresh_token";
	private static final String SCOPE = "scope";

	private SharedPreferences prefs;
	
	public SharedPreferencesCredentialStore(SharedPreferences prefs)
	{
		this.prefs = prefs;
	}
	
	@Override
	public AccessTokenResponse read()
	{
		AccessTokenResponse accessTokenResponse = new AccessTokenResponse();
			accessTokenResponse.accessToken = prefs.getString(ACCESS_TOKEN, "");
			accessTokenResponse.expiresIn = prefs.getLong(EXPIRES_IN, 0);
			accessTokenResponse.refreshToken = prefs.getString(REFRESH_TOKEN, "");
			accessTokenResponse.scope = prefs.getString(SCOPE, "");
		return accessTokenResponse;
	}

	@Override
	public void write(AccessTokenResponse accessTokenResponse)
	{
		Editor editor = prefs.edit();
		editor.putString(ACCESS_TOKEN,accessTokenResponse.accessToken);
		editor.putLong(EXPIRES_IN,accessTokenResponse.expiresIn);
		editor.putString(REFRESH_TOKEN,accessTokenResponse.refreshToken);
		editor.putString(SCOPE,accessTokenResponse.scope);
		editor.commit();
	}
	
	@Override
	public void clearCredentials()
	{
		Editor editor = prefs.edit();
		editor.remove(ACCESS_TOKEN);
		editor.remove(EXPIRES_IN);
		editor.remove(REFRESH_TOKEN);
		editor.remove(SCOPE);
		editor.commit();
	}
}
