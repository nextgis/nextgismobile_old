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
package com.nextgis.mobile.map;

import com.nextgis.mobile.datasource.Feature;
import com.nextgis.mobile.datasource.Field;
import com.nextgis.mobile.datasource.GeoEnvelope;
import com.nextgis.mobile.util.FileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.nextgis.mobile.util.Constants.DATA_GEOJSON;
import static com.nextgis.mobile.util.GeoConstants.GEOJSON_CRS;
import static com.nextgis.mobile.util.GeoConstants.GEOJSON_NAME;
import static com.nextgis.mobile.util.GeoConstants.GEOJSON_PROPERTIES;
import static com.nextgis.mobile.util.GeoConstants.GEOJSON_TYPE;
import static com.nextgis.mobile.util.GeoConstants.GEOJSON_TYPE_FEATURES;
import static com.nextgis.mobile.util.GeoConstants.GEOJSON_TYPE_FeatureCollection;

public abstract class GeoJsonLayer extends Layer{
    protected List<Feature> mFeatures;
    protected List<Field> mFields;
    protected int mGeometryType;
    protected GeoEnvelope mExtents;

    protected static boolean save(List<Feature> features, File path) throws IOException, JSONException {
        JSONObject oJSONRoot = new JSONObject();
        oJSONRoot.put(GEOJSON_TYPE, GEOJSON_TYPE_FeatureCollection);
        JSONObject crsJson = new JSONObject();
        JSONObject crsJsonProp = new JSONObject();
        crsJsonProp.put(GEOJSON_NAME, "urn:ogc:def:crs:EPSG::3857");
        crsJson.put(GEOJSON_TYPE, GEOJSON_NAME);
        crsJson.put(GEOJSON_PROPERTIES, crsJsonProp);
        oJSONRoot.put(GEOJSON_CRS, crsJson);

        JSONArray oJSONFeatures = new JSONArray();
        for(Feature feature : features){
            JSONObject oJSONFeature = feature.toJSON();
            oJSONFeatures.put(oJSONFeature);
        }
        oJSONRoot.put(GEOJSON_TYPE_FEATURES, oJSONFeatures);

        File file = new File(path, DATA_GEOJSON);
        FileUtil.writeToFile(file, oJSONRoot.toString());

        return true;
    }
}
