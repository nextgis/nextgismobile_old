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

public class GeoLineString extends GeoGeometry {

    protected List<GeoRawPoint> mPoints;

    public GeoLineString() {
        mPoints = new ArrayList<GeoRawPoint>();
    }

    public List<GeoRawPoint> getPoints() {
        return mPoints;
    }

    public void add(double x, double y) {
        mPoints.add(new GeoRawPoint(x, y));
    }

    public void add(GeoRawPoint rpt) {
        mPoints.add(rpt);
    }

    public void remove(int index) {
        mPoints.remove(index);
    }

    @Override
    public int getType() {
        return GTLineString;
    }

    @Override
    public boolean project(int crs) {
        if (mCRS == CRS_WGS84 && crs == CRS_WEB_MERCATOR
                || mCRS == CRS_WEB_MERCATOR && crs == CRS_WGS84) {
            boolean isOk = true;
            for (GeoRawPoint point : mPoints) {
                isOk = isOk && point.project(crs);
            }
            return isOk;
        }
        return false;
    }

    @Override
    public GeoEnvelope getEnvelope() {
        double minX = MERCATOR_MAX + 1;
        double minY = MERCATOR_MAX + 1;
        double maxX = -(MERCATOR_MAX + 1);
        double maxY = -(MERCATOR_MAX + 1);

        for (GeoRawPoint point : mPoints) {
            if(point.mX < minX)
                minX = point.mX;
            if(point.mY < minY)
                minY = point.mY;
            if(point.mX > maxX)
                maxX = point.mX;
            if(point.mY > maxY)
                maxY = point.mY;
        }

        return new GeoEnvelope(minX, maxX, minY, maxY);
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject jsonOutObject = new JSONObject();
        jsonOutObject.put(GEOJSON_TYPE, GEOJSON_TYPE_LineString);
        JSONArray coordinates = new JSONArray();
        jsonOutObject.put(GEOJSON_COORDINATES, coordinates);

        for (GeoRawPoint point : this.mPoints) {
            JSONArray pointCoordinates = new JSONArray();
            pointCoordinates.put(point.mX);
            pointCoordinates.put(point.mY);
            coordinates.put(pointCoordinates);
        }

        return jsonOutObject;
    }
}
