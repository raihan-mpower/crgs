
package com.mpower.clientcollection.services;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mpower.clientcollection.activity.R;
import com.mpower.clientcollection.application.ClientCollection;
import com.mpower.clientcollection.database.MpowerDatabaseHelper;
import com.mpower.clientcollection.models.MessageInfos;
import com.mpower.clientcollection.preferences.PreferencesActivity;
import com.mpower.clientcollection.utilities.NetUtils;
import com.mpower.clientcollection.utilities.UserCollection;


public class NotificationService {

	public static final String BROADCAST_ACTION_DISPLAY_NOTIFICATION = "com.mpower.clientcollection.broadcast.DISPLAY_NOTIFICATION";
	private String url;
	private int timeOut;
	private boolean isConnected = false;
	private Exception getE;
	private static final String TAG = NotificationService.class.getSimpleName();
	private volatile String getResponse = "";
	private volatile int getHTTPStatus = 0;
	

	public String prepareGetUrl() {
		init();
		MpowerDatabaseHelper dh = new MpowerDatabaseHelper(ClientCollection.getAppContext());
		String finalUrl = url;
		if (!finalUrl.endsWith("?"))
			finalUrl += "?";
		
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		String notificationId = dh.getMaxNotificationServerId();	
			params.add(new BasicNameValuePair("alertID", notificationId));
			params.add(new BasicNameValuePair("userID", getUserName()));
	
		String paramString = URLEncodedUtils.format(params, "utf-8");

		finalUrl += paramString;

		return finalUrl;
	}

	private void init() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ClientCollection.getAppContext());
		url = prefs.getString(PreferencesActivity.KEY_SERVER_URL, ClientCollection
				.getAppContext().getResources().getString(R.string.url))
				+ NetUtils.URL_PART_NOTIFICATION;
		timeOut = Integer.parseInt(prefs.getString(
				PreferencesActivity.KEY_TIMEOUT, ClientCollection.getAppContext()
						.getResources().getString((R.string.timeout)))) * 1000;
		isConnected = NetUtils.isConnected(ClientCollection.getAppContext());
	}

	public ArrayList<MessageInfos> getNotification() {
	
		ArrayList<MessageInfos> notificationMap = null;

		try {

			String finalUrl = prepareGetUrl();
			Log.d("NotificationService", "finalUrl = " + finalUrl);
			
			if(isConnected){
				
				HttpResponse response = NetUtils.stringResponseGet(finalUrl, NetUtils.getHttpContext(), NetUtils
						.createHttpClient(timeOut));
				getHTTPStatus = response.getStatusLine().getStatusCode();
				HttpEntity entity = response.getEntity();
	
				Log.d("getNotificationData Status", "getHTTPStatus code = "
						+ getHTTPStatus);

				if ((entity != null) && (getHTTPStatus == 200)) {
	
					getResponse = EntityUtils.toString(entity, HTTP.UTF_8);
					Log.d("getNotificationData JSON", getResponse);
					
					if(getResponse.length()>0){
						Type collectionType = new TypeToken<ArrayList<MessageInfos>>(){}.getType();
						notificationMap = new Gson().fromJson(getResponse, collectionType);
						inserDataIntoDatabase(notificationMap);
						
					}else{
						Log.d(TAG, "No data found");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			getE = e;
			Log.d("NotificationService","getE = " + getE);
		}
		
		return notificationMap;
	}
	
	private String getUserName(){
		String userName = null;
		if(UserCollection.getInstance().getUserData().getUsername() != null)
		{
			userName = UserCollection.getInstance().getUserData().getUsername();
		}else{
			SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(ClientCollection.getAppContext());
			userName = preference.getString(PreferencesActivity.KEY_USERNAME, null);
		}
		return userName;
	}
	
	
	private void inserDataIntoDatabase(ArrayList<MessageInfos> notificationMap){
		MpowerDatabaseHelper dh = new MpowerDatabaseHelper(ClientCollection.getAppContext());
		try {			
				dh.insertMessagesInfo(notificationMap);			
		} catch (Exception e) {
			e.printStackTrace();
		}
		dh.close();
	
	 }
}
 