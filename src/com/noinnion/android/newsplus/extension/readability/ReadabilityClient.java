package com.noinnion.android.newsplus.extension.readability;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.noinnion.android.newsplus.extension.readability.util.AndroidUtils;
import com.noinnion.android.newsplus.extension.readability.util.HttpUtils;
import com.noinnion.android.newsplus.extension.readability.util.MySSLSocketFactory;
import com.noinnion.android.newsplus.extension.readability.util.Utils;
import com.noinnion.android.reader.api.ReaderException;
import com.noinnion.android.reader.api.ReaderException.ReaderLoginException;
import com.noinnion.android.reader.api.ReaderExtension;
import com.noinnion.android.reader.api.internal.IItemIdListHandler;
import com.noinnion.android.reader.api.internal.IItemListHandler;
import com.noinnion.android.reader.api.internal.ISubscriptionListHandler;
import com.noinnion.android.reader.api.internal.ITagListHandler;
import com.noinnion.android.reader.api.provider.IItem;
import com.noinnion.android.reader.api.provider.ISubscription;
import com.noinnion.android.reader.api.provider.ITag;

public class ReadabilityClient extends ReaderExtension {
	private Context mContext;
	private List<String> lastItemIDList;
	private ArrayList<ITag> tagList; 	
	
	public final int LOGIN_OK=200;
	private static final String starredTag= "Tag/starred";
	
	protected DefaultHttpClient client;

	public static final int SOCKET_OPERATION_TIMEOUT = 20000; // 20s

	public ReadabilityClient() 
	{
		// mContext = getApplicationContext();
	}
	/**
	 * do you use this Constructer and not default
	 * 
	 * @param c
	 */
	public ReadabilityClient(Context c) 
	{
		mContext = c;
	}

	

	public Context getMContext()
	{
		if (mContext == null) mContext = getApplicationContext();
		return mContext;
	}

	@Override
	public boolean disableTag(String arg0, String arg1) throws IOException,ReaderException 
	{
		return false;
	}

