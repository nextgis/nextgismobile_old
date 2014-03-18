/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
*   Copyright (C) 2012-2014 NextGIS
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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

public class PreferencesActivity extends SherlockPreferenceActivity implements OnSharedPreferenceChangeListener {
	
	public static final String KEY_PREF_STORAGE_SITE = "storage_site";
	public static final String KEY_PREF_USER_ID = "user_id";
	public static final String KEY_PREF_MIN_DIST_CHNG_UPD = "min_dist_change_for_update";
	public static final String KEY_PREF_MIN_TIME_UPD = "min_time_beetwen_updates";
	public static final String KEY_PREF_SW_TRACK_SRV = "sw_track_service";
	public static final String KEY_PREF_SW_TRACKGPX_SRV = "sw_trackgpx_service";
	public static final String KEY_PREF_SW_SENDPOS_SRV = "sw_sendpos_service";
	public static final String KEY_PREF_SW_ENERGY_ECO = "sw_energy_economy";
	public static final String KEY_PREF_TIME_DATASEND = "time_between_datasend";
	public static final String KEY_PREF_COORD_FORMAT = "coordinates_format";
	public static final String KEY_PREF_START_SVC_ON_STARTUP = "start_services_on_startup";
	public static final String KEY_PREF_ACCURATE_LOC = "accurate_coordinates_pick";
	public static final String KEY_PREF_ACCURATE_GPSCOUNT = "accurate_coordinates_pick_count";
	public static final String KEY_PREF_ACCURATE_CE = "accurate_type";	
	public static final String KEY_PREF_TILE_SIZE = "map_tile_size";
	public static final String KEY_PREF_COMPASS_VIBRO = "compass_vibration";
	public static final String KEY_PREF_COMPASS_TRUE_NORTH = "compass_true_north";
	public static final String KEY_PREF_COMPASS_SHOW_MAGNET = "compass_show_magnetic";
	public static final String KEY_PREF_COMPASS_WAKE_LOCK = "compass_wake_lock";
	
	public static final String SERVICE_PREF = "preferences";
	
	
	CheckBoxPreference TrackServicePref;
	CheckBoxPreference SendPosServicePref;
	EditTextPreference StorageSitePref;
	EditTextPreference UserIdPref;
	ListPreference MinDistPref;
	ListPreference MinTimePref;
	CheckBoxPreference SendPosInSuspendPref;
	ListPreference MinTimePosSend;
	CheckBoxPreference runServicesOnStartPref;
	ListPreference coordinatesFormat;
	CheckBoxPreference showTrueNorth;
	CheckBoxPreference showMagnetic;
	CheckBoxPreference wakeLock;
	CheckBoxPreference vibration;
	CheckBoxPreference moreAccuratePosPref;
	EditTextPreference moreAccureatePosCountPref;
	ListPreference moreAccurateCEPref;
	ListPreference TileSizePref;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        
        addPreferencesFromResource(R.xml.preferences);
        
        //path to track points receiver script
	    StorageSitePref = (EditTextPreference) findPreference(KEY_PREF_STORAGE_SITE);
	    StorageSitePref.setSummary((String) StorageSitePref.getText());
        
        //user identificator
	    UserIdPref = (EditTextPreference) findPreference(KEY_PREF_USER_ID);
	    if(UserIdPref.getText().length() == 0){
	    	String szDevIDShort = GetDeviceId();
	    	UserIdPref.setSummary(szDevIDShort);
	    	UserIdPref.setText(szDevIDShort);	    	
	    }
	    else {
	    	UserIdPref.setSummary((String) UserIdPref.getText());
	    	UserIdPref.setText((String) UserIdPref.getText());
	    }
	    
