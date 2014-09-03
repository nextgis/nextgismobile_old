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
import android.util.Log;
import android.widget.Toast;

import static com.nextgis.mobile.util.Constants.*;
import static com.nextgis.mobile.util.GeoConstants.*;
import com.nextgis.mobile.R;

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

                //load contents to memory and reproject if needed
                JSONArray geoJSONFeatures = geoJSONObject.getJSONArray(GEOJSON_TYPE_FEATURES);
                for(int i = 0; i < geoJSONFeatures.length(); i++){
                    JSONObject feature = geoJSONFeatures.getJSONObject(i);

                    //convert JSONObject to GeoFeature
                    //reproject if needed
                    if(isWGS84){

                    }

                    //update bbox

                    //normalize attributes


                    /*You can get the object from the JSON using the get() method and then use the instanceof operator to check for the type of Object. Something like this:-

                            String jString = "{\"a\": 1, \"b\": \"str\"}";
                    JSONObject jObj = new JSONObject(jString);
                    Object aObj = jObj.get("a");
                    if(aObj instanceof Integer){
                        System.out.println(aObj);
                    }*/


                }


                File outputPath = map.cretateLayerStorage();
                //create layer description file
                JSONObject oJSONRoot = new JSONObject();
                oJSONRoot.put(JSON_NAME_KEY, layerName);
                oJSONRoot.put(JSON_VISIBILITY_KEY, true);
                oJSONRoot.put(JSON_TYPE_KEY, LAYERTYPE_LOCAL_GEOJSON);


                return;
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
