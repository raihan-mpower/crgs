package com.mpower.clientcollection.utilities;

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.mpower.clientcollection.activities.MsgDetailActivity;
import com.mpower.clientcollection.activity.R;
import com.mpower.clientcollection.application.ClientCollection;
import com.mpower.clientcollection.models.MessageInfos;

public class NotificationUtils {
	// Any Unique number
	private static int NETWORK_ACTIVITY_ID = 99039903;
	private static int NOTIFICATION_ID = 1;
	private static int NUMBER_OF_MESSAGE = 0;

	public static void setNetworkIndicator(boolean state) {

		Context context = ClientCollection.getAppContext();
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		if (!state) {
			//Hide icon
			nm.cancel(NETWORK_ACTIVITY_ID);
			return;
		}

		PendingIntent pI = PendingIntent.getActivity(context, 0, new Intent(),
				PendingIntent.FLAG_UPDATE_CURRENT);
		
		Notification n = new Notification(R.drawable.stat_notify_sync, null, System.currentTimeMillis());
		
		n.setLatestEventInfo(context, context.getString(R.string.app_name), context.getString(R.string.data_transfer_in_progress_), pI);
		n.flags |= Notification.FLAG_ONGOING_EVENT;
		n.flags |= Notification.FLAG_NO_CLEAR;
		
		//Show icon
		nm.notify(NETWORK_ACTIVITY_ID, n);
	}
			
	public static void showNotification(ArrayList<MessageInfos> infos){
		if(infos != null && infos.size() >0){
			for(MessageInfos info : infos){
				String notificationMessage = info.getNotificationMsg();
				Context context = ClientCollection.getAppContext();
				NUMBER_OF_MESSAGE = NUMBER_OF_MESSAGE + 1;
				notificationMessage += " (" + NUMBER_OF_MESSAGE + ")";
		
				int defaults = 0;
				//defaults |= Notification.DEFAULT_VIBRATE;
				defaults |= Notification.DEFAULT_SOUND;	
				
				NotificationCompat.Builder mBuilder =
				        new NotificationCompat.Builder(context)
				        .setSmallIcon(R.drawable.notifications)				       
				        .setAutoCancel(true)
				        .setDefaults(defaults)
				        .setTicker(info.getNotificationTitle())
				        .setWhen(System.currentTimeMillis())
				        .setLights(0xffff0000, 300, 1000)
				        .setContentTitle(context.getString(R.string.app_name))
				        .setContentText(notificationMessage);	
				
				Intent notifyIntent = new Intent(context, MsgDetailActivity.class);
				notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK /*| Intent.FLAG_ACTIVITY_CLEAR_TASK*/);
				notifyIntent.addCategory("android.intent.category.DEFAULT");
				notifyIntent.putExtra("hhId", info.getHhid());
						
				PendingIntent resultPendingIntent =
				        PendingIntent.getActivity(
				        context,
				        0,
				        notifyIntent,
				        PendingIntent.FLAG_UPDATE_CURRENT
				);
				mBuilder.setContentIntent(resultPendingIntent);
				
				NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				// NOTIFICATION_ID allows you to update the notification later on.
				mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
			}

		}
	}
	
	public static void showLargeNotification(){
		Context context = ClientCollection.getAppContext();
		int mId = 1;
		long[] vibration = {300, 2000};
		
		int defaults = 0;
		//defaults |= Notification.DEFAULT_VIBRATE;
		defaults |= Notification.DEFAULT_SOUND;	
	
		
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
	    .setSmallIcon(R.drawable.notifications)
	    .setContentTitle("Event tracker")
	    .setContentText("Events received")
	    .setVibrate(vibration)
		.setDefaults(defaults)
		.setTicker("New Msg")
		.setWhen(System.currentTimeMillis())
		.setLights(0xffff0000, 300, 1000);
		      
	   
		NotificationCompat.InboxStyle inboxStyle =
		        new NotificationCompat.InboxStyle();
		String[] events = new String[3];
		events[0] = "SSSSSSSSSSSSSSSSTTTTTTTTTTTtt";
		events[1] = "TTTTTTTTtt";
		events[2] = "SSSTTTTTtt";
		
		inboxStyle.setBigContentTitle("Event tracker details:");
		
		for (int i=0; i < events.length; i++) {

		    inboxStyle.addLine(events[i]);
		}
		
		mBuilder.setStyle(inboxStyle);

		PendingIntent resultPendingIntent =
		        PendingIntent.getActivity(
		        context,
		        0,
		        new Intent(),
		        PendingIntent.FLAG_UPDATE_CURRENT
		);
		mBuilder.setContentIntent(resultPendingIntent);
		
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(mId, mBuilder.build());
	}
	
	}
