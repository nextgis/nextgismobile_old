/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2012-2014 NextGIS
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
package com.nextgis.mobile;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.nextgis.mobile.datasource.GeoPoint;
import com.nextgis.mobile.dialogs.FieldEditorFragment;
import com.nextgis.mobile.dialogs.NgwResourcesDialog;
import com.nextgis.mobile.map.MapViewEditable;
import com.nextgis.mobile.services.TrackerService;
import com.nextgis.mobile.services.TrackerService.TSBinder;
import com.nextgis.mobile.util.Constants;

import static com.nextgis.mobile.util.GeoConstants.*;

public class MainActivity extends ActionBarActivity {

    protected TrackerService mTrackerService;
    protected Handler mTrackAddPointHandler;
    protected MapViewEditable mMap;
    protected MapFragment mMapFragment;
    protected boolean mbGpxRecord;
    protected LayersFragment mLayersFragment;
    protected boolean warInfoPaneShowBeforeEditMode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMap = new MapViewEditable(this);
        //mMap.initMap(nTileSize, mMapZoom, mMapScrollX, mMapScrollY);
        //mMap.showInfoPane(bInfoOn);
        //mMap.showCompass(bCompassOn);

        setContentView(R.layout.activity_main);

        // initialize the default settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//		boolean bInfoOn = prefs.getBoolean(Constants.PREFS_SHOW_INFO, false);
        mbGpxRecord = prefs.getBoolean(Constants.KEY_PREF_SW_TRACKGPX_SRV, false);
        boolean bCompassOn = prefs.getBoolean(Constants.PREFS_SHOW_COMPASS, false);
        int nTileSize = 256;//prefs.getInt(NGMConstants.KEY_PREF_TILE_SIZE + "_int", 256);
        warInfoPaneShowBeforeEditMode = prefs.getBoolean(Constants.PREFS_WAR_SHOW_INFO, false);

        restoreActionBar();

        mLayersFragment = (LayersFragment) getSupportFragmentManager().findFragmentById(R.id.layers);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        mMapFragment = (MapFragment) getSupportFragmentManager().findFragmentByTag("MAP");

        if (mMapFragment == null) {
            mMapFragment = new MapFragment();
            fragmentTransaction.add(R.id.map, mMapFragment, "MAP").commit();
        }

        getSupportFragmentManager().executePendingTransactions();

        LayersFragment layersFragment =
                (LayersFragment) getSupportFragmentManager().findFragmentById(R.id.layers);

