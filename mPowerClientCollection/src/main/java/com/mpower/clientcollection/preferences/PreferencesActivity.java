/*
 * Copyright (C) 2011 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.mpower.clientcollection.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.widget.Toast;

import com.mpower.clientcollection.activity.R;
import com.mpower.clientcollection.application.ClientCollection;
import com.mpower.clientcollection.utilities.UrlUtils;

/**
 * Handles general preferences.
 * 
 * @author Thomas Smyth, Sassafras Tech Collective (tom@sassafrastech.com; constraint behavior option)
 * @author ratna halder
 */
public class PreferencesActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {

	protected static final int IMAGE_CHOOSER = 0;

	public static final String KEY_INFO = "info";
	public static final String KEY_LAST_VERSION = "lastVersion";
	public static final String KEY_FIRST_RUN = "firstRun";
	public static final String KEY_FONT_SIZE = "font_size";
	

	public static final String KEY_SERVER_URL = "server_url";
	public static final String KEY_USERNAME = "username";
	public static final String KEY_PASSWORD = "password";

	public static final String KEY_PROTOCOL = "protocol";

	// must match /res/arrays.xml
	public static final String PROTOCOL_CLIENT_COLLECTION_DEFAULT = "clientcollection_default";
	public static final String PROTOCOL_GOOGLE = "google";
	public static final String PROTOCOL_OTHER = "";
	
	public static final String NAVIGATION_SWIPE = "swipe";
	public static final String NAVIGATION_BUTTONS = "buttons";
	public static final String NAVIGATION_SWIPE_BUTTONS = "swipe_buttons";

	public static final String CONSTRAINT_BEHAVIOR_ON_SWIPE = "on_swipe";
	public static final String CONSTRAINT_BEHAVIOR_ON_FINALIZE = "on_finalize";
	public static final String CONSTRAINT_BEHAVIOR_DEFAULT = "on_swipe";

	public static final String KEY_FORMLIST_URL = "formlist_url";
	public static final String KEY_SUBMISSION_URL = "submission_url";

	public static final String KEY_COMPLETED_DEFAULT = "default_completed";

	public static final String KEY_AUTH = "auth";

	//MPOWER: For language Change
	public static final String KEY_LANGUAGE_SETTING = "language";
	
	public static final String KEY_AUTOSEND_WIFI = "autosend_wifi";
	public static final String KEY_AUTOSEND_NETWORK = "autosend_network";
	public static final String KEY_AUTOSEND_WIFI_DEFAULT = "autosend_wifi_disable";
	public static final String KEY_AUTOSEND_NETWORK_DEFAULT = "autosend_network_disable";
	
	public static final String KEY_AUTOSEND_BY_SIZE = "by_size";
	public static final String KEY_AUTOSEND_BY_DATE = "by_date";
	public static final String KEY_AUTOSEND_BY_PRIORITY = "by_priority";
	
	public static final String KEY_NAVIGATION = "navigation";
	public static final String KEY_CONSTRAINT_BEHAVIOR = "constraint_behavior";
	public static final String KEY_TIMEOUT = "30";

	
	private EditTextPreference mSubmissionUrlPreference;
	private EditTextPreference mFormListUrlPreference;
	private EditTextPreference mServerUrlPreference;	
	private ListPreference mFontSizePreference;
	private ListPreference mNavigationPreference;
	
	private ListPreference mAutosendWifiPreference;
	private ListPreference mAutosendNetworkPreference;
	private ListPreference mProtocolPreference;
	
	private boolean adminMode;
	private SharedPreferences adminPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		setTitle(getString(R.string.app_name) + " > "
				+ getString(R.string.general_preferences));

		// not super safe, but we're just putting in this mode to help
		// administrate
		// would require code to access it
		adminMode = getIntent().getBooleanExtra("adminMode", false);

		adminPreferences = getSharedPreferences(
				AdminPreferencesActivity.ADMIN_PREFERENCES, 0);
		
		
		updateAutoSendWithWifiCategory();
		updateAutoSendWithNetworkCategory();
		updateNavigationCategory();
		updateFontSizeCategory();
		updateFinalizedFormCategory();
		updateServerUrlCategory();
		updateFormUploadDownloadUrlCategory();
		updateLanguageCategory();

/*		if (!(autosendNetworkAvailable || autosendWifiAvailable || adminMode)) {
			getPreferenceScreen().removePreference(autosendCategory);
		}
*/
		//TODO don't no will be worked
		
