package com.mpower.clientcollection.utilities;

public class ErrorMessageUtils {

	public static final String createServereErrorMsgGet(Exception serverException, int getResponseCode){
		String serverResponseMessage = null;
		if(serverException != null && serverException.getMessage().length() > 0){
			serverResponseMessage = "Error on getting information \n" + serverException.getMessage();
		}else{
			if(getResponseCode == 401){
				UserCollection.getInstance().logOffAndClearCache();
			}else if(getResponseCode != 0 && getResponseCode != 200 && getResponseCode != 401){
				if(getResponseCode == 404){
					serverResponseMessage = "Information or id is not present. \n Please check your information" ;
				}else if(getResponseCode == 500){
					serverResponseMessage = "Required all field is not fill up. Please check information";
				}else{
					serverResponseMessage = "Invalid https status " + getResponseCode;
				}
			}
		}		
		LogUtils.warningLog(new ErrorMessageUtils(), "serverResponseMessage = " + serverResponseMessage);
		return serverResponseMessage;
	}
	
	public static final String createServereErrorMsgPost(Exception serverException, int postResponseCode){
		String serverResponseMessage = null;
		if(serverException != null && serverException.getMessage().length() > 0){
			serverResponseMessage = "Error on posting information \n" + serverException.getMessage();
		}else{
			if(postResponseCode == 401){
				UserCollection.getInstance().logOffAndClearCache();
			}else if(postResponseCode != 0 && postResponseCode != 200 && postResponseCode != 401){
				if(postResponseCode == 404){
					serverResponseMessage = "Information or id is not present. \n Please check your information" ;
				}else if(postResponseCode == 500){
					serverResponseMessage = "Required all field is not fill up. Please check information";
				}else{
					serverResponseMessage = "Invalid https status " + postResponseCode;
				}
			}
		}		
		return serverResponseMessage;
	}
}
