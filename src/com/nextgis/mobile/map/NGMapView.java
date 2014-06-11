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
package com.nextgis.mobile.map;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.DirectedLocationOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.PathOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import com.actionbarsherlock.internal.ResourcesCompat;
import com.nextgis.mobile.NGMConstants;
import com.nextgis.mobile.PositionFragment;
import com.nextgis.mobile.PreferencesActivity;
import com.nextgis.mobile.R;
import com.nextgis.mobile.ResourceProxyImpl;
import com.nextgis.mobile.services.TrackerService.RecordedGeoPoint;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class NGMapView {
	
	protected Context m_oContext;
	
	protected MapView m_oOsmv;
	protected ResourceProxy m_oResourceProxy;	
	//overlays
	protected MyLocationNewOverlay m_oLocationOverlay;
	protected CompassOverlay m_oCompassOverlay;
	protected DirectedLocationOverlay m_oDirectedLocationOverlay;
	//TODO: private RotationGestureOverlay mRotationGestureOverlay;
	protected PathOverlay m_oGPXOverlay;
	protected ItemizedIconOverlay<OverlayItem> m_aoPointsOverlay;
	
	protected LocationManager m_oLocationManager;
	protected ChangeLocationListener m_oChangeLocationListener;
	
	private RelativeLayout m_oRelativeLayout;
	 
	protected ArrayList<OverlayItem> m_aoItems;
	
	protected boolean m_bInfoOn;
	protected boolean m_bCompassOn;
	protected View m_oInfoView;

	protected final static String CSV_CHAR = ";";
	protected final static int margings = 10;
	
	public NGMapView(Context oContext){
		m_oContext = oContext;
		
		m_oLocationManager = (LocationManager) m_oContext.getSystemService(Context.LOCATION_SERVICE);
		m_oChangeLocationListener = new ChangeLocationListener();
		
		LayoutInflater inflater = (LayoutInflater)m_oContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		m_oInfoView = inflater.inflate(R.layout.infopane, null, true);
				
		m_oResourceProxy = new ResourceProxyImpl(m_oContext);

	    m_oRelativeLayout = new RelativeLayout(m_oContext);		
	}
	
	public void addPointToRouteOverlay(double dfX, double dfY){    	
    	if(m_oGPXOverlay == null)
    		return;    	
    	m_oGPXOverlay.addPoint((int)(dfY * 1000000), (int)(dfX * 1000000));		
	}
	
	public void addPointsToRouteOverlay(List<RecordedGeoPoint> path){
    	if(m_oGPXOverlay == null)
    		return;    	
		m_oGPXOverlay.clearPath();
		if(path != null){
			for(int i = 0; i < path.size(); i++){
				addPointToRouteOverlay(path.get(i).getLongitude(), path.get(i).getLatitude());					
			}
		}
	}
	
	public void initMap(int nTileSize, int nZoom, int nScrollX, int nScrollY){
		if(m_oOsmv != null){
			m_oRelativeLayout.removeAllViews();
			m_oOsmv = null;
		}
		m_oOsmv = new MapView(m_oContext, nTileSize, m_oResourceProxy);
		m_oOsmv.setUseSafeCanvas(true);
		
		//add overlays
		m_oLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(m_oContext), m_oOsmv, m_oResourceProxy);
		m_oCompassOverlay = new CompassOverlay(m_oContext, new InternalCompassOrientationProvider(m_oContext), m_oOsmv, m_oResourceProxy);
		
		//TODO: addUserLayers();
		
		int nHeight = 0;
		TypedValue typeValue = new TypedValue();
		
		m_oContext.getTheme().resolveAttribute(com.actionbarsherlock.R.attr.actionBarSize, typeValue, true);
		nHeight = TypedValue.complexToDimensionPixelSize(typeValue.data,m_oContext.getResources().getDisplayMetrics());
    
		m_oCompassOverlay.setCompassCenter(40, nHeight + 20 );
		
		//TODO: mRotationGestureOverlay = new RotationGestureOverlay(this, mOsmv);
		//TODO: mRotationGestureOverlay.setEnabled(false);
		
		m_oDirectedLocationOverlay = new DirectedLocationOverlay(m_oContext, m_oResourceProxy);
		m_oDirectedLocationOverlay.setShowAccuracy(true);
		
		m_oGPXOverlay = new PathOverlay(android.graphics.Color.RED, m_oResourceProxy);

		//auto enable follow location if position is closed to center
		m_oOsmv.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_UP) {
					IGeoPoint map_center_pt = m_oOsmv.getMapCenter();					
					GeoPoint pt = m_oLocationOverlay.getMyLocation();
					if(map_center_pt == null || pt == null)
						return false;
					int nMaxDist = m_oOsmv.getBoundingBox().getDiagonalLengthInMeters() / 15;
					int nDist = pt.distanceTo(map_center_pt);
					if(nDist < nMaxDist){
						m_oLocationOverlay.enableFollowLocation();
					}
					else {
						m_oLocationOverlay.disableFollowLocation();
					}
				}
				return false;
			}
	    });		

		m_oOsmv.setMultiTouchControls(true);
		//m_Osmv.setBuiltInZoomControls(true);
		m_oOsmv.getOverlays().add(m_oDirectedLocationOverlay);
		m_oOsmv.getOverlays().add(m_oLocationOverlay);
		m_oOsmv.getOverlays().add(m_oCompassOverlay);
		m_oOsmv.getOverlays().add(m_oGPXOverlay);
		//TODO: mOsmv.getOverlays().add(mRotationGestureOverlay);
		//ScaleBarOverlay		

		//TODO: LoadPointsToOverlay();
		
		m_oRelativeLayout.addView(m_oOsmv, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));		
	
		m_oOsmv.getController().setZoom(nZoom);
		m_oOsmv.scrollTo(nScrollX, nScrollY);

		//mLocationOverlay.enableMyLocation();
		//m_LocationOverlay.enableCompass();
		m_oLocationOverlay.setDrawAccuracyEnabled(true);

		m_oOsmv.setKeepScreenOn(true);
		
		addMapButtons(m_oRelativeLayout);
	}
	
	protected void addMapButtons(RelativeLayout rl){
		final ImageView ivZoomIn = new ImageView(m_oContext);
		ivZoomIn.setImageResource(R.drawable.ic_plus);
		ivZoomIn.setId(R.drawable.ic_plus);
		
		final ImageView ivZoomOut = new ImageView(m_oContext);
		ivZoomOut.setImageResource(R.drawable.ic_minus);	
		ivZoomOut.setId(R.drawable.ic_minus);			
		
		final ImageView ivMark = new ImageView(m_oContext);
		ivMark.setImageResource(R.drawable.ic_mark);	
		ivMark.setId(R.drawable.ic_mark);	
		
		//show zoom level between plus and minus
		final TextView ivZoomLevel = new TextView(m_oContext);
		//ivZoomLevel.setAlpha(150);
		ivZoomLevel.setId(R.drawable.ic_zoomlevel);
		
		final float scale = m_oContext.getResources().getDisplayMetrics().density;
		int pixels = (int) (48 * scale + 0.5f);
		
		ivZoomLevel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
		//ivZoomLevel.setTextAppearance(this, android.R.attr.textAppearanceLarge);
		
		ivZoomLevel.setWidth(pixels);
		ivZoomLevel.setHeight(pixels);
		ivZoomLevel.setTextColor(Color.DKGRAY);
		ivZoomLevel.setBackgroundColor( Color.argb(50, 128, 128, 128) );//Color.LTGRAY R.drawable.ic_zoomlevel);
		ivZoomLevel.setGravity(Gravity.CENTER);
		ivZoomLevel.setLayoutParams(new LayoutParams(
	            LayoutParams.MATCH_PARENT,
	            LayoutParams.WRAP_CONTENT));
		ivZoomLevel.setText("" + m_oOsmv.getZoomLevel());

		ivZoomIn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				m_oOsmv.getController().zoomIn();
				ivZoomOut.getDrawable().setAlpha(255);	
				ivZoomLevel.setText("" + m_oOsmv.getZoomLevel());
				if(!m_oOsmv.canZoomIn())
				{
					ivZoomIn.getDrawable().setAlpha(50);
				}
			}
		});				
		
		ivZoomOut.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				m_oOsmv.getController().zoomOut();
				
				ivZoomIn.getDrawable().setAlpha(255);
				ivZoomLevel.setText("" + m_oOsmv.getZoomLevel());
				if(!m_oOsmv.canZoomOut())
				{						
					ivZoomOut.getDrawable().setAlpha(50);
				}
			}
		});
		
		ivMark.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//TODO: onMark();
			}
		});
		
		final RelativeLayout.LayoutParams RightParams1 = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		RightParams1.setMargins(margings + 5, margings - 5, margings + 5, margings - 5);
		RightParams1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		RightParams1.addRule(RelativeLayout.CENTER_IN_PARENT);//ALIGN_PARENT_TOP
		rl.addView(ivZoomLevel, RightParams1);
		
		final RelativeLayout.LayoutParams RightParams4 = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		RightParams4.setMargins(margings + 5, margings - 5, margings + 5, margings - 5);
		RightParams4.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		RightParams4.addRule(RelativeLayout.ABOVE, R.drawable.ic_zoomlevel);//ALIGN_PARENT_TOP
		rl.addView(ivZoomIn, RightParams4);
		
		final RelativeLayout.LayoutParams RightParams3 = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		RightParams3.setMargins(margings + 5, margings - 5, margings + 5, margings - 5);
		RightParams3.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		RightParams3.addRule(RelativeLayout.CENTER_IN_PARENT);//ALIGN_PARENT_TOP
		rl.addView(ivMark, RightParams3);
		
		final RelativeLayout.LayoutParams RightParams2 = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		RightParams2.setMargins(margings + 5, margings - 5, margings + 5, margings - 5);
		RightParams2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);			
		RightParams2.addRule(RelativeLayout.BELOW, R.drawable.ic_zoomlevel);//R.drawable.ic_plus);
		rl.addView(ivZoomOut, RightParams2);	
	}
	
	public void panToLocation(){
		GeoPoint pt = m_oLocationOverlay.getMyLocation();
		if(pt != null)
		{
			m_oOsmv.getController().animateTo(pt);
			m_oLocationOverlay.enableFollowLocation();
		}
		else
		{
			Toast.makeText(m_oContext, R.string.error_loc_fix, Toast.LENGTH_SHORT).show();
		}
	}	
	
	/*
	protected void AddPointsToOverlay(){
		//add new point		
		File file = new File(getExternalFilesDir(null), "points.csv");
		if (file != null && file.exists()) {
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
		m_aoItems = new ArrayList<OverlayItem>();
		Drawable ivPt10 = getResources().getDrawable(R.drawable.dot10);
		
		File file = new File(getExternalFilesDir(null), "points.csv");
		if (file != null && file.exists()) {
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
		
		mPointsOverlay = new ItemizedIconOverlay<OverlayItem>(maItems, ivPt10, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
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
		
		mOsmv.getOverlays().add(mPointsOverlay);
	}
	*/
	
	public void showInfo(boolean bShow){
		if(bShow){
			m_oLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, m_oChangeLocationListener);
	        m_oLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, m_oChangeLocationListener);
		
			final RelativeLayout.LayoutParams RightParams = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
					RightParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
					
					int nHeight = 0;
					if(ResourcesCompat.getResources_getBoolean(m_oContext, R.bool.abs__split_action_bar_is_narrow)){
					//if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
						TypedValue typeValue = new TypedValue();
						
						m_oContext.getTheme().resolveAttribute(com.actionbarsherlock.R.attr.actionBarSize, typeValue, true);
						nHeight = TypedValue.complexToDimensionPixelSize(typeValue.data,m_oContext.getResources().getDisplayMetrics());
				    
						//getTheme().resolveAttribute(android.R.attr.actionBarSize, typeValue, true);
						//nHeight = TypedValue.complexToDimensionPixelSize(typeValue.data,getResources().getDisplayMetrics());
					}
					RightParams.setMargins(0, 0, 0, nHeight);
		    
					m_oRelativeLayout.addView(m_oInfoView, RightParams);					
		} else {
			m_oLocationManager.removeUpdates(m_oChangeLocationListener);
			m_oRelativeLayout.removeView(m_oInfoView);			
		}
		m_bInfoOn = bShow;
	}
	
	public void hideInfo(){
		showInfo(false);
	}
	
	public void showInfo(){
		showInfo(true);
	}
	
	public void switchInfo(){
		showInfo(!m_bInfoOn);
	}
	
	public void showCompass(boolean bShow){
    	if(bShow){
    		m_oCompassOverlay.enableCompass();        		
    	}
    	else {
    		m_oCompassOverlay.disableCompass();
    	}
		m_bCompassOn = bShow;
	}
	
	public void showCompass(){
		
	}
	
	public void hideCompass(){
		
	}
	
	public void switchCompass(){
		showCompass(!m_bCompassOn);
	}
	
	/*
	protected void addUserLayers()
	{
		final MapTileProviderBase tileProvider = new MapTileProviderGroup(new SimpleRegisterReceiver(getApplicationContext()), null);
		final TilesOverlay tilesOverlay = new TilesOverlay(tileProvider, this.getBaseContext());
		tilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
		mOsmv.getOverlays().add(tilesOverlay);
		
/*		final MapTileProviderBasic tileProvider = new MapTileProviderBasic(getApplicationContext());
		final ITileSource tileSource = new XYTileSource("FietsRegionaal", null, 3, 18, 256, ".png",
				new String[] { "http://overlay.openstreetmap.nl/openfietskaart-rcn/" });
		tileProvider.setTileSource(tileSource);
		final TilesOverlay tilesOverlay = new TilesOverlay(tileProvider, this.getBaseContext());
		tilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
		mOsmv.getOverlays().add(tilesOverlay);*/