	@Override
	public boolean editItemTag(String[] itemUids, String[] subUids, String[] tags, int action) throws IOException, ReaderException 
	{
		android.util.Log.v("idltd","editing "+ Integer.toString(action) +"/"+Integer.toString(ReaderExtension.ACTION_ITEM_TAG_ADD_LABEL)+" tag count=" + Integer.toString( tags.length));
		if(action==ReaderExtension.ACTION_ITEM_TAG_ADD_LABEL)
		{	for(String tag:tags)
			{	android.util.Log.v("idltd","editing tag ="+tag);
				if(tag.equals(starredTag))
				{	BasicNameValuePair nvp=new BasicNameValuePair("favorite","1");
					ArrayList<NameValuePair> nvps =new ArrayList<NameValuePair>();
					nvps.add(nvp);			
					for(String itemBookmarkId:itemUids)
					{	android.util.Log.v("idltd","editing tag bookmark ="+itemBookmarkId);
						try
						{	doPostInputStream("https://www.readability.com/api/rest/v1/bookmarks/"+itemBookmarkId, nvps);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
				}	}	}
				else { // user defined tag - note, added as comma separated string
					for(String itemBookmarkId:itemUids)
					{ android.util.Log.v("idltd","editing tag bookmark ="+itemBookmarkId);
						try {
							ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();							
							nvps.add(new BasicNameValuePair("tags",tag.toString()));
							doPostInputStream("https://www.readability.com/api/rest/v1/bookmarks/"+itemBookmarkId+"/tags", nvps);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
		}	}	}	}	}
		if(action==ReaderExtension.ACTION_ITEM_TAG_REMOVE_LABEL)
		{	for(String tag:tags)
			{	if(tag.equals(starredTag))
				{	BasicNameValuePair nvp=new BasicNameValuePair("favorite","0");
					ArrayList<NameValuePair> nvps =new ArrayList<NameValuePair>();
					nvps.add(nvp);			
					for(String itemBookmarkId:itemUids)
					{	try
						{	doPostInputStream("https://www.readability.com/api/rest/v1/bookmarks/"+itemBookmarkId, nvps);
						} catch (Exception e)
						{	// TODO Auto-generated catch block
							e.printStackTrace();
				}	}	}
				else
				{	for(String itemBookmarkId:itemUids)
					{	try
						{
							doDeleteInputStream("https://www.readability.com/api/rest/v1/bookmarks/"+itemBookmarkId+"/tags/"+itemBookmarkId);
						} catch (Exception e)
						{	// TODO Auto-generated catch block
							e.printStackTrace();
						}	}	}
					
					
					
				}
			}	}
		return false;
	}

	@Override
	public boolean editSubscription(String uid, String title, String url, String[] tags, int action) throws IOException, ReaderException 
	{	switch (action) {
        case ReaderExtension.ACTION_SUBSCRIPTION_EDIT: break;
        case ReaderExtension.ACTION_SUBSCRIPTION_NEW_LABEL: break;
        case ReaderExtension.ACTION_SUBSCRIPTION_ADD_LABEL: break;
        case ReaderExtension.ACTION_SUBSCRIPTION_REMOVE_LABEL: break;
        case ReaderExtension.ACTION_SUBSCRIPTION_SUBCRIBE: break;
        case ReaderExtension.ACTION_SUBSCRIPTION_UNSUBCRIBE: break;
        }
        return false;
	}
	
	@Override
	public void handleItemIdList(IItemIdListHandler itemHandler, long arg1)throws IOException, ReaderException
	{	List<String>idList=new ArrayList<String>();
		for(String id:lastItemIDList) {	idList.add(id+""); }
		try
		{	itemHandler.items(idList); }
		catch (RemoteException e)
		{	e.printStackTrace(); }
	}
	
	@Override
	public void handleItemList(IItemListHandler itemHandler, long arg1) throws IOException, ReaderException 
	{	String content="";
		try
		{
			trace("Item Handler: "+itemHandler.stream().toString());
			HttpResponse response=null;
			if (itemHandler.stream().equals(STATE_READING_LIST)) response=doGetInputStream("https://www.readability.com/api/rest/v1/bookmarks?archive=0"); 				
			if (itemHandler.stream().equals(STATE_STARRED)) response=doGetInputStream("https://www.readability.com/api/rest/v1/bookmarks?favorite=1");
			// if (itemHandler.stream().equals(STATE_READ)) response=doGetInputStream("https://www.readability.com/api/rest/v1/bookmarks?archive=1");
			content = getContent(getInputStreamFromResponse(response));
			if (response.getStatusLine().getStatusCode() != 200)
			{	android.util.Log.w("idltd", "Readability API failed: " + content);
				 throw new Exception("wtf");
		}	}
		catch (Exception e)
		{}
		
		parseItems(itemHandler, content);
	}
	private void parseItems(IItemListHandler itemHandler,String content)throws IOException, ReaderException
	{	try
		{	List<Integer>articleIds=Prefs.getAllItemIDs(getMContext());
			if(itemHandler.startTime()==0) Prefs.removeALLItemIDs(getMContext());
			lastItemIDList=new ArrayList<String>();
			
			JSONObject obj=new JSONObject(content);
			JSONArray array=obj.getJSONArray("bookmarks");
			ArrayList<IItem>itemlist=new ArrayList<IItem>();
			int entryLength=0;
			for(int i=0;i<array.length();i++)
			{	JSONObject bookmark=array.getJSONObject(i);
				String articleHref=bookmark.getString("article_href");
				JSONArray tags=bookmark.getJSONArray("tags");
				// already got this one?
				if(articleIds.indexOf(Integer.valueOf(bookmark.getString("id")))>-1)
				{	lastItemIDList.add(bookmark.getString("id"));
					continue;
				}
				Prefs.addItemID(getMContext(), bookmark.getInt("id"));
				HttpResponse response=doGetInputStream("https://www.readability.com"+articleHref);
				content=getContent(getInputStreamFromResponse(response));
					JSONObject article=null;
					try{article=new JSONObject(content);}catch(Exception e){e.printStackTrace();continue;}
					IItem item=new IItem();
					if(!article.isNull("author"))
						item.author=article.getString("author");
					if(!article.isNull("content")) item.content=article.getString("content");
					else item.content="";
					if(!article.isNull("url")) item.link=article.getString("url");
					if(!article.isNull("lead_image_url")) item.image=article.getString("lead_image_url");
					if(!article.isNull("title")) item.title=article.getString("title");
					if(!article.isNull("id")) item.uid=bookmark.getString("id");
					
					for (int j=0; j<tags.length(); j++)
					{
						item.addTag(tags.getJSONObject(j).getString("id"));
						android.util.Log.v("idltd","adding to item:"+Integer.toString(i)+" tag: "+tags.getJSONObject(j).getString("text"));
					}
					SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					if(!article.isNull("date_published")) item.publishedTime=format.parse(article.getString("date_published")).getTime();
					if(!bookmark.isNull("date_added")) item.updatedTime=format.parse(bookmark.getString("date_added")).getTime()/1000;
					else item.updatedTime=item.publishedTime/1000;
					if((entryLength+item.getLength())>MAX_TRANSACTION_LENGTH)
					{	try {	itemHandler.items(itemlist, item.getLength()); }
						catch(Exception e) { Log.e("Readability.handleItem",e.toString()+" :  TransactionLength: "+entryLength); }
						itemlist.clear();
						entryLength=0;
					}
					item.starred = bookmark.getBoolean("favorite");
					if (item.starred) item.addTag(ReadabilityClient.starredTag);
				   // if (item.starred) item.addTag("Favourites");
					item.read = bookmark.getBoolean("archive");
					trace("item: "+item.uid.toString()+" read: "+ (item.read ? "true":"false"));
					trace("item: "+item.uid.toString()+" starred: "+ (item.starred ? "true":"false"));
					entryLength=entryLength+item.getLength();
					itemlist.add(item);
					lastItemIDList.add(item.uid);
				}
				itemHandler.items(itemlist, entryLength);
		}
		catch (Exception e) { e.printStackTrace(); }
	}

	private void trace(String message)
	{	android.util.Log.v("idltd",message); }
	
	@Override
	public void handleReaderList(ITagListHandler tagHandler,ISubscriptionListHandler subscriptionHandler, long arg2) throws IOException,ReaderException 
	{
		//tags
		try {
			tagList=new ArrayList<ITag>();
			ITag tag=new ITag();
			tag.label="Favorites";
			tag.uid=ReadabilityClient.starredTag;
			tag.type=ITag.TYPE_TAG_STARRED;
			tagList.add(tag);
			
			HttpResponse response=doGetInputStream("https://www.readability.com/api/rest/v1/tags");
			String content=getContent(getInputStreamFromResponse(response));
			JSONObject obj=new JSONObject(content);
			
			JSONArray array=(JSONArray)obj.get("tags");
			for(int i=0;i<array.length();i++)
			{
//				{"id": 44, "text": "new yorker", "applied_count": 3, "bookmark_ids": [1, 4, 539]}
				obj=array.getJSONObject(i);
				tag=new ITag();
				tag.label=obj.getString("text");
				tag.uid=obj.getString("id");
				tag.type=ITag.TYPE_TAG_LABEL;
				Log.v("idltd","id: "+tag.uid+" text: "+tag.label);
				tagList.add(tag);
			}
			tagHandler.tags(tagList); // Register Tags
	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean markAllAsRead(String stream, String title, String[] excludedStreams,long syncTime) throws IOException, ReaderException
	{
		//keine implementierung in Readability vorgesehen
		return true;
	}

	@Override
	public boolean markAsRead(String[] itemUids, String[] subUids) throws IOException,ReaderException 
	{
		for (String itemUid: itemUids) {
			try {
				trace("marking read");
				ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
				nvps.add(new BasicNameValuePair("archive","1"));			
				doPostInputStream("https://www.readability.com/api/rest/v1/bookmarks/"+itemUid, nvps);
				trace("marked read");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}
	    return true;
	}

	@Override
	public boolean markAsUnread(String[] itemUids, String[] subUids, boolean keepUnread)throws IOException, ReaderException 
	{
		for (String itemUid: itemUids) {
			try {
				trace("marking unread");			
				ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
				nvps.add(new BasicNameValuePair("archive","0"));
				doPostInputStream("https://www.readability.com/api/rest/v1/bookmarks/"+itemUid, nvps);
				trace("marked unread");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}
	    return true;
	}
	@Override
	public boolean renameTag(String arg0, String arg1, String arg2)throws IOException, ReaderException 
	{
		//nicht implementiert in FeedBin
		return false;
	}
	
    private void handleError(String error) throws ReaderException {
        if (error != null && error.equals("NOT_LOGGED_IN")) {
                throw new ReaderLoginException("NOT_LOGGED_IN");
        } else {
                throw new ReaderException(error);
        }
	}

	public DefaultHttpClient getClient() {
		if (client == null)
			client = HttpUtils.createHttpClient();
		return client;
	}

	public static DefaultHttpClient createHttpClient() {
		try {
			HttpParams params = new BasicHttpParams();

			// Turn off stale checking. Our connections break all the time
			// anyway,
			// and it's not worth it to pay the penalty of checking every time.
			HttpConnectionParams.setStaleCheckingEnabled(params, false);

			// Set the timeout in milliseconds until a connection is
			// established. The default value is zero, that means the timeout is
			// not used.
			HttpConnectionParams.setConnectionTimeout(params,
					SOCKET_OPERATION_TIMEOUT);
			// Set the default socket timeout (SO_TIMEOUT) in milliseconds which
			// is the timeout for waiting for data.
			HttpConnectionParams.setSoTimeout(params, SOCKET_OPERATION_TIMEOUT);

			HttpConnectionParams.setSocketBufferSize(params, 8192);

			// Don't handle redirects -- return them to the caller. Our code
			// often wants to re-POST after a redirect, which we must do
			// ourselves.
			HttpClientParams.setRedirecting(params, false);

			// HttpProtocolParams.setUserAgent(params, userAgent);
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
			HttpProtocolParams.setUseExpectContinue(params, true);

			// ssl
			KeyStore trustStore = KeyStore.getInstance(KeyStore
					.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(
					params, registry);

			return new DefaultHttpClient(ccm, params);
		} catch (Exception e) {
			return HttpUtils.createHttpClient();
		}
	}
    
    public HttpResponse doGetInputStream(String url) throws ClientProtocolException, IOException, ReaderException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException
    {
		HttpGet post = new HttpGet(url);
		post.addHeader("Content-Type", "application/x-www-form-urlencoded");		
		String tokenSecret=Prefs.getOAuthTokenSecret(getMContext());
		String token=Prefs.getOAuthToken(getMContext());
		OAuthConsumer mConsumer = new CommonsHttpOAuthConsumer(Prefs.KEY, Prefs.SECRET);
		mConsumer.setTokenWithSecret(token, tokenSecret);
		mConsumer.sign(post);
		
		HttpResponse response = getClient().execute(post);

		return toastCode(response);
    }
	
	public HttpResponse doPostInputStream(String url,List<NameValuePair>params) throws IOException, ReaderException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException 
	{
		// HttpClient client = new DefaultHttpClient();

		HttpPost post = new HttpPost(url);
		
		post.addHeader("Content-Type", "application/x-www-form-urlencoded");
		post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));		
		
		String tokenSecret=Prefs.getOAuthTokenSecret(getMContext());
		String token=Prefs.getOAuthToken(getMContext());
		OAuthConsumer mConsumer = new CommonsHttpOAuthConsumer(Prefs.KEY, Prefs.SECRET);
		mConsumer.setTokenWithSecret(token, tokenSecret);
		mConsumer.sign(post);
	
		android.util.Log.v("idltd","post requst: "+url);

		HttpResponse response  = null;	
		try { response = getClient().execute(post); }
		catch (Exception e) { e.printStackTrace(); }
		return toastCode(response);
	}
	public HttpResponse doDeleteInputStream(String url) throws IOException, ReaderException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException 
	{
		// HttpClient client = new DefaultHttpClient();

		HttpDelete post = new HttpDelete(url);

		//post.addHeader("Content-Type", "application/x-www-form-urlencoded");
		//post.set setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));		

		String tokenSecret=Prefs.getOAuthTokenSecret(getMContext());
		String token=Prefs.getOAuthToken(getMContext());
		OAuthConsumer mConsumer = new CommonsHttpOAuthConsumer(Prefs.KEY, Prefs.SECRET);
		mConsumer.setTokenWithSecret(token, tokenSecret);
		mConsumer.sign(post);

		android.util.Log.v("idltd","post requst: "+url);

		HttpResponse response  = null;	
		try { response = getClient().execute(post); }
		catch (Exception e) { e.printStackTrace(); }
		return toastCode(response);
	}

	private HttpResponse toastCode( HttpResponse response)
	{	Integer responseCode = response.getStatusLine().getStatusCode();
		if (responseCode==200 || responseCode==202) return response;
		if (responseCode == 401) AndroidUtils.showToast(getMContext(), "Authorization Required: Authentication failed or was not provided.");
		else if (responseCode == 404) AndroidUtils.showToast(getMContext(), "Not Found: The resource that you requested does not exist.");
		else if (responseCode == 500) AndroidUtils.showToast(getMContext(), "Internal Server Error: An unknown error has occurred.");
		else if (responseCode == 400) AndroidUtils.showToast(getMContext(), "Bad Request: The server could not understand your request.");
		else if (responseCode == 409) AndroidUtils.showToast(getMContext(), "Conflict: The resource that you are trying to create already exists.");
		else if (responseCode == 403) AndroidUtils.showToast(getMContext(), "Forbidden: You are not allowed to perform the requested action.");
		else AndroidUtils.showToast(getMContext(), "HTTP Error: "+response.getStatusLine().getReasonPhrase());
		return null;
	}
	
	public InputStream getInputStreamFromResponse(HttpResponse response) throws ReaderException, IllegalStateException, IOException
	{	final HttpEntity entity = response.getEntity();
		if (entity == null) { throw new ReaderException("null response entity"); }

		InputStream is = null;
		// create the appropriate stream wrapper based on the encoding type
		String encoding = HttpUtils.getHeaderValue(entity.getContentEncoding());
		if (encoding != null && encoding.equalsIgnoreCase("gzip")) { is = new GZIPInputStream(entity.getContent()); }
		else if (encoding != null && encoding.equalsIgnoreCase("deflate"))
		{	is = new InflaterInputStream(entity.getContent(),
			new Inflater(true));
		}
		else is = entity.getContent();
		
		return new FilterInputStream(is) {
			@Override
			public void close() throws IOException {
				super.close();
				entity.consumeContent();
			}
		}
	}

	public boolean login(String user, String password, Context mContext) throws IOException,ReaderException, JSONException {
		
		HttpClient client = new DefaultHttpClient();
		HttpPost request = new HttpPost(Prefs.AUTHORIZE_URL);
		CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(Prefs.KEY, Prefs.SECRET);
		List<BasicNameValuePair> params = Arrays.asList(new BasicNameValuePair("x_auth_username", user), new BasicNameValuePair("x_auth_password", password), new BasicNameValuePair("x_auth_mode", "client_auth"));
		UrlEncodedFormEntity entity = null;
android.util.Log.v("idltd","attempt login");
try {
			entity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("wtf");
		}
		request.setEntity(entity);
		try { consumer.sign(request);}
		catch (OAuthMessageSignerException e) { return false; }
		catch (OAuthExpectationFailedException e) { return false; }
		catch (OAuthCommunicationException e) { return false; }

		HttpResponse response;
		InputStream data = null;
		try {
			android.util.Log.v("idltd","sending login request");		
			response = client.execute(request);
			android.util.Log.v("idltd","executed "+response.getStatusLine());
			if (response.getStatusLine().getStatusCode()==401) return false;
			data = response.getEntity().getContent();
			android.util.Log.v("idltd","data received");
			
		} catch (ClientProtocolException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

		String responseString = null;
		try {
			final char[] buffer = new char[0x10000];
			StringBuilder out = new StringBuilder();
			Reader in = new InputStreamReader(data, HTTP.UTF_8);
			int read;
			do {	read = in.read(buffer, 0, buffer.length);
					if (read > 0) out.append(buffer, 0, read);
			} while (read >= 0);
			in.close();
			responseString = out.toString();
		} catch (IOException ioe) {
			// throw new IllegalStateException("Error while reading response body", ioe);
			return false;
		}
		String[]rPart=responseString.split("&");
		String tokenSecret=rPart[0].substring(rPart[0].indexOf("=")+1);
		String token=rPart[1].substring(rPart[1].indexOf("=")+1);		
		Prefs.setOAuth(getMContext(), tokenSecret, token);
		return true;
	}

	
	
	public String getContent(InputStream in) throws IOException
	{
		return getContent(in,"UTF-8");
	}
	public String getContent(InputStream in,String encoding) throws IOException
	{
		StringBuffer sb=new StringBuffer();
		int len=10000;
		byte[]b=new byte[len];
		while(len!=1)
		{
			len=in.read(b);
			if(len==-1)
				break;
			sb.append(new String(b,0,len,encoding));
		}
		in.close();
		return sb.toString();
	}
}
