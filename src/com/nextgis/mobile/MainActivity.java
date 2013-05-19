/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Baryshnikov Dmitriy (aka Bishop), polimax@mail.ru
 ******************************************************************************
*   Copyright (C) 2012-2013 NextGIS
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.mobile;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.DirectedLocationOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockActivity implements OnNavigationListener{
	final static String TAG = "nextgismobile";	

	private MapView mOsmv;
	private ResourceProxy mResourceProxy;	
	//overlays
	private MyLocationOverlay mLocationOverlay;
	private DirectedLocationOverlay mDirectedLocationOverlay;
	private ItemizedIconOverlay<OverlayItem> mPointsOverlay;
	
	private RelativeLayout rl;
 
	private LocationManager mLocationManager;
	protected ChangeLocationListener mChangeLocationListener;
	private ArrayList<OverlayItem> maItems;
	
	private boolean mbInfoOn;
	private boolean mbGpxRecord;
 	private int mnTileSize;
	protected View mInfoView;
   
	final static String PREFS_TILE_SOURCE = "map_tile_source";	
	final static String PREFS_SCROLL_X = "map_scroll_x";
	final static String PREFS_SCROLL_Y = "map_scroll_y";
	final static String PREFS_ZOOM_LEVEL = "map_zoom_level";
	final static String PREFS_SHOW_LOCATION = "map_show_loc";
	final static String PREFS_SHOW_COMPASS = "map_show_compass";
	final static String PREFS_SHOW_INFO = "map_show_info";
	
	private final static int MENU_MARK = 0;
    private final static int MENU_RECORD_GPX = 1;
	private final static int MENU_INFO = 2;
    private final static int MENU_PAN = 3;
	public final static int MENU_SETTINGS = 4;
	public final static int MENU_ABOUT = 5;
	
	final static String CSV_CHAR = ";";
	final static int mNotifyId = 9999;
	final static int margings = 10;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        // initialize the default settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mInfoView =  inflater.inflate(R.layout.infopane, null, true);
       
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mChangeLocationListener = new ChangeLocationListener();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mbInfoOn = prefs.getBoolean(PREFS_SHOW_INFO, false);
		mbGpxRecord = prefs.getBoolean(PreferencesActivity.KEY_PREF_SW_TRACKGPX_SRV, false);
		mnTileSize = prefs.getInt(PreferencesActivity.KEY_PREF_TILE_SIZE + "_int", 256);
		
	    ActionBar actionBar = getSupportActionBar();
		Context context = actionBar.getThemedContext();
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.views, R.layout.sherlock_spinner_dropdown_item);
		adapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
	    
	    actionBar.setDisplayShowTitleEnabled(false);
	    actionBar.setNavigationMode(com.actionbarsherlock.app.ActionBar.NAVIGATION_MODE_LIST);
	    actionBar.setListNavigationCallbacks((SpinnerAdapter)adapter, this);

		rl = new RelativeLayout(this);
		
	    mResourceProxy = new ResourceProxyImpl(context);
		
		mOsmv = new MapView(this, mnTileSize, mResourceProxy);
		
		//add overlays
		mLocationOverlay = new MyLocationOverlay(this, mOsmv, mResourceProxy);
		mDirectedLocationOverlay = new DirectedLocationOverlay(this, mResourceProxy);
		mDirectedLocationOverlay.setShowAccuracy(true);

		//auto enable follow location if position is closed to center
		mOsmv.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_UP) {
					IGeoPoint map_center_pt = mOsmv.getMapCenter();					
					GeoPoint pt = mLocationOverlay.getMyLocation();
					if(map_center_pt == null || pt == null)
						return false;
					int nMaxDist = mOsmv.getBoundingBox().getDiagonalLengthInMeters() / 15;
					int nDist = pt.distanceTo(map_center_pt);
					if(nDist < nMaxDist){
						mLocationOverlay.enableFollowLocation();
					}
					else {
						mLocationOverlay.disableFollowLocation();
					}
				}
				return false;
			}
	    });		

		mOsmv.setMultiTouchControls(true);
		//m_Osmv.setBuiltInZoomControls(true);
		mOsmv.getOverlays().add(mDirectedLocationOverlay);
		mOsmv.getOverlays().add(mLocationOverlay);
		mOsmv.getOverlays().add(mPointsOverlay);
		
		rl.addView(mOsmv, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));		
		
		AddMapButtons(rl);
		
		this.setContentView(rl);
		
		mOsmv.getController().setZoom(prefs.getInt(PREFS_ZOOM_LEVEL, 1));
		mOsmv.scrollTo(prefs.getInt(PREFS_SCROLL_X, 0), prefs.getInt(PREFS_SCROLL_Y, 0));

		//mLocationOverlay.enableMyLocation();
		//m_LocationOverlay.enableCompass();
		mLocationOverlay.setDrawAccuracyEnabled(true);
		
		PanToLocation();

		mOsmv.setKeepScreenOn(true);
		
        Log.d(TAG, "MainActivity: onCreate");
	}

	@Override
	protected void onPause() {
		final SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
		edit.putString(PREFS_TILE_SOURCE, mOsmv.getTileProvider().getTileSource().name());
		edit.putInt(PREFS_SCROLL_X, mOsmv.getScrollX());
		edit.putInt(PREFS_SCROLL_Y, mOsmv.getScrollY());
		edit.putInt(PREFS_ZOOM_LEVEL, mOsmv.getZoomLevel());
		edit.putBoolean(PREFS_SHOW_LOCATION, mLocationOverlay.isMyLocationEnabled());
		edit.putBoolean(PREFS_SHOW_COMPASS, mLocationOverlay.isCompassEnabled());
		edit.putBoolean(PREFS_SHOW_INFO, mbInfoOn);
		
		if(mbInfoOn)			
			ShowInfo(false);		

		edit.commit();
		
		mLocationOverlay.disableMyLocation();
		mLocationOverlay.disableCompass();
		
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final String tileSourceName = prefs.getString(PREFS_TILE_SOURCE, TileSourceFactory.DEFAULT_TILE_SOURCE.name());
		try {
			final ITileSource tileSource = TileSourceFactory.getTileSource(tileSourceName);
			mOsmv.setTileSource(tileSource);
		} catch (final IllegalArgumentException ignore) {
		}
		
		if (prefs.getBoolean(PREFS_SHOW_LOCATION, true)) {
			mLocationOverlay.enableMyLocation();
		}
		if (prefs.getBoolean(PREFS_SHOW_COMPASS, false)) {
			mLocationOverlay.enableCompass();
		}
		mbInfoOn = prefs.getBoolean(PREFS_SHOW_INFO, false);
		if (mbInfoOn) {
			ShowInfo(true);
		}
		
		mbGpxRecord = prefs.getBoolean(PreferencesActivity.KEY_PREF_SW_TRACKGPX_SRV, false);
		if (mbGpxRecord) {
			startGPXRecord();
		}
		
		AddPointsToOverlay();
		
		PanToLocation();
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getSupportMenuInflater().inflate(R.menu.main, menu);
        menu.add(Menu.NONE, MENU_MARK, Menu.NONE, R.string.sMark)
        .setIcon(R.drawable.ic_location_place)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, MENU_RECORD_GPX, Menu.NONE, R.string.sGPXRecord)
        .setIcon(R.drawable.ic_gpx_record_start)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, MENU_INFO, Menu.NONE, R.string.sInfo)
        .setIcon(R.drawable.ic_action_about)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);		
        menu.add(Menu.NONE, MENU_PAN, Menu.NONE, R.string.sPan)
        .setIcon(R.drawable.ic_pan2)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);		
        menu.add(Menu.NONE, MENU_SETTINGS, Menu.NONE, R.string.sSettings)
        .setIcon(R.drawable.ic_action_settings)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);		
        menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, R.string.sAbout)
        .setIcon(R.drawable.ic_action_about)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);	
        
		//mOsmv.getOverlayManager().onCreateOptionsMenu((android.view.Menu) menu, Menu.FIRST + 1, mOsmv);
       return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
        case android.R.id.home:
            return false;
        case MENU_SETTINGS:
            // app icon in action bar clicked; go home
            Intent intentSet = new Intent(this, PreferencesActivity.class);
            intentSet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentSet);
            return true;
        case MENU_ABOUT:
            Intent intentAbout = new Intent(this, AboutActivity.class);
            intentAbout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentAbout);
            return true;	  
        case MENU_PAN:
			GeoPoint pt = mLocationOverlay.getMyLocation();
			if(pt != null)
			{
				mOsmv.getController().animateTo(pt);
				mLocationOverlay.enableFollowLocation();
			}
			else
			{
				Toast.makeText(this, R.string.error_loc_fix, Toast.LENGTH_SHORT).show();
			}   	
        	return true;
        case MENU_INFO:
        	ShowInfo();
        	return true;
        case MENU_RECORD_GPX:
        	onRecordGpx();
        	return true;
        case MENU_MARK:        	
        	return true;
        }
		return super.onOptionsItemSelected(item);
	}
 
    @Override
	public boolean onNavigationItemSelected(int position, long itemId) {
		if(position == 1){ 
			startActivity (new Intent(getApplicationContext(), com.nextgis.mobile.CompassActivity.class));
			return true;
	    }
		return true;
	}

	protected void AddMapButtons(RelativeLayout rl){
		final ImageView ivZoomIn = new ImageView(this);
		ivZoomIn.setImageResource(R.drawable.ic_plus);
		ivZoomIn.setId(R.drawable.ic_plus);
		
		final ImageView ivZoomOut = new ImageView(this);
		ivZoomOut.setImageResource(R.drawable.ic_minus);	
		ivZoomOut.setId(R.drawable.ic_minus);			

		ivZoomIn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mOsmv.getController().zoomIn();
				ivZoomOut.getDrawable().setAlpha(255);	
				if(!mOsmv.canZoomIn())
				{
					ivZoomIn.getDrawable().setAlpha(50);						
				}
			}
		});				
		
		ivZoomOut.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mOsmv.getController().zoomOut();
				
				ivZoomIn.getDrawable().setAlpha(255);
				if(!mOsmv.canZoomOut())
				{						
					ivZoomOut.getDrawable().setAlpha(50);
				}
			}
		});
		
		final RelativeLayout.LayoutParams RightParams1 = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		RightParams1.setMargins(margings, margings, margings, margings);
		RightParams1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		RightParams1.addRule(RelativeLayout.CENTER_IN_PARENT);//ALIGN_PARENT_TOP
		rl.addView(ivZoomIn, RightParams1);
		
		final RelativeLayout.LayoutParams RightParams2 = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		RightParams2.setMargins(margings, margings, margings, margings);
		RightParams2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);			
		RightParams2.addRule(RelativeLayout.BELOW, R.drawable.ic_plus);
		rl.addView(ivZoomOut, RightParams2);	
	}
	
	protected void PanToLocation(){
		Location loc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if(loc != null){
			GeoPoint pt = new GeoPoint(loc.getLatitude(), loc.getLongitude());
			mOsmv.getController().animateTo(pt);
		}
	}	
	
	protected void AddPointsToOverlay(){
		//add new point		
		File file = new File(getExternalFilesDir(null), "points.csv");
		if (file != null) {
			Drawable ivPt10 = getResources().getDrawable(R.drawable.dot10);
        	InputStream in;
			try {
				in = new BufferedInputStream(new FileInputStream(file));

       	
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		        String line = reader.readLine();
		        int nCounter = 0;
		        while ((line = reader.readLine()) != null) {
		        	 nCounter++;
		        	 if( maItems.size() >= nCounter)
		        		 continue;
		             String[] RowData = line.split(CSV_CHAR);
 					 String sLat = RowData[1];
 					 String sLong = RowData[2];
 					 int nLatE6 = (int) (Float.parseFloat(sLat) * 1000000);
 					 int nLonE6 = (int) (Float.parseFloat(sLong) * 1000000);
		             OverlayItem item = new OverlayItem(RowData[9], RowData[10], new GeoPoint(nLatE6, nLonE6));
		             item.setMarker(ivPt10);
		             
		             mPointsOverlay.addItem(item);
		        }
		        
		        reader.close();
		        if (in != null) {
		        	in.close();
		    	} 
		    }
		    catch (IOException ex) {
		    	ex.printStackTrace();
			}			
		}
	}
	
	protected void LoadPointsToOverlay(){
		maItems = new ArrayList<OverlayItem>();
		Drawable ivPt10 = getResources().getDrawable(R.drawable.dot10);
		
		File file = new File(getExternalFilesDir(null), "points.csv");
		if (file != null) {
        	InputStream in;
			try {
				in = new BufferedInputStream(new FileInputStream(file));
       	
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		        String line = reader.readLine();
		        while ((line = reader.readLine()) != null) {
		             String[] RowData = line.split(CSV_CHAR);
 					 String sLat = RowData[1];
 					 String sLong = RowData[2];
 					 int nLatE6 = (int) (Float.parseFloat(sLat) * 1000000);
 					 int nLonE6 = (int) (Float.parseFloat(sLong) * 1000000);
		             OverlayItem item = new OverlayItem(RowData[9], RowData[10], new GeoPoint(nLatE6, nLonE6));
		             item.setMarker(ivPt10);
		             
		             maItems.add(item);
		        }
		        
		        reader.close();
		        if (in != null) {
		        	in.close();
		    	} 
		    }
		    catch (IOException ex) {
		    	ex.printStackTrace();
			}			
		}
		
		mPointsOverlay = new ItemizedIconOverlay<OverlayItem>(maItems, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
			public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
				//TODO: provide some text to tap
				//Toast.makeText( getApplicationContext(), "Item '" + item.mTitle + "' (index=" + index + ") got single tapped up", Toast.LENGTH_LONG).show();
				return true; // We 'handled' this event.
			}

			public boolean onItemLongPress(final int index, final OverlayItem item) {
				//TODO: provide some text to tap
				//Toast.makeText( getApplicationContext(), "Item '" + item.mTitle + "' (index=" + index + ") got long pressed", Toast.LENGTH_LONG).show();
				return false;
			}
		}
		, mResourceProxy);
	}
	
	void onRecordGpx(){
		/*m_bGpxRecord = !m_bGpxRecord;	
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor edit = prefs.edit();
		edit.putBoolean(PreferencesActivity.KEY_PREF_SW_TRACKGPX_SRV, m_bGpxRecord);
		edit.commit();
		
        final SharedPreferences.Editor editor1 = getSharedPreferences("preferences", Context.MODE_PRIVATE).edit();
        editor1.putBoolean(PreferencesActivity.KEY_PREF_SW_TRACKGPX_SRV, m_bGpxRecord);
        editor1.commit();   

		
		if(m_bGpxRecord){
			//start record
			startGPXRecord();
		}
		else{
			//stop record
			stopGPXRecord();
		}		*/
	}    
	
	void startGPXRecord(){
		/*startService(new Intent(TrackerService.ACTION_START_GPX));
		
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(this)
		        .setSmallIcon(R.drawable.record_start_notify)
		        .setContentTitle("NGISDroid")
		        .setOngoing(true)
		        .setContentText(getString(R.string.gpx_recording));        
		Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
	    stackBuilder.addParentStack(MainActivity.class);
	    stackBuilder.addNextIntent(resultIntent);
	     PendingIntent resultPendingIntent =
	             stackBuilder.getPendingIntent(
	                 0,
	                 PendingIntent.FLAG_UPDATE_CURRENT
	             );
	     mBuilder.setContentIntent(resultPendingIntent);
	     NotificationManager mNotificationManager =
	         (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	     mNotificationManager.notify(mNotifyId, mBuilder.getNotification());*/
     }
	
	void stopGPXRecord(){
		 /*startService(new Intent(TrackerService.ACTION_STOP_GPX));
	     NotificationManager mNotificationManager =
		         (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	     mNotificationManager.cancel(mNotifyId);*/
	}

	void ShowInfo()
	{
		mbInfoOn = !mbInfoOn;
		ShowInfo(mbInfoOn);
	}
	
	void ShowInfo(boolean bShow){
		if(bShow){
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mChangeLocationListener);
	        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mChangeLocationListener);
		
			final RelativeLayout.LayoutParams RightParams = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
					RightParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
					
					
					int nHeight = 0;
					if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
						TypedValue typeValue = new TypedValue();
						getTheme().resolveAttribute(android.R.attr.actionBarSize, typeValue, true);
						nHeight = TypedValue.complexToDimensionPixelSize(typeValue.data,getResources().getDisplayMetrics());
					}
					RightParams.setMargins(0, 0, 0, nHeight);
		    
			rl.addView(mInfoView, RightParams);					
		} else {
			mLocationManager.removeUpdates(mChangeLocationListener);
			rl.removeView(mInfoView);			
		}
	}		
    
	private final class ChangeLocationListener implements LocationListener {
		
		public void onLocationChanged(Location location) {
			
			TextView speedText = (TextView)mInfoView.findViewById(R.id.speed_text);
			DecimalFormat df = new DecimalFormat("0.0");
			double dfSpeed = location.getSpeed() * 3.6;//to km/h
			speedText.setText("" + df.format(dfSpeed) + " " + getString(R.string.info_speed_val));
			
			TextView heightText = (TextView)mInfoView.findViewById(R.id.height_text);
			double dfHeight = location.getAltitude();
			heightText.setText("" + df.format(dfHeight) + " " + getString(R.string.info_height_val));
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        	int nFormat = prefs.getInt(PreferencesActivity.KEY_PREF_COORD_FORMAT + "_int", Location.FORMAT_SECONDS);
			
			TextView latText = (TextView)mInfoView.findViewById(R.id.lat_text);
			latText.setText(PositionFragment.formatLat(location.getLatitude(), nFormat, getResources()) + getResources().getText(R.string.coord_lat));
			
			TextView lonText = (TextView)mInfoView.findViewById(R.id.lon_text);
			lonText.setText(PositionFragment.formatLng(location.getLongitude(), nFormat, getResources()) + getResources().getText(R.string.coord_lon));			

		}

		public void onProviderDisabled(String arg0) {
			// TODO Auto-generated method stub
			
		}

		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}   
	}
}
