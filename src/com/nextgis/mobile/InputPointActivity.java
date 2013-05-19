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

import java.util.ArrayList;

import android.location.Location;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class InputPointActivity extends SherlockFragmentActivity {
	
    protected Location m_CurrentLocation;
    protected String m_sCat, m_sSubCat;
    protected float m_fAzimuth;
    protected float m_fDist;
    protected String m_sNote;
    
    protected ArrayList <String> image_lst = new ArrayList<String>();
    protected ArrayList <Double> image_rotation = new ArrayList<Double>();
	
    protected static final String CSV_CHAR = ";";    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //get location from calling class
        Bundle extras = getIntent().getExtras();        
        m_CurrentLocation = (Location)extras.get("com.nextgis.mobile.location");

/*       // setup action bar for tabs
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(false);
        
        Tab tab = actionBar.newTab()
                .setText(R.string.tabs_description_tab)
                .setTabListener(new TabListener<DescriptionFragment>(
                        this, "desc", DescriptionFragment.class));
        actionBar.addTab(tab);

        tab = actionBar.newTab()
            .setText(R.string.tabs_position_tab)
            .setTabListener(new TabListener<PositionFragment>(
                    this, "pos", PositionFragment.class));
        actionBar.addTab(tab);
 */       
      
    }
    
    public void onFinish() {
/*   	
    	//add point to the file
    	File file = new File(getExternalFilesDir(null), "points.csv");
    	boolean bExist = file.exists();
    	file.setReadable(true, false);
    	file.setWritable(true, false);
    	file.setExecutable(false, false);
    	try {  
            FileOutputStream os = new FileOutputStream(file, true);
            PrintWriter pw = new PrintWriter(os);
            if(!bExist)
            {
            	pw.println("date_time;lat;lon;acc;error_est;h;dir;src;speed;gps_t;cat;subcut;az;len;desc;photos;photos_az");
            }
            
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            final String CE = prefs.getString(PreferencesActivity.KEY_PREF_ACCURATE_CE, "None");
            
            pw.println(
            		java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()) + CSV_CHAR + 	//0
            		m_CurrentLocation.getLatitude() + CSV_CHAR + 														//1
            		m_CurrentLocation.getLongitude() + CSV_CHAR + 														//2
            		m_CurrentLocation.getAccuracy() + CSV_CHAR + 	
            		CE + CSV_CHAR +
            		m_CurrentLocation.getAltitude() + CSV_CHAR + 														//4
            		m_CurrentLocation.getBearing() + CSV_CHAR + 														//5
            		m_CurrentLocation.getProvider() + CSV_CHAR + 														//6
            		m_CurrentLocation.getSpeed() + CSV_CHAR + 															//7
            		m_CurrentLocation.getTime() + CSV_CHAR + 															//8
            		m_sCat + CSV_CHAR + 																				//9
            		m_sSubCat + CSV_CHAR + 																				//10
        		    m_fAzimuth + CSV_CHAR + 																			//11
            		m_fDist + CSV_CHAR + 																				//12
            		m_sNote + CSV_CHAR +  																				//13
            		image_lst.toString() + CSV_CHAR +
            		image_rotation.toString()
            		);
            
            pw.flush();
            pw.close();
            os.close();
            
            MediaScannerConnection.scanFile(this,
                    new String[] { file.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                    Log.i("ExternalStorage", "Scanned " + path + ":");
                    Log.i("ExternalStorage", "-> uri=" + uri);
                }
            });
        } catch (IOException e) {
            Log.w("ExternalStorage", "Error writing " + file, e);
        } 
    	//add point to the map
    	Toast.makeText(InputPOI.this, R.string.input_poi_added, Toast.LENGTH_SHORT).show();    	

    	finish();
    	
    	*/
    }
    
    public void SetDescription(String sCat, String sSubCat) {
    	m_sCat = sCat;
    	m_sSubCat = sSubCat;
    }
    
    public void SetDistance(float fDist) {
    	m_fDist = fDist;
    }
    
    public void SetAzimuth(float fAz) {
    	m_fAzimuth = fAz;
    }
    
    public void SetNotes(String sNotes) {
    	m_sNote = sNotes;
    }
    
    public Location getLocation() {
    	return m_CurrentLocation;
    }
    
    public void AddImage(String sImageName, double dfRotation){
    	image_lst.add(sImageName);
    	image_rotation.add(dfRotation);
    }    
        

/*        public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
            private Fragment mFragment;
            private final Activity mActivity;
            private final String mTag;
            private final Class<T> mClass;

            public TabListener(Activity activity, String tag, Class<T> clz) {
                mActivity = activity;
                mTag = tag;
                mClass = clz;
            }

            public void onTabSelected(Tab tab, FragmentTransaction ft) {
                // Check if the fragment is already initialized
                if (mFragment == null) {
                    // If not, instantiate and add it to the activity
                    mFragment = Fragment.instantiate(mActivity, mClass.getName());
                    ft.add(android.R.id.content, mFragment, mTag);
                } else {
                    // If it exists, simply attach it in order to show it
                    ft.attach(mFragment);
                }
            }

            public void onTabUnselected(Tab tab, FragmentTransaction ft) {
                if (mFragment != null) {
                    // Detach the fragment, because another one is being attached
                    ft.detach(mFragment);
                }
            }

			public void onTabReselected(Tab tab, FragmentTransaction ft) {
				// TODO Auto-generated method stub
				
			}
        }
        */
}
