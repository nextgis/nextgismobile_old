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

import android.graphics.Canvas;
import android.graphics.Paint;
import com.nextgis.mobile.datasource.GeoGeometry;
import com.nextgis.mobile.datasource.GeoPoint;

import static com.nextgis.mobile.util.DisplayConstants.*;

public class EditMarkerStyle extends SimpleMarkerStyle {

    public EditMarkerStyle(int fillColor, int outColor, float size, int type) {
        super(fillColor, outColor, size, type);
    }

    @Override
    public void onDraw(GeoGeometry geoGeometry, GISDisplay display) {
        GeoPoint geoPoint = (GeoPoint) geoGeometry;
        GeoPoint pt = display.mapToScreen(geoPoint);

        switch (mType){
            case MarkerEditStyleCircle:
                Canvas editCanvas = display.getEditCanvas();

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
}
