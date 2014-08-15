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
import android.graphics.Canvas;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.nextgis.mobile.datasource.GeoEnvelope;
import com.nextgis.mobile.datasource.GeoPoint;

import java.io.File;

import static com.nextgis.mobile.util.Constants.*;

public class MapView extends MapBase implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener,  ScaleGestureDetector.OnScaleGestureListener  {

    protected final GestureDetector mGestureDetector;
    protected final ScaleGestureDetector mScaleGestureDetector;
    protected PointF mStartMouseLocation;
    protected PointF mCurrentMouseLocation;
    protected enumGISMap mDrawingState;
    protected float mScaleFactor;
    protected float mCurrentSpan;

    public MapView(Context context) {
        super(context);

        mGestureDetector = new GestureDetector(context, this);
        mGestureDetector.setOnDoubleTapListener(this);

        mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);

        mStartMouseLocation = new PointF();
        mCurrentMouseLocation = new PointF();

        mDrawingState = enumGISMap.drawing;
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
    protected void onDraw(Canvas canvas) {
        //Log.d(TAG, "state: " + mDrawingState + ", current loc: " +  mCurrentMouseLocation.toString());
        if(mDisplay != null){
            if(mDrawingState == enumGISMap.panning){
                canvas.drawBitmap(mDisplay.getDisplay(-mCurrentMouseLocation.x, -mCurrentMouseLocation.y, true), 0 , 0, null);
            }
            else if(mDrawingState == enumGISMap.zooming){
                canvas.drawBitmap(mDisplay.getDisplay(-mCurrentMouseLocation.x, -mCurrentMouseLocation.y, mScaleFactor), 0, 0, null);
            }
            else if(mDrawingState == enumGISMap.drawing_noclearbk){
                canvas.drawBitmap(mDisplay.getDisplay(false), 0 , 0, null);
            }
            else{
                canvas.drawBitmap(mDisplay.getDisplay(true), 0, 0, null);
            }

            if(mDrawingState == enumGISMap.double_tap)
                mDrawingState = enumGISMap.drawing;
        }
        else{
            super.onDraw(canvas);
        }
    }

    @Override
    protected synchronized void runDrawThread() {
        if(mDrawingState != enumGISMap.drawing_noclearbk)
            mDrawingState = enumGISMap.drawing;
        super.runDrawThread();
    }

    protected void zoomStart(ScaleGestureDetector scaleGestureDetector){
        mDrawingState = enumGISMap.zooming;
        mCurrentSpan = scaleGestureDetector.getCurrentSpan();
        mCurrentMouseLocation.set(-scaleGestureDetector.getFocusX(), -scaleGestureDetector.getFocusY());
        mScaleFactor = 1.f;
    }

    protected void zoom(ScaleGestureDetector scaleGestureDetector){
        if(mDrawingState == enumGISMap.zooming) {
            float scaleFactor = scaleGestureDetector.getCurrentSpan() / mCurrentSpan;
            int zoom = getZoomForScaleFactor(scaleFactor);
            if(zoom < mDisplay.getMinZoomLevel() || zoom > mDisplay.getMaxZoomLevel())
                return;

            mScaleFactor = scaleFactor;
            Log.d(TAG, "scale: " + mScaleFactor + " , zoom " + zoom);

            invalidate();
        }
    }

    protected int getZoomForScaleFactor(float scale){
        int zoom = mDisplay.getZoomLevel();
        if(scale > 1){
            zoom = (int) (mDisplay.getZoomLevel() + scale);
        }
        else if(scale < 1){
            zoom = (int) (mDisplay.getZoomLevel() - 1 / scale);
        }
        return zoom;
    }

    protected void zoomStop(MotionEvent e){
        if(mDrawingState == enumGISMap.zooming ) {
            mDrawingState = enumGISMap.drawing;
            //mScaleFactor *= scaleGestureDetector.getScaleFactor();
            int zoom = (int) (mDisplay.getZoomLevel() + mScaleFactor);

            Log.d(TAG, "onScale: " + mScaleFactor + ", add zoom: " + zoom);


            GeoPoint centerPt = mDisplay.screenToMap(new GeoPoint(mCurrentMouseLocation.x, mCurrentMouseLocation.y));

            setZoomAndCenter(zoom, centerPt);
        }
    }

    protected void panStart(final MotionEvent e){
        if(mDrawingState == enumGISMap.drawing || mDrawingState == enumGISMap.drawing_noclearbk) {
            cancelDrawThread();
            mStartMouseLocation.set(e.getX(), e.getY());
            mDrawingState = enumGISMap.panning;
        }
    }

    protected void panMoveTo(final MotionEvent e){
        //Log.d(TAG, "panMoveTo" + e.toString());
        if(mDrawingState == enumGISMap.panning){
            float x =  mStartMouseLocation.x - e.getX();
            float y =  mStartMouseLocation.y - e.getY();

            if(Math.abs(mCurrentMouseLocation.x - x) + Math.abs(mCurrentMouseLocation.y - y) < MIN_SCROLL_STEP) {
                return;
            }

            if(NO_MAP_LIMITS){
                mCurrentMouseLocation.set(x, y);
            }
            else {
                GeoEnvelope bounds = mDisplay.getScreenBounds();
                bounds.offset(x, y);

                GeoEnvelope limits = mDisplay.getLimits();
                if (bounds.getMinY() >= limits.getMinY() && bounds.getMaxY() <= limits.getMaxY()) {
                    mCurrentMouseLocation.set(x, y);
                } else {
                    mCurrentMouseLocation.set(x, mCurrentMouseLocation.y);
                }
            }

            invalidate();
        }
    }