        // Set up the drawer.
        layersFragment.setUp(R.id.layers, (DrawerLayout) findViewById(R.id.drawer_layout));
/*		
        showLayersList(m_bShowLayersList);
*/
    }

    @Override
    public void onPause() {
        final SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();

        if (mMap != null) {
            GeoPoint point = mMap.getMapCenter();
            edit.putLong(Constants.PREFS_ZOOM_LEVEL, Double.doubleToRawLongBits(mMap.getZoomLevel()));
            edit.putLong(Constants.PREFS_SCROLL_X, Double.doubleToRawLongBits(point.getX()));
            edit.putLong(Constants.PREFS_SCROLL_Y, Double.doubleToRawLongBits(point.getY()));

            mMap.onPause();
        }

        edit.putBoolean(Constants.KEY_PREF_SW_TRACKGPX_SRV, mbGpxRecord);

        if (mbGpxRecord) {
            unbindService(m_oConnection);
        }

        edit.commit();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mMap != null) {
            mMap.onResume();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            double mMapZoom = Double.longBitsToDouble(prefs.getLong(Constants.PREFS_ZOOM_LEVEL, 1));
            double mMapScrollX = Double.longBitsToDouble(prefs.getLong(Constants.PREFS_SCROLL_X, 0));
            double mMapScrollY = Double.longBitsToDouble(prefs.getLong(Constants.PREFS_SCROLL_Y, 0));
            mMap.setZoomAndCenter(mMapZoom, new GeoPoint(mMapScrollX, mMapScrollY));
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        mbGpxRecord = prefs.getBoolean(Constants.KEY_PREF_SW_TRACKGPX_SRV, false);
        if (mbGpxRecord) {
            startGPXRecord();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mMap != null) {
            mMap.onStart();
        }
    }

    @Override
    public void onStop() {
        if (mMap != null) {
            mMap.onStop();
        }
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (!mLayersFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.main, menu);
            MenuItem changeAttribute = menu.findItem(R.id.menu_ef_change_attributes);

            switch (getEditState()) {
                case EDIT_STATE_NONE:
                    menu.setGroupVisible(R.id.group_menu_main, true);
                    menu.setGroupVisible(R.id.group_menu_edit_layer, false);
                    menu.setGroupVisible(R.id.group_menu_edit_fields, false);
                    break;

                case EDIT_STATE_LAYER_EDIT:
                    menu.setGroupVisible(R.id.group_menu_main, false);
                    menu.setGroupVisible(R.id.group_menu_edit_layer, true);
                    menu.setGroupVisible(R.id.group_menu_edit_fields, false);
                    break;

                case EDIT_STATE_FIELD_EDIT:
                    menu.setGroupVisible(R.id.group_menu_main, false);
                    menu.setGroupVisible(R.id.group_menu_edit_layer, false);
                    menu.setGroupVisible(R.id.group_menu_edit_fields, true);
                    changeAttribute.setVisible(false);
                    break;

                case EDIT_STATE_FIELD_VIEW:
                    menu.setGroupVisible(R.id.group_menu_main, false);
                    menu.setGroupVisible(R.id.group_menu_edit_layer, false);
                    menu.setGroupVisible(R.id.group_menu_edit_fields, true);
                    changeAttribute.setVisible(true);
                    break;
            }

            if (mMap != null) {
                boolean isForHide = mMap.isEditModeActive() && mMapFragment.isInfoPaneShow();
                boolean isForShow = !mMap.isEditModeActive() && !mMapFragment.isInfoPaneShow()
                        && warInfoPaneShowBeforeEditMode;

                if (isForHide) {
                    warInfoPaneShowBeforeEditMode = true;
                } else if (isForShow) {
                    warInfoPaneShowBeforeEditMode = false;
                }

                if (isForHide || isForShow) {
                    mMapFragment.switchInfoPane();

                    SharedPreferences.Editor edit =
                            PreferenceManager.getDefaultSharedPreferences(this).edit();
                    edit.putBoolean(Constants.PREFS_WAR_SHOW_INFO, warInfoPaneShowBeforeEditMode);
                    edit.commit();
                }
            }

            restoreActionBar();
        }

        return true;
    }

    public void switchMenuView() {
        supportInvalidateOptionsMenu();
    }

    protected static final int EDIT_STATE_NONE = 0;
    protected static final int EDIT_STATE_LAYER_EDIT = 1;
    protected static final int EDIT_STATE_FIELD_EDIT = 2;
    protected static final int EDIT_STATE_FIELD_VIEW = 3;

    protected int getEditState() {
        if (mMap == null) return EDIT_STATE_NONE;

        FieldEditorFragment fieldEditor = (FieldEditorFragment)
                getSupportFragmentManager().findFragmentByTag("FieldEditor");

        if (fieldEditor != null) return fieldEditor.isEditMode()
                ? EDIT_STATE_FIELD_EDIT : EDIT_STATE_FIELD_VIEW;

        if (mMap.isEditModeActive()) return EDIT_STATE_LAYER_EDIT;

        return EDIT_STATE_NONE;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intentSet = new Intent(this, PreferencesActivity.class);
                intentSet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentSet);
                return true;
            case R.id.menu_about:
                Intent intentAbout = new Intent(this, AboutActivity.class);
                intentAbout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentAbout);
                return true;
            case R.id.menu_pan:
//        	mMap.panToLocation();
                return true;
            case R.id.menu_info:
                mMapFragment.switchInfoPane();
                return true;
            case R.id.menu_record_gpx:
                onRecordGpx();
                return true;
            case R.id.menu_compass:
