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
package com.nextgis.mobile.datasource;

import static com.nextgis.mobile.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.mobile.util.GeoConstants.CRS_WGS84;

public class GeoRawPoint {

    public double mX;
    public double mY;

    public GeoRawPoint() {
        mX = mY = 0.0;
    }

    public GeoRawPoint(double x, double y) {
        this.mX = x;
        this.mY = y;
    }

    public GeoRawPoint(GeoRawPoint rpt) {
        this.mX = rpt.mX;
        this.mY = rpt.mY;
    }

    public GeoRawPoint(GeoPoint gpt) {
        this.mX = gpt.getX();
        this.mY = gpt.getY();
    }

    public boolean equals(GeoRawPoint grp) {
        return mX == grp.mX && mY == grp.mY;
    }

    public boolean project(int toCrs) {
        switch (toCrs) {
            case CRS_WEB_MERCATOR:
                Geo.wgs84ToMercatorSphere(this);
                return true;
            case CRS_WGS84:
                Geo.mercatorToWgs84Sphere(this);
                return true;
            default:
                return false;
        }
    }
}
