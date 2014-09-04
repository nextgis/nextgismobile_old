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
    protected double mX;
    protected double mY;

    public GeoPoint(){
        mX = mY = 0;
    }

    public GeoPoint(double x, double y){
        mX = x;
        mY = y;
    }

    public GeoPoint(final GeoPoint pt){
        mX = pt.mX;
        mY = pt.mY;
    }

    public final double getX(){
        return mX;
    }

    public final double getY(){
        return mY;
    }

    public void setX(double x){
        mX = x;
    }

    public void setY(double y){
        mY = y;
    }

    public void setCoordinates(double x, double y){
        mX = x;
        mY = y;
    }

    @Override
    public final int getType(){
        return GTPoint;
    }

    @Override
    public boolean project(int crs) {
        if(mCRS == CRS_WGS84 && crs == CRS_WEB_MERCATOR){
            Geo.wgs84ToSphericalMercator(this);
            return true;
        }
        else if(mCRS == CRS_WEB_MERCATOR && crs == CRS_WGS84){
            Geo.mercatorToSphericalWGS(this);
            return true;
        }
        return false;
    }

    @Override
    public GeoEnvelope getEnvelope() {
        return new GeoEnvelope(mX - .5, mX + .5, mY - .5, mY + .5);
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject oJSONOut = new JSONObject();
        oJSONOut.put(GEOJSON_TYPE, GEOJSON_TYPE_Point);
        JSONArray coordinates = new JSONArray();
        oJSONOut.put(GEOJSON_COORDINATES, coordinates);
        coordinates.put(mX);
        coordinates.put(mY);
        return oJSONOut;
    }

    public String toString(){
        return "X: " + mX + ", Y: " + mY;
    }
}