		SharedPreferences mySharedPreferences = getSharedPreferences(SERVICE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor1 = mySharedPreferences.edit();
        editor1.putString(KEY_PREF_USER_ID, (String) UserIdPref.getText());
        editor1.commit();   
	    
	    MinDistPref = (ListPreference) findPreference(KEY_PREF_MIN_DIST_CHNG_UPD);
	    CharSequence newVal = GetArrayKey(R.array.tracker_min_dist_update, R.array.tracker_min_dist_update_val, MinDistPref.getValue());            
	    MinDistPref.setSummary((String) newVal);
	    
//	    TileSizePref = (ListPreference) findPreference(KEY_PREF_TILE_SIZE);
//	    TileSizePref.setSummary((String) TileSizePref.getValue());
	    
	    MinTimePref = (ListPreference) findPreference(KEY_PREF_MIN_TIME_UPD);
	    newVal = GetArrayKey(R.array.tracker_min_time_between_updates, R.array.tracker_min_time_between_updates_val, MinTimePref.getValue());            
	    MinTimePref.setSummary((String) newVal);	   
	    
	    TrackServicePref = (CheckBoxPreference) findPreference(KEY_PREF_SW_TRACK_SRV);
	    TrackServicePref.setSummary(TrackServicePref.isChecked() ? R.string.pref_tracker_service_on : R.string.pref_tracker_service_off);	    
	    
	    SendPosServicePref = (CheckBoxPreference) findPreference(KEY_PREF_SW_SENDPOS_SRV);
	    SendPosServicePref.setSummary(SendPosServicePref.isChecked() ? R.string.pref_sendpos_service_on : R.string.pref_sendpos_service_off);	    
	    
	    SendPosInSuspendPref = (CheckBoxPreference) findPreference(KEY_PREF_SW_ENERGY_ECO);
	    SendPosInSuspendPref.setSummary(SendPosInSuspendPref.isChecked() ? R.string.pref_energy_economy_on : R.string.pref_energy_economy_off);	    
	    
	    MinTimePosSend = (ListPreference) findPreference(KEY_PREF_TIME_DATASEND);
	    newVal = GetArrayKey(R.array.datapos_send_updates, R.array.datapos_send_updates_val, MinTimePosSend.getValue());            
	    MinTimePosSend.setSummary((String) newVal);
	    
	    coordinatesFormat = (ListPreference) findPreference(KEY_PREF_COORD_FORMAT);
	    coordinatesFormat.setSummary((String) coordinatesFormat.getValue());
	    
	    runServicesOnStartPref = (CheckBoxPreference) findPreference(KEY_PREF_START_SVC_ON_STARTUP);
	    
	    vibration = (CheckBoxPreference) findPreference(KEY_PREF_COMPASS_VIBRO);
		showTrueNorth = (CheckBoxPreference) findPreference(KEY_PREF_COMPASS_TRUE_NORTH);
		showMagnetic = (CheckBoxPreference) findPreference(KEY_PREF_COMPASS_SHOW_MAGNET);
		wakeLock = (CheckBoxPreference) findPreference(KEY_PREF_COMPASS_WAKE_LOCK);
		moreAccuratePosPref = (CheckBoxPreference) findPreference(KEY_PREF_ACCURATE_LOC);

		moreAccureatePosCountPref = (EditTextPreference) findPreference(KEY_PREF_ACCURATE_GPSCOUNT);
	    moreAccureatePosCountPref.setSummary((String) moreAccureatePosCountPref.getText());
	    
	    moreAccurateCEPref = (ListPreference) findPreference(KEY_PREF_ACCURATE_CE);
	    moreAccurateCEPref.setSummary((String) moreAccurateCEPref.getValue());        
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    } 

	@Override
	protected void onPause() {	
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);		
		sharedPref.unregisterOnSharedPreferenceChangeListener(this);
		
