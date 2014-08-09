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

package com.nextgis.mobile.display;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.WindowManager;
import android.view.Display;

import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.GeoEnvelope;
import com.nextgis.mobile.datasource.GeoGeometry;
import com.nextgis.mobile.datasource.GeoPoint;
import static com.nextgis.mobile.util.Constants.*;

public class GISDisplay {
    protected Canvas mMainCanvas;
    protected Canvas mBackgroundCanvas;
    protected Bitmap mMainBitmap;
    protected Bitmap mBackgroundBitmap;
    protected Context mContext;
    protected GeoEnvelope mFullBounds;
    protected GeoEnvelope mCurrentBounds;
    protected GeoPoint mCenter;
    protected GeoPoint mMapTileSize;
    protected Matrix mTransformMatrix;
    protected Matrix mInvertTransformMatrix;
    protected final int mTileSize = 256;
    protected int mMinZoomLevel;
    protected int mMaxZoomLevel;
    protected int mZoomLevel;
    protected double mScale;
    protected double mInvertScale;
    protected final double mHalfWidth;
    protected final double mHalfHeight;
    protected GeoEnvelope mLimits;
    protected final GeoEnvelope mScreenBounds;

    //TODO: create list of caches
    //mark each cache as done and merge it with previous if it done

    public GISDisplay(Context context) {
        this.mContext = context;
        Display disp = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int width = disp.getWidth();
        int height = disp.getHeight();

        mHalfWidth = width / 2.0;
        mHalfHeight = height / 2.0;

        //calc min zoom
        mMinZoomLevel = Math.min(width, height) / mTileSize;
        //set max zoom
        mMaxZoomLevel = 25;

        //default extent
        double val = 20037508.34;
        mFullBounds = new GeoEnvelope(-val, val, -val, val); //set full Mercator bounds
        mScreenBounds = new GeoEnvelope(0, width, 0, height);

        //default transform matrix
        mTransformMatrix = new Matrix();
        mInvertTransformMatrix = new Matrix();
        mMapTileSize = new GeoPoint();

        mBackgroundBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mBackgroundCanvas = new Canvas(mBackgroundBitmap);

        mMainBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mMainCanvas = new Canvas(mMainBitmap);

        //default zoom and center
        setZoomAndCenter(mMinZoomLevel, new GeoPoint());
    }

    public void clearLayer(int layerId){
        mMainBitmap.eraseColor(Color.TRANSPARENT);
    }

    public void setZoomAndCenter(final int  zoom, final GeoPoint center){
        if(zoom > mMaxZoomLevel || zoom < mMinZoomLevel)
            return;
        mZoomLevel = zoom;
        mCenter = center;
        Log.d(TAG, "Zoom: " + zoom + ", Center: " + center.toString());

        double mapTileSize = 1 << zoom;
        double mapPixelSize = mapTileSize * mTileSize;

        mMapTileSize.setCoordinates(mFullBounds.width() / mapTileSize, mFullBounds.height() / mapTileSize);

        double scaleX = mapPixelSize / mFullBounds.width();
        double scaleY = mapPixelSize / mFullBounds.height();

        mScale = (float) ((scaleX + scaleY) / 2.0);
        mInvertScale = 1 / mScale;

        //default transform matrix
        mTransformMatrix.reset();
        mTransformMatrix.postTranslate((float)-center.getX(), (float)-center.getY());
        mTransformMatrix.postScale((float)mScale, (float)-mScale);
        mTransformMatrix.postTranslate((float)mHalfWidth, (float)mHalfHeight);

        mInvertTransformMatrix.reset();
        mTransformMatrix.invert(mInvertTransformMatrix);

        mMainCanvas.setMatrix(mTransformMatrix);

        RectF rect = new RectF(0, 0, mMainBitmap.getWidth(), mMainBitmap.getHeight());
        mInvertTransformMatrix.mapRect(rect);

//        mCurrentBounds = new GeoEnvelope(rect.left, rect.right, rect.bottom, rect.top);
        mCurrentBounds = new GeoEnvelope(Math.min(rect.left, rect.right), Math.max(rect.left, rect.right), Math.min(rect.bottom, rect.top), Math.max(rect.bottom, rect.top));
        Log.d(TAG, "full: " + mFullBounds.toString());
        Log.d(TAG, "current: " + mCurrentBounds.toString());

        mLimits = mapToScreen(mFullBounds);
        mLimits.fix();
        mLimits.setMinX(mLimits.getMinX() - mHalfWidth);
        mLimits.setMaxX(mLimits.getMaxX() + mHalfWidth);
    }

    public final GeoEnvelope getLimits(){
        return mLimits;
    }

