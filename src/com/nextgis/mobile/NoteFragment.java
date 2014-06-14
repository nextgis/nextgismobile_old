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

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.os.Bundle;

public class NoteFragment extends Fragment {
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {   
    	
    	this.setRetainInstance(true);
    	
    	View view = inflater.inflate(R.layout.notefragment, container, false);

        UpdateSummary();
        
        return view;
    }

	public void onStoreValues() {		
		if(getView() != null)
		{
			InputPointActivity activity = (InputPointActivity) getActivity();
			if(activity == null)
				return;
			
			if (getView().findViewById(R.id.poi_notes_text) != null) {
				activity.SetNotes(((TextView) getView().findViewById(R.id.poi_notes_text)).getText().toString());
			}
		}	
		UpdateSummary();
	}
	
	protected void UpdateSummary(){
		if(getView() != null )
		{
			InputPointActivity activity = (InputPointActivity) getActivity();
			if(activity == null)
				return;
	    	
	        String sCoords = PositionFragment.getLocationText(getActivity(), activity.getLocation());
			if (getView().findViewById(R.id.poi_summary_text) != null) {
				TextView summary = (TextView)getView().findViewById(R.id.poi_summary_text);
		        summary.setText(
		        		activity.getResources().getText(R.string.sum_cat) + activity.m_sCat + "\n" + 
		        		activity.getResources().getText(R.string.sum_subcat) + activity.m_sSubCat + "\n" + 
		        		activity.getResources().getText(R.string.sum_coords) + sCoords + "\n" + 
		        		activity.getResources().getText(R.string.sum_az) + activity.m_fAzimuth + "\n" + 
		        		activity.getResources().getText(R.string.sum_dist) + activity.m_fDist
		        		);				
			}	
		}			
	}
}
