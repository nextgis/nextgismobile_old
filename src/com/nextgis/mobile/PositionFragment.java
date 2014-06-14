/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PositionFragment extends CompassFragment {
	protected float m_dfOrinetAngle = 0;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		
		this.setRetainInstance(true);
		
		View view = inflater.inflate(R.layout.posfragment, container, false);
        
		if (view.findViewById(R.id.bubbleSurfaceView) != null) {
			bubbleView = (BubbleSurfaceView) view.findViewById(R.id.bubbleSurfaceView);
		}

		// magnetic north compass
		if (view.findViewById(R.id.compass) != null) {

			mCompass = (CompassImage) view.findViewById(R.id.compass);

			mCompass.setOnTouchListener(this);
		}

		if (view.findViewById(R.id.azimuth) != null) {
			((TextView) view.findViewById(R.id.azimuth)).setOnLongClickListener(resetCompass);
		}
		
		InputPointActivity activity = (InputPointActivity) getActivity();
		if(activity != null)
			mCurrentLocation = activity.getLocation();

		
		if (view.findViewById(R.id.poi_coordinates_text) != null && mCurrentLocation != null) {
			TextView tv = ((TextView) view.findViewById(R.id.poi_coordinates_text));       	
        	tv.setText(getLocationText(activity, mCurrentLocation));
		}
		
		if (view.findViewById(R.id.poi_azimuth_text) != null) {
			((TextView) view.findViewById(R.id.poi_azimuth_text)).setText(formatNumber(m_dfOrinetAngle, 0, 0) + DEGREE_CHAR + " " + getDirectionCode((float) m_dfOrinetAngle, getResources()));
		}
		
		if (view.findViewById(R.id.poi_dist_num) != null) {
			((TextView) view.findViewById(R.id.poi_dist_num)).setText("0");
		}
	        
		return view;
    }	
	
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////
	// FORMAT COORDINATES
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static String getLocationText(Context context, Location location)
	{
    	if(context == null || location == null)
    		return null;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	int nFormat = prefs.getInt(NGMConstants.KEY_PREF_COORD_FORMAT + "_int", Location.FORMAT_SECONDS);
    	return formatLat(location.getLatitude(), nFormat, context.getResources()) + ", " + formatLng(location.getLongitude(), nFormat, context.getResources());
	}

	public static String formatLat(double lat, int outputType, Resources res) {

		String direction = (String) res.getText(R.string.compas_N);
		if (lat < 0) {
			direction = (String) res.getText(R.string.compas_S);
			lat = -lat;
		}

		return formatCoord(lat, outputType) + direction;

	}

	public static String formatLat(double lat, Resources res) {
		return formatLat(lat, Location.FORMAT_DEGREES, res);
	}

	public static String formatLng(double lng, int outputType, Resources res) {

		String direction = (String) res.getText(R.string.compas_E);
		if (lng < 0) {
			direction = (String) res.getText(R.string.compas_W);
			lng = -lng;
		}

		return formatCoord(lng, outputType) + direction;

	}

	public static String formatLng(double lng, Resources res) {
		return formatLng(lng, Location.FORMAT_DEGREES, res);
	}

	public static String formatCoord(double coordinate, int outputType) {

		StringBuilder sb = new StringBuilder();
		char endChar = DEGREE_CHAR;

		DecimalFormat df = new DecimalFormat("###.######");
		if (outputType == Location.FORMAT_MINUTES || outputType == Location.FORMAT_SECONDS) {

			df = new DecimalFormat("##.###");

			int degrees = (int) Math.floor(coordinate);
			sb.append(degrees);
			sb.append(DEGREE_CHAR); // degrees sign
			endChar = '\''; // minutes sign
			coordinate -= degrees;
			coordinate *= 60.0;

			if (outputType == Location.FORMAT_SECONDS) {

				df = new DecimalFormat("##.##");

				int minutes = (int) Math.floor(coordinate);
				sb.append(minutes);
				sb.append('\''); // minutes sign
				endChar = '\"'; // seconds sign
				coordinate -= minutes;
				coordinate *= 60.0;
			}
		}

		sb.append(df.format(coordinate));
		sb.append(endChar);

		return sb.toString();
	}

	public static String formatCoord(double coord) {
		DecimalFormat df = new DecimalFormat("###.######");
		df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		return df.format(coord);
	}
	
	protected void rotateCompass(float angle) {

		if(mCompass != null)
		{
			m_dfOrinetAngle = getAzimuth(mAzimuth + mCompass.getAngle() + angle);
		}
		else
		{
			m_dfOrinetAngle = 0;
		}
		
		if (getView().findViewById(R.id.poi_azimuth_text) != null) {
			((TextView) getView().findViewById(R.id.poi_azimuth_text)).setText(formatNumber(m_dfOrinetAngle, 0, 0) + DEGREE_CHAR + " " + getDirectionCode((float) m_dfOrinetAngle, getResources()));
		}		
		
		super.rotateCompass(angle);
	}

	public void updateCompass(float azimuth) {
		
		super.updateCompass(azimuth);	
		
		if(mCompass != null)
		{
			m_dfOrinetAngle = getAzimuth(mAzimuth + mCompass.getAngle());
		}
		else
		{
			m_dfOrinetAngle = 0;
		}
		
		if (getView().findViewById(R.id.poi_azimuth_text) != null) {
			((TextView) getView().findViewById(R.id.poi_azimuth_text)).setText(formatNumber(m_dfOrinetAngle, 0, 0) + DEGREE_CHAR + " " + getDirectionCode((float) m_dfOrinetAngle, getResources()));
		}

	}
	

	public void onStoreValues() {	
		if(getView() != null)
		{
			InputPointActivity activity1 = (InputPointActivity) getActivity();
			if(activity1 == null)
				return;
			
			activity1.SetAzimuth(m_dfOrinetAngle);
			if ( getView().findViewById(R.id.poi_dist_num) != null) {
				String sDist = ((TextView) getView().findViewById(R.id.poi_dist_num)).getText().toString();
				if(sDist.length() == 0) {				
					activity1.SetDistance(0);
				}
				else {
					activity1.SetDistance(Float.parseFloat(sDist));
				}
			}
		}
	} 
}
