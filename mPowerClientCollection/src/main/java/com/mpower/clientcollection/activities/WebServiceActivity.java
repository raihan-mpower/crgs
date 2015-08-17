package com.mpower.clientcollection.activities;

import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ProgressBar;

import com.mpower.clientcollection.activity.R;
import com.mpower.clientcollection.preferences.PreferencesActivity;
import com.mpower.clientcollection.utilities.ErrorMessageUtils;
import com.mpower.clientcollection.utilities.LogUtils;
import com.mpower.clientcollection.utilities.NetUtils;
import com.mpower.clientcollection.utilities.WebUtils;

public class WebServiceActivity extends Activity{
	/** 
	 * @author ratna halder (ratnacse06@gmail.com)
	 */
	private ProgressBar dialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_webservice);
		dialog = (ProgressBar)this.findViewById(R.id.progressBarweb);
		new SyncWithServer().execute();
	}
	
	class SyncWithServer extends  AsyncTask<Void, Void, String>{
		private String url;
		private Exception getE;
		private int statusCode = 0;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			init();
		}
		
		@Override
		protected String doInBackground(Void... params) {
			String serverResponseString = null;
			try {
				HttpResponse response = NetUtils.stringResponseGet(url, NetUtils.getHttpContext(), NetUtils.createHttpClient(WebUtils.CONNECTION_TIMEOUT));
				statusCode = response.getStatusLine().getStatusCode();
				HttpEntity entity = response.getEntity();
				LogUtils.debugLog(this, "statusCode = " + statusCode);
				if(entity != null && statusCode == 200){
					serverResponseString = EntityUtils.toString(entity);
					LogUtils.debugLog(WebServiceActivity.this, "serverResponseString = " + serverResponseString);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				getE =e;
			}
			return serverResponseString;
		}
		
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			dialog.setVisibility(View.GONE);
			String errorMsg = ErrorMessageUtils.createServereErrorMsgGet(getE, statusCode);
			if(errorMsg != null && result == null){
				result = errorMsg;
			}
			showDialog(result);
		}
		
		void init(){
			String id = getIntent().getStringExtra("searchedValue");
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(WebServiceActivity.this);
			url = pref.getString(PreferencesActivity.KEY_SERVER_URL, getString(R.string.url)) + NetUtils.URL_PART_IDENTIFICATION;
			if(!url.endsWith("?")){
				url += "?";
			}
			List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
			params.add(new BasicNameValuePair("id", id));
			String paramString = URLEncodedUtils.format(params, "utf-8");
			url += paramString;
			LogUtils.informationLog(this, "url = " + url);
		}
	} 
	
	private void showDialog(final String result){
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setMessage(result.toString());
		dialog.setCancelable(false);
		dialog.setPositiveButton(getString(R.string.ok), new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					returnInformation(result);
			}
		});
		dialog.show();
	}
	
	private void returnInformation(String result) {
        if (result != null) {
            Intent i = new Intent();
            i.putExtra(
                FormEntryActivity.WEB_RESULT,
                result);
            setResult(RESULT_OK, i);
        }
        finish();
    }
}
