package com.mpower.clientcollection.tasks;

import java.net.SocketTimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import com.mpower.clientcollection.activity.R;
import com.mpower.clientcollection.listeners.DownloadGenericInformationListener;
import com.mpower.clientcollection.utilities.ErrorMessageUtils;
import com.mpower.clientcollection.utilities.LogUtils;
import com.mpower.clientcollection.utilities.NetUtils;
import com.mpower.clientcollection.utilities.NotificationUtils;
import com.mpower.clientcollection.utilities.WebUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

/**
 * @author Ratna Halder(ratnacse06@gmail.com)
 *
 */

public class DownloadGenericInformationTask extends AsyncTask<String, Void, String>{
	
	private Context context;
	private Exception getE = null;
	private int statusCode = 0;
	private String serverResponse = null;
	private String errorMsg = null;
	private ProgressDialog dialog;
	private DownloadGenericInformationListener listener;
	
	public DownloadGenericInformationTask(Context context){
		this.context = context;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		dialog = new ProgressDialog(context);
		dialog.setMessage(context.getString(R.string.please_wait));
		dialog.show();
	}
	
	@Override
	protected String doInBackground(String... params) {
		if(NetUtils.isConnected(context)){
			NotificationUtils.setNetworkIndicator(true);
			
			serverResponse = getInformation(params[0].toString());
			
			NotificationUtils.setNetworkIndicator(false);
		}else{
			LogUtils.debugLog(this, "Internet not found");
			errorMsg = "Internet not found";
		}
		return serverResponse;
	}
	
	private String getInformation(String url){
		String serverResponse = null;
		try {
			HttpResponse response = NetUtils.stringResponseGet(url, NetUtils.getHttpContext(), NetUtils.createHttpClient(WebUtils.CONNECTION_TIMEOUT));
			HttpEntity entity = response.getEntity();
			statusCode = response.getStatusLine().getStatusCode();
			if(entity != null && statusCode == 200){
				serverResponse = EntityUtils.toString(entity, "utf-8");
			}
			LogUtils.debugLog(this, "serverResponse = " + serverResponse);
			LogUtils.debugLog(this, "statusCode = " + statusCode);
			
		} catch(SocketTimeoutException e){
			e.printStackTrace();
			getE = e;
			errorMsg = "Socket Timeout Exception";
		}catch (Exception e) {
			e.printStackTrace();
			getE = e;
		}
		return serverResponse;
	}
	
	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		if(dialog != null && dialog.isShowing()){
			try {
				dialog.dismiss();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(result != null){
			listener.downloadCompleted(result);
		}else {
			if(errorMsg != null){
				errorMsg = ErrorMessageUtils.createServereErrorMsgGet(getE, statusCode);
				LogUtils.debugLog(this, "Error Message is  : " +  errorMsg);
			}	
			listener.showErrorMsg(errorMsg);
		}		
	}
	
	public void setDownloaderListener(DownloadGenericInformationListener listener){
		this.listener = listener;
	}
}