    public final GeoEnvelope getScreenBounds(){
        return mScreenBounds;
    }

    public Bitmap getDisplay() {
        return getDisplay(0, 0);
    }

    public Bitmap getDisplay(float x, float y) {
        clearBackground();
        mBackgroundCanvas.drawBitmap(mMainBitmap, x, y, null);
        return mBackgroundBitmap;
    }

    public void clearBackground() {
        final Bitmap bkBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.bk_tile);
        for (int i = 0; i < mBackgroundBitmap.getWidth(); i += bkBitmap.getWidth()) {
            for (int j = 0; j < mBackgroundBitmap.getHeight(); j += bkBitmap.getHeight()) {
                mBackgroundCanvas.drawBitmap(bkBitmap, i, j, null);
            }
        }
    }

    public void drawTile(final Bitmap bitmap, final GeoPoint pt){
        Matrix matrix = new Matrix();

        matrix.postScale((float)mInvertScale, (float)-mInvertScale);
        matrix.postTranslate((float)pt.getX(), (float)pt.getY());
        if(bitmap.getWidth() != mTileSize) {
            Matrix matrix1 = new Matrix();
            float scale = (float)mTileSize / bitmap.getWidth();
            matrix1.postScale(scale, scale);
            matrix.preConcat(matrix1);
        }

        mMainCanvas.drawBitmap(bitmap, matrix, null);
    }

    public void drawGeometry(final GeoGeometry geom, final Paint paint){
        switch (geom.getType()){
            case GEOMTYPE_Point:
                drawPoint((GeoPoint) geom, paint);
                return;
        }
    }

    protected void drawPoint(final GeoPoint pt, final Paint paint){
        mMainCanvas.drawPoint((float)pt.getX(), (float)pt.getY(), paint);
    }

    protected void drawTest(){
        Bitmap testBitmap = Bitmap.createBitmap(mTileSize, mTileSize, Bitmap.Config.ARGB_8888);
        testBitmap.eraseColor(Color.RED);
        //mMainCanvas.drawTile(testBitmap, 100, 100, null);

        drawTile(testBitmap, new GeoPoint(2000000, -2000000));

        Paint pt = new Paint();
        pt.setColor(Color.RED);
        pt.setStrokeWidth((float)(25 / mScale));
        pt.setStrokeCap(Paint.Cap.ROUND);
        drawGeometry(new GeoPoint(2000000, 2000000), pt);
    }

    public final int getZoomLevel() {
        return mZoomLevel;
    }

    public final GeoEnvelope getBounds(){
        return mCurrentBounds;
    }

    public final GeoEnvelope getFullBounds(){
        return mFullBounds;
    }

    public GeoPoint getTileSize(){
        return mMapTileSize;

        //RectF rect = new RectF(0, 0, mTileSize - 1, mTileSize - 1);
        //mInvertTransformMatrix.mapRect(rect);
        //return new double[] {rect.width(), rect.height()};
    }

    public GeoPoint screenToMap(final GeoPoint pt){
        float points[] = new float[2];
        points[0] = (float) pt.getX();
        points[1] = (float) pt.getY();
        mInvertTransformMatrix.mapPoints(points);

        return new GeoPoint(points[0], points[1]);
    }

    public GeoPoint mapToScreen(final GeoPoint pt){
        float points[] = new float[2];
        points[0] = (float) pt.getX();
        points[1] = (float) pt.getY();
        mTransformMatrix.mapPoints(points);

        return new GeoPoint(points[0], points[1]);
    }

    public GeoEnvelope mapToScreen(final GeoEnvelope env){
        GeoEnvelope outEnv = new GeoEnvelope();
        RectF rect = new RectF();
        rect.set((float) env.getMinX(), (float) env.getMaxY(), (float) env.getMaxX(), (float) env.getMinY());

        mTransformMatrix.mapRect(rect);
        outEnv.setMin(rect.left, rect.bottom);
        outEnv.setMax(rect.right, rect.top);

        return outEnv;
    }

    public GeoEnvelope screenToMap(final GeoEnvelope env){
        GeoEnvelope outEnv = new GeoEnvelope();
        RectF rect = new RectF();
        rect.set((float) env.getMinX(), (float) env.getMaxY(), (float) env.getMaxX(), (float) env.getMinY());

        mInvertTransformMatrix.mapRect(rect);
        outEnv.setMin(rect.left, rect.bottom);
        outEnv.setMax(rect.right, rect.top);

        return outEnv;
    }

    public int getMinZoomLevel() {
        return mMinZoomLevel;
    }

    public int getMaxZoomLevel() {
        return mMaxZoomLevel;
    }

    public GeoPoint getCenter() {
        return mCenter;
    }
}
