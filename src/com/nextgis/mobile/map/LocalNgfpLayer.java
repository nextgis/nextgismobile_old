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
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import com.nextgis.mobile.R;
import com.nextgis.mobile.util.Constants;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.nextgis.mobile.util.Constants.TAG;

public class LocalNgfpLayer extends LocalGeoJsonLayer {

    public LocalNgfpLayer() {
        super();
    }

    public LocalNgfpLayer(MapBase map, File path, JSONObject config) {
        super(map, path, config);
    }

    @Override
    public int getType() {
        return Constants.LAYERTYPE_LOCAL_NGFP;
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
                        map.getContext().getString(R.string.message_opening_progress));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(true);
                progressDialog.show();

                // Connection for NGFP
                ZipInputStream zis = new ZipInputStream(inputStream);
                JSONObject connectionJson =
                        getJsonObjectFromZipInputStream(zis, Constants.CONNECTION_JSON);

                // TODO: find connection or create it
                if (connectionJson != null) {

                }

                // XML-form for NGFP
                inputStream = map.getContext().getContentResolver().openInputStream(uri);
                zis = new ZipInputStream(inputStream);
                BufferedReader formDataReader =
                        getTextFileReaderFromZipInputStream(zis, Constants.FORM_XML, "UTF-8");

                if (formDataReader != null) {
                    StringBuilder responseStrBuilder = new StringBuilder();

                    String inputStr;
                    while ((inputStr = formDataReader.readLine()) != null) {
                        responseStrBuilder.append(inputStr);
                    }

                    // TODO: write String to file

                }

                // GeoJSON-data for NGFP
                inputStream = map.getContext().getContentResolver().openInputStream(uri);
                zis = new ZipInputStream(inputStream);
                JSONObject geoJSONObject =
                        getJsonObjectFromZipInputStream(zis, Constants.FORM_DATA_GEOJSON);

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

    public BufferedReader getTextFileReaderFromZipInputStream(
            ZipInputStream zis, String textFileInArchive, String textFileEncoding) {

        try {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName().toLowerCase();

                if (entryName.contains(File.separator)) {
                    entryName = entryName.substring(entryName.lastIndexOf(File.separator) + 1);
                }

                String fileName = textFileInArchive.toLowerCase();

                if (entryName.equals(fileName)) {
                    InputStreamReader isr = new InputStreamReader(zis, textFileEncoding);
                    return new BufferedReader(isr);
                }
            }

            String message = String.format("File %1$s in zip-arhive not found", textFileInArchive);
            Log.e(TAG, message);
            throw new FileNotFoundException(message);

        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

        return null;
    }

    public JSONObject getJsonObjectFromZipInputStream(ZipInputStream zis, String textFileInArchive)
            throws IOException, JSONException {

        BufferedReader formDataReader =
                getTextFileReaderFromZipInputStream(zis, textFileInArchive, "UTF-8");

        if (formDataReader == null) return null;

        StringBuilder responseStrBuilder = new StringBuilder();

        String inputStr;
        while ((inputStr = formDataReader.readLine()) != null) {
            responseStrBuilder.append(inputStr);
        }

        return new JSONObject(responseStrBuilder.toString());
    }
}