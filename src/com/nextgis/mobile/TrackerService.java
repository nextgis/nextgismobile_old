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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.TimeZone;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

public class TrackerService extends Service {
    public static final String ACTION_START = "com.nextgis.mobile.tracker.action.START";
    public static final String ACTION_STOP = "com.nextgis.mobile.tracker.action.STOP";
    public static final String ACTION_START_GPX = "com.nextgis.mobile.tracker.action.START_GPX";
    public static final String ACTION_STOP_GPX = "com.nextgis.mobile.tracker.action.STOP_GPX";
    
    protected LocationManager locationManager;
    protected TrackerLocationListener trackerLocationListener;

	private SQLiteDatabase PositionDB;
	private PositionDatabase dbHelper;
	
	private final IBinder mBinder = new TSBinder();
    
	private Handler m_TrakAddPointHandler = null;
	
	final static int mNotifyId = 9999;

	@Override
	public void onCreate() {
		Log.d(MainActivity.TAG, "onCreate()");
		super.onCreate();
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        trackerLocationListener = new TrackerLocationListener();
	}


	@Override
	public void onDestroy() {
		Log.d(MainActivity.TAG, "onDestroy()");
		super.onDestroy();
		
		locationManager.removeUpdates(trackerLocationListener);
		
		if(trackerLocationListener.isWriteTrack())
			trackerLocationListener.StoreTrack(false);
		
		SharedPreferences prefs = getSharedPreferences(PreferencesActivity.SERVICE_PREF, MODE_PRIVATE | MODE_MULTI_PROCESS); 
		final SharedPreferences.Editor edit = prefs.edit();
		edit.putBoolean(PreferencesActivity.KEY_PREF_SW_TRACKGPX_SRV, false);
		edit.commit();
		
		trackerLocationListener = null;
		
  	     NotificationManager mNotificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	     mNotificationManager.cancel(mNotifyId);
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		Log.d(MainActivity.TAG, "Received start id " + startId + ": " + intent);
		
		super.onStartCommand(intent, flags, startId);
		
		if(intent == null)
			return START_STICKY;
		
		String action = intent.getAction();
		if(action == null)
			return START_STICKY;
		
		Log.d(MainActivity.TAG, "action " + action);
        if (action.equals(ACTION_STOP))
        {
        	trackerLocationListener.setWritePostion(false);
        	if(dbHelper != null)
        		dbHelper.close();
        	if(!trackerLocationListener.isWriteTrack())
        		stopSelf(); 
        }
        else if (action.equals(ACTION_STOP_GPX))
        {
        	m_TrakAddPointHandler = null;
	   	    NotificationManager mNotificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		    mNotificationManager.cancel(mNotifyId);
		     
		    trackerLocationListener.StoreTrack(false);
        	trackerLocationListener.setWriteTrack(false);
        	if(!trackerLocationListener.isWritePostion())
        		stopSelf();
        }
        else if(action.equals(ACTION_START))
        {	
    		SharedPreferences prefs = getSharedPreferences(PreferencesActivity.SERVICE_PREF, MODE_PRIVATE | MODE_MULTI_PROCESS); 
        	boolean isStrarted = prefs.getBoolean(PreferencesActivity.KEY_PREF_SW_TRACK_SRV, false);
        	
        	if(isStrarted)
        	{ 	
	        	if(!trackerLocationListener.isWritePostion())
	        	{
	        		trackerLocationListener.setWritePostion(true);
	        		
	        		dbHelper = new PositionDatabase(getApplicationContext());
	        		PositionDB = dbHelper.getWritableDatabase();
	        		
		    		long nMinDistChangeForUpdates = prefs.getLong(PreferencesActivity.KEY_PREF_MIN_DIST_CHNG_UPD + "_long", 25);
		    		long nMinTimeBetweenUpdates = prefs.getLong(PreferencesActivity.KEY_PREF_MIN_TIME_UPD + "_long", 0);
		    		
		    		Log.d(MainActivity.TAG, "start LocationManager MinDist:" + nMinDistChangeForUpdates + " MinTime:" + nMinTimeBetweenUpdates); 
        	
		        	Log.d(MainActivity.TAG, "start LocationManager.GPS_PROVIDER");
			        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, nMinTimeBetweenUpdates, nMinDistChangeForUpdates, trackerLocationListener);
			        Log.d(MainActivity.TAG, "start LocationManager.NETWORK_PROVIDER");
			        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, nMinTimeBetweenUpdates, nMinDistChangeForUpdates, trackerLocationListener);
			        Log.d(MainActivity.TAG, "request end");
			        
	        	}
		        boolean bEnergyEconomy = prefs.getBoolean(PreferencesActivity.KEY_PREF_SW_ENERGY_ECO, true);
		        long nMinTimeBetweenSend = prefs.getLong(PreferencesActivity.KEY_PREF_TIME_DATASEND + "_long", DateUtils.MINUTE_IN_MILLIS); 
	        	ScheduleNextUpdate(this.getApplicationContext(), TrackerService.ACTION_START, nMinTimeBetweenSend, bEnergyEconomy, isStrarted);
        	}        	
        }
        else if(action.equals(ACTION_START_GPX))
        {
    		SharedPreferences prefs = getSharedPreferences(PreferencesActivity.SERVICE_PREF, MODE_PRIVATE | MODE_MULTI_PROCESS); 
        	boolean isStrarted_GPX = prefs.getBoolean(PreferencesActivity.KEY_PREF_SW_TRACKGPX_SRV, false);

        	if(isStrarted_GPX)
        	{ 	
	        	if(!trackerLocationListener.isWriteTrack())
	        	{
	        		trackerLocationListener.setWriteTrack(true);

	        		long nMinDistChangeForUpdates = prefs.getLong(PreferencesActivity.KEY_PREF_MIN_DIST_CHNG_UPD + "_long", 25);
	        		long nMinTimeBetweenUpdates = prefs.getLong(PreferencesActivity.KEY_PREF_MIN_TIME_UPD + "_long", 0);
	    		
	        		Log.d(MainActivity.TAG, "start GPX LocationManager MinDist:" + nMinDistChangeForUpdates + " MinTime:" + nMinTimeBetweenUpdates); 
    	
	        		Log.d(MainActivity.TAG, "start GPX LocationManager.GPS_PROVIDER");
	        		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, nMinTimeBetweenUpdates, nMinDistChangeForUpdates, trackerLocationListener);
	        		
	        		Intent resultIntent = new Intent(this, MainActivity.class);
	                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this)
	                		.addParentStack(MainActivity.class)
	        	    		.addNextIntent(resultIntent);
	        	    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
	        		
	        		NotificationCompat.Builder mBuilder =
	        		        new NotificationCompat.Builder(this)
	        		        .setSmallIcon(R.drawable.record_start_notify)
	        		        .setContentTitle(getString(R.string.app_name))
	        		        .setOngoing(true)
	        		        .setContentText(getString(R.string.gpx_recording))        
	        	            .setContentIntent(resultPendingIntent);
	        		
	        	     Notification noti = mBuilder.getNotification();
	        	     //noti.flags |= Notification.FLAG_FOREGROUND_SERVICE;//Notification.FLAG_NO_CLEAR | 
	        	     NotificationManager mNotificationManager =
	        	         (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	        	     mNotificationManager.notify(mNotifyId, noti);

	        	}
        	}
	        boolean bEnergyEconomy = prefs.getBoolean(PreferencesActivity.KEY_PREF_SW_ENERGY_ECO, true);
	        long nMinTimeBetweenSend = prefs.getLong(PreferencesActivity.KEY_PREF_TIME_DATASEND + "_long", DateUtils.MINUTE_IN_MILLIS); 
        	ScheduleNextUpdate(getApplicationContext(), TrackerService.ACTION_START_GPX, nMinTimeBetweenSend, bEnergyEconomy, isStrarted_GPX);
        }
        return START_STICKY;
	}
	
 	 protected void ScheduleNextUpdate(Context context, String action, long nMinTimeBetweenSend, boolean bEnergyEconomy, boolean bStart)
	 {
		if(context == null)
			return;
		Log.d(MainActivity.TAG, "Schedule Next Update for tracker " + bStart);
		if(bStart == false)
			return;
		Intent intent = new Intent(action);
		PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

       // The update frequency should often be user configurable.  This is not.

		long currentTimeMillis = System.currentTimeMillis();
		long nextUpdateTimeMillis = currentTimeMillis + nMinTimeBetweenSend;
		Time nextUpdateTime = new Time();
		nextUpdateTime.set(nextUpdateTimeMillis);

		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		if(bEnergyEconomy)
			alarmManager.set(AlarmManager.RTC, nextUpdateTimeMillis, pendingIntent);
		else
			alarmManager.set(AlarmManager.RTC_WAKEUP, nextUpdateTimeMillis, pendingIntent);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
	    return mBinder;
	}

	public class TSBinder extends Binder {
		TrackerService getService() {
			return TrackerService.this;
	    }
	}
	  
	public ArrayList<RecordedGeoPoint> GetPath(){
		if(trackerLocationListener == null)
			return null;
		return trackerLocationListener.mRecords;
	}
	
	private final class TrackerLocationListener implements LocationListener {
		private boolean bWritePostion;
		private boolean bWriteTrack;
		
		protected final ArrayList<RecordedGeoPoint> mRecords = new ArrayList<RecordedGeoPoint>();

		private static final String XML_VERSION = "<?xml version=\"1.0\"?>";
		private static final String GPX_VERSION = "1.1";
		private static final String GPX_TAG = "<gpx version=\""
				+ GPX_VERSION
				+ "\" creator=\"%s\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/1\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">";
		private static final String GPX_TAG_CLOSE = "</gpx>";
		private static final String GPX_TAG_TIME = "<time>%s</time>";
		private static final String GPX_TAG_TRACK = "<trk>";
		private static final String GPX_TAG_TRACK_CLOSE = "</trk>";
		private static final String GPX_TAG_TRACK_NAME = "<name>%s</name>";
		private static final String GPX_TAG_TRACK_SEGMENT = "<trkseg>";
		private static final String GPX_TAG_TRACK_SEGMENT_CLOSE = "</trkseg>";
		public static final String GPX_TAG_TRACK_SEGMENT_POINT = "<trkpt lat=\"%s\" lon=\"%s\">";
		public static final String GPX_TAG_TRACK_SEGMENT_POINT_CLOSE = "</trkpt>";
		public static final String GPX_TAG_TRACK_SEGMENT_POINT_TIME = "<time>%s</time>";
		public static final String GPX_TAG_TRACK_SEGMENT_POINT_SAT = "<sat>%d</sat>";
		public static final String GPX_TAG_TRACK_SEGMENT_POINT_ELE = "<ele>%d</ele>";
		
		private String mGPXFileName = "";
		
		public TrackerLocationListener() {
			super();
			setWritePostion(setWriteTrack(false));
		}

		public void onLocationChanged(Location location) {
			
			if(bWritePostion)
			{
				String message = String.format("New Location \n Longitude: %1$s \n Latitude: %2$s", location.getLongitude(), location.getLatitude() );
				Log.d(MainActivity.TAG, message);
				//store coordinates to db
				ContentValues values = new ContentValues();
				values.put(PositionDatabase.COLUMN_ACCURACY, location.getAccuracy());
				values.put(PositionDatabase.COLUMN_ALTITUDE, location.getAltitude());
				values.put(PositionDatabase.COLUMN_DIRECTION, location.getBearing());
				values.put(PositionDatabase.COLUMN_LAT, location.getLatitude());
				values.put(PositionDatabase.COLUMN_LON, location.getLongitude());
				values.put(PositionDatabase.COLUMN_PROVIDER, location.getProvider());
				values.put(PositionDatabase.COLUMN_SPEED, location.getSpeed());
				values.put(PositionDatabase.COLUMN_TIME_UTC, location.getTime());
				PositionDB.insert(PositionDatabase.TABLE_POS, null, values);//long insertId = 
				//delete old records
			}
			
			if(bWriteTrack){
				int nSatNum = location.getExtras().getInt("satellites");			
				mRecords.add(new RecordedGeoPoint(location.getLatitude(), location.getLongitude(), System.currentTimeMillis(), nSatNum, location.getAltitude()));		
				
				StoreTrack(true);
				
				if(m_TrakAddPointHandler != null){
		            Bundle bundle = new Bundle();
		            bundle.putDouble("lat", location.getLatitude());
		            bundle.putDouble("lon", location.getLongitude());
		            
		            Message msg = new Message();
		            msg.setData(bundle);
	            	m_TrakAddPointHandler.sendMessage(msg);
				}
			}
	    }
	
	    public void onStatusChanged(String s, int i, Bundle b) {
	    	Log.d(MainActivity.TAG, "Provider status changed. " + s);
	    }
	
	    public void onProviderDisabled(String s) {
	    	Log.d(MainActivity.TAG, "Provider disabled by the user. " + s);
	    }
	
	    public void onProviderEnabled(String s) {
	    	Log.d(MainActivity.TAG, "Provider enabled by the user. " + s);
	    }

		public boolean isWritePostion() {
			return bWritePostion;
		}

		public void setWritePostion(boolean bWritePostion) {
			this.bWritePostion = bWritePostion;
		}

		public boolean isWriteTrack() {
			return bWriteTrack;
		}

		public boolean setWriteTrack(boolean bWriteTrack) {
			this.bWriteTrack = bWriteTrack;
			
			if(mGPXFileName.length() == 0){
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
				mGPXFileName = "loc_" + timeStamp + ".gpx";
			}
			
			return bWriteTrack;
		}

		public void StoreTrack(boolean bAutoSave) {
			Log.d(MainActivity.TAG, "store gpx track");
			if(!mRecords.isEmpty()){
			
				if(mGPXFileName.length() == 0){
					String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
					mGPXFileName = "loc_" + timeStamp + ".gpx";
				}
				
				final StringBuilder sb = new StringBuilder();
				final Formatter f = new Formatter(sb);
				sb.append(XML_VERSION);
				
	    		SharedPreferences prefs = getSharedPreferences(PreferencesActivity.SERVICE_PREF, MODE_PRIVATE | MODE_MULTI_PROCESS); 
				String sId = prefs.getString(PreferencesActivity.KEY_PREF_USER_ID, PreferencesActivity.GetDeviceId());
				
				f.format(GPX_TAG, sId);
				f.format(GPX_TAG_TIME, RecordedGeoPoint.getTimeStampAsString(System.currentTimeMillis()));
				sb.append(GPX_TAG_TRACK);
				f.format(
						GPX_TAG_TRACK_NAME,
						sId
								+ ":"
								+ mRecords.get(0).getTimeStamp()
								+ "-"
								+ mRecords.get(mRecords.size() - 1).getTimeStamp() );
				sb.append(GPX_TAG_TRACK_SEGMENT);
				
				for (final RecordedGeoPoint rgp : mRecords) {
					String sLat = "" + rgp.getLatitude();
					String sLon = "" + rgp.getLongitude();
					f.format(GPX_TAG_TRACK_SEGMENT_POINT, sLat.replace(',', '.'), sLon.replace(',', '.'));
					f.format(GPX_TAG_TRACK_SEGMENT_POINT_TIME, rgp.getTimeStamp());
					f.format(GPX_TAG_TRACK_SEGMENT_POINT_SAT, rgp.getNumSatellites());
					f.format(GPX_TAG_TRACK_SEGMENT_POINT_ELE, rgp.getEle());
					sb.append(GPX_TAG_TRACK_SEGMENT_POINT_CLOSE);
				}

				sb.append(GPX_TAG_TRACK_SEGMENT_CLOSE).append(GPX_TAG_TRACK_CLOSE).append(GPX_TAG_CLOSE);
				//sb.toString();
				
				File gpxStorageDir = new File(getExternalFilesDir(null), "gpx");
				
			    // Create the storage directory if it does not exist
			    if (! gpxStorageDir.exists()){
			        if (! gpxStorageDir.mkdirs()){
			            Log.d(MainActivity.TAG, "failed to create directory");
			            f.close();
			            return;
			        }
			    }				
				
		    	File file = new File(gpxStorageDir, mGPXFileName);
		    	try {  
		            FileOutputStream os = new FileOutputStream(file, false);
		            PrintWriter pw = new PrintWriter(os);
		            pw.print(sb.toString());	            
		            pw.flush();
		            pw.close();
		            os.close();
		            
		            if(!bAutoSave){
		            	MediaScannerConnection.scanFile(getApplicationContext(),
		                    new String[] { file.toString() }, null,
		                    new MediaScannerConnection.OnScanCompletedListener() {
		            		public void onScanCompleted(String path, Uri uri) {
		            			Log.i(MainActivity.TAG, "Scanned " + path + ":");
		                    	Log.i(MainActivity.TAG, "-> uri=" + uri);
		                	}
		            	});
		            	Toast.makeText( getApplicationContext(), getResources().getText(R.string.create_nex_gpx) + mGPXFileName, Toast.LENGTH_LONG).show();
		            }
		    		
		        } catch (IOException e) {
		            Log.w(MainActivity.TAG, "Error writing " + file, e);
		        }
				
				f.close();
				if(!bAutoSave){
					mRecords.clear();
					mGPXFileName = "";
				}
			}
		}	
	}
	
	public static class RecordedGeoPoint {

		protected final long mTimeStamp;
		protected final int mNumSatellites;
		protected final double mdfLat, mdfLong;
		protected final double mdfAlt;

		public RecordedGeoPoint(final double dfLatitude, final double dfLongitude, final long aTimeStamp, final int aNumSatellites, final double dfAlt) {
			this.mdfLat = dfLatitude;
			this.mdfLong = dfLongitude;
			this.mTimeStamp = aTimeStamp;
			this.mNumSatellites = aNumSatellites;
			this.mdfAlt = dfAlt;
		}

		public String getTimeStamp() {
			return getTimeStampAsString(this.mTimeStamp);
		}

		public double getLatitude() {
			return this.mdfLat;
		}

		public double getLongitude() {
			return this.mdfLong;
		}

		public int getNumSatellites() {
			return this.mNumSatellites;
		}
		
		public int getEle(){
			return (int)this.mdfAlt;
		}
		
		public static String getTimeStampAsString (long nTimeStamp) {
			final SimpleDateFormat utcFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );
			utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			return utcFormat.format(new Date(nTimeStamp));
		}
	}
	
	public void setPathHanler(Handler h){
		m_TrakAddPointHandler = h;
	}
}
