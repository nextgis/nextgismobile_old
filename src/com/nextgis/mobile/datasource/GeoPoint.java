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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.nextgis.mobile.util.GeoConstants.*;

public class GeoPoint extends GeoGeometry{

    protected GeoRawPoint mPoint;

    public GeoPoint(){
        mPoint = new GeoRawPoint();
    }

    public GeoPoint(double x, double y){
        mPoint = new GeoRawPoint(x, y);
    }

    public GeoPoint(final GeoPoint pt){
        mPoint = new GeoRawPoint(pt.getX(), pt.getY());
    }

    public GeoRawPoint getRawPoint() {
        return mPoint;
    }

    public final double getX(){
        return mPoint.mX;
    }

    public final double getY(){
        return mPoint.mY;
    }

    public void setX(double x){
        mPoint.mX = x;
    }

    public void setY(double y){
        mPoint.mY = y;
    }

    public void setCoordinates(double x, double y){
        mPoint.mX = x;
        mPoint.mY = y;
    }

    @Override
    public final int getType(){
        return GTPoint;
    }

    @Override
    public boolean project(int crs) {
        return (mCRS == CRS_WGS84 && crs == CRS_WEB_MERCATOR
                || mCRS == CRS_WEB_MERCATOR && crs == CRS_WGS84)
                && mPoint.project(crs);
    }

    @Override
    public GeoEnvelope getEnvelope() {
        return new GeoEnvelope(mPoint.mX - .5, mPoint.mX + .5, mPoint.mY - .5, mPoint.mY + .5);
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject oJSONOut = new JSONObject();
        oJSONOut.put(GEOJSON_TYPE, GEOJSON_TYPE_Point);
        JSONArray coordinates = new JSONArray();
        oJSONOut.put(GEOJSON_COORDINATES, coordinates);
        coordinates.put(mPoint.mX);
        coordinates.put(mPoint.mY);
        return oJSONOut;
    }

    public String toString(){
        return "X: " + mPoint.mX + ", Y: " + mPoint.mY;
    }
}
