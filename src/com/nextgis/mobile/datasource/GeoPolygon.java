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

    private List<GeoLinearRing> linearRings;

    public GeoPolygon() {
        linearRings = new ArrayList<GeoLinearRing>();
        linearRings.add(new GeoLinearRing());
    }

    public List<GeoLinearRing> getLinearRings() {
        return linearRings;
    }

    public void add(double x, double y) {
        linearRings.get(0).add(x, y);
    }

    public void add(GeoRawPoint rpt) {
        linearRings.get(0).add(rpt);
    }

    public void remove(int index) {
        linearRings.get(0).remove(index);
    }

    @Override
    public int getType() {
        return GTPolygon;
    }

    @Override
    public boolean project(int crs) {
        if (mCRS == CRS_WGS84 && crs == CRS_WEB_MERCATOR) {
            for (GeoRawPoint point : linearRings.get(0).getCoordinates()) {
                Geo.wgs84ToMercatorSphere(point);
            }
            return true;
        } else if (mCRS == CRS_WEB_MERCATOR && crs == CRS_WGS84) {
            for (GeoRawPoint point : linearRings.get(0).getCoordinates()) {
                Geo.mercatorToWgs84Sphere(point);
            }
            return true;
        }
        return false;
    }

    @Override
    public GeoEnvelope getEnvelope() {
        double minX = MERCATOR_MAX + 1;
        double minY = MERCATOR_MAX + 1;
        double maxX = -(MERCATOR_MAX + 1);
        double maxY = -(MERCATOR_MAX + 1);

        for (GeoRawPoint point : linearRings.get(0).getCoordinates()) {
            if(point.x < minX)
                minX = point.x;
            if(point.y < minY)
                minY = point.y;
            if(point.x > maxX)
                maxX = point.x;
            if(point.y > maxY)
                maxY = point.y;
        }

        return new GeoEnvelope(minX, maxX, minY, maxY);
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject jsonOutObject = new JSONObject();
        jsonOutObject.put(GEOJSON_TYPE, GEOJSON_TYPE_Polygon);
        JSONArray coordinates = new JSONArray();
        jsonOutObject.put(GEOJSON_COORDINATES, coordinates);
        JSONArray linearRingCoordinates = new JSONArray();
        coordinates.put(linearRingCoordinates);

        for (GeoRawPoint point : linearRings.get(0).getCoordinates()) {
            JSONArray pointCoordinates = new JSONArray();
            pointCoordinates.put(point.x);
            pointCoordinates.put(point.y);
            linearRingCoordinates.put(pointCoordinates);
        }

        return jsonOutObject;
    }
}
