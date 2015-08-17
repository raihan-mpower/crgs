package com.mpower.clientcollection.utilities;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;

import com.mpower.clientcollection.activity.R;
import com.mpower.clientcollection.application.ClientCollection;
import com.mpower.clientcollection.preferences.PreferencesActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Base64;


/**
 * NetUtils - URL and network related utility class
 * 
 * @author Mehdi Hasan <mhasan@mpower-health.com>
 * @author ratna halder  <ratna@mpower-health.com>
 * 
 */
public class NetUtils {

	public static final String URL_PART_LOGIN = "/m/login";
	public static final String URL_PART_ACTIVE_INTERVIEW = "/m/interview/active";
	
	public static final String HTTP_CONTENT_TYPE_JSON = "application/json";
	public static final String HTTP_CONTENT_TYPE_JPEG = "image/jpeg";
	public static final String URL_PART_NOTIFICATION = "/m/notification";
	public static final String URL_PART_IDENTIFICATION = "/m/identification";
	private static HttpContext localContext = null;

	public static final AuthScope buildAuthScopes(String host) {

		AuthScope a;
		// allow digest auth on any port...
		a = new AuthScope(host, -1, null, AuthPolicy.DIGEST);

		return a;
	}

	public static final void clearAllCredentials() {
		HttpContext localContext = getHttpContext();
		CredentialsProvider credsProvider = (CredentialsProvider) localContext
				.getAttribute(ClientContext.CREDS_PROVIDER);
		credsProvider.clear();
		
		//Clear credentials from odk authentication
		WebUtils.clearAllCredentials();
	}

	public static boolean hasCredentials(String username, String host, HttpContext context) {
		HttpContext localContext = context;
		CredentialsProvider credsProvider = (CredentialsProvider) localContext
				.getAttribute(ClientContext.CREDS_PROVIDER);

		AuthScope a = buildAuthScopes(host);
		boolean hasCreds = true;

		Credentials c = credsProvider.getCredentials(a);
		if (c == null) {
			hasCreds = false;
		}

		return hasCreds;
	}

	public static String getSHA512(String input) {
		String retval = "";
		try {
			MessageDigest m = MessageDigest.getInstance("SHA-512");
			byte[] out = m.digest(input.getBytes());
			retval = Base64.encodeToString(out, Base64.NO_WRAP);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return retval;
	}

	public static void addCredentials(String username, String password) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ClientCollection.getAppContext());
		String scheduleUrl = prefs.getString(PreferencesActivity.KEY_SERVER_URL, ClientCollection.getAppContext()
				.getResources().getString(R.string.url));

		String host = "";

		try {
			host = new URL(URLDecoder.decode(scheduleUrl, "utf-8")).getHost();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		HttpContext context = getHttpContext();

		addCredentials(username, password, host, context);
	}

