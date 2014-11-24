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
import android.graphics.Paint;
import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.GeoGeometry;
import com.nextgis.mobile.datasource.GeoMultiPoint;
import com.nextgis.mobile.datasource.GeoPoint;
import com.nextgis.mobile.util.GeoConstants;

import static com.nextgis.mobile.util.DisplayConstants.*;

public class EditMarkerStyle extends SimpleMarkerStyle {

    protected Context mContext;
    protected Bitmap mAnchor;
    protected float mAnchorRectOffsetX, mAnchorRectOffsetY;
    protected float mAnchorCenterX, mAnchorCenterY;


    public EditMarkerStyle(int fillColor, int outColor, float size, int type, Context context) {
        super(fillColor, outColor, size, type);

        mContext = context;

        mAnchor = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_action_anchor);
        mAnchorRectOffsetX = -mAnchor.getWidth() * 0.1f;
        mAnchorRectOffsetY = -mAnchor.getHeight() * 0.1f;
        mAnchorCenterX = mAnchor.getWidth() * 0.75f;
        mAnchorCenterY = mAnchor.getHeight() * 0.75f;
    }

    @Override
    public void onDraw(GeoGeometry geoGeometry, GISDisplay display) {

        GeoPoint geoPoint;

        switch (geoGeometry.getType()) {
            case GeoConstants.GTPoint:
                geoPoint = (GeoPoint) geoGeometry;
                break;
            case GeoConstants.GTMultiPoint:
                GeoMultiPoint geoMultiPoint = (GeoMultiPoint) geoGeometry;
                geoPoint = geoMultiPoint.get(0); // TODO: make for all points
                break;
            default:
                return;
        }

        GeoPoint pt = display.mapToScreen(geoPoint);

        switch (mType){
            case MarkerEditStyleCircle:
                Canvas editCanvas = display.getEditCanvas();

                float anchorX = (float) pt.getX() + mAnchorRectOffsetX;
                float anchorY = (float) pt.getY() + mAnchorRectOffsetY;
                editCanvas.drawBitmap(mAnchor, anchorX, anchorY, null);

                Paint fillCirclePaint = new Paint();
                fillCirclePaint.setColor(mColor);
                fillCirclePaint.setStrokeCap(Paint.Cap.ROUND);

                editCanvas.drawCircle((float) pt.getX(), (float) pt.getY(), mSize, fillCirclePaint);

                Paint outCirclePaint = new Paint();
                outCirclePaint.setColor(mOutColor);
                outCirclePaint.setStrokeWidth((float) (mWidth));
                outCirclePaint.setStyle(Paint.Style.STROKE);
                outCirclePaint.setAntiAlias(true);

                editCanvas.drawCircle((float) pt.getX(), (float) pt.getY(), mSize, outCirclePaint);

                break;

            default:
                break;
        }
    }

    public float getAnchorCenterX() {
        return mAnchorCenterX;
    }

    public float getAnchorCenterY() {
        return mAnchorCenterY;
    }
}
