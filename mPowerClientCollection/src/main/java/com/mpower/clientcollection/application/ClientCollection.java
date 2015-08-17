package com.mpower.clientcollection.application;

import java.io.File;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.opendatakit.httpclientandroidlib.client.CookieStore;
import org.opendatakit.httpclientandroidlib.client.CredentialsProvider;
import org.opendatakit.httpclientandroidlib.client.protocol.ClientContext;
import org.opendatakit.httpclientandroidlib.impl.client.BasicCookieStore;
import org.opendatakit.httpclientandroidlib.protocol.BasicHttpContext;
import org.opendatakit.httpclientandroidlib.protocol.HttpContext;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mpower.clientcollection.activity.R;
import com.mpower.clientcollection.database.ActivityLogger;
import com.mpower.clientcollection.database.MpowerDatabaseHelper;
import com.mpower.clientcollection.logic.FormController;
import com.mpower.clientcollection.logic.PropertyManager;
import com.mpower.clientcollection.models.MessageInfos;
import com.mpower.clientcollection.preferences.PreferencesActivity;
import com.mpower.clientcollection.services.NotificationService;
import com.mpower.clientcollection.services.PushService;
import com.mpower.clientcollection.services.SyncNotificationWithServer;
import com.mpower.clientcollection.utilities.AgingCredentialsProvider;
import com.mpower.clientcollection.utilities.LogUtils;
import com.mpower.clientcollection.utilities.NotificationUtils;
import com.mpower.clientcollection.utilities.UserCollection;

/**
 * @author Ratna Halder(ratnacse06@gmail.com)
 *
 */

public abstract class ClientCollection extends Application {
	
	  
	    public static final String DEFAULT_FONTSIZE = "21";

	    // share all session cookies across all sessions...
	    private CookieStore cookieStore = new BasicCookieStore();
	    // retain credentials for 7 minutes...
	    private CredentialsProvider credsProvider = new AgingCredentialsProvider(7 * 60 * 1000);
	  //  private CredentialsProvider credsProvider = new BasicCredentialsProvider();
	    private ActivityLogger mActivityLogger;
	    private FormController mFormController = null;
	    private Locale locale = null; 
	    private static ClientCollection singleton = null;
	    private static Context context;
	    private int questionFontsize = 21;
	    private static String applicationName = null;
	    public static String DYN_FOLDER_INTRNT_FILTER = "android.intent.action.ODK.Table";
	    public static String DYN_FOLDER_NAME = "Table";
	    public static String FORM_AUTHORITY_NAME = "com.mpower.clientcollection.provider.clientcollection.forms";
	    public static String INSTANCE_AUTHORITY_NAME = "com.mpower.clientcollection.provider.clientcollection.instance";
	    
	    private CallListener listener = new CallListener();
	    
	    public static final class FormsDirectory {
	    	FormsDirectory (){};
	    	
	    	public static final String APPLICATION_ROOT = Environment.getExternalStorageDirectory()
	  	            + File.separator + getApplicationName();
	  	  
	  	 	public static final String FORMS_PATH = APPLICATION_ROOT + File.separator + "forms";
	  	    public static final String INSTANCES_PATH = APPLICATION_ROOT + File.separator + "instances";
	  	    public static final String CACHE_PATH = APPLICATION_ROOT + File.separator + ".cache";
	  	    public static final String METADATA_PATH = APPLICATION_ROOT + File.separator + "metadata";
	  	    public static final String TMPFILE_PATH = CACHE_PATH + File.separator + "tmp.jpg";
	  	    public static final String TMPDRAWFILE_PATH = CACHE_PATH + File.separator + "tmpDraw.jpg";
	  	    public static final String TMPXML_PATH = CACHE_PATH + File.separator + "tmp.xml";
	  	    public static final String LOG_PATH = APPLICATION_ROOT + File.separator + "log";

	    }
	    
	    public static final class AssetsDirectory {
	    	AssetsDirectory (){};
	    	
	    	public static final String APPLICATION_ROOT = Environment.getExternalStorageDirectory()
	  	            + File.separator + "opendatakit";
	  	  
	  	 	public static final String ASSETS_PATH = APPLICATION_ROOT + File.separator + "tables" + File.separator + "assets";
	  	    public static final String CSV_PATH = ASSETS_PATH + File.separator + "csv";
	  	 }
	    
	    public static ClientCollection getInstance() {
	        return singleton;
	    }

	    public ActivityLogger getActivityLogger() {
	        return mActivityLogger;
	    }

	    public FormController getFormController() {
	        return mFormController;
	    }

	    public void setFormController(FormController controller) {
	        mFormController = controller;
	    }

	    public int getQuestionFontsize() {
	         return questionFontsize;
	    }

	    public void setQusFontSize(int size){
	    	this.questionFontsize = size;
	    }
	    	    
	    public static String getApplicationName(){
	    	return applicationName;
	    }
	    
	    public static void setApplicationName(String applicationName){
	    	ClientCollection.applicationName = applicationName;
	    }
	    
