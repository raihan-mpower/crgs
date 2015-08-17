package com.mpower.clientcollection.utilities;

import android.util.Log;

public class LogUtils {
	
	public static final void debugLog(Object classObject, String msg){
		Log.d(classObject.getClass().getSimpleName(), msg);
	}
	
	public static final void warningLog(Object classObject, String msg){
		Log.w(classObject.getClass().getSimpleName(), msg);
	}
	
	public static final void informationLog(Object classObject, String msg){
		Log.i(classObject.getClass().getSimpleName(), msg);
	}
}