/*	
	}
	*/
	protected String getTileSource(){
		if(m_oOsmv != null)
			return m_oOsmv.getTileProvider().getTileSource().name();
		else
			return "";
	}
	
	public void onPause(SharedPreferences.Editor edit){
		edit.putString(NGMConstants.PREFS_TILE_SOURCE, getTileSource());
		edit.putInt(NGMConstants.PREFS_SCROLL_X, m_oOsmv.getScrollX());
		edit.putInt(NGMConstants.PREFS_SCROLL_Y, m_oOsmv.getScrollY());
		edit.putInt(NGMConstants.PREFS_ZOOM_LEVEL, m_oOsmv.getZoomLevel());
		edit.putBoolean(NGMConstants.PREFS_SHOW_LOCATION, m_oLocationOverlay.isMyLocationEnabled());
		edit.putBoolean(NGMConstants.PREFS_SHOW_COMPASS, m_oCompassOverlay.isCompassEnabled());
		
		edit.putBoolean(NGMConstants.PREFS_SHOW_INFO, m_bInfoOn);
		
		if(m_bInfoOn)			
			hideInfo();		
		
		m_oLocationOverlay.disableMyLocation();
		m_oCompassOverlay.disableCompass();
		
	}
	
	public void onResume(SharedPreferences prefs){
		final String tileSourceName = prefs.getString(NGMConstants.PREFS_TILE_SOURCE, TileSourceFactory.DEFAULT_TILE_SOURCE.name());
		try {
			final ITileSource tileSource = TileSourceFactory.getTileSource(tileSourceName);
			m_oOsmv.setTileSource(tileSource);
		} catch (final IllegalArgumentException ignore) {
		}
		
		if (prefs.getBoolean(NGMConstants.PREFS_SHOW_LOCATION, true)) {
			m_oLocationOverlay.enableMyLocation();
		}
		if (prefs.getBoolean(NGMConstants.PREFS_SHOW_COMPASS, false)) {
			m_oCompassOverlay.enableCompass();
		}
		m_bInfoOn = prefs.getBoolean(NGMConstants.PREFS_SHOW_INFO, false);
		if (m_bInfoOn) {
			showInfo(true);
		}
		//AddPointsToOverlay();
		
		panToLocation();
	}
	
	private final class ChangeLocationListener implements LocationListener {
		
		public void onLocationChanged(Location location) {
			
			TextView speedText = (TextView)m_oInfoView.findViewById(R.id.speed_text);
			DecimalFormat df = new DecimalFormat("0.0");
			double dfSpeed = location.getSpeed() * 3.6;//to km/h
			speedText.setText("" + df.format(dfSpeed) + " " + m_oContext.getString(R.string.info_speed_val));
			
			TextView heightText = (TextView)m_oInfoView.findViewById(R.id.height_text);
			double dfHeight = location.getAltitude();
			heightText.setText("" + df.format(dfHeight) + " " + m_oContext.getString(R.string.info_height_val));
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(m_oContext);
        	int nFormat = prefs.getInt(NGMConstants.KEY_PREF_COORD_FORMAT + "_int", Location.FORMAT_SECONDS);
			
			TextView latText = (TextView)m_oInfoView.findViewById(R.id.lat_text);
			latText.setText(PositionFragment.formatLat(location.getLatitude(), nFormat, m_oContext.getResources()) + m_oContext.getResources().getText(R.string.coord_lat));
			
			TextView lonText = (TextView)m_oInfoView.findViewById(R.id.lon_text);
			lonText.setText(PositionFragment.formatLng(location.getLongitude(), nFormat, m_oContext.getResources()) + m_oContext.getResources().getText(R.string.coord_lon));			

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