	    public static void setAuthorityName(String formAutorityName, String instanceAuthorityName){
	    	ClientCollection.FORM_AUTHORITY_NAME = formAutorityName;
	    	ClientCollection.INSTANCE_AUTHORITY_NAME = instanceAuthorityName;
	    }
	    
	    /**
	     * Target application defined intent filter is being used to find a activity located in target application (not in library application)
	     * 
	     * @return void
	     * @param intentFilter - should be like: android.intent.action.ODK.Table
	     */
	    public static void setDynActivityIntentFilter(String intentFilter, String folderName){
	    	ClientCollection.DYN_FOLDER_INTRNT_FILTER = intentFilter;
	    	ClientCollection.DYN_FOLDER_NAME = folderName;
	    }
	    
	    
	    /**
	     * Creates required directories on the SDCard (or other external storage)
	     *
	     * @throws RuntimeException if there is no SDCard or the directory exists as a non directory
	     */
	    public static void createApplicationDir() throws RuntimeException {
	    	
	        String cardstatus = Environment.getExternalStorageState();
	        
	        if (!cardstatus.equals(Environment.MEDIA_MOUNTED)) {
	            RuntimeException e =
	                    new RuntimeException("ClientCollection reports :: SDCard error: "
	                            + Environment.getExternalStorageState());
	            throw e;
	        }else if(getApplicationName() == null){	        	
	        	RuntimeException e = new RuntimeException("ApplicationName not found");
	        	throw e;
	        	
	        }

	        String[] dirs = {
	        		FormsDirectory.APPLICATION_ROOT, FormsDirectory.FORMS_PATH, FormsDirectory.INSTANCES_PATH, FormsDirectory.CACHE_PATH, FormsDirectory.METADATA_PATH
	        };

	        for (String dirName : dirs) {
	            File dir = new File(dirName);
	            if (!dir.exists()) {
	                if (!dir.mkdirs()) {
	                    RuntimeException e =
	                            new RuntimeException("ClientCollection reports :: Cannot create directory: "
	                                    + dirName);
	                    throw e;
	                }
	            } else {
	                if (!dir.isDirectory()) {
	                    RuntimeException e =
	                            new RuntimeException("ClientCollection reports :: " + dirName
	                                    + " exists, but is not a directory");
	                    throw e;
	                }
	            }
	        }
	    }

	    /**
	     * Predicate that tests whether a directory path might refer to an
	     * ClientCollection Tables instance data directory (e.g., for media attachments).
	     *
	     * @param directory
	     * @return
	     */
	
	    public static boolean isClientCollectionTablesInstanceDataDirectory(File directory) {
			/**
			 * Special check to prevent deletion of files that
			 * could be in use by mIntel Tables.
			 */
	    	String dirPath = directory.getAbsolutePath();
	    	if ( dirPath.startsWith(FormsDirectory.APPLICATION_ROOT) ) {
	    		dirPath = dirPath.substring(FormsDirectory.APPLICATION_ROOT.length());
	    		String[] parts = dirPath.split(File.separator);
	    		// [appName, instances, tableId, instanceId ]
	    		if ( parts.length == 4 && parts[1].equals("instances") ) {
	    			return true;
	    		}
	    	}
	    	return false;
		}

	   /**
	     * Construct and return a session context with shared cookieStore and credsProvider so a user
	     * does not have to re-enter login information.
	     *
	     * @return
	     */
	    public synchronized HttpContext getODKHttpContext() {

	        // context holds authentication state machine, so it cannot be
	        // shared across independent activities.
	        HttpContext localContext = new BasicHttpContext();

	        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	        localContext.setAttribute(ClientContext.CREDS_PROVIDER, credsProvider);

	        return localContext;
	    }

	    public CredentialsProvider getCredentialsProvider() {
	        return credsProvider;
	    }

	    public CookieStore getCookieStore() {
	        return cookieStore;
	    }

	    @Override
	    public void onCreate() {
	    	singleton = this;
	        ClientCollection.context = getApplicationContext();
	        super.onCreate();
	        
	        PropertyManager mgr = new PropertyManager(this);
	        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	        mActivityLogger = new ActivityLogger(
	                mgr.getSingularProperty(PropertyManager.DEVICE_ID_PROPERTY));
	        //MPOWER: Set language based on user preference
	    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getAppContext());
	    	String language = sp.getString(PreferencesActivity.KEY_LANGUAGE_SETTING, null);
	    		    	
	    	setSelectedLanguage(language);
	    
	       if(listener == null) listener = new CallListener(); 
	       MpowerDatabaseHelper db = new MpowerDatabaseHelper(ClientCollection.context);
	       db.close();
	       
	       IntentFilter showNotificationIntent = new IntentFilter(
					NotificationService.BROADCAST_ACTION_DISPLAY_NOTIFICATION);
			showNotificationIntent.addCategory(Intent.CATEGORY_DEFAULT);
			registerReceiver(showNotificationReceiver, showNotificationIntent);
			
