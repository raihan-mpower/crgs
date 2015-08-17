package com.mpower.android.msurvey.application;

import com.mpower.clientcollection.application.ClientCollection;

public class MSurvey extends ClientCollection{
	
	public static String FORM_AUTHORITY_NAME = "com.mpower.msurvey.provider.msurvey.forms";
	public static String INSTANCE_AUTHORITY_NAME = "com.mpower.msurvey.provider.msurvey.instances";
	
	@Override
	protected void setApplicationName() {
		 ClientCollection.setApplicationName("mSurvey");	
	}

	@Override
	protected void setAuthorityName() {
		   ClientCollection.setAuthorityName(FORM_AUTHORITY_NAME, INSTANCE_AUTHORITY_NAME);		   
	}

	@Override
	protected void setDynActivityIntentFilter() {
		ClientCollection.setDynActivityIntentFilter("android.intent.action.mpower.msurvey.setting", "Setting");
	}
}
