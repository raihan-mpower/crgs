package com.mpower.clientcollection.receivers;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mpower.clientcollection.activity.R;
import com.mpower.clientcollection.listeners.InstanceUploaderListener;
import com.mpower.clientcollection.preferences.PreferencesActivity;
import com.mpower.clientcollection.provider.InstanceProviderAPI;
import com.mpower.clientcollection.provider.InstanceProviderAPI.InstanceColumns;
import com.mpower.clientcollection.tasks.InstanceUploaderTask;
import com.mpower.clientcollection.utilities.WebUtils;

public class NetworkReceiver extends BroadcastReceiver implements InstanceUploaderListener {

    // turning on wifi often gets two CONNECTED events. we only want to run one thread at a time
    public static boolean running = false;
    InstanceUploaderTask mInstanceUploaderTask;

   @Override
	public void onReceive(Context context, Intent intent) {
        // make sure sd card is ready, if not don't try to send
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return;
        }
        
		String action = intent.getAction();

		NetworkInfo currentNetworkInfo = (NetworkInfo) intent
				.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

		if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			if (currentNetworkInfo.getState() == NetworkInfo.State.CONNECTED) {
				if (interfaceIsEnabled(context, currentNetworkInfo)) {
					uploadForms(context);
				}
			}
		} else if (action.equals("com.mpower.clientcollection.FormSaved")) {
			ConnectivityManager connectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

			if (ni == null || !ni.isConnected()) {
				// not connected, do nothing
			} else {
				if (interfaceIsEnabled(context, ni)) {
					uploadForms(context);
				}
			}
		}
	}

	private boolean interfaceIsEnabled(Context context,
			NetworkInfo currentNetworkInfo) {
		// make sure autosend is enabled on the given connected interface
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		String sendwifi = sharedPreferences.getString(
				PreferencesActivity.KEY_AUTOSEND_WIFI, PreferencesActivity.KEY_AUTOSEND_WIFI_DEFAULT);
		String sendnetwork = sharedPreferences.getString(
				PreferencesActivity.KEY_AUTOSEND_NETWORK, PreferencesActivity.KEY_AUTOSEND_NETWORK_DEFAULT);

		return (currentNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI
				&& !sendwifi.equalsIgnoreCase(PreferencesActivity.KEY_AUTOSEND_WIFI_DEFAULT) || currentNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE
				&& !sendnetwork.equalsIgnoreCase(PreferencesActivity.KEY_AUTOSEND_NETWORK_DEFAULT));
	}


    private void uploadForms(Context context) {
        if (!running) {
            running = true;

          /*  String selection = InstanceColumns.STATUS + "=? or " + InstanceColumns.STATUS + "=?";
            String selectionArgs[] =
                {
                        InstanceProviderAPI.STATUS_COMPLETE,
                        InstanceProviderAPI.STATUS_SUBMISSION_FAILED
                };

            Cursor c =
                context.getContentResolver().query(InstanceColumns.CONTENT_URI, null, selection,
                    selectionArgs, null);
           */
            Cursor c = createQueryCursor(context);
            
            ArrayList<Long> toUpload = new ArrayList<Long>();
            if (c != null && c.getCount() > 0) {
                c.move(-1);
                while (c.moveToNext()) {
                    Long l = c.getLong(c.getColumnIndex(InstanceColumns._ID));
                    toUpload.add(Long.valueOf(l));
                }
                
                addCredentials(context);
                
                mInstanceUploaderTask = new InstanceUploaderTask();
                mInstanceUploaderTask.setUploaderListener(this);

                Long[] toSendArray = new Long[toUpload.size()];
                toUpload.toArray(toSendArray);
                mInstanceUploaderTask.execute(toSendArray);
            } else {
                running = false;
            }
        }
    }
    
    private Cursor createQueryCursor(Context context){
        String selection = InstanceColumns.STATUS + "=? or " + InstanceColumns.STATUS + "=?";
        String selectionArgs[] =
            {
                    InstanceProviderAPI.STATUS_COMPLETE,
                    InstanceProviderAPI.STATUS_SUBMISSION_FAILED
            };
        String sortOrder = getQuerySortOrder(context);
        
        Log.d("NetworkReceiver", "SortOrder = " + sortOrder);
        Cursor c =
            context.getContentResolver().query(InstanceColumns.CONTENT_URI, null, selection,
                selectionArgs, sortOrder);
    	return c;
    }
    
    private String getQuerySortOrder(Context context){
    	
    	 SharedPreferences setting = PreferenceManager.getDefaultSharedPreferences(context);
         String wifiParameter = setting.getString(PreferencesActivity.KEY_AUTOSEND_WIFI, PreferencesActivity.KEY_AUTOSEND_WIFI_DEFAULT);
         String networkParameter = setting.getString(PreferencesActivity.KEY_AUTOSEND_NETWORK, PreferencesActivity.KEY_AUTOSEND_NETWORK_DEFAULT);
         String sortOrder = null;
         if(!wifiParameter.equalsIgnoreCase(PreferencesActivity.KEY_AUTOSEND_WIFI_DEFAULT)){ // default is sending option is disable
        	 sortOrder = getQuerySortOrder(wifiParameter);         	
         }else if(!networkParameter.equalsIgnoreCase(PreferencesActivity.KEY_AUTOSEND_NETWORK_DEFAULT)){ // default is sending option is disable
        	 sortOrder = getQuerySortOrder(networkParameter);
         }
         return sortOrder;
    }
    
    private String getQuerySortOrder(String parameter){
    	
    	String sortOrder = null;
    	if(parameter.equalsIgnoreCase(PreferencesActivity.KEY_AUTOSEND_BY_DATE)){
    		 sortOrder = InstanceColumns.LAST_STATUS_CHANGE_DATE + " ASC";
    	}else if(parameter.equalsIgnoreCase(PreferencesActivity.KEY_AUTOSEND_BY_SIZE)){
    		 sortOrder = InstanceColumns.FILE_SIZE + " ASC";        		
    	}else if(parameter.equalsIgnoreCase(PreferencesActivity.KEY_AUTOSEND_BY_PRIORITY)){
    		 sortOrder = InstanceColumns.PRIORITY_VALUE + " ASC";
    	}
    	return sortOrder;
    }

    private void addCredentials(Context context){
        // get the username, password, and server from preferences
        SharedPreferences settings =
                PreferenceManager.getDefaultSharedPreferences(context);

        String storedUsername = settings.getString(PreferencesActivity.KEY_USERNAME, null);
        String storedPassword = settings.getString(PreferencesActivity.KEY_PASSWORD, null);
        String server = settings.getString(PreferencesActivity.KEY_SERVER_URL,
                context.getString(R.string.default_server_url));
        String url = server
                + settings.getString(PreferencesActivity.KEY_FORMLIST_URL,
                        context.getString(R.string.default_clientcollection_formlist));

        Uri u = Uri.parse(url);
        WebUtils.addCredentials(storedUsername, storedPassword, u.getHost());

    }
    
    @Override
    public void uploadingComplete(HashMap<String, String> result) {
        // task is done
        mInstanceUploaderTask.setUploaderListener(null);
        running = false;
    }


    @Override
    public void progressUpdate(int progress, int total) {
        // do nothing
    }


    @Override
    public void authRequest(Uri url, HashMap<String, String> doneSoFar) {
        // if we get an auth request, just fail
        mInstanceUploaderTask.setUploaderListener(null);
        running = false;
    }

}