    protected void panStop(final MotionEvent e){
        if(mDrawingState == enumGISMap.panning ) {
            float x = mCurrentMouseLocation.x; //mStartMouseLocation.x - e.getX();
            float y = mCurrentMouseLocation.y; //mStartMouseLocation.y - e.getY();

            GeoEnvelope bounds = mDisplay.getScreenBounds();
            bounds.offset(x, y);
            GeoEnvelope mapBounds = mDisplay.screenToMap(bounds);

            GeoPoint pt = mapBounds.getCenter();
            GeoPoint screenPt = mDisplay.mapToScreen(new GeoPoint(mapBounds.getMinX(), mapBounds.getMinY()));
            Log.d(TAG, "panStop. x: " + x + ", y:" + y + ", sx:" + screenPt.getX() + ", sy:" + screenPt.getY());
            mDisplay.panStop((float)screenPt.getX(), (float)screenPt.getY());

            mDisplay.setZoomAndCenter(mDisplay.getZoomLevel(), pt);
            mDrawingState = enumGISMap.drawing_noclearbk;

            mHandler.removeMessages(MSGTYPE_PANNING_DONE);

            Bundle bundle = new Bundle();
            bundle.putBoolean(BUNDLE_HASERROR_KEY, false);
            bundle.putInt(BUNDLE_TYPE_KEY, MSGTYPE_PANNING_DONE);

            Message msg = new Message();
            msg.setData(bundle);
            msg.what = MSGTYPE_PANNING_DONE;
            mHandler.sendMessageDelayed(msg, 350);
        }
    }

    @Override
    protected void onLayerDrawFinished(float percent){
        if(mDrawingState == enumGISMap.panning )
            return;
        super.onLayerDrawFinished(percent);
    }

    @Override
    protected void processMessage(Bundle bundle){
        switch (bundle.getInt(BUNDLE_TYPE_KEY)){
            case MSGTYPE_DS_TYPE_ZIP: //the new layer was create and need to be added on map
                File path = (File) bundle.getSerializable(BUNDLE_PATH_KEY);
                addLayer(path);
                break;
            case MSGTYPE_PANNING_DONE:
                if(mDrawingState == enumGISMap.drawing_noclearbk)
                    runDrawThread();
                break;
            default:
                super.processMessage(bundle);
        }
    }

    // delegate the event to the gesture detector
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean retVal = mScaleGestureDetector.onTouchEvent(e);
        //Log.d(TAG, "onTouchEvent: " + e.toString());
        if(e.getAction() == MotionEvent.ACTION_UP){
            panStop(e);
            zoomStop(e);
        }
        else if(e.getAction() == MotionEvent.ACTION_DOWN){
            panStart(e);
        }
        else if(e.getAction() == MotionEvent.ACTION_MOVE){
            panMoveTo(e);
        }

        retVal = mGestureDetector.onTouchEvent(e) || retVal;
        return retVal || super.onTouchEvent(e);

    }

    @Override
    public boolean onDown(MotionEvent e) {
        //Log.d(TAG, "onDown: " + e.toString());
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
//        Log.d(TAG, "onScroll: " + e1.toString() + ", " + e2.toString() + ", " + distanceX + ", " + distanceY);
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.d(TAG, "onSingleTapUp: " + e.toString());
        return false;
    }

    @Override
    public boolean onDoubleTap(final MotionEvent e) {
        Log.d(TAG, "onDoubleTap: " + e.getX() + ", " + e.getY());
        mDrawingState = enumGISMap.double_tap;
        final GeoPoint pt = mDisplay.screenToMap(new GeoPoint(e.getX(), e.getY()));
        setZoomAndCenter(mDisplay.getZoomLevel() + 1, pt);
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

    public void zoomIn() {
        mDrawingState = enumGISMap.drawing;
        setZoomAndCenter(getZoomLevel() + 1, getMapCenter());
    }

    public void zoomOut() {
        mDrawingState = enumGISMap.drawing;
        setZoomAndCenter(getZoomLevel() - 1, getMapCenter());
    }

    protected void onTest(){
        GeoEnvelope bounds = mDisplay.getScreenBounds();
        bounds.offset(250, -250);
        GeoEnvelope mapBounds = mDisplay.screenToMap(bounds);

        GeoPoint pt = mapBounds.getCenter();

        mDisplay.setZoomAndCenter(2, pt);
        Log.d(TAG, mDisplay.screenToMap(new GeoPoint(360, 567)).toString());

        bounds = mDisplay.getScreenBounds();
        bounds.offset(-250, 250);
        mapBounds = mDisplay.screenToMap(bounds);

        pt = mapBounds.getCenter();

        mDisplay.setZoomAndCenter(2, pt);
        Log.d(TAG, mDisplay.screenToMap(new GeoPoint(360, 567)).toString());
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        zoom(scaleGestureDetector);
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        zoomStart(scaleGestureDetector);
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        //zoomStop(scaleGestureDetector);
    }
}



