/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2014 NextGIS
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
package com.nextgis.mobile.map;


import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.nextgis.mobile.datasource.GeoPoint;

import java.io.File;

import static com.nextgis.mobile.util.Constants.*;

public class MapView extends MapBase implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    protected final GestureDetector mGestureDetector;

    public MapView(Context context) {
        super(context);

        mGestureDetector = new GestureDetector(context, this);
        mGestureDetector.setOnDoubleTapListener(this);
    }

    public void createLayer(Uri uri, int type){
        Log.d(TAG, "File Uri: " + uri.toString());
        switch (type) {
            case DS_TYPE_ZIP:
                LocalTMSLayer.create(this, uri);
                return;
        }
    }

    @Override
    protected void processMessage(Bundle bundle){
        switch (bundle.getInt(BUNDLE_TYPE_KEY)){
            case MSGTYPE_DS_TYPE_ZIP: //the new layer was create and need to be added on map
                File path = (File) bundle.getSerializable(BUNDLE_PATH_KEY);
                addLayer(path);
                break;
            default:
                super.processMessage(bundle);
        }
    }

    // delegate the event to the gesture detector
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return mGestureDetector.onTouchEvent(e);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(final MotionEvent e) {
        Log.d(TAG, "onDoubleTap: " + e.getX() + ", " + e.getY());
        final GeoPoint pt = mDisplay.screenToMap(new GeoPoint(e.getX(), e.getY()));
        mDisplay.setZoomAndCenter(mDisplay.getZoomLevel() + 1, pt);
        runDrawThread();
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(final MotionEvent e) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e) {
        return false;
    }
}