//        	mMap.switchCompass();
                return true;
            case R.id.menu_add_zip:
                onAdd(DS_TYPE_ZIP);
                return true;
            case R.id.menu_add_tms:
                onAdd(DS_TYPE_TMS);
                return true;
            case R.id.menu_add_json:
                onAdd(DS_TYPE_LOCAL_GEOJSON);
                return true;
            case R.id.menu_add_ngfp:
                onAdd(DS_TYPE_LOCAL_NGFP);
                return true;
            case R.id.menu_add_ngw:
                onAddNgwLayers();
                return true;
            case R.id.menu_save:
                mMap.onSaveEditLayer();
                return true;
            case R.id.menu_change_attributes:
                onEditFeatureAttributes();
                return true;
            case R.id.menu_cancel:
                mMap.onCancelEditLayer();
                return true;
            case R.id.menu_ef_save:
                onSaveEditedFeatureFields();
                return true;
            case R.id.menu_ef_change_attributes:
                onEditFeatureAttributesFromShow();
                return true;
            case R.id.menu_ef_cancel:
                onCancelEditFeatureAttributes();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
    }

    void doBindService() {

        mTrackAddPointHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                Bundle resultData = msg.getData();
                double dfLat = resultData.getDouble("lat");
                double dfLon = resultData.getDouble("lon");
//            	mMap.addPointToRouteOverlay(dfLon, dfLat);
            }
        };


        bindService(new Intent(this, TrackerService.class), m_oConnection, 0);
    }

    private ServiceConnection m_oConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            TSBinder tsBinder = (TSBinder) binder;
            if (tsBinder == null)
                return;

            mTrackerService = tsBinder.getService();
            if (mTrackerService == null)
                return;

            mTrackerService.setPathHanler(mTrackAddPointHandler);
            //fill path
