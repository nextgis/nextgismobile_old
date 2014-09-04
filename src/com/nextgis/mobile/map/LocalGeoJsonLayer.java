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


import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import static com.nextgis.mobile.util.Constants.*;
import static com.nextgis.mobile.util.GeoConstants.*;
import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.Feature;
import com.nextgis.mobile.datasource.Field;
import com.nextgis.mobile.datasource.Geo;
import com.nextgis.mobile.datasource.GeoEnvelope;
import com.nextgis.mobile.datasource.GeoGeometry;
import com.nextgis.mobile.util.FileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class LocalGeoJsonLayer extends GeoJsonLayer {
    @Override
    public Drawable getIcon() {
        return getContext().getResources().getDrawable(R.drawable.ic_local_json);
    }

    @Override
    public int getType() {
        return LAYERTYPE_LOCAL_GEOJSON;
    }

    @Override
    public void changeProperties() {

    }

    protected static void create(final MapBase map, String layerName, Uri uri) {
        String sErr = map.getContext().getString(R.string.error_occurred);
        try {
            InputStream inputStream = map.getContext().getContentResolver().openInputStream(uri);
            if (inputStream != null) {

                ProgressDialog progressDialog = new ProgressDialog(map.getContext());
                progressDialog.setMessage(map.getContext().getString(R.string.message_loading_progress));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(true);
                progressDialog.show();

                int nSize = inputStream.available();
                int nIncrement = 0;
                progressDialog.setMax(nSize);


                //read all geojson
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = streamReader.readLine()) != null) {
                    nIncrement += inputStr.length();
                    progressDialog.setProgress(nIncrement);
                    responseStrBuilder.append(inputStr);
                }

                JSONObject geoJSONObject = new JSONObject(responseStrBuilder.toString());

                if(!geoJSONObject.has(GEOJSON_TYPE)){
                    sErr += ": " + map.getContext().getString(R.string.error_geojson_unsupported);
                    Toast.makeText(map.getContext(), sErr, Toast.LENGTH_SHORT).show();
                    return;
                }

                //check crs
                boolean isWGS84 = true; //if no crs tag - WGS84 CRS
                if(geoJSONObject.has(GEOJSON_CRS)) {
                    JSONObject crsJSONObject = geoJSONObject.getJSONObject(GEOJSON_CRS);
                    if(!crsJSONObject.getString(GEOJSON_TYPE).equals(GEOJSON_NAME)){ //the link is unsupported yet.
                        sErr += ": " + map.getContext().getString(R.string.error_geojson_crs_unsupported);
                        Toast.makeText(map.getContext(), sErr, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    JSONObject crsPropertiesJSONObject = crsJSONObject.getJSONObject(GEOJSON_PROPERTIES);
                    String crsName = crsPropertiesJSONObject.getString(GEOJSON_NAME);
                    if(crsName.equals("urn:ogc:def:crs:OGC:1.3:CRS84")) // WGS84
                        isWGS84 = true;
                    else if(crsName.equals("urn:ogc:def:crs:EPSG::3857")) //Web Mercator
                        isWGS84 = false;
                    else{
                        sErr += ": " + map.getContext().getString(R.string.error_geojson_crs_unsupported);
                        Toast.makeText(map.getContext(), sErr, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                GeoEnvelope extents = new GeoEnvelope();
                List<Feature> features = new ArrayList<Feature>();
                List<Field> fields = new ArrayList<Field>();
                int geometryType = GTNone;
                //load contents to memory and reproject if needed
                JSONArray geoJSONFeatures = geoJSONObject.getJSONArray(GEOJSON_TYPE_FEATURES);
                for(int i = 0; i < geoJSONFeatures.length(); i++){
                    JSONObject jsonFeature = geoJSONFeatures.getJSONObject(i);

                    //get geometry
                    JSONObject jsonGeometry = jsonFeature.getJSONObject(GEOJSON_GEOMETRY);
                    GeoGeometry geometry = GeoGeometry.fromJson(jsonGeometry);

                    if(geometryType == GTNone){
                        geometryType = geometry.getType();
                    }
                    else if(!Geo.isGeometryTypeSame(geometryType, geometry.getType())) { //skip different geometry type
                        continue;
                    }

                    //reproject if needed
                    if(isWGS84){
                        geometry.setCRS(CRS_WGS84);
                        geometry.project(CRS_WEB_MERCATOR);
                    }
                    else{
                        geometry.setCRS(CRS_WEB_MERCATOR);
                    }

                    Feature feature = new Feature(fields);
                    feature.setGeometry(geometry);
                    //TODO: add to RTree for fast spatial queries

                    //update bbox
                    extents.merge(geometry.getEnvelope());

                    //normalize attributes
                    JSONObject jsonAttributes = jsonFeature.getJSONObject(GEOJSON_PROPERTIES);
                    Iterator<String> iter = jsonAttributes.keys();
                    while (iter.hasNext()) {
                        String key = iter.next();
                        Object value = jsonAttributes.get(key);

                        int nType = -1;
                        //check type
                        if (value instanceof Integer || value instanceof Long)
                            nType = FTInteger;
                        else if(value instanceof Double || value instanceof Float)
                            nType = FTReal;
                        else if(value instanceof Date)
                            nType = FTDateTime;
                        else if(value instanceof String)
                            nType = FTString;
                        else if(value instanceof JSONObject)
                            nType = -1; //the some list - need to check it type FTIntegerList, FTRealList, FTStringList

                        int nField = -1;
                        for(int j = 0; j < fields.size(); j++){
                            if(fields.get(j).getFieldName().equals(key)){
                                nField = j;
                            }
                        }

                        if(nField == -1) { //add new field
                            Field field = new Field(key, key, nType);
                            nField = fields.size();
                            fields.add(field);
                        }

                        feature.setField(nField, value);
                    }

                    features.add(feature);
                }


                File outputPath = map.cretateLayerStorage();
                //create layer description file
                JSONObject oJSONRoot = new JSONObject();
                oJSONRoot.put(JSON_NAME_KEY, layerName);
                oJSONRoot.put(JSON_VISIBILITY_KEY, true);
                oJSONRoot.put(JSON_TYPE_KEY, LAYERTYPE_LOCAL_GEOJSON);
                //add bbox
                JSONObject oJSONBBox = extents.toJSON();
                oJSONRoot.put(JSON_BBOX_KEY, oJSONBBox);
                //add fields description
                JSONArray oJSONFields = new JSONArray();
                for(Field field : fields){
                    oJSONFields.put(field.toJSON());
                }
                oJSONRoot.put(JSON_FIELDS_KEY, oJSONFields);

                File file = new File(outputPath, LAYER_CONFIG);
                FileUtil.createDir(outputPath);
                FileUtil.writeToFile(file, oJSONRoot.toString());

                //store GeoJson to file
                File geoJsonFile = new File(outputPath, DATA_GEOJSON);
                if(save(features, geoJsonFile)) {
                    if(map.getMapEventsHandler() != null){
                        Bundle bundle = new Bundle();
                        bundle.putBoolean(BUNDLE_HASERROR_KEY, false);
                        bundle.putString(BUNDLE_MSG_KEY, map.getContext().getString(R.string.message_layer_added));
                        bundle.putInt(BUNDLE_TYPE_KEY, MSGTYPE_LAYER_ADDED);
                        bundle.putSerializable(BUNDLE_PATH_KEY, outputPath);

                        Message msg = new Message();
                        msg.setData(bundle);
                        map.getMapEventsHandler().sendMessage(msg);
                    }
                    return;
                }

                sErr += ": " + map.getContext().getString(R.string.error_savefile_failed) + " - " + geoJsonFile.toString();
            }
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, "Exception: " + e.getLocalizedMessage());
            sErr += ": " + e.getLocalizedMessage();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "Exception: " + e.getLocalizedMessage());
            sErr += ": " + e.getLocalizedMessage();
        } catch (IOException e) {
            Log.d(TAG, "Exception: " + e.getLocalizedMessage());
            sErr += ": " + e.getLocalizedMessage();
        }  catch (JSONException e) {
            Log.d(TAG, "Exception: " + e.getLocalizedMessage());
            sErr += ": " + e.getLocalizedMessage();
        }
        //if we here something wrong occurred
        Toast.makeText(map.getContext(), sErr, Toast.LENGTH_SHORT).show();
    }

}
