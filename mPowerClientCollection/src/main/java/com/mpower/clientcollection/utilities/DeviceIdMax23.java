package com.mpower.clientcollection.utilities;

import java.util.UUID;

import com.mpower.clientcollection.application.ClientCollection;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;


/**
 * DeviceID - Get device ID not more than 23 character long (used for MQTT
 * client ID)
 * 
 * @author Mehdi Hasan <mhasan@mpower-health.com> Modified version of emmby
 *         <http://stackoverflow.com/users/82156/emmby>
 * 
 */
public class DeviceIdMax23 {
	protected static final String PREFS_FILE = "device_id.xml";
	protected static final String PREFS_DEVICE_ID = "device_id";

	protected static String mId = null;

	public DeviceIdMax23() {

		Context context = ClientCollection.getAppContext();
		
		if (mId == null) {
			synchronized (DeviceIdMax23.class) {
				if (mId == null) {
					final SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE,
							Context.MODE_PRIVATE);
					final String id = prefs.getString(PREFS_DEVICE_ID, null);

					if (id != null) {
						// Use the ids previously computed and stored in the
						// prefs file
						mId = id;

					} else {

						final String androidId = Secure.getString(context.getContentResolver(),
								Secure.ANDROID_ID);

						// Use the Android ID unless it's broken, in which case
						// fallback on deviceId,
						// unless it's not available, then fallback on a random
						// number which we store
						// to a prefs file
						if (!"9774d56d682e549c".equals(androidId)) {
							mId = androidId;
						} else {
							final String deviceId = ((TelephonyManager) context
									.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();

							mId = deviceId != null ? mId : UUID.randomUUID().toString();
						}

						if (mId.length() >= 24) {
							//Get not more than 23 characters
							mId = mId.substring(0, 24);
						}

						// Write the value out to the prefs file
						prefs.edit().putString(PREFS_DEVICE_ID, mId).commit();

					}

				}
			}
		}

	}

	public String getDeviceId() {
		return mId;
	}

}
