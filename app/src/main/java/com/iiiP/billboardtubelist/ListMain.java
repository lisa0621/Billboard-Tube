package com.iiiP.billboardtubelist;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeIntents;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.splunk.mint.Mint;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class ListMain extends ListActivity {

	private ListView lv;

	private LinkedList<HashMap<String, Object>> rssdata;
	private Document document;

	private ArrayList<String> titles;

	private static final String DEV_KEY = "AIzaSyDSzcSp7BVggGkQjpz2Cus9VPQwJHxPlbQ";

    public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    public static final JsonFactory JSON_FACTORY = new JacksonFactory();

	private static final HttpRequestInitializer HTTP_REQUEST_INITIALIZER = new HttpRequestInitializer() {

		
		@Override
		public void initialize(HttpRequest request) throws IOException {

		}
	};

	private static YouTube youtube;
	private static final long NUMBER_OF_VIDEOS_RETURNED = 1;
	String sList = "id,snippet";
	String sType = "video";
	String sFields = "items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)";
	static String VIDEO_ID = "";
	static String thumbnailimg = "";

	private SQLitHelper helper;
	private static final String createTable = "CREATE TABLE IF NOT EXISTS youtubedb"
			+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ "yindex INTEGER, ytitle TEXT, yartist TEXT, yvideoimg TEXT)";
	public static final String PREFS_NAME = "MyPrefsFile";
	private List<MyRecord> exampleRecords;
	private RemoteImageHelper lazyImageHelper = new RemoteImageHelper();
	static URL img_url;
	private MenuItem menuItem;
	private MyAdapter adapter;
	private PullToRefreshListView mPullRefreshListView;
	private ActionBar actionBar;
	SharedPreferences settings;
	private String pubDateString = null;
	//Date firstTime;
	//Timer timer;

    private static boolean isDeubgMode = true;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Mint.initAndStartSession(ListMain.this, "5ca86f8b");
		setContentView(R.layout.activity_ptr_list);
		actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setTitle("Billboard Hot 100");
		//actionBar.setSubtitle(pubDateString);
		settings = getSharedPreferences(PREFS_NAME,
				MODE_PRIVATE);
		boolean isRssStart = settings.getBoolean("RSSMode", false);
		String pubDate = settings.getString("pubDate", "");
		helper = new SQLitHelper(this, "youtubetest", null, 1);
		
		mPullRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);
		mPullRefreshListView.setOnRefreshListener(new OnRefreshListener<ListView>() {
			
			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				String label = DateUtils.formatDateTime(getApplicationContext(), System.currentTimeMillis(),
						DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);

				// Update the LastUpdatedLabel
				refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);
				new TestTask(ListMain.this).execute();
			}
		});
		
		lv = mPullRefreshListView.getRefreshableView();
		registerForContextMenu(lv);
		if (!isRssStart) {
			FirstTask first = new FirstTask(ListMain.this);
			first.execute("first");
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("RSSMode", true);
			editor.commit();
			
		} else {
			fillRecord();
			lv.setAdapter(new MyAdapter(this));
			actionBar.setSubtitle(settings.getString("pubDate", ""));
//			timer = new Timer();
//			toDate();
		}
		
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index,
					long arg3) {
				toFunction(index, 0);
			}
		});
		 
		
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	    // Checks the orientation of the screen
	    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {	      	
//			lv.setAdapter(new MyAdapter(this));
			Log.i("lisa","fL");
	    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
	      	
//			lv.setAdapter(new MyAdapter(this));
			Log.i("lisa","fP");
	    }
	}

	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle("Play Mode");
		menu.setHeaderIcon(R.drawable.play);
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.youtubemenu, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		int x = info.position;
		switch (item.getItemId()) {
        
        case R.id.auto:
        		toFunction(x, 0);
        		Log.i("iQuery",String.valueOf(x));
            return true;
        case R.id.option:
        		toFunction(x, 1);
        		Log.i("iQuery",String.valueOf(x));
            return true;
        default:
        	return super.onContextItemSelected(item);
        }
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_load:
			menuItem = item;
			menuItem.setActionView(R.layout.progressbar);
			menuItem.expandActionView();
			ABTask abtask = new ABTask(ListMain.this);
			abtask.execute("test");
			
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
//	@Override
//	public void finish() {
//		if (timer != null){
//			timer.cancel();
//			timer = null;
//		}
//		super.finish();
//	}
	
	private class MyHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == 1){
				actionBar.setSubtitle(settings.getString("pubDate", ""));
				Log.i("mydate", "Handler" + settings.getString("pubDate", ""));
			}
		}
	}
	
