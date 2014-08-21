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
    protected double mMinZoomLevel;
    protected double mMaxZoomLevel;
    protected double mZoomLevel;
    protected double mScale;
    protected double mInvertScale;
    protected float mMainBitmapOffsetX;
    protected float mMainBitmapOffsetY;
    protected GeoEnvelope mLimits;
    protected GeoEnvelope mScreenBounds;
    protected final Paint mRasterPaint;

    //TODO: create list of caches
    //mark each cache as done and merge it with previous if it done

    public GISDisplay(Context context) {
        mContext = context;
        //set max zoom
        mMaxZoomLevel = 25;

        //default extent
        double val = 20037508.34;
        mFullBounds = new GeoEnvelope(-val, val, -val, val); //set full Mercator bounds

        //default transform matrix
        mTransformMatrix = new Matrix();
        mInvertTransformMatrix = new Matrix();
        mMapTileSize = new GeoPoint();
        mCenter = new GeoPoint();

        setSize(100, 100);

        mRasterPaint = new Paint();
        mRasterPaint.setAntiAlias(true);
        mRasterPaint.setFilterBitmap(true);
        mRasterPaint.setDither(true);
    }

    public void setSize(int w, int h){
        Log.d(TAG, "new size: " + w + " x " + h);

        mScreenBounds = new GeoEnvelope(0, w, 0, h);

        //calc min zoom
        mMinZoomLevel = Math.min(w, h) / mTileSize;

        mBackgroundBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mBackgroundCanvas = new Canvas(mBackgroundBitmap);

        mMainBitmap = Bitmap.createBitmap((int)(w * OFFSCREEN_EXTRASIZE_RATIO), (int)(h * OFFSCREEN_EXTRASIZE_RATIO), Bitmap.Config.ARGB_8888);
        mMainCanvas = new Canvas(mMainBitmap);

        mMainBitmapOffsetX = (mMainBitmap.getWidth() - mBackgroundBitmap.getWidth()) / 2;
        mMainBitmapOffsetY = (mMainBitmap.getHeight() - mBackgroundBitmap.getHeight()) / 2;

        if(mZoomLevel < mMinZoomLevel)
            mZoomLevel = mMinZoomLevel;
        //default zoom and center
        setZoomAndCenter(mZoomLevel, mCenter);
    }

    public void clearLayer(int layerId){
        mMainBitmap.eraseColor(Color.TRANSPARENT);
    }

    public void setZoomAndCenter(final double  zoom, final GeoPoint center){
        if(zoom > mMaxZoomLevel || zoom < mMinZoomLevel)
            return;
        mZoomLevel = zoom;
        mCenter = center;

        int nZoom = (int) Math.floor(zoom);
        Log.d(TAG, "Zoom: " + zoom + ", Center: " + center.toString());

        double mapTileSize = 1 << nZoom;
        mapTileSize *= 1 + zoom - nZoom;
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
        mTransformMatrix.postTranslate((float)mBackgroundBitmap.getWidth() / 2, (float)mBackgroundBitmap.getHeight() / 2);

        mInvertTransformMatrix.reset();
        mTransformMatrix.invert(mInvertTransformMatrix);

        Matrix matrix = new Matrix();
        matrix.postTranslate((float)-center.getX(), (float)-center.getY());
        matrix.postScale((float)mScale, (float)-mScale);
        matrix.postTranslate((float)mMainBitmap.getWidth() / 2, (float)mMainBitmap.getHeight() / 2);
        mMainCanvas.setMatrix(matrix);



        RectF rect = new RectF(-mMainBitmapOffsetX, mBackgroundBitmap.getHeight() + mMainBitmapOffsetY, mBackgroundBitmap.getWidth() + mMainBitmapOffsetX, -mMainBitmapOffsetY);
        mInvertTransformMatrix.mapRect(rect);

//        mCurrentBounds = new GeoEnvelope(rect.left, rect.right, rect.bottom, rect.top);
        mCurrentBounds = new GeoEnvelope(Math.min(rect.left, rect.right), Math.max(rect.left, rect.right), Math.min(rect.bottom, rect.top), Math.max(rect.bottom, rect.top));
        Log.d(TAG, "current: " + mCurrentBounds.toString());

        mLimits = mapToScreen(mFullBounds);
        mLimits.fix();
        mLimits.setMinX(mLimits.getMinX() - mMainBitmap.getWidth());
        mLimits.setMaxX(mLimits.getMaxX() + mMainBitmap.getWidth());
    }

    public final GeoEnvelope getLimits(){
        return new GeoEnvelope(mLimits);
    }

    public final GeoEnvelope getScreenBounds(){
        return new GeoEnvelope(mScreenBounds);
    }

    public synchronized Bitmap getDisplay(boolean clearBackground) {
        return getDisplay(0, 0, clearBackground);
    }

    public synchronized Bitmap getDisplay(float x, float y, boolean clearBackground) {
        if(clearBackground)
            clearBackground();
        synchronized (mMainBitmap) {
            mBackgroundCanvas.drawBitmap(mMainBitmap, x - mMainBitmapOffsetX, y - mMainBitmapOffsetY, null);
        }
        return mBackgroundBitmap;
    }

    public synchronized Bitmap getDisplay(float x, float y, float scale) {
        clearBackground();

        GeoPoint pt = getScaledOffset(x, y, scale);

        float mainBitmapOffsetX = (float) pt.getX();
        float mainBitmapOffsetY = (float) pt.getY();

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        matrix.postTranslate(-mainBitmapOffsetX, -mainBitmapOffsetY);
        //Log.d(TAG, "matix: " + matrix.toShortString());

        synchronized (mMainBitmap) {
            mBackgroundCanvas.drawBitmap(mMainBitmap, matrix, mRasterPaint);
        }
        return mBackgroundBitmap;
    }

    public GeoPoint getScaledOffset(float x, float y, float scale){
        float dxOld = x - mBackgroundBitmap.getWidth() / 2;
        float dyOld = y - mBackgroundBitmap.getHeight() / 2;

        float scaledWidth = mMainBitmap.getWidth() * scale;
        float scaledHeight = mMainBitmap.getHeight() * scale;

        GeoPoint ret = new GeoPoint();
        ret.setX((scaledWidth - mBackgroundBitmap.getWidth()) / 2 - (1 - scale) * dxOld);
        ret.setY( (scaledHeight - mBackgroundBitmap.getHeight()) / 2 - (1 - scale) * dyOld);
        return ret;
    }

    public synchronized void clearBackground() {
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

        float scale = (float) (1 + mZoomLevel - Math.floor(mZoomLevel));

        if(bitmap.getWidth() != mTileSize) {
            scale = (float)mTileSize / bitmap.getWidth() * scale;
        }
        Matrix matrix1 = new Matrix();
        matrix1.postScale(scale, scale);
        matrix.preConcat(matrix1);


        synchronized (mMainBitmap) {
            mMainCanvas.drawBitmap(bitmap, matrix, mRasterPaint);
        }
    }

    public void drawGeometry(final GeoGeometry geom, final Paint paint){
        synchronized (mMainBitmap) {
            switch (geom.getType()) {
                case GEOMTYPE_Point:
                    drawPoint((GeoPoint) geom, paint);
                    return;
            }
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

    public final double getZoomLevel() {
        return mZoomLevel;
    }

    public final double getScale() {
        return mScale;
    }

    public final GeoEnvelope getBounds(){
        return new GeoEnvelope(mCurrentBounds);
    }

    public final GeoEnvelope getFullBounds(){
        return new GeoEnvelope(mFullBounds);
    }

    public GeoPoint getTileSize(){
        return new GeoPoint(mMapTileSize);

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

    public double getMinZoomLevel() {
        return mMinZoomLevel;
    }

    public double getMaxZoomLevel() {
        return mMaxZoomLevel;
    }

    public GeoPoint getCenter() {
        return new GeoPoint(mCenter);
    }
}
