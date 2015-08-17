package com.mpower.clientcollection.services;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mpower.clientcollection.activity.R;
import com.mpower.clientcollection.application.ClientCollection;
import com.mpower.clientcollection.models.MessageInfos;
import com.mpower.clientcollection.preferences.PreferencesActivity;
import com.mpower.clientcollection.utilities.DeviceIdMax23;
import com.mpower.clientcollection.utilities.NotificationUtils;
import com.mpower.clientcollection.utilities.UserCollection;


/**
 * PushService - Service to manage push notifications
 * 
 * @author Mehdi Hasan <mhasan@mpower-health.com>
 * @author Ratna <ratna@mpower-health.com>
 * 
 * 
 */
public class PushService extends Service {

	private static final String TAG = "PushService";

	private static final String BROADCAST_ACTION_PUSH = "com.mpower.clientcollection.services.PushService";

	// private static final String MQTT_TEST_HOST = "m2m.eclipse.org";
	private static final int MQTT_BROKER_PORT_NUM = 1886;
	private static final boolean MQTT_CLEAN_START = true;
	// In seconds
	private static final int MQTT_KEEP_ALIVE = 240;
	/**
	 * <li>Quality of service 0 - indicates that a message should be delivered
	 * at most once (zero or one times). The message will not be persisted to
	 * disk, and will not be acknowledged across the network. This QoS is the
	 * fastest, but should only be used for messages which are not valuable -
	 * note that if the server cannot process the message (for example, there is
	 * an authorization problem), then an exception will <em>not</em> be thrown,
	 * nor will a call be made to
	 * {@link MqttCallback#deliveryFailed(MqttDeliveryToken, MqttException)} or
	 * {@link MqttCallback#deliveryComplete(MqttDeliveryToken)}. Also known as
	 * "fire and forget".</li>
	 * 
	 * <li>Quality of service 1 - indicates that a message should be delivered
	 * at least once (one or more times). The message can only be delivered
	 * safely if it can be persisted, so the application must supply a means of
	 * persistence using <code>MqttConnectOptions</code>. If a persistence
	 * mechanism is not specified, the message will not be delivered in the
	 * event of a client failure. The message will be acknowledged across the
	 * network. This is the default QoS.</li>
	 * 
	 * <li>Quality of service 2 - indicates that a message should be delivered
	 * once. The message will be persisted to disk, and will be subject to a
	 * two-phase acknowledgement across the network. The message can only be
	 * delivered safely if it can be persisted, so the application must supply a
	 * means of persistence using <code>MqttConnectOptions</code>. If a
	 * persistence mechanism is not specified, the message will not be delivered
	 * in the event of a client failure.</li>
	 **/
	// Message delivery at least once
	private static final int MQTT_QUALITY_OF_SERVICE = 2;

	public static String API_VERSION = "1";
	public static String TOPIC_SHEDULE_UPDATED = "scheduleUpdate";
	public static String TOPIC_REFUSAL_UPDATED = "refusalUpdate";
	public static String TOPIC_ACTIVE_SECTOR_UPDATED = "activeSectorUpdate";

	public static String TOPIC_NOTIFICATION = "NotificationPending";
	public static String TOPIC_PING = "/ping";

	// These are the actions for the service (name are descriptive enough)
	private static final String ACTION_START = BROADCAST_ACTION_PUSH + ".START";
	private static final String ACTION_STOP = BROADCAST_ACTION_PUSH + ".STOP";
	private static final String ACTION_RECONNECT = BROADCAST_ACTION_PUSH
			+ ".RECONNECT";
	private static final String ACTION_KEEPALIVE = BROADCAST_ACTION_PUSH
			+ ".KEEP_ALIVE";

	// Connectivity manager to determining, when the phone loses connection
	private ConnectivityManager mConnMan;

	// Whether or not the service has been started.
	private boolean mStarted;

	// This the application level keep-alive interval, that is used by the
	// AlarmManager
	// to keep the connection active, even when the device goes to sleep.
	private static final long KEEP_ALIVE_INTERVAL = 1000 * MQTT_KEEP_ALIVE;

	// Retry intervals, when the connection is lost.
	private static final long INITIAL_RETRY_INTERVAL = 1000 * 10;
	private static final long MAXIMUM_RETRY_INTERVAL = 1000 * 60 * 30;

	// Preferences instance
	private SharedPreferences mPrefs;
	// We store in the preferences, whether or not the service has been started
	public static final String PREF_STARTED = "isStarted";
	// We store the last retry interval
	public static final String PREF_RETRY = "retryInterval";

	// This is the instance of an MQTT connection.
	private MQTTConnection mConnection;
	private long mStartTime;
		
