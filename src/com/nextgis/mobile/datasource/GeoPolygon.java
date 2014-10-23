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

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.mobile.util.GeoConstants.*;

public class GeoPolygon extends GeoGeometry {

    protected GeoLinearRing mOuterRing;

    public GeoPolygon() {
        mOuterRing = new GeoLinearRing();
    }

    public GeoLinearRing getOuterRing() {
        return mOuterRing;
    }

    public void add(double x, double y) {
        mOuterRing.add(x, y);
    }

    public void add(GeoRawPoint rpt) {
        mOuterRing.add(rpt);
    }

    public void remove(int index) {
        mOuterRing.remove(index);
    }

    @Override
    public int getType() {
        return GTPolygon;
    }

    @Override
    public boolean project(int crs) {
        return (mCRS == CRS_WGS84 && crs == CRS_WEB_MERCATOR
                || mCRS == CRS_WEB_MERCATOR && crs == CRS_WGS84)
                && mOuterRing.project(crs);
    }

    @Override
    public GeoEnvelope getEnvelope() {
        return mOuterRing.getEnvelope();
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject jsonOutObject = new JSONObject();
        jsonOutObject.put(GEOJSON_TYPE, GEOJSON_TYPE_Polygon);
        JSONArray coordinates = new JSONArray();
        jsonOutObject.put(GEOJSON_COORDINATES, coordinates);
        JSONArray linearRingCoordinates = new JSONArray();
        coordinates.put(linearRingCoordinates);

        for (GeoRawPoint point : mOuterRing.getPoints()) {
            JSONArray pointCoordinates = new JSONArray();
            pointCoordinates.put(point.mX);
            pointCoordinates.put(point.mY);
            linearRingCoordinates.put(pointCoordinates);
        }

        return jsonOutObject;
    }
}
