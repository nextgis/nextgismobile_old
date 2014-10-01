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

import android.graphics.Color;

import com.nextgis.mobile.datasource.*;
import com.nextgis.mobile.display.SimpleMarkerStyle;
import com.nextgis.mobile.util.FileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.nextgis.mobile.util.Constants.*;
import static com.nextgis.mobile.util.GeoConstants.*;
import static com.nextgis.mobile.util.DisplayConstants.*;

public abstract class GeoJsonLayer extends Layer{
    protected List<Feature> mFeatures;
    protected List<Field> mFields;
    protected int mGeometryType;
    protected GeoEnvelope mExtents;
    protected float mPointSize = 6;

    public GeoJsonLayer(MapBase map, File path, JSONObject config) {
        super(map, path, config);

        if(mGeometryType == GTPoint) {
            SimpleMarkerStyle style =
                    new SimpleMarkerStyle(Color.RED, Color.BLACK, mPointSize, MarkerStyleCircle);
            style.setWidth(2);
            mRenderer = new SimpleFeatureRenderer(this, style);
        }
    }

    public final int getGeometryType(){
        return mGeometryType;
    }

    protected static void store(List<Feature> features, File path)
            throws IOException, JSONException {

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
    }

    protected void loadFeatures() throws IOException, JSONException {
        mFeatures = new ArrayList<Feature>();

        File geoJsonFile = new File(mPath, DATA_GEOJSON);
        String jsonContent = FileUtil.readFromFile(geoJsonFile);
        JSONObject geoJSONObject = new JSONObject(jsonContent);
        JSONArray geoJSONFeatures = geoJSONObject.getJSONArray(GEOJSON_TYPE_FEATURES);
        for(int i = 0; i < geoJSONFeatures.length(); i++){
            JSONObject jsonFeature = geoJSONFeatures.getJSONObject(i);

            //get geometry
            JSONObject jsonGeometry = jsonFeature.getJSONObject(GEOJSON_GEOMETRY);
            GeoGeometry geometry = GeoGeometry.fromJson(jsonGeometry);
            geometry.setCRS(CRS_WEB_MERCATOR);

            Feature feature = new Feature(mFields);
            feature.setGeometry(geometry);
            //TODO: add to RTree for fast spatial queries

            //normalize attributes
            JSONObject jsonAttributes = jsonFeature.getJSONObject(GEOJSON_PROPERTIES);
            Iterator<String> iter = jsonAttributes.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                Object value = jsonAttributes.get(key);

                feature.setField(key, value);
            }

            mFeatures.add(feature);
        }
    }

    @Override
    public void save() {
        super.save();
        //save features
        try {
            store(mFeatures, mPath);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setDetails(JSONObject config) {
        super.setDetails(config);
        try {
            //add geometry type
            mGeometryType = config.getInt(JSON_GEOMETRY_TYPE_KEY);
            //add bbox
            mExtents = new GeoEnvelope();
            mExtents.fromJSON(config.getJSONObject(JSON_BBOX_KEY));
            //add fields description
            mFields = new ArrayList<Field>();
            JSONArray oJSONFields = config.getJSONArray(JSON_FIELDS_KEY);
            for(int i = 0; i < oJSONFields.length(); i++){
                JSONObject jsonField = oJSONFields.getJSONObject(i);
                Field field = new Field();
                field.fromJSON(jsonField);
                mFields.add(field);
            }
            loadFeatures();

        } catch (JSONException e){
            reportError(e.getLocalizedMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected JSONObject getDetails() throws JSONException{
        JSONObject rootObject = super.getDetails();
        //add geometry type
        rootObject.put(JSON_GEOMETRY_TYPE_KEY, mGeometryType);
        //add bbox
        JSONObject oJSONBBox = mExtents.toJSON();
        rootObject.put(JSON_BBOX_KEY, oJSONBBox);
        //add fields description
        JSONArray oJSONFields = new JSONArray();
        for(Field field : mFields){
            oJSONFields.put(field.toJSON());
        }
        rootObject.put(JSON_FIELDS_KEY, oJSONFields);
        return rootObject;
    }

    public List<Feature> getFeatures(GeoEnvelope bounds){
        if(mFeatures.isEmpty()) {
            try {
                loadFeatures();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        List<Feature> out = new ArrayList<Feature>();
        //get features
        for(Feature feature : mFeatures){
            if(feature.getGeometry() != null && feature.getGeometry().getEnvelope().intersects(bounds))
                out.add(feature);
        }
        return out;
    }

    @Override
    public void changeProperties(){

    }

    public Feature getSelectedFeature(GeoEnvelope geoMapEnvelope) {
        for (Feature feature : mFeatures) {
            if (geoMapEnvelope.contains((GeoPoint) feature.getGeometry())) {
                return feature;
            }
        }

        return null;
    }
}
