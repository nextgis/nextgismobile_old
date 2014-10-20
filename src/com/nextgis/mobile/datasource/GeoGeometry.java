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
            for (int i = 0; i < coordinates.length(); ++i) {
                double x = coordinates.getJSONArray(i).getDouble(0);
                double y = coordinates.getJSONArray(i).getDouble(1);
                lineString.add(x, y);
            }
            // TODO: if (pointCount < 2) then error
            output = lineString;
        }
        else if(sType.equals(GEOJSON_TYPE_MultiLineString)){

        }
        else if(sType.equals(GEOJSON_TYPE_Polygon)){
            GeoPolygon polygon = new GeoPolygon();
            JSONArray coordinates = jsonObject.getJSONArray(GEOJSON_COORDINATES);
            for (int i = 0; i < coordinates.getJSONArray(0).length(); ++i) {
                double x = coordinates.getJSONArray(0).getJSONArray(i).getDouble(0);
                double y = coordinates.getJSONArray(0).getJSONArray(i).getDouble(1);
                polygon.add(x, y);
            }
            // TODO: for each linearRing
            // TODO: if (pointCount < 4 || points.get(0) != points.get(last)) then error
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