			// Register receiver for login event, we start Push service here
			IntentFilter authenticationDoneFilter = new IntentFilter(UserCollection.BROADCAST_ACTION_AUTHENTICATION_DONE);
			authenticationDoneFilter.addCategory(Intent.CATEGORY_DEFAULT);
			registerReceiver(authenticationDoneReceiver, authenticationDoneFilter);
	    }	  
	    
	    private BroadcastReceiver authenticationDoneReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
			//	PushService.actionStart(getApplicationContext());
				PushService.actionStart(getAppContext());
				Log.i("MjivitaPlus", "Authentication done");
			}
		};
		
		private BroadcastReceiver showNotificationReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String message = intent.getStringExtra("notification");
				Type collectionType = new TypeToken<ArrayList<MessageInfos>>(){}.getType();
				ArrayList<MessageInfos> notifyInfo = new Gson().fromJson(message, collectionType);			
				NotificationUtils.showNotification(notifyInfo);						 
			}
		};
		
		public void syncNotificationData() {
			Intent syncData = new Intent(getApplicationContext(), SyncNotificationWithServer.class);
			startService(syncData);
		}
	    
		@Override
		public void onTerminate() {
			super.onTerminate();
			unregisterReceiver(showNotificationReceiver);
			unregisterReceiver(authenticationDoneReceiver);
		}

		
	    public static Context getAppContext() {
			return ClientCollection.context;
		}
	    
	    
	    private class CallListener{
	    	CallListener(){
	    		setApplicationName();
	    		setAuthorityName();
	    		setDynActivityIntentFilter();
	    	}
	    }
	    
	 // MPOWER: Encrypt and decrypt a file.
	    public static byte[] generateKey(String password) throws Exception
	    {
	        byte[] keyStart = password.getBytes("UTF-8");

	        KeyGenerator kgen = KeyGenerator.getInstance("AES");
	        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG", "Crypto");
	        sr.setSeed(keyStart);
	        kgen.init(128, sr);
	        SecretKey skey = kgen.generateKey();
	        return skey.getEncoded();
	    }
	    
	    public static byte[] encodeFile(byte[] key, byte[] fileData) throws Exception
	    {

	        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
	        Cipher cipher = Cipher.getInstance("AES");
	        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

	        byte[] encrypted = cipher.doFinal(fileData);

	        return encrypted;
	    }

	    public static byte[] decodeFile(byte[] key, byte[] fileData) throws Exception
	    {
	        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
	        Cipher cipher = Cipher.getInstance("AES");
	        cipher.init(Cipher.DECRYPT_MODE, skeySpec);

	        byte[] decrypted = cipher.doFinal(fileData);

	        return decrypted;
	    }
	    
	  /*  public static void encodeFile(byte[] byteArrayContainigDataToEncrypt) throws Exception{
	    	//File file = new File(Environment.getExternalStorageDirectory() + File.separator + "your_folder_on_sd", "file_name");
	    	File file = new File(FormsDirectory.APPLICATION_ROOT + "1");
	    	BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
	    	byte[] yourKey = generateKey("password");
	    	byte[] filesBytes = encodeFile(yourKey, byteArrayContainigDataToEncrypt);
	    	bos.write(filesBytes);
	    	bos.flush();
	    	bos.close();
	    }
	  */
	    
	 
	    public static void decodeFile(byte[] bytesOfFile) throws Exception{
	    	byte[] yourKey = generateKey("password");
	    	byte[] decodedData = decodeFile(yourKey, bytesOfFile);
	    }
	    
	    @Override
	    public void onConfigurationChanged(Configuration newConfig) {	   
	    	super.onConfigurationChanged(newConfig);
	    	if(locale != null){
	    		Configuration config = getBaseContext().getResources().getConfiguration();
	    		Locale.setDefault(locale);
	            config.locale = locale;
	            getBaseContext().getResources().updateConfiguration(newConfig, getBaseContext().getResources().getDisplayMetrics());	    			  
	    	}	    
	    }
	    
	    public void setSelectedLanguage(String languageValue){
	    	LogUtils.debugLog(this, "onCofig_lanuage = " + languageValue);
	    	Configuration config = getBaseContext().getResources().getConfiguration();
			if(languageValue != null && languageValue.equalsIgnoreCase("bn")){				
				locale = new Locale("bn");
	            Locale.setDefault(locale);
	            config.locale = locale;
	            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());        
			}else{				
				locale = new Locale("");
	            Locale.setDefault(locale);
	            config.locale = locale;
	            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());        
			}
		}
			    
	    
	    protected abstract void setApplicationName();
	    /**
	     * To start a fresh form or edited form, ClientCollection library must be required
		 * two authority name to create content URI that should be same as resource string name.
		 * also, ClientCollection manifest need authority from this project's resource string named by form_provider_authority and 
		 * instance_provider_authority to define provider			
	     *
	     */
	    protected abstract void setAuthorityName();
	    /**
	     * Target application defined intent filter is being used to find a activity located in target application (not in library application)
	     * 
	     * @return void
	     * @param intentFilter - should be like: android.intent.action.ODK.Table
	     */
	    protected abstract void setDynActivityIntentFilter();
	    	    
	}