		onTrackService(sharedPref.getBoolean(KEY_PREF_SW_TRACK_SRV, false));
		onSendPosService(sharedPref.getBoolean(KEY_PREF_SW_SENDPOS_SRV, false));
		super.onPause();
	}
	
    @Override
	public void onResume() {
        super.onResume();
        
 		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
 		sharedPref.registerOnSharedPreferenceChangeListener(this);
    }	
	
    public void onTrackService(boolean bStart) {
    	Log.d(MainActivity.TAG, "Preferences: onTrackService " + bStart);
    	if(bStart)
			startService(new Intent(TrackerService.ACTION_START));
		else
			startService(new Intent(TrackerService.ACTION_STOP));    	
    }
    
    public void onSendPosService(boolean bStart) {
    	Log.d(MainActivity.TAG, "Preferences: onSendPosService " + bStart);
    	if(bStart)
			startService(new Intent(DataSendService.ACTION_START));
		else
			startService(new Intent(DataSendService.ACTION_STOP));
    }    
 
	public static String GetDeviceId(){
		String szDevIDShort = "77" + //we make this look like a valid IMEI
                Build.BOARD.length()%10+ Build.BRAND.length()%10 +
                Build.CPU_ABI.length()%10 + Build.DEVICE.length()%10 +
                Build.DISPLAY.length()%10 + Build.HOST.length()%10 +
                Build.ID.length()%10 + Build.MANUFACTURER.length()%10 +
                Build.MODEL.length()%10 + Build.PRODUCT.length()%10 +
                Build.TAGS.length()%10 + Build.TYPE.length()%10 +
                Build.USER.length()%10 ; //13 digits
		return szDevIDShort;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		CharSequence newVal = "";
		Preference Pref = findPreference(key);
		if (key.equals(KEY_PREF_STORAGE_SITE) || key.equals(KEY_PREF_USER_ID)) {            
            // Set summary to be the user-description for the selected value
            newVal = sharedPreferences.getString(key, "");
            
			SharedPreferences mySharedPreferences = getSharedPreferences(SERVICE_PREF, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = mySharedPreferences.edit();
            editor1.putString(key, (String) newVal);
            editor1.commit();            
        }	
		else if (key.equals(KEY_PREF_ACCURATE_CE)) {  
            newVal = sharedPreferences.getString(key, "");
		}
		else if(key.equals(KEY_PREF_MIN_DIST_CHNG_UPD))
		{
			newVal = sharedPreferences.getString(key, "25");			
    		long nVal = Long.parseLong((String) newVal);
    		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
    		editor.putLong(key + "_long", nVal);
    		editor.commit();
    		
			SharedPreferences mySharedPreferences = getSharedPreferences(SERVICE_PREF, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = mySharedPreferences.edit();
            editor1.putLong(key + "_long", nVal);
            editor1.commit();  
            
            newVal = GetArrayKey(R.array.tracker_min_dist_update, R.array.tracker_min_dist_update_val, newVal);            
		}
		else if(key.equals(KEY_PREF_MIN_TIME_UPD))
		{
			newVal = sharedPreferences.getString(key, "0");
			long nVal = Long.parseLong((String) newVal) * DateUtils.SECOND_IN_MILLIS;
    		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
    		editor.putLong(key + "_long", nVal);
    		editor.commit();
    		
			SharedPreferences mySharedPreferences = getSharedPreferences(SERVICE_PREF, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = mySharedPreferences.edit();
            editor1.putLong(key + "_long", nVal);
            editor1.commit();    
            
            newVal = GetArrayKey(R.array.tracker_min_time_between_updates, R.array.tracker_min_time_between_updates_val, newVal);            
		}
		else if(key.equals(KEY_PREF_TIME_DATASEND))
		{
			newVal = sharedPreferences.getString(key, "0");
			long nVal = Long.parseLong((String) newVal) * DateUtils.MINUTE_IN_MILLIS;
    		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
    		editor.putLong(key + "_long", nVal);
    		editor.commit();
    		
			SharedPreferences mySharedPreferences = getSharedPreferences(SERVICE_PREF, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = mySharedPreferences.edit();
            editor1.putLong(key + "_long", nVal);
            editor1.commit();    
            
            newVal = GetArrayKey(R.array.datapos_send_updates, R.array.datapos_send_updates_val, newVal);
		}
		else if(key.equals(KEY_PREF_TILE_SIZE))
		{
			newVal = sharedPreferences.getString(key, "256");
			int nVal = Integer.parseInt((String) newVal);
    		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
    		editor.putInt(key + "_int", nVal);
    		editor.commit(); 		
		}	
		else if(key.equals(KEY_PREF_SW_TRACK_SRV))
		{
			boolean bPref = sharedPreferences.getBoolean(key, false); 
			newVal = bPref ? getText(R.string.pref_tracker_service_on) : getText(R.string.pref_tracker_service_off);
			
			SharedPreferences mySharedPreferences = getSharedPreferences(SERVICE_PREF, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = mySharedPreferences.edit();
            editor.putBoolean(key, bPref);
            editor.commit();			
		}		
		else if(key.equals(KEY_PREF_SW_SENDPOS_SRV))
		{
			boolean bPref = sharedPreferences.getBoolean(key, false); 
			newVal = bPref ? getText(R.string.pref_sendpos_service_on) : getText(R.string.pref_sendpos_service_off);
			
			SharedPreferences mySharedPreferences = getSharedPreferences(SERVICE_PREF, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = mySharedPreferences.edit();
            editor.putBoolean(key, bPref);
            editor.commit();			
		}       
		else if(key.equals(KEY_PREF_SW_ENERGY_ECO))
		{
			boolean bPref = sharedPreferences.getBoolean(key, false); 
			newVal = bPref ? getText(R.string.pref_energy_economy_on) : getText(R.string.pref_energy_economy_off);	
			
			SharedPreferences mySharedPreferences = getSharedPreferences(SERVICE_PREF, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = mySharedPreferences.edit();
            editor.putBoolean(key, bPref);
            editor.commit();
		} 	    
		else if(key.equals(KEY_PREF_ACCURATE_GPSCOUNT))
		{
			newVal = sharedPreferences.getString(key, "");
			int nVal = Integer.parseInt((String) newVal);
    		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
    		editor.putInt(key + "_int", nVal);
    		editor.commit();
		} 		
		else if(key.equals(KEY_PREF_COORD_FORMAT))
		{		
			int nIndex = coordinatesFormat.findIndexOfValue(coordinatesFormat.getValue());
			int nVal;
			if(nIndex == 0)
				nVal = Location.FORMAT_SECONDS;
			else if(nIndex == 1)
				nVal = Location.FORMAT_MINUTES;
			else if(nIndex == 2)
				nVal = Location.FORMAT_DEGREES;
			else
				nVal = Location.FORMAT_SECONDS;
    		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
			editor.putInt(key + "_int", nVal);	
    		editor.commit();
    		newVal = sharedPreferences.getString(key, "");
		}
		
		if(newVal.length() > 0)
        	Pref.setSummary(newVal);		
	}
	
	public CharSequence GetArrayKey(int nKeyArray, int nValueArray, CharSequence nDefault){
        CharSequence[] keys = getResources().getTextArray(nKeyArray);
        CharSequence[] values = getResources().getTextArray(nValueArray);
        int len = values.length;
        for (int i = 0; i < len; i++) {
            if (values[i].equals(nDefault)) {
            	nDefault = keys[i];
            	break;
            }
        }
		return nDefault;		
	}
	
}