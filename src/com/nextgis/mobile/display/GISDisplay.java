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
    protected Bitmap mMainBitmap;
    protected Context mContext;
    protected GeoEnvelope mFullBounds;
    protected GeoEnvelope mCurrentBounds;
    protected GeoPoint mCenter;
    protected Matrix mTransformMatrix;
    protected final Matrix mDefaultMatrix;
    protected final int mTileSize = 256;
    protected int mMinZoomLevel;
    protected int mMaxZoomLevel;
    protected int mZoomLevel;
    protected float mScale;
    protected float mInverceScale;
    protected final float mHalfWidth;
    protected final float mHaldHeight;

    public GISDisplay(Context context) {
        this.mContext = context;
        Display disp = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int width = disp.getWidth();
        int height = disp.getHeight();

        mHalfWidth = (float) (width / 2.0);
        mHaldHeight = (float) (height / 2.0);

        //calc min zoom
        mMinZoomLevel = Math.min(width, height) / mTileSize;
        //set max zoom
        mMaxZoomLevel = 25;

        //default extent
        double val = 20037508.34;
        mFullBounds = new GeoEnvelope(-val, val, -val, val); //set full Mercator bounds

        //default transform matrix
        mTransformMatrix = new Matrix();

        //default zoom and center
        setZoomAndCenter(mMinZoomLevel, new GeoPoint());

        mMainBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mMainCanvas = new Canvas(mMainBitmap);
        mDefaultMatrix = mMainCanvas.getMatrix();
        mMainCanvas.setMatrix(mTransformMatrix);
    }

    public void setZoomAndCenter(final int  zoom, final GeoPoint center){
        if(zoom > mMaxZoomLevel || zoom < mMinZoomLevel)
            return;
        mZoomLevel = zoom;
        mCenter = center;

        double mapTileSize = Math.pow(2, zoom);
        double mapPixelSize = mapTileSize * mTileSize;

        double scaleX = mapPixelSize / mFullBounds.width();
        double scaleY = mapPixelSize / mFullBounds.height();

        mScale = (float) ((scaleX + scaleY) / 2.0);
        mInverceScale = 1 / mScale;

        //default transform matrix
        mTransformMatrix.reset();
        mTransformMatrix.postTranslate((float)center.getX(), (float)center.getY());
        mTransformMatrix.postScale(mScale, -mScale);
        mTransformMatrix.postTranslate(mHalfWidth, mHaldHeight);

        RectF rect = new RectF(0, 0, mMainBitmap.getWidth(), mMainBitmap.getHeight());
        mTransformMatrix.mapRect(rect);
        mCurrentBounds = new GeoEnvelope(rect.left, rect.right, rect.bottom, rect.top);
    }

    public Bitmap getMainBitmap() {
        //Test
        clearBackground();
        return mMainBitmap;
    }

    protected void clearBackground() {
        mMainCanvas.setMatrix(mDefaultMatrix);
        Bitmap bkBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.bk_tile);
        for (int i = 0; i < mMainBitmap.getWidth(); i += bkBitmap.getWidth()) {
            for (int j = 0; j < mMainBitmap.getHeight(); j += bkBitmap.getHeight()) {
                mMainCanvas.drawBitmap(bkBitmap, i, j, null);
            }
        }
        mMainCanvas.setMatrix(mTransformMatrix);
    }

    public void drawBitmap(final Bitmap bitmap, final GeoPoint pt){
        Matrix matrix = new Matrix();

        matrix.postScale(mInverceScale, -mInverceScale);
        matrix.postTranslate((float)pt.getX(), (float)pt.getY());

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
        //mMainCanvas.drawBitmap(testBitmap, 100, 100, null);

        drawBitmap(testBitmap, new GeoPoint(2000000, -2000000));

        Paint pt = new Paint();
        pt.setColor(Color.RED);
        pt.setStrokeWidth(25 / mScale);
        pt.setStrokeCap(Paint.Cap.ROUND);
        drawGeometry(new GeoPoint(2000000, 2000000), pt);
    }

    public final int getZoomLevel() {
        return mZoomLevel;
    }

    public final GeoEnvelope getBounds(){
        return mCurrentBounds;
    }
}
