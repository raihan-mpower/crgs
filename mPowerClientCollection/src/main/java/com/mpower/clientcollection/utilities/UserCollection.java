package com.mpower.clientcollection.utilities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mpower.clientcollection.activities.ClientCollectionLoginActivity;
import com.mpower.clientcollection.application.ClientCollection;
import com.mpower.clientcollection.preferences.PreferencesActivity;

public class UserCollection {

	public static final String BROADCAST_ACTION_AUTHENTICATION_DONE = "com.mpower.clientcollection.android.broadcast.AUTHENTICATION_DONE";
	public static final String BROADCAST_ACTION_AUTHENTICATION_NEEDED = "com.mpower.clientcollection.android.broadcast.AUTHENTICATION_NEEDED";

	public static final String LOGOUT_MESSAGE_NETWORK = "Internet not available.";
	public static final String LOGOUT_MESSAGE_NETWORK_SERVER = "Internet not available or server caused an error.";
	public static final String LOGOUT_MESSAGE_ID_MISSMATCH = "ID/password mismatch.";
	public static final String LOGOUT_MESSAGE_SERVER_ERROR = "Server unreachable or caused an error.";
	public static final String LOGOUT_MESSAGE_SESSION_EXPIRED = "Session expired.";
	public static final String LOGOUT_MESSAGE_INTERNAL_ERROR = "Internal application error.";
	public static final String LOGOUT_MESSAGE_USER_REQUEST = "Logged out.";
	public static final String LOGOUT_MESSAGE_UNKNOWN = "Unknown error.";

	private static boolean OFFLINE_LOGIN_DEFAULT = false;

	private static volatile UserCollection instance = null;

	private volatile UserDataCollection mUserData = null;
	private volatile boolean mLoggedin = false;

	private UserCollection() {
		mUserData = new UserDataCollection();
	}

	private void loginFinalCheck(boolean initialLoginPassed, String logoutMessage) {
		if (initialLoginPassed) {
			if ((mUserData.getUsername().length() > 0) && (mUserData.getPassword().length() > 0)) {
				login();
			} else {
				logOff(LOGOUT_MESSAGE_INTERNAL_ERROR);
			}
		} else {
			logOff(logoutMessage);
		}
	}

	public boolean isLoggedin() {
		return mLoggedin;
	}

	public void setLoginResult(boolean succeed, UserDataCollection ld, String logoutMessage) {
		if (succeed && (ld != null)) {
			mUserData = ld;
			loginFinalCheck(true, null);
		} else {
			loginFinalCheck(false, logoutMessage);
		}
	}

	public boolean checkOfflineLogin(String username, String password) throws Exception {
		boolean retVal = false;

		if ("".equals(username.trim()) || "".equals(password.trim())) {
			return retVal;
		}

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ClientCollection.getAppContext());
		String storedUsername = settings.getString(PreferencesActivity.KEY_USERNAME, null);
		String storedPassword = settings.getString(PreferencesActivity.KEY_PASSWORD, null);

		if (username.equals(storedUsername) && password.equals(storedPassword)) {
			retVal = true;
		}

		return retVal;
	}

	public UserDataCollection extractOfflineLoginData() {

		UserDataCollection lr = new UserDataCollection();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ClientCollection.getAppContext());
		String storedUsername = settings.getString(PreferencesActivity.KEY_USERNAME, null);
		String storedPassword = settings.getString(PreferencesActivity.KEY_PASSWORD, null);

		lr.setUsername(storedUsername);
		lr.setPassword(storedPassword);

		return lr;
	}

	public boolean offlineUserDataAvailable() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ClientCollection.getAppContext());
		String storedUsername = settings.getString(PreferencesActivity.KEY_USERNAME, null);
		String storedPassword = settings.getString(PreferencesActivity.KEY_PASSWORD, null);

		if ((storedUsername != null) && (storedPassword != null)) {
			if ((storedUsername.length() > 0) && (storedPassword.length() > 0)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public void checkLogin(String u, String p, ClientCollectionLoginActivity activityInstance) throws Exception {

		boolean offlineUserDataAvailable = offlineUserDataAvailable();

		if (OFFLINE_LOGIN_DEFAULT) {
			// Offline login default
			if (offlineUserDataAvailable) {
				// Login offline if data is available
				if (checkOfflineLogin(u, p)) {
					UserDataCollection ld = extractOfflineLoginData();
					setLoginResult(true, ld, null);
				} else {
					// Login doesn't match with offline data, check online
					if (NetUtils.isConnected(ClientCollection.getAppContext())) {
						validateLoginFromServer(activityInstance);
					} else {
						loginFinalCheck(false, LOGOUT_MESSAGE_ID_MISSMATCH + "\n" + LOGOUT_MESSAGE_NETWORK);
					}
				}
			} else {
				// User data is not available, proceed with regular login
				if (NetUtils.isConnected(ClientCollection.getAppContext())) {
					validateLoginFromServer(activityInstance);
				} else {
					loginFinalCheck(false, LOGOUT_MESSAGE_NETWORK);
				}
			}

		} else {
			// Online login default
			if (NetUtils.isConnected(ClientCollection.getAppContext())) {
				validateLoginFromServer(activityInstance);
			} else {
				// Network not available
				if (offlineUserDataAvailable) {
					if (checkOfflineLogin(u, p)) {
						UserDataCollection ld = extractOfflineLoginData();
						setLoginResult(true, ld, null);
					} else {
						loginFinalCheck(false, LOGOUT_MESSAGE_ID_MISSMATCH + "\n" + LOGOUT_MESSAGE_NETWORK);
					}
				} else {
					loginFinalCheck(false, LOGOUT_MESSAGE_NETWORK);
				}
			}
		}

	}

	private void validateLoginFromServer(ClientCollectionLoginActivity activityInstance) {
		activityInstance.checkLoginOnline();
	}

	public void logOff(String logoutMessage) {
		mUserData.resetAll();
		mLoggedin = false;

		WebUtils.clearAllCredentials();

		Intent broadcastIntent = new Intent();
		broadcastIntent.putExtra("message", logoutMessage);
		broadcastIntent.setAction(BROADCAST_ACTION_AUTHENTICATION_NEEDED);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		ClientCollection.getAppContext().sendBroadcast(broadcastIntent);
	}

	public void logOffAndClearCache() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ClientCollection.getAppContext());

		settings.edit().putString(PreferencesActivity.KEY_USERNAME, null).commit();
		settings.edit().putString(PreferencesActivity.KEY_PASSWORD, null).commit();

		logOff(LOGOUT_MESSAGE_SESSION_EXPIRED);
	}

	private void login() {

		mLoggedin = true;

		// Let all broadcast receivers know that we have logged in
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(BROADCAST_ACTION_AUTHENTICATION_DONE);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		ClientCollection.getAppContext().sendBroadcast(broadcastIntent);

		WebUtils.clearAllCredentials();
		WebUtils.addCredentials(mUserData.getUsername(), mUserData.getPassword());
		
		ClientCollection.getInstance().syncNotificationData();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ClientCollection.getAppContext());

		settings.edit().putString(PreferencesActivity.KEY_USERNAME, mUserData.getUsername()).commit();
		settings.edit().putString(PreferencesActivity.KEY_PASSWORD, mUserData.getPassword()).commit();
	}

	public UserDataCollection getUserData() {
		return mUserData;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public static UserCollection getInstance() {
		if (instance == null) {
			synchronized (UserCollection.class) {
				if (instance == null) {
					instance = new UserCollection();
				}
			}
		}
		return instance;
	}

}
