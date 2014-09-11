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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.*;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nextgis.mobile.datasource.GeoPoint;
import com.nextgis.mobile.map.Layer;
import com.nextgis.mobile.map.MapEventListener;
import com.nextgis.mobile.map.MapView;
import com.nextgis.mobile.util.Constants;

import java.text.DecimalFormat;
import java.util.Iterator;


public class MapFragment extends Fragment implements MapEventListener {

    protected final static int mMargings = 10;

    protected Context mContext;

    protected MapView mMap;
    protected ImageView mivZoomIn;
    protected ImageView mivZoomOut;
    protected TextView mivZoomLevel;
    protected View mInfoPane;

    protected RelativeLayout mMapRelativeLayout;

    protected LocationManager mLocationManager;
    protected ChangeLocationListener mChangeLocationListener;
    protected GpsStatusListener mGpsStatusListener;

    protected boolean mIsInfoPaneShow;


    private final class ChangeLocationListener implements LocationListener {

        public void onLocationChanged(Location location) {

            TextView speedText = (TextView) mInfoPane.findViewById(R.id.speed_text);
            DecimalFormat df = new DecimalFormat("0.0");
            double dfSpeed = location.getSpeed() * 3.6;//to km/h
            speedText.setText("" + df.format(dfSpeed) + " " +
                    mContext.getString(R.string.info_speed_val));

            TextView heightText = (TextView) mInfoPane.findViewById(R.id.height_text);
            double dfHeight = location.getAltitude();
            heightText.setText("" + df.format(dfHeight) + " " +
                    mContext.getString(R.string.info_height_val));

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            int nFormat = prefs.getInt(Constants.KEY_PREF_COORD_FORMAT + "_int",
                    Location.FORMAT_SECONDS);

            TextView latText = (TextView) mInfoPane.findViewById(R.id.lat_text);
            latText.setText(PositionFragment.formatLat(
                    location.getLatitude(), nFormat, mContext.getResources()) +
                    mContext.getResources().getText(R.string.coord_lat));

            TextView lonText = (TextView) mInfoPane.findViewById(R.id.lon_text);
            lonText.setText(PositionFragment.formatLng(
                    location.getLongitude(), nFormat, mContext.getResources()) +
                    mContext.getResources().getText(R.string.coord_lon));

            TextView accuracyText = (TextView) mInfoPane.findViewById(R.id.accuracy_text);
            float accuracy = location.getAccuracy();
            accuracyText.setText("" + df.format(accuracy) + " " +
                    mContext.getString(R.string.info_accuracy_val));
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

    private final class GpsStatusListener implements GpsStatus.Listener {

        @Override
        public void onGpsStatusChanged(int event) {
            if (GpsStatus.GPS_EVENT_SATELLITE_STATUS == event) {
                Iterator<GpsSatellite> iterator =
                        mLocationManager.getGpsStatus(null).getSatellites().iterator();

                int countSat = 0;
                while (iterator.hasNext()) {
                    iterator.next();
                    ++countSat;
                }

                TextView countSatText = (TextView) mInfoPane.findViewById(R.id.count_sat_text);
                countSatText.setText(countSat > 0 ? "" + countSat : "-");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mContext = getActivity().getApplicationContext();

        if (mMap == null) {
            MainActivity activity = (MainActivity) getActivity();
            mMap = activity.getMap();
            mMap.addListener(this);
        }

        View view = inflater.inflate(R.layout.mapfragment, container, false);
        FrameLayout layout = (FrameLayout) view.findViewById(R.id.mapholder);
        //search relative view of map, if not found - add it
        if (mMap != null) {
            mMapRelativeLayout = (RelativeLayout) layout.findViewById(R.id.maprl);
            if (mMapRelativeLayout != null) {
                mMapRelativeLayout.addView(mMap, new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT));
                addMapButtons(mMapRelativeLayout);
            } // layout.addView(mMap);//.getRelativeLayout());
        }

        mInfoPane = inflater.inflate(R.layout.infopane, null, true);

        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mChangeLocationListener = new ChangeLocationListener();
        mGpsStatusListener = new GpsStatusListener();

        return view;
    }

    protected void addMapButtons(RelativeLayout rl) {
        mivZoomIn = new ImageView(getActivity());
        mivZoomIn.setImageResource(R.drawable.ic_plus);
        //mivZoomIn.setId(R.drawable.ic_plus);

        mivZoomOut = new ImageView(getActivity());
        mivZoomOut.setImageResource(R.drawable.ic_minus);
        //mivZoomOut.setId(R.drawable.ic_minus);

        final ImageView ivMark = new ImageView(getActivity());
        ivMark.setImageResource(R.drawable.ic_mark);
        //ivMark.setId(R.drawable.ic_mark);

        //show zoom level between plus and minus
        mivZoomLevel = new TextView(getActivity());
        //ivZoomLevel.setAlpha(150);
        mivZoomLevel.setId(R.drawable.ic_zoomlevel);

        final float scale = getResources().getDisplayMetrics().density;
        int pixels = (int) (48 * scale + 0.5f);

        mivZoomLevel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        //ivZoomLevel.setTextAppearance(this, android.R.attr.textAppearanceLarge);

        mivZoomLevel.setWidth(pixels);
        mivZoomLevel.setHeight(pixels);
        mivZoomLevel.setTextColor(Color.DKGRAY);
        mivZoomLevel.setBackgroundColor(Color.argb(50, 128, 128, 128)); //Color.LTGRAY R.drawable.ic_zoomlevel);
        mivZoomLevel.setGravity(Gravity.CENTER);
        mivZoomLevel.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        mivZoomLevel.setText("" + (int) Math.floor(mMap.getZoomLevel()));

        mivZoomIn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mMap.zoomIn();
            }
        });