	// Static method to start the service
	public static void actionStart(Context ctx) {
		Intent i = new Intent(ctx, PushService.class);
		i.setAction(ACTION_START);
		ctx.startService(i);
		Log.i(TAG, "PushService Starting..");
	}

	// Static method to stop the service
	public static void actionStop(Context ctx) {
		Log.d(TAG, "service action stop");
		Intent i = new Intent(ctx, PushService.class);
		i.setAction(ACTION_STOP);
		ctx.startService(i);
	}

	// Static method to send a keep alive message
	public static void actionPing(Context ctx) {
		Log.d(TAG, "service action keep alive");
		Intent i = new Intent(ctx, PushService.class);
		i.setAction(ACTION_KEEPALIVE);
		ctx.startService(i);
	}

	// Static method to reconnect the service
	public static void actionReconnect(Context ctx) {
		Log.d(TAG, "service action reconnect");
		Intent i = new Intent(ctx, PushService.class);
		i.setAction(ACTION_RECONNECT);
		ctx.startService(i);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		log("Creating service");
		mStartTime = System.currentTimeMillis();

		// Get instances of preferences, connectivity manager and notification
		// manager
		mPrefs = getSharedPreferences(TAG, MODE_PRIVATE);
		mConnMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

		/*
		 * If our process was reaped by the system for any reason we need to
		 * restore our state with merely a call to onCreate. We record the last
		 * "started" value and restore it here if necessary.
		 */
		handleCrashedService();
		
	}

