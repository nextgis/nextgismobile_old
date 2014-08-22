/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
*   Copyright (C) 2012-2013 NextGIS
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
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
package com.nextgis.mobile.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.nextgis.mobile.util.Constants;
import com.nextgis.mobile.PositionDatabase;
import com.nextgis.mobile.PreferencesActivity;
import com.nextgis.mobile.util.NetworkUtil;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import static com.nextgis.mobile.util.Constants.*;

public class DataSendService extends Service {
    
	private SQLiteDatabase PositionDB;
	private PositionDatabase dbHelper;
	
	protected NetworkUtil networkUtil;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate()");
		super.onCreate();
		
        dbHelper = new PositionDatabase(getApplicationContext());
        PositionDB = dbHelper.getWritableDatabase();

        networkUtil = new NetworkUtil(this);
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
		dbHelper.close();
		
		super.onDestroy();		
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Received start id " + startId + ": " + intent);
		super.onStartCommand(intent, flags, startId);
		if(intent == null)
			return START_STICKY;
		String action = intent.getAction();
        if (action.equals(DATASEND_ACTION_STOP))
        {
        	//Toast.makeText(getApplicationContext(), "Send position service stoped", Toast.LENGTH_SHORT).show();
        	stopSelf(); 
        }
        else if(action.equals(DATASEND_ACTION_START))
        {
        	SharedPreferences prefs = getSharedPreferences(Constants.SERVICE_PREF, MODE_MULTI_PROCESS);
        	boolean bStart = prefs.getBoolean(Constants.KEY_PREF_SW_SENDPOS_SRV, false);
        	if(bStart)
        		new SendPositionDataTask().execute(getApplicationContext());
        	else
        	//Toast.makeText(getApplicationContext(), "Send position service started", Toast.LENGTH_SHORT).show();
        		stopSelf(); 
        }        
        return START_STICKY;
	}
	

	private class SendPositionDataTask extends AsyncTask<Context, Void, Void> {
		 @Override
	     protected Void doInBackground(Context... context) {
        	SharedPreferences prefs = getSharedPreferences("preferences", MODE_MULTI_PROCESS);
        	String sHost = prefs.getString(Constants.KEY_PREF_STORAGE_SITE, "http://gis-lab.info");
        	long nMinTimeBetweenSend = prefs.getLong(Constants.KEY_PREF_TIME_DATASEND + "_long", DateUtils.MINUTE_IN_MILLIS);

        	boolean bEnergyEconomy = prefs.getBoolean(Constants.KEY_PREF_SW_ENERGY_ECO, true);
        	
        	String sId = prefs.getString(Constants.KEY_PREF_USER_ID, PreferencesActivity.GetDeviceId());
        	
        	Log.d(TAG, "start SendPositionDataTask MinTime:" + nMinTimeBetweenSend + " user id:" + sId); 
        	
        	//Send data
        	SendPostionData(sHost, sId);
        	//Setup next send
        	ScheduleNextUpdate(context[0], sHost, nMinTimeBetweenSend, bEnergyEconomy, prefs.getBoolean(Constants.KEY_PREF_SW_ENERGY_ECO, false));
 
			return null;

	     }
	
	     protected void ScheduleNextUpdate(Context context, String sHost, long nMinTimeBetweenSend, boolean bEnergyEconomy, boolean bStart)
		 {
	 		if(context == null)
				return;
	 		
	 		Log.d(TAG, "Schedule Next Update for sender " + bStart);
			if(!bStart)
				return;
			Intent intent = new Intent(DATASEND_ACTION_START);
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
		
		protected void SendPostionData(String sHost, String sId)
		{
			Log.d(TAG, "SendPostionData");
			if(!networkUtil.isNetworkAvailible())
				return;
			
			//Queue records to send
	    	Cursor cursor = PositionDB.query(PositionDatabase.TABLE_POS, null, null, null, null, null, null);
	    	cursor.moveToFirst();
	    	Log.d(TAG, "record count: " + cursor.getCount());
	    	
	    	List<Long> delete_ids = new ArrayList<Long>();
	   	
	    	while(!cursor.isAfterLast())
	    	{
		    	String sAcc = cursor.getString(cursor.getColumnIndexOrThrow(PositionDatabase.COLUMN_ACCURACY));
		    	String sAlt = cursor.getString(cursor.getColumnIndexOrThrow(PositionDatabase.COLUMN_ALTITUDE));
		    	String sDir = cursor.getString(cursor.getColumnIndexOrThrow(PositionDatabase.COLUMN_DIRECTION));    	
		    	String sLat = cursor.getString(cursor.getColumnIndexOrThrow(PositionDatabase.COLUMN_LAT));
		    	String sLon = cursor.getString(cursor.getColumnIndexOrThrow(PositionDatabase.COLUMN_LON));
		    	String sProv = cursor.getString(cursor.getColumnIndexOrThrow(PositionDatabase.COLUMN_PROVIDER));
		    	String sSpeed = cursor.getString(cursor.getColumnIndexOrThrow(PositionDatabase.COLUMN_SPEED));    	
		    	String sTime = cursor.getString(cursor.getColumnIndexOrThrow(PositionDatabase.COLUMN_TIME));
		    	String sTime_UTC = cursor.getString(cursor.getColumnIndexOrThrow(PositionDatabase.COLUMN_TIME_UTC));
		    	Long nId = cursor.getLong(cursor.getColumnIndexOrThrow(PositionDatabase.COLUMN_ID));
		    	
		    	List<NameValuePair> params = new LinkedList<NameValuePair>();
		    	params.add(new BasicNameValuePair("uid", sId));
		    	params.add(new BasicNameValuePair("acc", sAcc));
		    	params.add(new BasicNameValuePair("alt", sAlt));
		    	params.add(new BasicNameValuePair("dir", sDir));
		    	params.add(new BasicNameValuePair("lat", sLat));
		    	params.add(new BasicNameValuePair("lon", sLon));
		    	params.add(new BasicNameValuePair("prov", sProv));
		    	params.add(new BasicNameValuePair("speed", sSpeed));
		    	params.add(new BasicNameValuePair("time", sTime));
		    	params.add(new BasicNameValuePair("time_utc", sTime_UTC));
		    	
		    	String sData = URLEncodedUtils.format(params, "utf-8");
		    	
		    	if(SendData(sHost, sData))
		    	{
		    		//delete record
		    		delete_ids.add(nId);
		    	}
		    	cursor.moveToNext();
	    	}
			cursor.close();

            for (Long delete_id : delete_ids) {
                PositionDB.delete(PositionDatabase.TABLE_POS, PositionDatabase.COLUMN_ID + " = " + delete_id, null);
            }
		}
		
		protected boolean SendData(String sHost, String sData)
		{
			Log.d(TAG, "SendData: host=" + sHost + " data=" + sData);
	
			HttpClient httpClient = new DefaultHttpClient();
			//HttpContext localContext = new BasicHttpContext();
			HttpGet httpGet = new HttpGet(sHost + "?" + sData);
			try {
				//HttpResponse response = httpClient.execute(httpGet, localContext);
				HttpResponse response = httpClient.execute(httpGet);
				if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
					return true;
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return false;
			}
			return false;
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			stopSelf(); 
		}
	}
}
