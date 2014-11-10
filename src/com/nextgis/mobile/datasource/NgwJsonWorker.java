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
import com.nextgis.mobile.util.Constants;
import com.nextgis.mobile.util.FileUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static com.nextgis.mobile.util.Constants.*;

public class NgwJsonWorker {

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

    public void loadNgwRootJsonObjectString(NgwConnection connection) {
        NgwConnection resourceConnection = new NgwConnection(
                connection.getName(),
                connection.getUrl() + "/resource/-/child/0",
                connection.getLogin(),
                connection.getPassword());

        new NgwConnectionRunner(false).execute(resourceConnection);
    }

    public void loadNgwRootJsonArrayString(NgwConnection connection) {
        NgwConnection resourceConnection = new NgwConnection(
                connection.getName(),
                connection.getUrl() + "/resource/-/child/",
                connection.getLogin(),
                connection.getPassword());

        new NgwConnectionRunner(true).execute(resourceConnection);
    }

    public void loadNgwJsonObjectString(NgwConnection connection, int parentId, int resourceId) {
        NgwConnection resourceConnection = new NgwConnection(
                connection.getName(),
                connection.getUrl() + "/resource/" + parentId + "/child/" + resourceId,
                connection.getLogin(),
                connection.getPassword());

        new NgwConnectionRunner(true).execute(resourceConnection);
    }

    public void loadNgwJsonArrayString(NgwConnection connection, int parentId) {
        NgwConnection resourceConnection = new NgwConnection(
                connection.getName(),
                connection.getUrl() + "/resource/" + parentId + "/child/",
                connection.getLogin(),
                connection.getPassword());

        new NgwConnectionRunner(true).execute(resourceConnection);
    }

    public static List<NgwConnection> loadNgwConnections(File path) {
        Log.d(TAG, "Load NGW connections");

        List<NgwConnection> ngwConnections = new ArrayList<NgwConnection>();

        try {
            File configFile = new File(path, NGW_CONNECTIONS_JSON);
            String jsonData = FileUtil.readFromFile(configFile);
            JSONObject rootObject = new JSONObject(jsonData);
            final JSONArray jsonArray = rootObject.getJSONArray(JSON_NGW_CONNECTIONS_KEY);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonConnection = jsonArray.getJSONObject(i);
                String name = jsonConnection.getString(JSON_NAME_KEY);
                String url = jsonConnection.getString(JSON_URL_KEY);
                String login = jsonConnection.getString(JSON_LOGIN_KEY);
                String password = jsonConnection.getString(JSON_PASSWORD_KEY);

                NgwConnection connection = new NgwConnection(name, url, login, password);
                ngwConnections.add(connection);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ngwConnections;
    }

    public static boolean saveNgwConnections(List<NgwConnection> ngwConnections, File path) {
        Log.d(TAG, "Save NGW connections");

        try {
            JSONObject rootObject = new JSONObject();
            JSONArray jsonConnectionArray = new JSONArray();
            rootObject.put(JSON_NGW_CONNECTIONS_KEY, jsonConnectionArray);

            for (NgwConnection connection : ngwConnections) {
                JSONObject jsonConnection = new JSONObject();
                jsonConnection.put(JSON_NAME_KEY, connection.getName());
                jsonConnection.put(JSON_URL_KEY, connection.getUrl());
                jsonConnection.put(JSON_LOGIN_KEY, connection.getLogin());
                jsonConnection.put(JSON_PASSWORD_KEY, connection.getPassword());

                jsonConnectionArray.put(jsonConnection);
            }

            File configFile = new File(path, NGW_CONNECTIONS_JSON);
            FileUtil.writeToFile(configFile, rootObject.toString());
            return true;

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static Integer ngwClsToType(String typeString) {
        if (typeString.equals(Constants.JSON_RESOURCE_GROUP_VALUE)) {
            return Constants.NGWTYPE_RESOURCE_GROUP;
        } else if (typeString.equals(Constants.JSON_POSTGIS_LAYER_VALUE)) {
            return Constants.NGWTYPE_POSTGIS_LAYER;
        } else if (typeString.equals(Constants.JSON_WMSSERVER_SERVICE_VALUE)) {
            return Constants.NGWTYPE_WMSSERVER_SERVICE;
        } else if (typeString.equals(Constants.JSON_BASELAYERS_VALUE)) {
            return Constants.NGWTYPE_BASELAYERS;
        } else if (typeString.equals(Constants.JSON_POSTGIS_CONNECTION_VALUE)) {
            return Constants.NGWTYPE_POSTGIS_CONNECTION;
        } else if (typeString.equals(Constants.JSON_WEBMAP_VALUE)) {
            return Constants.NGWTYPE_WEBMAP;
        } else if (typeString.equals(Constants.JSON_WFSSERVER_SERVICE_VALUE)) {
            return Constants.NGWTYPE_WFSSERVER_SERVICE;
        } else if (typeString.equals(Constants.JSON_VECTOR_LAYER_VALUE)) {
            return Constants.NGWTYPE_VECTOR_LAYER;
        } else if (typeString.equals(Constants.JSON_RASTER_LAYER_VALUE)) {
            return Constants.NGWTYPE_RASTER_LAYER;
        } else if (typeString.equals(Constants.JSON_VECTOR_STYLE_VALUE)) {
            return Constants.NGWTYPE_VECTOR_STYLE;
        } else if (typeString.equals(Constants.JSON_RASTER_STYLE_VALUE)) {
            return Constants.NGWTYPE_RASTER_STYLE;
        } else if (typeString.equals(Constants.JSON_FILE_BUCKET_VALUE)) {
            return Constants.NGWTYPE_FILE_BUCKET;
        } else if (typeString.equals(Constants.JSON_PARENT_RESOURCE_GROUP_VALUE)) {
            return Constants.NGWTYPE_PARENT_RESOURCE_GROUP;
        } else {
            return Constants.NGWTYPE_UNKNOWN;
        }
    }


    protected String getNgwResourceString(NgwConnection connection) {

        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(connection.getUrl());
            httpGet.setHeader("Authorization", "Basic " + Base64.encodeToString(
                    (connection.getLogin() + ":" + connection.getPassword()).getBytes(),
                    Base64.NO_WRAP));
            HttpResponse response = httpClient.execute(httpGet);

            // Check to see if we got success
            final org.apache.http.StatusLine line = response.getStatusLine();
            if (line.getStatusCode() != 200) {
                Log.w(TAG, "Problem downloading Resource: " + connection.getUrl() + " HTTP response: " + line);
                return null;
            }

            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                Log.w(TAG, "No content downloading Resource: " + connection.getUrl());
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
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    protected class NgwConnectionRunner extends AsyncTask<NgwConnection, Void, String> {
        boolean isConnectionForJsonArray = false;

        private NgwConnectionRunner(boolean typeConnection) {
            super();
            this.isConnectionForJsonArray = typeConnection;
        }

        @Override
        protected String doInBackground(NgwConnection... params) {
            return getNgwResourceString(params[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result == null) {
                return;
            }

            try {
                if (isConnectionForJsonArray) {
                    if (mJsonArrayLoadedListener != null) {
                        mJsonArrayLoadedListener.onJsonArrayLoaded(new JSONArray(result));
                    }

                } else {
                    if (mJsonObjectLoadedListener != null) {
                        mJsonObjectLoadedListener.onJsonObjectLoaded(new JSONObject(result));
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
