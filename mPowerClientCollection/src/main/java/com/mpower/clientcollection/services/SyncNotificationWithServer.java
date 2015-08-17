package com.mpower.clientcollection.services;

import java.util.ArrayList;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.mpower.clientcollection.application.ClientCollection;
import com.mpower.clientcollection.models.MessageInfos;
import com.mpower.clientcollection.utilities.NetUtils;

public class SyncNotificationWithServer extends IntentService{

	public SyncNotificationWithServer() {
		super("ClientCollection");		
	}

	@Override
	protected void onHandleIntent(Intent intent) {
			if(NetUtils.isConnected(ClientCollection.getAppContext())){
				NotificationService notifyService = new NotificationService();
				ArrayList<MessageInfos> notifyMap = notifyService.getNotification();
				if(notifyMap != null && notifyMap.size()>0)
					processResult(notifyMap);	
			}	
	}
	
	private void processResult(ArrayList<MessageInfos> notifyInfo){
		final String retVal = new Gson().toJson(notifyInfo);
		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(new Runnable() {
			
			@Override
			public void run() {
				Intent notificationIntent = new Intent();
				notificationIntent.setAction(NotificationService.BROADCAST_ACTION_DISPLAY_NOTIFICATION);
				notificationIntent.addCategory(Intent.CATEGORY_DEFAULT);
				notificationIntent.putExtra("notification",
						retVal);
				ClientCollection.getAppContext().sendBroadcast(notificationIntent);
			}
		});
	}

}