//			ArrayList<RecordedGeoPoint> path = mTrackerService.GetPath();
//			mMap.addPointsToRouteOverlay(path);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mTrackerService = null;
        }
    };


    void onRecordGpx() {
        mbGpxRecord = !mbGpxRecord;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(Constants.KEY_PREF_SW_TRACKGPX_SRV, mbGpxRecord);
        edit.commit();

        final SharedPreferences.Editor editor1 = getSharedPreferences("preferences", Context.MODE_PRIVATE).edit();
        editor1.putBoolean(Constants.KEY_PREF_SW_TRACKGPX_SRV, mbGpxRecord);
        editor1.commit();


        if (mbGpxRecord) {
            //start record
            startGPXRecord();
        } else {
            //stop record
            stopGPXRecord();
        }
    }

    void startGPXRecord() {
        startService(new Intent(TrackerService.ACTION_START_GPX));

        bindService(new Intent(this, TrackerService.class), m_oConnection, Context.BIND_AUTO_CREATE);
    }

    void stopGPXRecord() {
        startService(new Intent(TrackerService.ACTION_STOP_GPX));
        unbindService(m_oConnection);
    }

    protected void onAdd(int nType) {
        switch (nType) {
            case DS_TYPE_ZIP:
                Intent intent_zip = new Intent(Intent.ACTION_GET_CONTENT);
                intent_zip.setType("application/zip");
                intent_zip.addCategory(Intent.CATEGORY_OPENABLE);

                try {
                    startActivityForResult(Intent.createChooser(intent_zip,
                            getString(R.string.message_select_file)), DS_TYPE_ZIP);
                } catch (ActivityNotFoundException e) {
                    // Potentially direct the user to the Market with a Dialog
                    Toast.makeText(this, R.string.error_file_manager, Toast.LENGTH_SHORT).show();
                }

                break;

            case DS_TYPE_TMS:
                mMap.createLayer(null, nType);
                break;

            case DS_TYPE_LOCAL_GEOJSON:
                Intent intent_geojson = new Intent(Intent.ACTION_GET_CONTENT);
                //intent_geojson.setType("application/json");
                intent_geojson.setType("application/*");
                intent_geojson.addCategory(Intent.CATEGORY_OPENABLE);

                try {
                    startActivityForResult(Intent.createChooser(intent_geojson,
                            getString(R.string.message_select_file)), DS_TYPE_LOCAL_GEOJSON);
                } catch (ActivityNotFoundException e) {
                    // Potentially direct the user to the Market with a Dialog
                    Toast.makeText(this, R.string.error_file_manager, Toast.LENGTH_SHORT).show();
                }

                break;

            case DS_TYPE_LOCAL_NGFP:
                Intent intent_ngfp = new Intent(Intent.ACTION_GET_CONTENT);
                //intent_ngfp.setType("application/ngfp");
                intent_ngfp.setType("application/*");
                intent_ngfp.addCategory(Intent.CATEGORY_OPENABLE);

                try {
                    startActivityForResult(Intent.createChooser(intent_ngfp,
                            getString(R.string.message_select_file)), DS_TYPE_LOCAL_NGFP);
                } catch (ActivityNotFoundException e) {
                    // Potentially direct the user to the Market with a Dialog
                    Toast.makeText(this, R.string.error_file_manager, Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    public void onAddNgwLayers() {
        NgwResourcesDialog dialog = new NgwResourcesDialog();
        dialog.show(getSupportFragmentManager(), "NgwResourcesDialog");
    }

    public void onEditFeatureAttributes() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        FieldEditorFragment fieldEditor = (FieldEditorFragment)
                getSupportFragmentManager().findFragmentByTag("FieldEditor");

        if (fieldEditor == null) {
            fieldEditor = new FieldEditorFragment();
            fieldEditor.setParams(getMap(), null, null, true);
        }

        fragmentTransaction.replace(R.id.map, fieldEditor, "FieldEditor");
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        getSupportFragmentManager().executePendingTransactions();

        switchMenuView();
    }

    public void onSaveEditedFeatureFields() {
        FieldEditorFragment fieldEditor = (FieldEditorFragment)
                getSupportFragmentManager().findFragmentByTag("FieldEditor");

        if (fieldEditor != null) {
            fieldEditor.saveEditedFields();
        }

        getSupportFragmentManager().popBackStackImmediate();
        switchMenuView();
    }

    public void onEditFeatureAttributesFromShow() {
        FieldEditorFragment fieldEditor = (FieldEditorFragment)
                getSupportFragmentManager().findFragmentByTag("FieldEditor");

        if (fieldEditor != null) {
            fieldEditor.setEditMode();
            switchMenuView();
        }
    }

    public void onCancelEditFeatureAttributes() {
        getSupportFragmentManager().popBackStackImmediate();
        switchMenuView();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            mMap.createLayer(data.getData(), requestCode);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void onMark() {
    /* TODO:   
		final Location loc = mLocationOverlay.getLastFix();
		
		if(loc == null)
		{
			Toast.makeText(getApplicationContext(), R.string.error_loc_fix, Toast.LENGTH_SHORT).show();
		}
		else
		{	
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			final boolean bAccCoord = prefs.getBoolean(PreferencesActivity.KEY_PREF_ACCURATE_LOC, false);
			if(bAccCoord)
			{				          
	          class AccLocation extends Handler  implements LocationListener{
	        	  int nPointCount;
	        	  double dfXsum, dfYsum, dfXmean, dfYmean, dfXmin, dfYmin, dfXmax, dfYmax;
	        	  double dfAsum, dfAmean, dfAmin, dfAmax;
	        	  double dfXSumSqDev, sdYSumSqDev;
	        	  ArrayList<GeoPoint> GPSRecords = new ArrayList<GeoPoint>();
	        	  
	        	  public AccLocation() {
						dfXsum = dfYsum = dfXmean = dfYmean = dfXmin = dfYmin = dfXmax = dfYmax = 0;
						dfAsum = dfAmean = dfAmin = dfAmax = 0;
						dfXSumSqDev = sdYSumSqDev = 0;
						
						mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	        		  	
						pd = new ProgressDialog(MainActivity.this);
						pd.setTitle(R.string.acc_gather_dlg_title);
						//pd.setMessage("Wait");
						pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
						final int nGPSCount = prefs.getInt(PreferencesActivity.KEY_PREF_ACCURATE_GPSCOUNT + "_int", 60);
						pd.setMax(nGPSCount);
						pd.setIndeterminate(true);
						pd.show();									
	        	  }
	        	  
		        public void handleMessage(Message msg) {
		        	pd.setIndeterminate(false);
		        	if (pd.getProgress() < pd.getMax()) {
		        		sendEmptyMessageDelayed(0, 100);
		        	}
		        	else {
		        		mLocationManager.removeUpdates(this);
		        		
						dfXmean = dfXsum / nPointCount;
						dfYmean = dfYsum / nPointCount;	
						dfAmean = dfAsum / nPointCount;		
						
						Location newLoc = new Location("GPS Accurate");
						
		        		newLoc.setSpeed(0);
		        		newLoc.setLatitude(dfYmean);
		        		newLoc.setLongitude(dfXmean);
		        		newLoc.setAltitude(dfAmean);
		        		newLoc.setTime(System.currentTimeMillis());									
						
		        		GeoPoint basept = new GeoPoint(newLoc);
		        		
		        		ArrayList<Integer> GPSDist = new ArrayList<Integer>();
		        		
		        		for (final GeoPoint gp : GPSRecords) {
		        			dfXSumSqDev += ( (gp.getLongitudeE6() - basept.getLongitudeE6()) / 1000000 ) * ( (gp.getLongitudeE6() - basept.getLongitudeE6()) / 1000000 );
		        			sdYSumSqDev += ( (gp.getLatitudeE6() - basept.getLatitudeE6()) / 1000000 ) * ( (gp.getLatitudeE6() - basept.getLatitudeE6()) / 1000000 );
		        			
		        			GPSDist.add(basept.distanceTo(gp));
		    			}
		        		
		        		Collections.sort(GPSDist);
		        		

			        	float dfAcc;
			        	int nIndex = 0;
						final String CE = prefs.getString(PreferencesActivity.KEY_PREF_ACCURATE_CE, "CE50");

			        	if(CE.compareTo("CE50") == 0)
			        		nIndex = (int) (GPSDist.size() * 0.5);
						else if(CE.compareTo("CE90") == 0)
							nIndex = (int) (GPSDist.size() * 0.9);
						else if(CE.compareTo("CE95") == 0)
							nIndex = (int) (GPSDist.size() * 0.95);
						else if(CE.compareTo("CE98") == 0)
							nIndex = (int) (GPSDist.size() * 0.98);

			        	dfAcc = GPSDist.get(nIndex);
		        		newLoc.setAccuracy(dfAcc);
		        		
		        		Intent newIntent = new Intent(MainActivity.this, InputPointActivity.class);
		        		newIntent.putExtra(LOACTION_HINT, newLoc);
		        		startActivity (newIntent);
		        		pd.dismiss();
		        	}	
		        }

				public void onLocationChanged(Location location) {
					GPSRecords.add(new GeoPoint(location.getLatitude(), location.getLongitude()));
					if ( dfXmin == 0 )
					{
						dfXmin = location.getLongitude();
						dfXmax = location.getLongitude();
					}
					else {
						dfXmin = Math.min(dfXmin, location.getLongitude());
						dfXmax = Math.max(dfXmin, location.getLongitude());
					}
					
					if ( dfYmin == 0 )
					{
						dfYmin = location.getLatitude();
						dfYmax = location.getLatitude();
					}
					else {
						dfYmin = Math.min(dfYmin, location.getLatitude());
						dfYmax = Math.max(dfYmin, location.getLatitude());
					}
					
					if ( dfAmin == 0 )
					{
						dfAmin = location.getAltitude();
						dfAmax = location.getAltitude();
					}
					else {
						dfAmin = Math.min(dfAmin, location.getAltitude());
						dfAmax = Math.max(dfAmax, location.getAltitude());
					}								
					
					dfXsum += location.getLongitude();
					dfYsum += location.getLatitude();
					dfAsum += location.getAltitude();
					
					nPointCount++;
					
					//dfXmean = dfXsum / nPointCount;
					//dfYmean = dfYsum / nPointCount;
							
					//pd.setMessage("X: " + (( location.getLongitude() - dfXmean ) * ( location.getLongitude() - dfXmean )) + "Y: " + (( location.getLatitude() - dfYmean ) * ( location.getLatitude() - dfYmean )));
					pd.incrementProgressBy(1);								
				}

				public void onProviderDisabled(String provider) {
					// TODO Auto-generated method stub
					
				}

				public void onProviderEnabled(String provider) {
					// TODO Auto-generated method stub
					
				}

				public void onStatusChanged(String provider,
						int status, Bundle extras) {
					// TODO Auto-generated method stub
					
				}
	          }
	          
	          AccLocation h = new AccLocation();
	          h.sendEmptyMessageDelayed(0, 2000);
			}
			else
			{
				Toast.makeText(getApplicationContext(), PositionFragment.getLocationText(getApplicationContext(), loc), Toast.LENGTH_SHORT).show();
				Intent newIntent = new Intent(this, InputPointActivity.class);		
				newIntent.putExtra(LOACTION_HINT, loc);
				startActivity (newIntent);
			}
		}*/
    }

    public MapViewEditable getMap() {
        return mMap;
    }

    public void setMap(MapViewEditable map) {
        this.mMap = map;
    }

    boolean mIsFieldEditorVisible = false;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        FieldEditorFragment fieldEditor = (FieldEditorFragment)
                getSupportFragmentManager().findFragmentByTag("FieldEditor");
        mIsFieldEditorVisible = fieldEditor != null && fieldEditor.isVisible();

        if (mIsFieldEditorVisible) {
            switchMenuView();
            return super.dispatchKeyEvent(event);
        }

        if (mMap.isEditModeActive()
                && event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_UP) {

            mMap.onCancelEditLayer();
            return true;
        }

        return super.dispatchKeyEvent(event);
    }
}
