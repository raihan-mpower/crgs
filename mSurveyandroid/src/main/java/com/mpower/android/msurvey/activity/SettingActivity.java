package com.mpower.android.msurvey.activity;

import com.mpower.clientcollection.preferences.PreferencesActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SettingActivity extends Activity{
@Override
protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	Intent start = new Intent(this, PreferencesActivity.class);
	startActivity(start);
	finish();
}
}
