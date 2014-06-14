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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.actionbarsherlock.app.SherlockFragment;
import com.nextgis.mobile.map.NGMapView;

public class MapFragment extends SherlockFragment {
	protected NGMapView m_oMap;   

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {		
		
		if(m_oMap == null){
			MainActivity activity = (MainActivity) getSherlockActivity();
			m_oMap = activity.getMap();
		}
		
    	View view = inflater.inflate(R.layout.mapfragment, container, false);
    	FrameLayout layout = (FrameLayout) view.findViewById(R.id.mapholder);
    	//search relative view of map, if not found - add it 
    	if(m_oMap != null && layout.findViewById(NGMConstants.MAP_RELATIVE_LAYOUT) == null){
    		layout.addView(m_oMap.getRelativeLayout());
    	}
		return view;
	}

}
