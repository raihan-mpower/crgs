package com.mpower.clientcollection.listeners;

/**
 * @author Ratna Halder(ratnacse06@gmail.com)
 *
 */
public interface DownloadGenericInformationListener {

	void downloadCompleted(String serverResponse);
	void showErrorMsg(String errorMsg);
	
}
