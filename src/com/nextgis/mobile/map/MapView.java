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
import static com.nextgis.mobile.util.GeoConstants.*;

public class MapView extends MapBase implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener,  ScaleGestureDetector.OnScaleGestureListener  {

    protected final GestureDetector mGestureDetector;
    protected final ScaleGestureDetector mScaleGestureDetector;
    protected PointF mStartMouseLocation;
    protected PointF mCurrentMouseLocation;
    protected PointF mCurrentFocusLocation;
    protected int mDrawingState;
    protected double mScaleFactor;
    protected double mCurrentSpan;

    public MapView(Context context) {
        super(context);

        mGestureDetector = new GestureDetector(context, this);
        mGestureDetector.setOnDoubleTapListener(this);

        mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);

        mStartMouseLocation = new PointF();
        mCurrentMouseLocation = new PointF();
        mCurrentFocusLocation = new PointF();

        mDrawingState = DRAW_SATE_drawing;
    }

    public void createLayer(Uri uri, int type){
        //Log.d(TAG, "File Uri: " + uri.toString());
        switch (type) {
            case DS_TYPE_ZIP:
                LocalTMSLayer.create(this, uri);
                return;
            case DS_TYPE_TMS:
                RemoteTMSLayer.create(this);
                return;
            case DS_TYPE_LOCAL_GEOJSON:
                LocalGeoJsonLayer.create(this, uri);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //Log.d(TAG, "state: " + mDrawingState + ", current loc: " +  mCurrentMouseLocation.toString() + " current focus: " + mCurrentFocusLocation.toString() + " scale: "  + mScaleFactor);

        if (mDisplay != null) {

            switch (mDrawingState) {

                case DRAW_SATE_panning:
                    canvas.drawBitmap(mDisplay.getDisplay(
                            -mCurrentMouseLocation.x, -mCurrentMouseLocation.y, true), 0, 0, null);
                    break;

                case DRAW_SATE_zooming:
                    canvas.drawBitmap(
                            mDisplay.getDisplay(-mCurrentFocusLocation.x, -mCurrentFocusLocation.y,
                                    (float) mScaleFactor), 0, 0, null);
                    break;

                case DRAW_SATE_drawing_noclearbk:
                    canvas.drawBitmap(mDisplay.getDisplay(false), 0, 0, null);
                    break;

                case DRAW_SATE_drawing:
                    canvas.drawBitmap(mDisplay.getDisplay(true), 0, 0, null);
                    break;

                default: // mDrawingState == DRAW_SATE_none
                    if (mDrawingState == DRAW_SATE_double_tap) {
                        mDrawingState = DRAW_SATE_drawing;
                    }
                    break;
            }

        } else {
            super.onDraw(canvas);
        }
    }

    @Override
    protected synchronized void runDrawThread() {
        cancelDrawThread();
        if(mDrawingState != DRAW_SATE_drawing_noclearbk) {
            mDrawingState = DRAW_SATE_drawing;
            mDisplay.clearLayer();
        }

        mStartDrawTime = System.currentTimeMillis();
        for(Layer layer : mLayers) {
            if (layer.isVisible()) {
                mDrawThreadPool.execute(layer);
            }
        }
    }

    protected void zoomStart(ScaleGestureDetector scaleGestureDetector){
        if(mDrawingState == DRAW_SATE_zooming)
            return;

        mHandler.removeMessages(MSGTYPE_ZOOMING_DONE);
        mDrawingState = DRAW_SATE_zooming;
        mCurrentSpan = scaleGestureDetector.getCurrentSpan();
        mCurrentFocusLocation.set(-scaleGestureDetector.getFocusX(), -scaleGestureDetector.getFocusY());
        mScaleFactor = 1.f;
    }

    protected void zoom(ScaleGestureDetector scaleGestureDetector){
        if(mDrawingState == DRAW_SATE_zooming) {
            double scaleFactor = scaleGestureDetector.getCurrentSpan() / mCurrentSpan;
            double zoom = getZoomForScaleFactor(scaleFactor);
            if(zoom < mDisplay.getMinZoomLevel() || zoom > mDisplay.getMaxZoomLevel())
                return;

            mScaleFactor = scaleFactor;
            invalidate();
        }
    }

    protected double getZoomForScaleFactor(double scale){
        double zoom = mDisplay.getZoomLevel();

        if(scale > 1){
            zoom = mDisplay.getZoomLevel() + lg(scale);
        }
        else if(scale < 1){
            zoom = mDisplay.getZoomLevel() - lg( 1 / scale);
        }
        return zoom;
    }

    protected void zoomStop(){
        if(mDrawingState == DRAW_SATE_zooming ) {
            mDrawingState = DRAW_SATE_drawing_noclearbk;

            double zoom = getZoomForScaleFactor(mScaleFactor);

            GeoEnvelope env = mDisplay.getScreenBounds();
            GeoPoint focusPt = new GeoPoint(-mCurrentFocusLocation.x, -mCurrentFocusLocation.y);

            double invertScale = 1 / mScaleFactor;

            double offX = (1 - invertScale) * focusPt.getX();
            double offY = (1 - invertScale) * focusPt.getY();
            env.scale(invertScale);
            env.offset(offX, offY);

            GeoPoint newCenterPt = env.getCenter();
            GeoPoint newCenterPtMap = mDisplay.screenToMap(newCenterPt);

            mDisplay.clearLayer();
            mDisplay.setZoomAndCenter(zoom, newCenterPtMap);

            Bundle bundle = new Bundle();
            bundle.putBoolean(BUNDLE_HASERROR_KEY, false);
            bundle.putInt(BUNDLE_TYPE_KEY, MSGTYPE_ZOOMING_DONE);
            bundle.putInt(BUNDLE_DRAWSTATE_KEY, DRAW_SATE_drawing);

            Message msg = new Message();
            msg.setData(bundle);
            msg.what = MSGTYPE_ZOOMING_DONE;
            mHandler.sendMessageDelayed(msg, DISPLAY_REDRAW_TIMEOUT);
        }
    }

    protected void panStart(final MotionEvent e){
        if (mDrawingState == DRAW_SATE_zooming
                || mDrawingState == DRAW_SATE_panning)
            return;

        if(mDrawingState == DRAW_SATE_drawing || mDrawingState == DRAW_SATE_drawing_noclearbk) {
            mHandler.removeMessages(MSGTYPE_PANNING_DONE);
            mStartMouseLocation.set(e.getX(), e.getY());
            mCurrentMouseLocation.set(e.getX(), e.getY());
            cancelDrawThread();
            mDrawingState = DRAW_SATE_panning;
        }
    }

    protected void panMoveTo(final MotionEvent e){
        //Log.d(TAG, "panMoveTo" + e.toString());
        if(mDrawingState == DRAW_SATE_panning){
            float x =  mStartMouseLocation.x - e.getX();
            float y =  mStartMouseLocation.y - e.getY();

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

    protected void panStop(){
        if(mDrawingState == DRAW_SATE_panning ) {

            mDrawingState = DRAW_SATE_drawing_noclearbk;

            float x = mCurrentMouseLocation.x;
            float y = mCurrentMouseLocation.y;


            GeoEnvelope bounds = mDisplay.getScreenBounds();
            bounds.offset(x, y);
            GeoEnvelope mapBounds = mDisplay.screenToMap(bounds);

            GeoPoint pt = mapBounds.getCenter();
            //GeoPoint screenPt = mDisplay.mapToScreen(new GeoPoint(mapBounds.getMinX(), mapBounds.getMinY()));
            //Log.d(TAG, "panStop. x: " + x + ", y:" + y + ", sx:" + screenPt.getX() + ", sy:" + screenPt.getY());
            //mDisplay.panStop((float) screenPt.getX(), (float) screenPt.getY());

            mDisplay.clearLayer();
            mDisplay.setZoomAndCenter(mDisplay.getZoomLevel(), pt);

            Bundle bundle = new Bundle();
            bundle.putBoolean(BUNDLE_HASERROR_KEY, false);
            bundle.putInt(BUNDLE_TYPE_KEY, MSGTYPE_PANNING_DONE);
            bundle.putInt(BUNDLE_DRAWSTATE_KEY, DRAW_SATE_drawing_noclearbk);

            Message msg = new Message();
            msg.setData(bundle);
            msg.what = MSGTYPE_PANNING_DONE;
            mHandler.sendMessageDelayed(msg, DISPLAY_REDRAW_TIMEOUT);
        }
    }

    @Override
    protected void onLayerDrawFinished(float percent){
        if(mDrawingState == DRAW_SATE_panning )
            return;
        super.onLayerDrawFinished(percent);
    }

    @Override
    protected void processMessage(Bundle bundle) {
        switch (bundle.getInt(BUNDLE_TYPE_KEY)) {
            case MSGTYPE_PANNING_DONE:
            case MSGTYPE_ZOOMING_DONE:
                mDrawingState = bundle.getInt(BUNDLE_DRAWSTATE_KEY); // TODO: remove it
                onExtentChanged((int) mDisplay.getZoomLevel(), mDisplay.getCenter());
                break;

            case MSGTYPE_LAYER_ADDED: //the new layer was create and need to be added on map
                File path = (File) bundle.getSerializable(BUNDLE_PATH_KEY);
                addLayer(path);
                saveMap();
                break;

            default:
                super.processMessage(bundle);
        }
    }

    // delegate the event to the gesture detector
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);

        if (!mGestureDetector.onTouchEvent(event)) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    break;

                case MotionEvent.ACTION_MOVE:
                    break;

                case MotionEvent.ACTION_UP:
                    panStop();
                    break;

                default:
                    break;
            }
        }

        return true;
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
    public void onLongPress(MotionEvent event) {

    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
//        Log.d(TAG, "onScroll: " + event1.toString() + ", " + event2.toString() + ", "
//                + distanceX + ", " + distanceY);

        panStart(event1);
        panMoveTo(event2);
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
//        Log.d(TAG, "onSingleTapUp: " + e.toString());
        return false;
    }

    @Override
    public boolean onDoubleTap(final MotionEvent e) {
        GeoPoint newCenter = new GeoPoint(e.getX(), e.getY());
        mDrawingState = DRAW_SATE_double_tap;
        final GeoPoint pt = mDisplay.screenToMap(newCenter);
        if(mDisplay.getBounds().contains(pt)){
            Log.d(TAG, "onDoubleTap " + newCenter + " geo: " + pt);
            setZoomAndCenter((float)Math.ceil(getZoomLevel() + 0.5), pt);
            return true;
        }
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
        mDrawingState = DRAW_SATE_drawing;
        setZoomAndCenter((float)Math.ceil(getZoomLevel() + 0.5), getMapCenter());
    }

    public void zoomOut() {
        mDrawingState = DRAW_SATE_drawing;
        setZoomAndCenter((float)Math.floor(getZoomLevel() - 0.5), getMapCenter());
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
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        zoomStart(scaleGestureDetector);
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        zoomStop();
    }

    public static double lg(double x) {
        return Math.log(x)/Math.log(2.0);
    }

    /**
     * Send layer added event to all listeners
     *
     * @param layer A new layer
     */
    @Override
    protected void onLayerAdded(Layer layer) {
        mDrawingState = DRAW_SATE_drawing;
        super.onLayerAdded(layer);
    }

    /**
     * Send layer changed event to all listeners
     *
     * @param layer A changed layer
     */
    @Override
    protected void onLayerChanged(Layer layer) {
        mDrawingState = DRAW_SATE_drawing;
        super.onLayerChanged(layer);
    }

    /**
     * Send layer delete event to all listeners
     *
     * @param id A deleted layer identificator
     */
    @Override
    protected void onLayerDeleted(int id) {
        mDrawingState = DRAW_SATE_drawing;
        super.onLayerDeleted(id);
    }
}