	// This method does any necessary clean-up need in case the server has been
	// destroyed by the system
	// and then restarted
	private void handleCrashedService() {
		if (wasStarted() == true) {
			log("Handling crashed service...");
			// stop the keep alives
			stopKeepAlives();

			// Do a clean start
			start();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		log("Service destroyed (started=" + mStarted + ")");
		// Stop the services, if it has been started
		if (mStarted == true) {
			stop();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		log("Service started with intent=" + intent);

		if (!UserCollection.getInstance().isLoggedin()) {
			// We are not logged in, stop service and alerm schedule
			stop();
			stopSelf();
		} else {
			// Do an appropriate action based on the intent.
			if (intent.getAction().equals(ACTION_STOP) == true) {
				stop();
				stopSelf();
			} else if (intent.getAction().equals(ACTION_START) == true) {
				start();
			} else if (intent.getAction().equals(ACTION_KEEPALIVE) == true) {
				keepAlive();
			} else if (intent.getAction().equals(ACTION_RECONNECT) == true) {
				if (isNetworkAvailable()) {
					reconnectIfNecessary();
				}
			}
		}

		return super.onStartCommand(intent, flags, startId);
	}

	// log helper function
	private void log(String message) {
		Log.d(TAG, message);
	}

	// Reads whether or not the service has been started from the preferences
	private boolean wasStarted() {
		return mPrefs.getBoolean(PREF_STARTED, false);
	}

	// Sets whether or not the services has been started in the preferences.
	private void setStarted(boolean started) {
		mPrefs.edit().putBoolean(PREF_STARTED, started).commit();
		mStarted = started;
	}

	private synchronized void start() {
		log("Starting service...");

		// Do nothing, if the service is already running.
		if (mStarted == true) {
			Log.w(TAG, "Attempt to start connection that is already active");
			return;
		}

		// Establish an MQTT connection
		connect();

		// Register a connectivity listener
		registerReceiver(mConnectivityChanged, new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION));
			
		}

	private synchronized void stop() {
		// Do nothing, if the service is not running.
		if (mStarted == false) {
			Log.w(TAG, "Attempt to stop connection not active.");
			return;
		}

		// Save stopped state in the preferences
		setStarted(false);

		// Remove the connectivity receiver
		unregisterReceiver(mConnectivityChanged);

/*		// Remove the Sector change receiver
		unregisterReceiver(sectorChangedReceiver);
*/		
		// Any existing reconnect timers should be removed, since we explicitly
		// stopping the service.
		cancelReconnect();

		// Destroy the MQTT connection if there is one
		if (mConnection != null) {
			mConnection.disconnect();
			mConnection = null;
		}
	}

	private String getHost() {
		String retval = "";

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		String url = prefs.getString(PreferencesActivity.KEY_SERVER_URL,
				getResources().getString(R.string.url));

		try {
			retval = new URL(URLDecoder.decode(url, "utf-8")).getHost();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return retval;
	}

	private int getTimeout() {
		int retval = 30;
		retval = Integer.parseInt(getResources().getString((R.string.timeout)));
		return retval;
	}

	private synchronized void connect() {
		log("Connecting...");

		mConnection = new MQTTConnection(getHost());	
		setStarted(true);
	}

	// Schedule application level keep-alives using the AlarmManager
	private void startKeepAlives() {
		Intent i = new Intent();
		i.setClass(this, PushService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + KEEP_ALIVE_INTERVAL,
				KEEP_ALIVE_INTERVAL, pi);		
	}

	// Remove all scheduled keep alives
	private void stopKeepAlives() {
		Intent i = new Intent();
		i.setClass(this, PushService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.cancel(pi);
		Log.d(TAG, "Stop keep alives");
	}

	// We schedule a reconnect based on the start time of the service
	public void scheduleReconnect(long startTime) {
		Log.d(TAG, "Schedule reconnect");
		// the last keep-alive interval
		long interval = mPrefs.getLong(PREF_RETRY, INITIAL_RETRY_INTERVAL);

		// Calculate the elapsed time since the start
		long now = System.currentTimeMillis();
		long elapsed = now - startTime;

		// Set an appropriate interval based on the elapsed time since start
		if (elapsed < interval) {
			interval = Math.min(interval * 4, MAXIMUM_RETRY_INTERVAL);
		} else {
			interval = INITIAL_RETRY_INTERVAL;
		}

		log("Rescheduling connection in " + interval + "ms.");

		// Save the new interval
		mPrefs.edit().putLong(PREF_RETRY, interval).commit();

		// Schedule a reconnect using the alarm manager.
		Intent i = new Intent();
		i.setClass(this, PushService.class);
		i.setAction(ACTION_RECONNECT);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.set(AlarmManager.RTC_WAKEUP, now + interval, pi);

	}

	// Remove the scheduled reconnect
	public void cancelReconnect() {
		Intent i = new Intent();
		i.setClass(this, PushService.class);
		i.setAction(ACTION_RECONNECT);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.cancel(pi);
	}

	private synchronized void reconnectIfNecessary() {
		if (mStarted == true && mConnection == null) {
			log("Reconnecting...");
			connect();
		}
	}

	private synchronized void keepAlive() {
		// Send a keep alive, if there is a connection.
		if (mStarted == true && mConnection != null) {
			mConnection.keepAlive();
		}
	}

	// This receiver listeners for network changes and updates the MQTT
	// connection
	// accordingly
	private BroadcastReceiver mConnectivityChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get network info
			NetworkInfo info = (NetworkInfo) intent
					.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

			// Is there connectivity?
			boolean hasConnectivity = (info != null && info.isConnected()) ? true
					: false;

			log("Connectivity changed: connected=" + hasConnectivity);

			if (hasConnectivity) {
				reconnectIfNecessary();
			} else if (mConnection != null) {
				// if there no connectivity, make sure MQTT connection is
				// destroyed
				mConnection.disconnect();
				cancelReconnect();
				mConnection = null;
			}
		}
	};

/*	private BroadcastReceiver sectorChangedReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (mConnection != null) {
				try {
					mConnection.subscribeToTopics();
				} catch (MqttException e) {
					log("Resubscription failed due to SECTOR change...");
					e.printStackTrace();
				}
			}
		}
	};
*/
	// Check if we are online
	private boolean isNetworkAvailable() {
		NetworkInfo info = mConnMan.getActiveNetworkInfo();
		if (info == null) {
			return false;
		}
		return info.isConnected();
	}

	// This inner class is a wrapper on top of MQTT client.

	private class MQTTConnection implements MqttCallback {

		private MqttClient mqttClient = null;
		private PowerManager.WakeLock wl = null;
		private String subscribedTopic = "";

		class Connector implements Callable<Void> {

			private String host;

			public Connector(String host) {
				this.host = host;
			}

			@Override
			// The call method may throw an exception
			public Void call() {
				try {
					// Create connection spec
					String mqttConnSpec = "tcp://" + host + ":"
							+ MQTT_BROKER_PORT_NUM;

					// Create the client and connect
					mqttClient = new MqttClient(mqttConnSpec,
							new DeviceIdMax23().getDeviceId(),
							new MemoryPersistence());

					// register this client app has being able to receive
					// messages
					mqttClient.setCallback(MQTTConnection.this);

					MqttConnectOptions conOpt = new MqttConnectOptions();
					conOpt.setCleanSession(MQTT_CLEAN_START);
					conOpt.setConnectionTimeout(getTimeout());
					conOpt.setKeepAliveInterval(MQTT_KEEP_ALIVE);
					
					log("Trying to Connect to host :" + host);
					log("mqttconnection url = " + mqttConnSpec);
					log("Connection details " + conOpt.toString());
					
					mqttClient.connect(conOpt);

					subscribeToTopics();

					// Save start time
					mStartTime = System.currentTimeMillis();

					// Star the keep-alives
					startKeepAlives();

					
					
				} catch (Exception e) {
					log("Failed to Connect host: " + host + " Port: " + MQTT_BROKER_PORT_NUM);
					Log.e(TAG,
							"MqttException: "
									+ (e.getMessage() != null ? e.getMessage()
											: "NULL"));
					onConnectError(mStartTime);
				}
				return null;
			}
		}

		private void onConnectError(long mStartTime) {
			// Schedule a reconnect, if we failed to connect
			if (isNetworkAvailable()) {
				scheduleReconnect(mStartTime);
			}
		}

		// Creates a new connection given the broker address and initial topic
		public MQTTConnection(String brokerHostName) {

			// Build a task and an executor
			Connector task = new Connector(brokerHostName);
			ExecutorService threadExecutor = Executors
					.newSingleThreadExecutor();

			threadExecutor.submit(task);
		}

		public void subscribeToTopics() throws MqttException {
			if ((mqttClient != null) && (mqttClient.isConnected())) {

				// Unsubscribe from previous username if necessary
				unSubscribeFromTopics();

				String topic = "/" + getResources().getString(R.string.app_name) + "/" + API_VERSION + "/"
						+ getUserName();

				mqttClient.subscribe(topic, MQTT_QUALITY_OF_SERVICE);

				log("Subscribed to: " + topic);
				subscribedTopic = topic;
			}
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
		
		private void unSubscribeFromTopics() throws MqttException {
			if ((mqttClient != null) && (mqttClient.isConnected())) {
				if (subscribedTopic.length() > 0) {
					mqttClient.unsubscribe(subscribedTopic);
				}
			}
		}

		// Disconnect
		public void disconnect() {
			try {

				stopKeepAlives();

				if ((mqttClient != null) && (mqttClient.isConnected())) {
					mqttClient.disconnect();
				}

			} catch (MqttException e) {
				Log.e(TAG,
						"MqttException"
								+ (e.getMessage() != null ? e.getMessage()
										: " NULL"), e);
			}
		}

		/*
		 * Called if the application loses it's connection to the message
		 * broker.
		 */
		@Override
		public void connectionLost(Throwable cause) {
			log("Loss of connection" + "connection downed");
			stopKeepAlives();

			// null itself
			mConnection = null;
			if (isNetworkAvailable() == true) {
				reconnectIfNecessary();
			}

		}

		@Override
		public void messageArrived(MqttTopic topic, MqttMessage message)
				throws Exception {

			// Keep CPU on until we process a message
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			PowerManager.WakeLock wl_message = pm.newWakeLock(
					PowerManager.PARTIAL_WAKE_LOCK, TAG);
			wl_message.acquire();

			Log.d(TAG, "  Topic:\t " + topic.getName() + "\n" + "  Message:\t "
					+ new String(message.getPayload()) + "\n" + "  QoS:\t"
					+ message.getQos());
			
			NotificationService notifyService = new NotificationService();

			if (TOPIC_NOTIFICATION.equalsIgnoreCase(new String(message
					.getPayload()).trim())) {
				// get Notification
				log("Launching notification from Push");

				ArrayList<MessageInfos> notifyMap = notifyService
						.getNotification();
				
				NotificationUtils.showNotification(notifyMap);

			} 
			// Release the wake lock after processing messages
			wl_message.release();
		}
				
		@Override
		public void deliveryComplete(MqttDeliveryToken token) {
			// Since we are not publishing anything except ping,
			// we are here because ping sent successfully

			// Release the wake lock
			releaseWakeLock();
		}

		public void keepAlive() {
			try {
				// Keep CPU on until we successfully send a ping
				acquireWakeLock();

				MqttTopic topic = mqttClient.getTopic(TOPIC_PING);

				// Why 12? Partly meaningless, partly because MQTT client use
				// this for ping command, though that doesn't make this publish
				// a ping command
				byte[] payload = { 12 };

				MqttMessage message = new MqttMessage(payload);
				message.setQos(MQTT_QUALITY_OF_SERVICE);
				topic.publish(message);

			} catch (Exception e) {
				log(getString(R.string.error_in_push_service_ping_operation_));
				e.printStackTrace();

				// An error occurred, we should release the wake lock
				releaseWakeLock();
			}
		}

		private void acquireWakeLock() {
			if (wl != null) {
				return;
			}

			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
			wl.acquire();
		}

		private void releaseWakeLock() {
			if (wl == null) {
				return;
			}

			wl.release();
			wl = null;
		}
	}

}