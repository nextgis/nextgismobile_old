/******************************************************************************
 * Project:  NextGISDroid
 * Purpose:  Position database class.
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

import com.nextgis.mobile.util.Constants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import static com.nextgis.mobile.util.Constants.*;

public class StartupReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		try
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(arg0);
			boolean startImmediately = prefs.getBoolean(Constants.KEY_PREF_START_SVC_ON_STARTUP, false);
			Log.i(TAG, "Did the user ask for start on bootup? - " + String.valueOf(startImmediately));

			if (startImmediately)
			{
				Log.i(TAG, "Launching services");
				boolean startTrackService = prefs.getBoolean(Constants.KEY_PREF_SW_TRACK_SRV, false);
				if(startTrackService){
					Intent trackerServiceIntent = new Intent(TrackerService.ACTION_START);
					//Intent serviceIntent = new Intent(arg0, GpsLoggingService.class);
					//serviceIntent.putExtra("immediate", true);
					arg0.startService(trackerServiceIntent);
				}
				
				boolean startDSService = prefs.getBoolean(Constants.KEY_PREF_SW_SENDPOS_SRV, false);
				if(startDSService) {
					Intent datasendServiceIntent = new Intent(DataSendService.ACTION_START);
					arg0.startService(datasendServiceIntent);
				}
			}
			
		}
		catch (Exception ex)
		{
			Log.e("StartupReceiver", ex.getMessage());
		}
	}
}
