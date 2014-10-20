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

    public static GeoPoint mercatorToWgs84SphereRet(final GeoPoint pt) {
        GeoPoint retPt = new GeoPoint();
        retPt.setX(mercatorToWgs84SphereX(pt.getX()));
        retPt.setY(mercatorToWgs84SphereY(pt.getY()));
        return retPt;
    }

    public static void mercatorToWgs84Sphere(GeoPoint pt) {
        pt.setX(mercatorToWgs84SphereX(pt.getX()));
        pt.setY(mercatorToWgs84SphereY(pt.getY()));
    }

    public static void mercatorToWgs84Sphere(GeoRawPoint pt) {
        pt.x = mercatorToWgs84SphereX(pt.x);
        pt.y = mercatorToWgs84SphereY(pt.y);
    }

    public static double mercatorToWgs84SphereX(final double x) {
        return Math.toDegrees(x / mEarthMajorRadius);
    }

    public static double mercatorToWgs84SphereY(final double y) {
        return Math.toDegrees(2 * Math.atan(Math.exp(y / mEarthMajorRadius)) - Math.PI / 2);
    }

    public static GeoPoint mercatorToWgs84EllipseRet(final GeoPoint pt) {
        GeoPoint retPt = new GeoPoint();
        retPt.setX(mercatorToWgs84EllipseX(pt.getX()));
        retPt.setY(mercatorToWgs84EllipseY(pt.getY()));
        return retPt;
    }

    public static void mercatorToWgs84Ellipse(GeoPoint pt) {
        pt.setX(mercatorToWgs84EllipseX(pt.getX()));
        pt.setY(mercatorToWgs84EllipseY(pt.getY()));
    }

    public static void mercatorToWgs84Ellipse(GeoRawPoint pt) {
        pt.x = mercatorToWgs84EllipseX(pt.x);
        pt.y = mercatorToWgs84EllipseY(pt.y);
    }

    public static double mercatorToWgs84EllipseX(final double x) {
        return Math.toDegrees(x / mEarthMajorRadius);
    }

    public static double mercatorToWgs84EllipseY(final double y) {
        double phi = Math.toRadians(y);
        double sinphi = Math.sin(phi);
        double con = mEccent * sinphi;
        con = Math.pow(((1.0 - con) / (1.0 + con)), mCom);
        double ts = Math.tan(0.5 * ((Math.PI * 0.5) - phi)) / con;
        return 0 - mEarthMajorRadius * Math.log(ts);
    }

    public static GeoPoint wgs84ToMercatorSphereRet(final GeoPoint pt) {
        GeoPoint retPt = new GeoPoint();
        retPt.setX(wgs84ToMercatorSphereX(pt.getX()));
        retPt.setY(wgs84ToMercatorSphereY(pt.getY()));
        return retPt;
    }

    public static void wgs84ToMercatorSphere(GeoPoint pt) {
        pt.setX(wgs84ToMercatorSphereX(pt.getX()));
        pt.setY(wgs84ToMercatorSphereY(pt.getY()));
    }

    public static void wgs84ToMercatorSphere(GeoRawPoint pt) {
        pt.x = wgs84ToMercatorSphereX(pt.x);
        pt.y = wgs84ToMercatorSphereY(pt.y);
    }

    public static double wgs84ToMercatorSphereX(final double x) {
        return mEarthMajorRadius * Math.toRadians(x);
    }

    public static double wgs84ToMercatorSphereY(final double y) {
        return mEarthMajorRadius * Math.log(Math.tan(Math.PI / 4 + Math.toRadians(y) / 2));
    }

    public static boolean isGeometryTypeSame(final int type1, final int type2) {
        return type1 == type2 || Math.abs(type1 - type2) == 3;
    }
}
