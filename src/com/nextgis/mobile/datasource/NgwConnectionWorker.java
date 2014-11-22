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

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import static com.nextgis.mobile.util.Constants.*;

public class NgwConnectionWorker {

    protected JsonObjectLoadedListener mJsonObjectLoadedListener;
    protected JsonArrayLoadedListener mJsonArrayLoadedListener;


    public interface JsonObjectLoadedListener {
        void onJsonObjectLoaded(JSONObject jsonObject);
    }

    public interface JsonArrayLoadedListener {
        void onJsonArrayLoaded(JSONArray jsonArray);
    }

    public void setJsonObjectLoadedListener(JsonObjectLoadedListener jsonObjectLoadedListener) {
        mJsonObjectLoadedListener = jsonObjectLoadedListener;
    }

    public void setJsonArrayLoadedListener(JsonArrayLoadedListener jsonArrayLoadedListener) {
        mJsonArrayLoadedListener = jsonArrayLoadedListener;
    }


    protected String getNgwResourceString(NgwConnection connection) {

        try {
            HttpParams httpParameters = new BasicHttpParams();
            // Sets the timeout until a connection is established.
            HttpConnectionParams.setConnectionTimeout(httpParameters, TIMEOUT_CONNECTION);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT_SOKET);

            DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
            HttpGet httpGet = new HttpGet(connection.getLoadUrl());
            httpGet.setHeader("Authorization", "Basic " + Base64.encodeToString(
                    (connection.getLogin() + ":" + connection.getPassword()).getBytes(),
                    Base64.NO_WRAP));
            HttpResponse response = httpClient.execute(httpGet);

            // Check to see if we got success
            final org.apache.http.StatusLine line = response.getStatusLine();
            if (line.getStatusCode() != 200) {
                Log.w(TAG, "Problem downloading Resource: " + connection.getLoadUrl() + " HTTP response: " + line);
                return null;
            }

            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                Log.w(TAG, "No content downloading Resource: " + connection.getLoadUrl());
                return null;
            }

            InputStream inputStream = entity.getContent();

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"), 8);
            StringBuilder sb = new StringBuilder();
            String str = null;
            while ((str = reader.readLine()) != null) {
                sb.append(str).append("\n");
            }
            reader.close();
            return sb.toString();

        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "Problem downloading Resource: " + connection.getLoadUrl()
                    + ", error: " + e.getLocalizedMessage());
        } catch (ClientProtocolException e) {
            Log.w(TAG, "Problem downloading Resource: " + connection.getLoadUrl()
                    + ", error: " + e.getLocalizedMessage());
        } catch (IOException e) {
            Log.w(TAG, "Problem downloading Resource: " + connection.getLoadUrl()
                    + ", error: " + e.getLocalizedMessage());
        }

        return null;
    }

    public void loadNgwJson(NgwConnection connection) {
        new NgwConnectionRunner().execute(connection);
    }


    protected class NgwConnectionRunner extends AsyncTask<NgwConnection, Void, String> {

        protected NgwConnection mConnection;

        @Override
        protected String doInBackground(NgwConnection... connections) {
            mConnection = connections[0];
            return getNgwResourceString(mConnection);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            try {
                if (mConnection.isForJsonArray()) {
                    if (mJsonArrayLoadedListener != null) {
                        mJsonArrayLoadedListener.onJsonArrayLoaded(
                                result == null ? null : new JSONArray(result));
                    }

                } else {
                    if (mJsonObjectLoadedListener != null) {
                        mJsonObjectLoadedListener.onJsonObjectLoaded(
                                result == null ? null : new JSONObject(result));
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
