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

public class Geo {

    protected final static double mEarthMajorRadius = 6378137.0;
    protected final static double mEarthMinorRadius = 6356752.3142;
    protected final static double mE = mEarthMinorRadius / mEarthMajorRadius;
    protected final static double mES = 1.0 - (mE * mE);
    protected final static double mEccent = Math.sqrt(mES);
    protected final static double mCom = 0.5 * mEccent;

    public GeoPoint wgs84ToSphericalMercator(final GeoPoint pt){
        GeoPoint retPt = new GeoPoint();
        retPt.setX(Math.toDegrees(pt.getX() / mEarthMajorRadius));
        retPt.setY(Math.toDegrees(2 * Math.atan(Math.exp(pt.getY() / mEarthMajorRadius)) - Math.PI / 2));
        return retPt;
    }

    public GeoPoint wgs84TotoEllipticalMercator(final GeoPoint pt){
        GeoPoint retPt = new GeoPoint();
        retPt.setX(Math.toDegrees(pt.getX() / mEarthMajorRadius));

        double phi = Math.toRadians(pt.getY());
        double sinphi = Math.sin(phi);
        double con = mEccent * sinphi;
        con = Math.pow(((1.0 - con) / (1.0 + con)), mCom);
        double ts = Math.tan(0.5 * ((Math.PI * 0.5) - phi)) / con;
        retPt.setY(0 - mEarthMajorRadius * Math.log(ts));
        return retPt;
    }

    public GeoPoint mercatorToSphericalWGS(final GeoPoint pt){
        GeoPoint retPt = new GeoPoint();
        retPt.setX(mEarthMajorRadius * Math.toRadians(pt.getX()));
        retPt.setY(mEarthMajorRadius * Math.log(Math.tan(Math.PI / 4 + Math.toRadians(pt.getY()) / 2)));
        return retPt;
    }
}