        mivZoomOut.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mMap.zoomOut();
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
        RightParams1.setMargins(mMargings + 5, mMargings - 5, mMargings + 5, mMargings - 5);
        RightParams1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        RightParams1.addRule(RelativeLayout.CENTER_IN_PARENT);//ALIGN_PARENT_TOP
        rl.addView(mivZoomLevel, RightParams1);

        final RelativeLayout.LayoutParams RightParams4 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        RightParams4.setMargins(mMargings + 5, mMargings - 5, mMargings + 5, mMargings - 5);
        RightParams4.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        RightParams4.addRule(RelativeLayout.ABOVE, R.drawable.ic_zoomlevel);//ALIGN_PARENT_TOP
        rl.addView(mivZoomIn, RightParams4);

        final RelativeLayout.LayoutParams RightParams3 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        RightParams3.setMargins(mMargings + 5, mMargings - 5, mMargings + 5, mMargings - 5);
        RightParams3.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        RightParams3.addRule(RelativeLayout.CENTER_IN_PARENT);//ALIGN_PARENT_TOP
        rl.addView(ivMark, RightParams3);

        final RelativeLayout.LayoutParams RightParams2 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        RightParams2.setMargins(mMargings + 5, mMargings - 5, mMargings + 5, mMargings - 5);
        RightParams2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        RightParams2.addRule(RelativeLayout.BELOW, R.drawable.ic_zoomlevel);//R.drawable.ic_plus);
        rl.addView(mivZoomOut, RightParams2);

        setZoomInEnabled(mMap.canZoomIn());
        setZoomOutEnabled(mMap.canZoomOut());
    }

    @Override
    public void onLayerAdded(Layer layer) {

    }

    @Override
    public void onLayerDeleted(int id) {

    }

    @Override
    public void onLayerChanged(Layer layer) {

    }

    @Override
    public void onExtentChanged(int zoom, GeoPoint center) {
        mivZoomLevel.setText("" + (int) Math.floor(mMap.getZoomLevel()));
        setZoomInEnabled(mMap.canZoomIn());
        setZoomOutEnabled(mMap.canZoomOut());
    }

    protected void setZoomInEnabled(boolean bEnabled) {
        if (bEnabled) {
            mivZoomIn.getDrawable().setAlpha(255);
        } else {
            mivZoomIn.getDrawable().setAlpha(50);
        }
    }

    protected void setZoomOutEnabled(boolean bEnabled) {
        if (bEnabled) {
            mivZoomOut.getDrawable().setAlpha(255);
        } else {
            mivZoomOut.getDrawable().setAlpha(50);
        }
    }

    public void showInfoPane(boolean isShow) {

        if (isShow) {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0, 0, mChangeLocationListener);
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 0, 0, mChangeLocationListener);
            mLocationManager.addGpsStatusListener(mGpsStatusListener);

            final RelativeLayout.LayoutParams RightParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            RightParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

            int nHeight = 0;

            if (mContext.getResources().getConfiguration().orientation !=
                    Configuration.ORIENTATION_LANDSCAPE) {

                TypedValue typeValue = new TypedValue();

                mContext.getTheme().resolveAttribute(
                        android.R.attr.actionBarSize, typeValue, true);
                nHeight = TypedValue.complexToDimensionPixelSize(
                        typeValue.data, mContext.getResources().getDisplayMetrics());

                //getTheme().resolveAttribute(android.R.attr.actionBarSize, typeValue, true);
                //nHeight = TypedValue.complexToDimensionPixelSize(
                //        typeValue.data,getResources().getDisplayMetrics());
            }

            RightParams.setMargins(0, 0, 0, nHeight);
            mMapRelativeLayout.addView(mInfoPane, RightParams);

        } else {
            mLocationManager.removeUpdates(mChangeLocationListener);
            mLocationManager.removeGpsStatusListener(mGpsStatusListener);
            mMapRelativeLayout.removeView(mInfoPane);
        }

        mIsInfoPaneShow = isShow;
    }

    public void switchInfoPane() {
        showInfoPane(!mIsInfoPaneShow);
    }

    @Override
    public void onResume() {
        super.onResume();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        mIsInfoPaneShow = prefs.getBoolean(Constants.PREFS_SHOW_INFO, false);
        if (mIsInfoPaneShow) {
            showInfoPane(true);
        }
    }

    @Override
    public void onPause() {
        SharedPreferences.Editor edit =
                PreferenceManager.getDefaultSharedPreferences(mContext).edit();

        edit.putBoolean(Constants.PREFS_SHOW_INFO, mIsInfoPaneShow);
        edit.commit();

        if (mIsInfoPaneShow) {
            showInfoPane(false); // for mLocationManager.removeUpdates(mChangeLocationListener)
        }

        super.onPause();
    }
}