		/*if (!(serverAvailable || urlAvailable || usernameAvailable || passwordAvailable
				|| googleAccountAvailable || adminMode)) {
			getPreferenceScreen().removePreference(serverCategory);
		}*/

		
		/*if (!(fontAvailable || defaultAvailable
			|| navigationAvailable || adminMode)) {
			getPreferenceScreen().removePreference(clientCategory);
		   }*/
		//TODO

	}

	private void disableFeaturesInDevelopment() {
		// remove Google Collections from protocol choices in preferences
		ListPreference protocols = (ListPreference) findPreference(KEY_PROTOCOL);
		int i = protocols.findIndexOfValue(PROTOCOL_GOOGLE);
		if (i != -1) {
			CharSequence[] entries = protocols.getEntries();
			CharSequence[] entryValues = protocols.getEntryValues();

			CharSequence[] newEntries = new CharSequence[entryValues.length - 1];
			CharSequence[] newEntryValues = new CharSequence[entryValues.length - 1];
			for (int k = 0, j = 0; j < entryValues.length; ++j) {
				if (j == i)
					continue;
				newEntries[k] = entries[j];
				newEntryValues[k] = entryValues[j];
				++k;
			}

			protocols.setEntries(newEntries);
			protocols.setEntryValues(newEntryValues);
		}
	}

	private void updateAutoSendWithWifiCategory(){
		PreferenceCategory autosendCategory = (PreferenceCategory) findPreference(getString(R.string.autosend));
		mAutosendWifiPreference = (ListPreference) findPreference(KEY_AUTOSEND_WIFI);
		boolean autosendWifiAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_AUTOSEND_WIFI, true);
		if (!(autosendWifiAvailable || adminMode)) {
			autosendCategory.removePreference(mAutosendWifiPreference);
		}
		mAutosendWifiPreference.setSummary(mAutosendWifiPreference.getEntry());
		mAutosendWifiPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int index = ((ListPreference) preference)
						.findIndexOfValue(newValue.toString());
				String entry = (String) ((ListPreference) preference)
						.getEntries()[index];
				((ListPreference) preference).setSummary(entry);
				return true;
			}
		});
	}
		
	private void updateAutoSendWithNetworkCategory(){
		PreferenceCategory autosendCategory = (PreferenceCategory) findPreference(getString(R.string.autosend));
		mAutosendNetworkPreference = (ListPreference) findPreference(KEY_AUTOSEND_NETWORK);
		boolean autosendNetworkAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_AUTOSEND_NETWORK, true);
		if (!(autosendNetworkAvailable || adminMode)) {
			autosendCategory.removePreference(mAutosendNetworkPreference);
		}
		
		mAutosendNetworkPreference.setSummary(mAutosendNetworkPreference.getEntry());
		mAutosendNetworkPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int index = ((ListPreference) preference)
						.findIndexOfValue(newValue.toString());
				String entry = (String) ((ListPreference) preference)
						.getEntries()[index];
				((ListPreference) preference).setSummary(entry);
				return true;
			}
		});
	}
	
	private void updateNavigationCategory(){
		PreferenceCategory clientCategory = (PreferenceCategory) findPreference(getString(R.string.client));
	
		boolean navigationAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_NAVIGATION, true);
		mNavigationPreference = (ListPreference) findPreference(KEY_NAVIGATION);
		mNavigationPreference.setSummary(mNavigationPreference.getEntry());
		mNavigationPreference
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
	
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						int index = ((ListPreference) preference)
								.findIndexOfValue(newValue.toString());
						String entry = (String) ((ListPreference) preference)
								.getEntries()[index];
						((ListPreference) preference).setSummary(entry);
						return true;
					}
				});
		if (!(navigationAvailable || adminMode)) {
			clientCategory.removePreference(mNavigationPreference);
		}
	}
	
	
	private void updateFontSizeCategory(){
		
		PreferenceCategory clientCategory = (PreferenceCategory) findPreference(getString(R.string.client));
		boolean fontAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_CHANGE_FONT_SIZE, true);
		mFontSizePreference = (ListPreference) findPreference(KEY_FONT_SIZE);
		mFontSizePreference.setSummary(mFontSizePreference.getEntry());
		mFontSizePreference
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
	
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						int index = ((ListPreference) preference)
								.findIndexOfValue(newValue.toString());
						String entry = (String) ((ListPreference) preference)
								.getEntries()[index];
						((ListPreference) preference).setSummary(entry);
						return true;
					}
				});
		if (!(fontAvailable || adminMode)) {
			clientCategory.removePreference(mFontSizePreference);
		}
	}
	
	private void updateFinalizedFormCategory(){
		
		PreferenceCategory clientCategory = (PreferenceCategory) findPreference(getString(R.string.client));
		boolean defaultAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_DEFAULT_TO_FINALIZED, true);
	
		Preference defaultFinalized = findPreference(KEY_COMPLETED_DEFAULT);
		if (!(defaultAvailable || adminMode)) {
			clientCategory.removePreference(defaultFinalized);
		}
	}
	
	
	private void updateServerUrlCategory(){
		
		boolean serverAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_CHANGE_SERVER, true);
		boolean urlAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_CHANGE_URL, true);
		PreferenceCategory serverCategory = (PreferenceCategory) findPreference(getString(R.string.server_preferences));
	
		mServerUrlPreference = (EditTextPreference) findPreference(KEY_SERVER_URL);
		mServerUrlPreference
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						String url = newValue.toString();
	
						// remove all trailing "/"s
						while (url.endsWith("/")) {
							url = url.substring(0, url.length() - 1);
						}
	
						if (UrlUtils.isValidUrl(url)) {
							preference.setSummary(newValue.toString());
							Log.d(PreferencesActivity.class.getSimpleName(), "url = " + url);
							return true;
						} else {
							Toast.makeText(getApplicationContext(),
									R.string.url_error, Toast.LENGTH_SHORT)
									.show();
							return false;
						}
					}
				});
		mServerUrlPreference.setSummary(mServerUrlPreference.getText());
		mServerUrlPreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter() });
	
		
	
		if (!(serverAvailable || adminMode)) {
			Preference protocol = findPreference(KEY_PROTOCOL);
			serverCategory.removePreference(protocol);
		} else {
			// this just removes the value from protocol, but protocol doesn't
			// exist if we take away access
			disableFeaturesInDevelopment();
		}
		
		if (!(urlAvailable || adminMode)) {
			serverCategory.removePreference(mServerUrlPreference);
		}	
	}
	
	private void updateFormUploadDownloadUrlCategory(){
		
		PreferenceCategory serverCategory = (PreferenceCategory) findPreference(getString(R.string.server_preferences));
	
		// declared early to prevent NPE in toggleServerPaths
		mFormListUrlPreference = (EditTextPreference) findPreference(KEY_FORMLIST_URL);
		mSubmissionUrlPreference = (EditTextPreference) findPreference(KEY_SUBMISSION_URL);
	
		mProtocolPreference = (ListPreference) findPreference(KEY_PROTOCOL);
		mProtocolPreference.setSummary(mProtocolPreference.getEntry());
		if (mProtocolPreference.getValue().equals(PROTOCOL_CLIENT_COLLECTION_DEFAULT)) {
			mFormListUrlPreference.setEnabled(false);
			mSubmissionUrlPreference.setEnabled(false);
		} else {
			mFormListUrlPreference.setEnabled(true);
			mSubmissionUrlPreference.setEnabled(true);
		}
		mProtocolPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
	
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				int index = ((ListPreference) preference)
						.findIndexOfValue(newValue.toString());
				String entry = (String) ((ListPreference) preference)
						.getEntries()[index];
				String value = (String) ((ListPreference) preference)
						.getEntryValues()[index];
				((ListPreference) preference).setSummary(entry);
				if (value.equals(PROTOCOL_CLIENT_COLLECTION_DEFAULT)) {
					mFormListUrlPreference.setEnabled(false);
					mFormListUrlPreference.setText(getString(R.string.default_clientcollection_formlist));
					mFormListUrlPreference.setSummary(mFormListUrlPreference.getText());
					mSubmissionUrlPreference.setEnabled(false);
					mSubmissionUrlPreference.setText(getString(R.string.default_clientcollection_submission));
					mSubmissionUrlPreference.setSummary(mSubmissionUrlPreference.getText());
				} else {
					mFormListUrlPreference.setEnabled(true);
					mSubmissionUrlPreference.setEnabled(true);
				}
				
				return true;
			}
		});
		
		boolean serverAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_CHANGE_SERVER, true);
		
		mFormListUrlPreference.setOnPreferenceChangeListener(this);
		mFormListUrlPreference.setSummary(mFormListUrlPreference.getText());
		mServerUrlPreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter(), getWhitespaceFilter() });
		if (!(serverAvailable || adminMode)) {
			serverCategory.removePreference(mFormListUrlPreference);
		}

		mSubmissionUrlPreference.setOnPreferenceChangeListener(this);
		mSubmissionUrlPreference.setSummary(mSubmissionUrlPreference.getText());
		mServerUrlPreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter(), getWhitespaceFilter() });
		if (!(serverAvailable || adminMode)) {
			serverCategory.removePreference(mSubmissionUrlPreference);
		}
	}
	
	private void updateLanguageCategory(){
		//TODO
		PreferenceCategory category = (PreferenceCategory)findPreference(getResources().getString(R.string.language_setting));
		ListPreference mLanguage = (ListPreference)findPreference(KEY_LANGUAGE_SETTING);
		mLanguage.setSummary(getResources().getString(R.string.language_setting_summary, mLanguage.getEntry()));
		mLanguage.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Log.d(PreferencesActivity.class.getSimpleName(), "NewValue = " + newValue.toString());
				//Set Summary  
				int index = ((ListPreference)preference).findIndexOfValue(newValue.toString());
				String entry = (String)((ListPreference)preference).getEntries()[index];
				((ListPreference) preference).setSummary(getResources().getString(R.string.language_setting_summary, entry));
				
				setSelectedLanguage(newValue.toString());
				
				return true;
			}
		});
	}
	
	private void setSelectedLanguage(String languageValue){
		ClientCollection.getInstance().setSelectedLanguage(languageValue);
		}
		
	/*
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (resultCode == RESULT_CANCELED) {
			// request was canceled, so do nothing
			return;
		}

		switch (requestCode) {
		case IMAGE_CHOOSER:
			String sourceImagePath = null;

			// get gp of chosen file
			Uri uri = intent.getData();
			if (uri.toString().startsWith("file")) {
				sourceImagePath = uri.toString().substring(6);
			} else {
				String[] projection = { Images.Media.DATA };
				Cursor c = null;
				try {
					c = getContentResolver().query(uri, projection, null, null,
							null);
					int i = c.getColumnIndexOrThrow(Images.Media.DATA);
					c.moveToFirst();
					sourceImagePath = c.getString(i);
				} finally {
					if (c != null) {
						c.close();
					}
				}
			}

			// setting image path
			setSplashPath(sourceImagePath);
			break;
		}
	}

*/	/**
	 * Disallows whitespace from user entry
	 * 
	 * @return
	 */
	private InputFilter getWhitespaceFilter() {
		InputFilter whitespaceFilter = new InputFilter() {
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				for (int i = start; i < end; i++) {
					if (Character.isWhitespace(source.charAt(i))) {
						return "";
					}
				}
				return null;
			}
		};
		return whitespaceFilter;
	}

	/**
	 * Disallows carriage returns from user entry
	 * 
	 * @return
	 */
	private InputFilter getReturnFilter() {
		InputFilter returnFilter = new InputFilter() {
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				for (int i = start; i < end; i++) {
					if (Character.getType((source.charAt(i))) == Character.CONTROL) {
						return "";
					}
				}
				return null;
			}
		};
		return returnFilter;
	}

	/**
	 * Generic listener that sets the summary to the newly selected/entered
	 * value
	 */
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		preference.setSummary((CharSequence) newValue);
		return true;
	}

}
