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


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
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

    public LocalGeoJsonLayer() {
    }

    public LocalGeoJsonLayer(MapBase map, File path, JSONObject config) {
        super(map, path, config);
    }

    @Override
    public Drawable getIcon() {
        return getContext().getResources().getDrawable(R.drawable.ic_local_json);
    }

    @Override
    public int getType() {
        return LAYERTYPE_LOCAL_GEOJSON;
    }

    protected int getMsgType() {
        return MSGTYPE_LAYER_ADDED;
    }

    public void create(final MapBase map, Uri uri) {
        String sName = getFileNameByUri(map.getContext(), uri, "new layer.geojson");
        sName = (String) sName.subSequence(0, sName.length() - 8);
        showPropertiesDialog(map, true, sName, uri, null);
    }

    @Override
    public void changeProperties() {
        showPropertiesDialog(mMap, false, mName, null, this);
    }

    protected void showPropertiesDialog(final MapBase map, final boolean bCreate, String layerName, final Uri uri, final LocalGeoJsonLayer layer) {
        final LinearLayout linearLayout = new LinearLayout(map.getContext());
        final EditText input = new EditText(map.getContext());
        input.setText(layerName);

        final TextView stLayerName = new TextView(map.getContext());
        stLayerName.setText(map.getContext().getString(R.string.layer_name) + ":");

        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(stLayerName);
        linearLayout.addView(input);

        if (!bCreate) {
            //TODO: style for drawing
        }

        new AlertDialog.Builder(map.getContext())
                .setTitle(bCreate ? R.string.input_layer_properties : R.string.change_layer_properties)
//                                    .setMessage(message)
                .setView(linearLayout)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (bCreate) {
                            create(map, input.getText().toString(), uri);
                        } else {
                            layer.setName(input.getText().toString());
                            map.onLayerChanged(layer);
                        }
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
                Toast.makeText(map.getContext(), R.string.error_cancel_by_user, Toast.LENGTH_SHORT).show();
            }
        }).show();
    }

    /**
     * Create a LocalGeoJsonLayer from the GeoJson data submitted by uri.
     */
    protected void create(final MapBase map, String layerName, Uri uri) {

        String sErr = map.getContext().getString(R.string.error_occurred);
        ProgressDialog progressDialog = new ProgressDialog(map.getContext());

        try {
            InputStream inputStream = map.getContext().getContentResolver().openInputStream(uri);
            if (inputStream != null) {

                progressDialog.setMessage(
                        map.getContext().getString(R.string.message_loading_progress));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(true);
                progressDialog.show();

                int nSize = inputStream.available();
                int nIncrement = 0;
                progressDialog.setMax(nSize);


                //read all geojson
                BufferedReader streamReader =
                        new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = streamReader.readLine()) != null) {
                    nIncrement += inputStr.length();
                    progressDialog.setProgress(nIncrement);
                    responseStrBuilder.append(inputStr);
                }

                progressDialog.setMessage(
                        map.getContext().getString(R.string.message_opening_progress));

                JSONObject geoJSONObject = new JSONObject(responseStrBuilder.toString());
                create(map, layerName, geoJSONObject, progressDialog);
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
        } catch (JSONException e) {
            Log.d(TAG, "Exception: " + e.getLocalizedMessage());
            sErr += ": " + e.getLocalizedMessage();
            Toast.makeText(map.getContext(), sErr, Toast.LENGTH_SHORT).show();
        }

        progressDialog.hide();
        //if we here something wrong occurred
        Toast.makeText(map.getContext(), sErr, Toast.LENGTH_SHORT).show();
    }

    /**
     * Create a LocalGeoJsonLayer from the GeoJson data submitted by geoJSONObject.
     */
    public void create(
            final MapBase map, String layerName, JSONObject geoJSONObject,
            ProgressDialog progressDialog)
            throws JSONException, IOException {

        if (!geoJSONObject.has(GEOJSON_TYPE)) {
            progressDialog.hide();
            String error = map.getContext().getString(R.string.error_geojson_unsupported);
            throw new JSONException(error);
        }

        //check crs
        boolean isWGS84 = true; //if no crs tag - WGS84 CRS

        if (geoJSONObject.has(GEOJSON_CRS)) {
            JSONObject crsJSONObject = geoJSONObject.getJSONObject(GEOJSON_CRS);

            //the link is unsupported yet.
            if (!crsJSONObject.getString(GEOJSON_TYPE).equals(GEOJSON_NAME)) {
                progressDialog.hide();
                String error = map.getContext().getString(R.string.error_geojson_crs_unsupported);
                throw new JSONException(error);
            }

            JSONObject crsPropertiesJSONObject =
                    crsJSONObject.getJSONObject(GEOJSON_PROPERTIES);
            String crsName = crsPropertiesJSONObject.getString(GEOJSON_NAME);

            if (crsName.equals("urn:ogc:def:crs:OGC:1.3:CRS84")) { // WGS84
                isWGS84 = true;
            } else if (crsName.equals("urn:ogc:def:crs:EPSG::3857")) { //Web Mercator
                isWGS84 = false;
            } else {
                progressDialog.hide();
                String error = map.getContext().getString(R.string.error_geojson_crs_unsupported);
                throw new JSONException(error);
            }
        }

        //load contents to memory and reproject if needed
        JSONArray geoJSONFeatures = geoJSONObject.getJSONArray(GEOJSON_TYPE_FEATURES);

        if (0 == geoJSONFeatures.length()) {
            progressDialog.hide();
            String error = map.getContext().getString(R.string.error_geojson_crs_unsupported);
            throw new JSONException(error);
        }

        List<Feature> features = geoJSONFeaturesToFeatures(
                geoJSONFeatures, isWGS84, map.getContext(), progressDialog);

        progressDialog.hide();

        create(map, layerName, features);
    }

    protected void create(
            final MapBase map, String layerName, List<Feature> features)
            throws JSONException, IOException {

        GeoEnvelope extents = new GeoEnvelope();
        for (Feature feature : features) {
            //update bbox
            extents.merge(feature.getGeometry().getEnvelope());
        }

        Feature feature = features.get(0);
        int geometryType = feature.getGeometry().getType();
        List<Field> fields = feature.getFields();

        //create layer description file
        JSONObject oJSONRoot = createDetails();
        oJSONRoot.put(JSON_NAME_KEY, layerName);

        //add geometry type
        oJSONRoot.put(JSON_GEOMETRY_TYPE_KEY, geometryType);

        //add bbox
        JSONObject oJSONBBox = extents.toJSON();
        oJSONRoot.put(JSON_BBOX_KEY, oJSONBBox);

        //add fields description
        JSONArray oJSONFields = new JSONArray();
        for (Field field : fields) {
            oJSONFields.put(field.toJSON());
        }
        oJSONRoot.put(JSON_FIELDS_KEY, oJSONFields);

        // store layer description to file
        File outputPath = map.cretateLayerStorage();
        FileUtil.createDir(outputPath);
        File file = new File(outputPath, LAYER_CONFIG);
        FileUtil.writeToFile(file, oJSONRoot.toString());

        //store GeoJson to file
        store(features, outputPath);

        if (map.getMapEventsHandler() != null) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(BUNDLE_HASERROR_KEY, false);
            bundle.putInt(BUNDLE_TYPE_KEY, getMsgType());
            bundle.putSerializable(BUNDLE_PATH_KEY, outputPath);

            Message msg = new Message();
            msg.setData(bundle);
            map.getMapEventsHandler().sendMessage(msg);
        }
    }

    protected JSONObject createDetails() throws JSONException{
        JSONObject rootConfig = new JSONObject();
        rootConfig.put(JSON_VISIBILITY_KEY, true);
        rootConfig.put(JSON_TYPE_KEY, getType());
        rootConfig.put(JSON_MAXLEVEL_KEY, 50);
        rootConfig.put(JSON_MINLEVEL_KEY, 0);
        return rootConfig;
    }

    private List<Feature> geoJSONFeaturesToFeatures(
            JSONArray geoJSONFeatures, boolean isWGS84, Context context,
            ProgressDialog progressDialog)
            throws JSONException {

        List<Feature> features = new ArrayList<Feature>();
        List<Field> fields = new ArrayList<Field>();
        int geometryType = GTNone;

        progressDialog.setMessage(context.getString(R.string.message_loading_progress));
        progressDialog.setMax(geoJSONFeatures.length());
        for (int i = 0; i < geoJSONFeatures.length(); i++) {
            progressDialog.setProgress(i);
            JSONObject jsonFeature = geoJSONFeatures.getJSONObject(i);

            //get geometry
            JSONObject jsonGeometry = jsonFeature.getJSONObject(GEOJSON_GEOMETRY);
            GeoGeometry geometry = GeoGeometry.fromJson(jsonGeometry);

            if (geometryType == GTNone) {
                geometryType = geometry.getType();
            } else if (!Geo.isGeometryTypeSame(geometryType, geometry.getType())) {
                //skip different geometry type
                continue;
            }

            //reproject if needed
            if (isWGS84) {
                geometry.setCRS(CRS_WGS84);
                geometry.project(CRS_WEB_MERCATOR);
            } else {
                geometry.setCRS(CRS_WEB_MERCATOR);
            }

            Feature feature = new Feature(fields);
            feature.setGeometry(geometry);
            //TODO: add to RTree for fast spatial queries

            //normalize attributes
            JSONObject jsonAttributes = jsonFeature.getJSONObject(GEOJSON_PROPERTIES);
            Iterator<String> iter = jsonAttributes.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                Object value = jsonAttributes.get(key);

                int nType = -1;
                //check type
                if (value instanceof Integer || value instanceof Long) {
                    nType = FTInteger;
                } else if (value instanceof Double || value instanceof Float) {
                    nType = FTReal;
                } else if (value instanceof Date) {
                    nType = FTDateTime;
                } else if (value instanceof String) {
                    nType = FTString;
                } else if (value instanceof JSONObject) {
                    nType = -1;
                    //the some list - need to check it type FTIntegerList, FTRealList, FTStringList
                }

                int nField = -1;
                for (int j = 0; j < fields.size(); j++) {
                    if (fields.get(j).getFieldName().equals(key)) {
                        nField = j;
                    }
                }

                if (nField == -1) { //add new field
                    Field field = new Field(key, key, nType);
                    nField = fields.size();
                    fields.add(field);
                }

                feature.setField(nField, value);
            }

            features.add(feature);
        }

        return features;
    }
}
