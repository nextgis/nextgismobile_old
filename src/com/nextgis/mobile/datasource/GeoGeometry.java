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

import java.util.List;

import static com.nextgis.mobile.util.GeoConstants.*;

public abstract class GeoGeometry{

    protected int mCRS;

    public abstract int getType();
    public abstract boolean project(int crs);
    public abstract GeoEnvelope getEnvelope();
    public void setCRS(int crs){
        mCRS = crs;
    }

    public static GeoGeometry fromJson(JSONObject jsonObject) throws JSONException{
        GeoGeometry output = null;
        String sType = jsonObject.getString(GEOJSON_TYPE);
        if(sType.equals(GEOJSON_TYPE_Point)){
            JSONArray coordinates = jsonObject.getJSONArray(GEOJSON_COORDINATES);
            double x = coordinates.getDouble(0);
            double y = coordinates.getDouble(1);
            output = new GeoPoint(x, y);
        }
        else if(sType.equals(GEOJSON_TYPE_MultiPoint)){

        }
        else if(sType.equals(GEOJSON_TYPE_LineString)){
            GeoLineString lineString = new GeoLineString();
            JSONArray coordinates = jsonObject.getJSONArray(GEOJSON_COORDINATES);

            if (coordinates.length() < 2) {
                throw new JSONException("For type \"LineString\", the \"coordinates\" member must be an array of two or more positions.");
            }

            for (int i = 0; i < coordinates.length(); ++i) {
                double x = coordinates.getJSONArray(i).getDouble(0);
                double y = coordinates.getJSONArray(i).getDouble(1);
                lineString.add(x, y);
            }
            output = lineString;
        }
        else if(sType.equals(GEOJSON_TYPE_MultiLineString)){

        }
        else if(sType.equals(GEOJSON_TYPE_Polygon)){
            GeoPolygon polygon = new GeoPolygon();
            JSONArray coordinates = jsonObject.getJSONArray(GEOJSON_COORDINATES);

            if (coordinates.getJSONArray(0).length() < 4) {
                throw new JSONException("For type \"Polygon\", the \"coordinates\" member must be an array of LinearRing coordinate arrays. A LinearRing must be with 4 or more positions.");
            }

            int i = 0;

            for (; i < coordinates.getJSONArray(0).length(); ++i) {
                double x = coordinates.getJSONArray(0).getJSONArray(i).getDouble(0);
                double y = coordinates.getJSONArray(0).getJSONArray(i).getDouble(1);
                polygon.add(x, y);
            }

            List<GeoRawPoint> points = polygon.getOuterRing().getPoints();

            if (!points.get(0).equals(points.get(i))) {
                throw new JSONException("For type \"Polygon\", the \"coordinates\" member must be an array of LinearRing coordinate arrays. The first and last positions of LinearRing must be equivalent (they represent equivalent points).");
            }

            output = polygon;
        }
        else if(sType.equals(GEOJSON_TYPE_MultiPolygon)){

        }
        else if(sType.equals(GEOJSON_TYPE_GeometryCollection)){

        }
        return output;
    }

    public abstract JSONObject toJSON() throws JSONException;
}
