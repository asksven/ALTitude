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

/**
 * OAuth 2 credentials found in the <a
 * href="https://code.google.com/apis/console">Google apis console</a>.
 */
public class OAuth2ClientConstants
{

	// see https://code.google.com/apis/console
	/** Value of the "Client ID" shown under "Client ID for installed applications". */
	public static final String CLIENT_ID = "463013748014.apps.googleusercontent.com";

	/** Value of the "Client secret" shown under "Client ID for installed applications". */
	public static final String CLIENT_SECRET = "LaDeyavQ7smf66f8jLVmsVpI";

	/** OAuth 2 scope to use */
	public static final String SCOPE = "https://www.googleapis.com/auth/latitude.all.best";

	/** OAuth 2 redirect uri */
	public static final String REDIRECT_URI = "http://localhost";

	/** Latitude API key */
	public static final String API_KEY = "";

}