	public static void addCredentials(String username, String password, HttpContext context) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ClientCollection.getAppContext());
		String scheduleUrl = prefs.getString(PreferencesActivity.KEY_SERVER_URL, ClientCollection.getAppContext()
				.getResources().getString(R.string.url));

		String host = "";

		try {
			host = new URL(URLDecoder.decode(scheduleUrl, "utf-8")).getHost();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		addCredentials(username, password, host, context);
	}

	
	private static void addCredentials(String username, String password, String host, HttpContext context) {
		HttpContext localContext = context;
		Credentials c = new UsernamePasswordCredentials(username, password);
		addCredentials(localContext, c, host);
		
		//also add credentials for odk authentication;
		WebUtils.addCredentials(username, password, host);
		
	}

	/*public static void refreshCredential() {
		if (User.getInstance().isLoggedin()) {
			clearAllCredentials();
			addCredentials(User.getInstance().getUserData().getUsername(), User.getInstance().getUserData()
					.getPassword());
		}
	}
*/
	private static final void addCredentials(HttpContext localContext, Credentials c, String host) {
		CredentialsProvider credsProvider = (CredentialsProvider) localContext
				.getAttribute(ClientContext.CREDS_PROVIDER);

		AuthScope a = buildAuthScopes(host);
		credsProvider.setCredentials(a, c);
	}

	public static HttpClient createHttpClient(int timeout) {
		// configure connection
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, timeout);
		HttpConnectionParams.setSoTimeout(params, timeout);

		// support redirecting to handle http: => https: transition
		HttpClientParams.setRedirecting(params, true);
		// support authenticating
		HttpClientParams.setAuthenticating(params, true);
		// if possible, bias toward digest auth (may not be in 4.0 beta 2)
		List<String> authPref = new ArrayList<String>();
		authPref.add(AuthPolicy.DIGEST);
		// authPref.add(AuthPolicy.BASIC);
		params.setParameter("http.auth-target.scheme-pref", authPref);

		// setup client
		HttpClient httpclient = null;

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));
		ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);

		httpclient = new DefaultHttpClient(cm, params);

		httpclient.getParams().setParameter(ClientPNames.MAX_REDIRECTS, 1);
		httpclient.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);

		return httpclient;
	}

	public static HttpResponse stringResponseGet(String urlString, HttpContext localContext,
			HttpClient httpclient) throws Exception {

		URL url = new URL(URLDecoder.decode(urlString, "utf-8"));
		URI u = url.toURI();

		HttpGet req = new HttpGet();
		req.setURI(u);

		HttpResponse response = null;
		response = httpclient.execute(req, localContext);

		return response;
	}

	public static HttpResponse stringResponsePost(String urlString, String content, byte[] image,
			HttpContext localContext, HttpClient httpclient) throws Exception {

		URL url = new URL(URLDecoder.decode(urlString, "utf-8"));
		URI u = url.toURI();

		HttpPost post = new HttpPost();
		post.setURI(u);

		MultipartEntity reqEntity = new MultipartEntity();
		StringBody sb = new StringBody(content, HTTP_CONTENT_TYPE_JSON, Charset.forName("UTF-8"));
		ByteArrayBody ib = new ByteArrayBody(image, HTTP_CONTENT_TYPE_JPEG, "image");

		reqEntity.addPart("interview_data", sb);
		reqEntity.addPart("interview_image", ib);

		//Enable chunking in entity
		ByteArrayOutputStream bArrOS = new ByteArrayOutputStream();
		reqEntity.writeTo(bArrOS);
		bArrOS.flush();
		ByteArrayEntity bArrEntity = new ByteArrayEntity(bArrOS.toByteArray());
		bArrOS.close();
		
		bArrEntity.setChunked(true);
		bArrEntity.setContentEncoding(reqEntity.getContentEncoding());
		bArrEntity.setContentType(reqEntity.getContentType());
        
		//Post chunked entity
		post.setEntity(bArrEntity);

		HttpResponse response = null;
		response = httpclient.execute(post, localContext);

		return response;
	}

	public static HttpResponse stringResponsePost(String urlString, String content, HttpContext localContext,
			HttpClient httpclient) throws Exception {

		URL url = new URL(URLDecoder.decode(urlString, "utf-8"));
		URI u = url.toURI();

		HttpPost post = new HttpPost();
		post.setURI(u);

		MultipartEntity reqEntity = new MultipartEntity();
		StringBody sb = new StringBody(content, HTTP_CONTENT_TYPE_JSON, Charset.forName("UTF-8"));

		reqEntity.addPart("data", sb);
		post.setEntity(reqEntity);

		HttpResponse response = null;
		response = httpclient.execute(post, localContext);

		return response;
	}

	public static synchronized HttpContext getHttpContext() {
		if (localContext == null) {
			// set up one context for all HTTP requests so that authentication
			// and cookies can be retained.
			localContext = new SyncBasicHttpContext(new BasicHttpContext());

			// establish a local cookie store for this attempt at downloading...
			CookieStore cookieStore = new BasicCookieStore();
			localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

			// and establish a credentials provider. Default is 7 minutes.
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			localContext.setAttribute(ClientContext.CREDS_PROVIDER,
					credsProvider);
		}
		return localContext;
	}
	
	public static boolean isConnected(Context context) {
		final ConnectivityManager conMgr = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
		if (activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isValidUrl(String url) {

		try {
			new URL(URLDecoder.decode(url, "utf-8"));
			return true;
		} catch (MalformedURLException e) {
			return false;
		} catch (UnsupportedEncodingException e) {
			return false;
		}
	}
}
