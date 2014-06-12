package com.noinnion.android.newsplus.extension.readability;


	import java.net.MalformedURLException;
	import java.net.URL;

	import android.app.Activity;
	import android.content.Context;
	import android.content.Intent;
	import android.os.AsyncTask;
	import android.os.Bundle;
	import android.util.Log;
	import android.util.Patterns;
	import android.widget.Toast;

import java.util.ArrayList;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

	import com.noinnion.android.reader.api.ReaderException;
// import com.noinnion.android.newsplus.readability.util.Utils;

	public class AddActivity extends Activity {

		/* 
		 * Add anew URL to Pocket using Pocket+
		 */
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			Intent intent = getIntent();
			String action = intent.getAction();
			String type = intent.getType();

			if (Intent.ACTION_SEND.equals(action) && type != null && "text/plain".equals(type)) {
				try {
					String urlStr = extractURL(intent.getStringExtra(Intent.EXTRA_TEXT));
					URL u = new URL(urlStr);
					new AddToReadability().execute(u.toString());
				}
				catch (MalformedURLException e) {
					Log.e("Pocket+ Debug", "Add to Pocket+ Exception:" + e.getMessage());
					Context c = getApplicationContext();
					Toast.makeText(c, getString(R.string.not_added_invalid), Toast.LENGTH_LONG).show();
				}
			}
			finish();
		}

		/*
		 * Asynchronous call to add a URL to Pocket
		 */
		private class AddToReadability extends AsyncTask<String, Void, Boolean> {
			protected Boolean doInBackground(String... params) {
				final Context c = getApplicationContext();
				try {
					ReadabilityClient rc = new ReadabilityClient(c);
					ArrayList<NameValuePair> nvps =new ArrayList<NameValuePair>();
					nvps.add(new BasicNameValuePair("url",params[0]));
					nvps.add(new BasicNameValuePair("favorite","0"));	
					nvps.add(new BasicNameValuePair("archive","0"));
					rc.doPostInputStream("https://www.readability.com/api/rest/v1/bookmarks/",nvps);
				}
				catch (Exception e) {
					Log.e("Readability+ Debug", "JSONException: " + e.getMessage());
					return false;
				}
				return true;
			}

			protected void onPostExecute(Boolean result) {
				final Context c = getApplicationContext();
				if (result) {
					Toast.makeText(c, getString(R.string.added), Toast.LENGTH_SHORT).show();
				}
				else {
					Toast.makeText(c, getString(R.string.not_added), Toast.LENGTH_LONG).show();
				}
			}
		}
	// Extract URLs from text containing them
    public static String extractURL(String args) {
        String s = args;
        String [] parts = s.split("\\s");
        String withURL = "";
        for (String item : parts) {
            if (Patterns.WEB_URL.matcher(item).matches()) {
                return item;
            }
        }
        return withURL;
    }
	}