//	public class DateTask extends TimerTask {
//	      
//	      @ Override
//	      public void run() {
//	    	  Log.i("mydate","Task 執行時間：" + new Date());
//	     }
//}
	
//	private void toDate(){
//		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzzz", Locale.US);
//		try {
//			firstTime = sdf.parse("Sat, 06 Mar 2013 05:00:00 GMT");
//			//firstTime = sdf.parse(pubDateString);
//			Log.i("mydate","pubdate"+ firstTime.getTime());
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		//Timer timer = new Timer();
//		timer.schedule(new MyTimerTask(), firstTime, 604800000);
//			
//			
//	}
	
//	private Handler mHandler = new Handler(); 
//	
//	public class MyTimerTask extends TimerTask {
//		
//		@Override
//		public void run() {
//	        mHandler.post(   
//	                new Runnable() {    
//	                    public void run() {  
//	                    	Log.i("mydate","Task 執行時間：" + new Date());
//	                        new ABTask(ListMain.this).execute("");   
//	                    } 
//	                }        
//	       );  
//			
//		}
//		
//	}
	
	
	private void toFunction(int index, int x) {
	
		Intent intent = new Intent();
		SQLiteDatabase y_db = helper.getReadableDatabase();
		String fquery = "";
		
		try {
			Cursor c = y_db.rawQuery(
					"SELECT ytitle, yartist FROM youtubedb WHERE _id = "
							+ index, null);
			c.moveToFirst();

			if (c != null) {
				if (c.moveToFirst()) {
					do {
						String sName = c.getString(c.getColumnIndex("ytitle"));
						String sArtist = c.getString(c
								.getColumnIndex("yartist"));
						//fquery = sName + ", " + sArtist;
						fquery = sArtist + " - " + sName;
						Log.i("queryty", fquery);
					} while (c.moveToNext());
				}
			}
			c.close();
			y_db.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		SearchYoutube svideo = new SearchYoutube(fquery);
		svideo.start();
		while (!svideo.isDone)
			;		
		switch(x){
			case 0:
				intent = YouTubeStandalonePlayer.createVideoIntent(this,
						DeveloperKey.DEVELOPER_KEY, VIDEO_ID, 0, true, false);
				
				
				break;
			case 1:
				intent = YouTubeIntents.createSearchIntent(this, fquery);
				break;
		}	
		
		startActivity(intent);
	}

	private class FirstTask extends AsyncTask<String, Void, String> {

		private ProgressDialog dialog;
		private Context context;
		private ListActivity activity;
		public FirstTask(ListActivity activity) {
			this.activity = activity;
			context = activity;
			dialog = new ProgressDialog(context);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			this.dialog.setTitle("Downloading...");
			this.dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			this.dialog.setMessage("Loading...");
			this.dialog.setCancelable(false);
			this.dialog.show();
		}

		protected String doInBackground(String... params) {
			ReadRSS rssreader = new ReadRSS();
			rssreader.start();
			while (!rssreader.isDone)
				;
			addRssDataToSqlite();
			editYoutubeImgToSqlite();
			fillRecord();
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (dialog.isShowing())
				dialog.dismiss();
			
			activity.setListAdapter(new MyAdapter(activity));
			
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("pubDate", pubDateString);
			editor.commit();
			MyHandler handler = new MyHandler();
			Message msg = new Message();
			msg.what = 1;
			handler.sendMessage(msg);
		}
	}

	private class TestTask extends AsyncTask<String, Void, String> {
//		private ProgressDialog dialog;
		private Context context;
		private ListActivity activity;

		public TestTask(ListActivity activity) {
			this.activity = activity;
			context = activity;
			// dialog = new ProgressDialog(context);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// this.dialog.setTitle("Updating data!");
			// this.dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			// this.dialog.setMessage("Loading...");
			// this.dialog.setCancelable(false);
			// this.dialog.show();
		}

		protected String doInBackground(String... params) {
			initAdapter();
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			
			//menuItem.collapseActionView();
			//menuItem.setActionView(null);
			// if (dialog.isShowing())
			// dialog.dismiss();
			//MyAdapter adapter = new MyAdapter(activity);
            //adapter.notifyDataSetChanged();
			//activity.setListAdapter(adapter);	
			//lv.setAdapter(adapter);
            
			//new MyAdapter(activity).notifyDataSetChanged();
			activity.setListAdapter(new MyAdapter(activity));
			// Call onRefreshComplete when the list has been refreshed.
			mPullRefreshListView.onRefreshComplete();
			

			SharedPreferences.Editor editor = settings.edit();
			editor.putString("pubDate", pubDateString);
			editor.commit();
			MyHandler handler = new MyHandler();
			Message msg = new Message();
			msg.what = 1;
			handler.sendMessage(msg);
			super.onPostExecute(result);
		}
	}

	
	private class ABTask extends AsyncTask<String, Void, String> {
//		private ProgressDialog dialog;
		private Context context;
		private ListActivity activity;

		public ABTask(ListActivity activity) {
			this.activity = activity;
			context = activity;
			// dialog = new ProgressDialog(context);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// this.dialog.setTitle("Updating data!");
			// this.dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			// this.dialog.setMessage("Loading...");
			// this.dialog.setCancelable(false);
			// this.dialog.show();
		}

		protected String doInBackground(String... params) {
			initAdapter();
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			
			menuItem.collapseActionView();
			menuItem.setActionView(null);
			// if (dialog.isShowing())
			// dialog.dismiss();
			//MyAdapter adapter = new MyAdapter(activity);
            //adapter.notifyDataSetChanged();
			//activity.setListAdapter(adapter);	
			//lv.setAdapter(adapter);
            
			//new MyAdapter(activity).notifyDataSetChanged();
			activity.setListAdapter(new MyAdapter(activity));
			// Call onRefreshComplete when the list has been refreshed.
			//mPullRefreshListView.onRefreshComplete();

			SharedPreferences.Editor editor = settings.edit();
			editor.putString("pubDate", pubDateString);
			editor.commit();
			MyHandler handler = new MyHandler();
			Message msg = new Message();
			msg.what = 1;
			handler.sendMessage(msg);
			super.onPostExecute(result);
		}
	}

	private void initAdapter() {
		SQLiteDatabase dbc = helper.getWritableDatabase();
		dbc.execSQL("DROP TABLE IF EXISTS youtubedb");
		dbc.execSQL(createTable);
		dbc.close();
		ReadRSS rreader = new ReadRSS();
		rreader.start();
		while (!rreader.isDone)
			;
		addRssDataToSqlite();
		editYoutubeImgToSqlite();
		fillRecord();
		
		
	}

	private void addRssDataToSqlite() {
		int scount = rssdata.size();
		for (int i = 0; i < scount; i++) {
			String aName = rssdata.get(i).get("name").toString();
			String aArtist = rssdata.get(i).get("artist").toString();
			addData(i, aName, aArtist,
					"");
		}
	}

	private void addData(int index, String title, String artist, String videoimg) {
		SQLiteDatabase temp_db = helper.getWritableDatabase();
		ContentValues values = new ContentValues();
		int pindex = index;
		String ptitle = title;
		String partist = artist;
		String pvideoimg = videoimg;
		values.put("yindex", pindex);
		values.put("ytitle", ptitle);
		values.put("yartist", partist);
		values.put("yvideoimg", pvideoimg);
		temp_db.insert("youtubedb", null, values);
		temp_db.close();
	}

	private void editYoutubeImgToSqlite() {
		 SQLiteDatabase edit_db = helper.getReadableDatabase();
		 try{	
			 Cursor ce = edit_db.rawQuery("SELECT _id, ytitle, yartist FROM youtubedb",
			 null);
			 int sCount = ce.getCount();
			 Log.i("querysqlyou","sCount"+sCount);
			 ce.moveToFirst();		
			 if (ce != null ) {
				 if (ce.moveToFirst()) {
				 do {
					 int sId = ce.getInt(ce.getColumnIndex("_id"));
					 String sName = ce.getString(ce.getColumnIndex("ytitle"));
					 String sArtist = ce.getString(ce.getColumnIndex("yartist"));
					 editData(sId, sName, sArtist);
					 Log.i("querysqlyou","sId, sName, sArtist:" + sId + ":" + sName + sArtist);
					 
				 }while (ce.moveToNext());
			 }
		 }
		
		 }catch(Exception ex){
			 Log.i("querysqlyou","ex"+String.valueOf(ex));
			 ex.printStackTrace();
		 }
	}

	private void editData(int index, String name, String artist) {
		//String equery = name + ", " + artist;
		String equery = artist + " - " + name;
		SearchYoutube ty = new SearchYoutube(equery);
		ty.start();
		while (!ty.isDone)
			;
		String inputImg = "";
		inputImg = thumbnailimg;
		SQLiteDatabase up_db = helper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("yvideoimg", inputImg);
		Log.i("querysqlyou","inputImg:" + inputImg);
		up_db.update("youtubedb", values, "_id = ?",
				new String[] { String.valueOf(index) });
		up_db.close();
	}

	private void fillRecord() {
		exampleRecords = new ArrayList<MyRecord>();
		SQLiteDatabase a_db = helper.getReadableDatabase();
		try {
			Cursor c = a_db.rawQuery("SELECT * FROM youtubedb", null);
			c.moveToFirst();
			if (c != null) {
				if (c.moveToFirst()) {
					do {
						int sIndex = c.getInt(c.getColumnIndex("yindex"));
						int sId = c.getInt(c.getColumnIndex("_id"));
						String sName = c.getString(c.getColumnIndex("ytitle"));
						String sArtist = c.getString(c
								.getColumnIndex("yartist"));
						String sVideoimg = c.getString(c
								.getColumnIndex("yvideoimg"));
						exampleRecords.add(new MyRecord(sIndex, sId + ":" + sName,
								sArtist, sVideoimg));
					} while (c.moveToNext());
				}
			}
			
			c.close();
			a_db.close();
			Log.i("lisa", "fillRecord");

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	class MyAdapter extends ArrayAdapter<MyRecord> {

		public MyAdapter(Context context) {
			super(context, R.layout.list_main, R.id.item_name, exampleRecords);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			MyRecord record = getItem(position);
			TextView i_name = (TextView) view.findViewById(R.id.item_name);
			TextView i_artist = (TextView) view.findViewById(R.id.item_artist);
			ImageView i_img = (ImageView) view.findViewById(R.id.item_img);
			i_name.setText(record.getLabel());
			i_artist.setText(record.getArtist());
			lazyImageHelper.loadImage(i_img, record.getImageUrl(), true);
			return view;
		}
	}

	public Bitmap getBitmap(String bitmapUrl) {
		try {
			URL url = new URL(bitmapUrl);
			return BitmapFactory.decodeStream(url.openConnection()
					.getInputStream());
		} catch (Exception ex) {
			return null;
		}
	}

	public class ReadRSS extends Thread {

		boolean isDone = false;

		@Override
		public void run() {
			super.run();
			try {
				URL url = new URL(
						"http://www.billboard.com/rss/charts/hot-100");
				SAXReader reader = new SAXReader();
				document = reader.read(url.openStream());
				Element channel = (Element) document.getRootElement().element(
						"channel");
				titles = new ArrayList();
				rssdata = new LinkedList();
				pubDateString = channel.elementIterator("item").next().elementText("pubDate");
//				DateFormat RSSformat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
//				Date pubdate = RSSformat.parse(pubDateString);
//
                if(isDeubgMode){
                    Log.i("RSS",pubDateString);

                    //Log.i("RSS","pubdate"+String.valueOf(pubdate));
                }

				
				for (Iterator i = channel.elementIterator("item"); i.hasNext();) {
					Element element = (Element) i.next();
					titles.add(element.elementText("title"));
					// links.add(element.elementText("link"));
					// descibe.add(element.elementText("description"));

                    if(isDeubgMode){
                        Log.i("RSS",element.elementText("title"));
                        Log.i("RSS",element.elementText("link"));
                        Log.i("RSS",element.elementText("artist"));
                        Log.i("RSS",element.elementText("chart_item_title"));
                        Log.i("RSS",element.elementText("rank_this_week"));
                        Log.i("RSS",element.elementText("rank_last_week"));
                        Log.i("RSS",element.elementText("description"));

                        //Log.i("RSS","pubdate"+String.valueOf(pubdate));
                    }

                    HashMap<String, Object> item = new HashMap<String, Object>();
                    item.put("name", element.elementText("chart_item_title"));
                    item.put("artist", element.elementText("artist"));
                    rssdata.add(item);


                }

//				for (int i = 0; i < titles.size(); i++) {
//					HashMap<String, Object> item = new HashMap<String, Object>();
//					String a[] = titles.get(i).split(":");
//					String t[] = a[1].split(",");
//					item.put("name", t[0]);
//					item.put("artist", t[1]);
//					rssdata.add(item);
//
//                    if(isDeubgMode) {
//                        Log.i("RSS", "name"+ t[0] +"artist"+ t[1]);
//                    }
//				}


                if(isDeubgMode){
                    Log.i("RSS","rssdata" + rssdata.size());
                }

			} catch (Exception e) {
                if(isDeubgMode) {
                    Log.i("RSS", e.toString());
                }
				e.printStackTrace();
			}

			isDone = true;

		}

	}

	public class SearchYoutube extends Thread {
		boolean isDone = false;
		String iQuery = "";
		public SearchYoutube(String inputquery){
			super();
			iQuery = inputquery;
            if(isDeubgMode) {
                Log.i("SearchYoutube", "iQuery" + iQuery);
            }
		}
		
		@Override
		public void run() {
			super.run();
			try {
				youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY,
						HTTP_REQUEST_INITIALIZER).setApplicationName(
						"iYoutubeSearchTest").build();
				YouTube.Search.List search = youtube.search().list(sList);
				search.setKey(DEV_KEY);
				search.setQ(iQuery);
				search.setType(sType);
				search.setFields(sFields);
				//search.setVideoEmbeddable("true");
				search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

				SearchListResponse searchResponse = search.execute();

				List<SearchResult> searchResultList = searchResponse.getItems();

				if (searchResultList != null) {
					prettyPrint(searchResultList.iterator(), iQuery);
				}

			} catch (GoogleJsonResponseException e) {
                if(isDeubgMode) {
                    Log.i("SearchYoutube", "There was a service error: "
                            + e.getDetails().getCode() + " : "
                            + e.getDetails().getMessage());
                }
			} catch (IOException e) {
                if(isDeubgMode) {
                    Log.i("SearchYoutube", "There was an IO error: " + e.getCause()
                            + " : " + e.getMessage());
                }
			} catch (Throwable t) {
				t.printStackTrace();
			}

			isDone = true;

		}


	}

	private static void prettyPrint(
			Iterator<SearchResult> iteratorSearchResults, String query) {

		if (!iteratorSearchResults.hasNext()) {
            if(isDeubgMode) {
                Log.i("SearchYoutube", " There aren't any results for your query.");
            }
		}

		while (iteratorSearchResults.hasNext()) {

			SearchResult singleVideo = iteratorSearchResults.next();
			ResourceId rId = singleVideo.getId();
			
			// Double checks the kind is video.
			if (rId.getKind().equals("youtube#video")) {
				Thumbnail thumbnail = (Thumbnail) singleVideo.getSnippet().getThumbnails()
						.get("default");
                if(isDeubgMode) {
                    Log.i("SearchYoutube", " Video Id" + rId.getVideoId());
                    Log.i("SearchYoutube", " Title: "
                            + singleVideo.getSnippet().getTitle());
                    // Log.i("yquery", " Thumbnail: " + thumbnail.getUrl());
                }

				VIDEO_ID = rId.getVideoId();
				thumbnailimg = thumbnail.getUrl();
				 

			}
		}
	}

}
