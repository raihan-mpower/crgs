package com.mpower.clientcollection.utilities;

import com.mpower.clientcollection.activities.FileManagerTabs;
import com.mpower.clientcollection.activities.FormChooserList;
import com.mpower.clientcollection.activities.FormDownloadList;
import com.mpower.clientcollection.activities.InstanceChooserList;
import com.mpower.clientcollection.activities.InstanceUploaderList;
import com.mpower.clientcollection.preferences.PreferencesActivity;

import android.content.Context;
import android.content.Intent;

public class ActivityManager {
	
	public static final int FORM_CHOSSER_LIST = 0;
	public static final int FORM_REVIEW_LIST = 2;
	public static final int FORM_UPLOAD_LIST = 4;
	public static final int FORM_DOWNLOAD_LIST = 3;
	
	
	private Context context = null;
	
	public ActivityManager(Context context){
		this.context = context;
	}
	
	public void startLibraryActivity(int value){
		switch (value) {
		case FORM_CHOSSER_LIST:
			// New
			Intent iNew = new Intent(context,
					FormChooserList.class);
			context.startActivity(iNew);
			break;
		case 1:
			// Edit Data
			Intent iEdit = new Intent(context,
					InstanceChooserList.class);
			context.startActivity(iEdit);
			break;
		case 2:
			// Send Data
			Intent iSend = new Intent(context,
					InstanceUploaderList.class);
			context.startActivity(iSend);
			break;
		case 3:
			// Download Form
			Intent iDownload = new Intent(context,
					FormDownloadList.class);
			context.startActivity(iDownload);
			break;
		case 4:
			// Delete
			Intent iManage = new Intent(context,
					FileManagerTabs.class);
			context.startActivity(iManage);
			break;
		case 5:
			// Settings
			Intent ig = new Intent(context, PreferencesActivity.class);
			context.startActivity(ig);
			break;
		
		default:
			break;
		}
	}
}