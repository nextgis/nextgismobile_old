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

import android.util.Log;
import com.nextgis.mobile.util.FileUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.nextgis.mobile.util.Constants.*;
import static com.nextgis.mobile.util.Constants.NGW_CONNECTIONS_JSON;

public class NgwConnection {

    public static final int LOAD_PARENT = 1;
    public static final int LOAD_RESOURCE = 2;
    public static final int LOAD_CHILDREN = 3;
    public static final int LOAD_GEOJSON = 4;

    protected int mId;
    protected String mName;
    protected String mUrl;
    protected String mLogin;
    protected String mPassword;

    protected Integer mParentResourceId;
    protected Integer mResourceId;

    protected Integer mChildId;

    protected int mThatLoad;


    public NgwConnection(String name, String url, String login, String password) {
        Init(generateId(), name, url, login, password);
    }

    public NgwConnection(int id, String name, String url, String login, String password) {
        Init(id, name, url, login, password);
    }

    public void Init(int id, String name, String url, String login, String password) {
        mId = id;
        mName = name;
        mUrl = url;
        mLogin = login;
        mPassword = password;

        mParentResourceId = null;
        mResourceId = null;
        mChildId = 0;

        mThatLoad = LOAD_RESOURCE;
    }

    public int getId() {
        return mId;
    }

    public int generateId() {
        return new Random().nextInt();
    }

    public String getName() {
        return mName;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getLogin() {
        return mLogin;
    }

    public String getPassword() {
        return mPassword;
    }

    protected Integer getParentResourceIdFromResource(NgwResource resource) {
        return resource == null
                || resource.getParent() == null
                || resource.getParent().getId() == null
                ? null
                : resource.getParent().getId();
    }

    protected Integer getResourceIdFromResource(NgwResource resource) {
        return resource == null
                || resource.getId() == null
                ? null
                : resource.getId();
    }

    public String getLoadUrl() {
        switch (mThatLoad) {
            case LOAD_PARENT:
                return getParentArrayUrl();
            case LOAD_RESOURCE:
            default:
                return getResourceArrayUrl();
            case LOAD_CHILDREN:
                return getChildObjectUrl();
            case LOAD_GEOJSON:
                return getGeoJsonUrl();
        }
    }

    public String getParentArrayUrl() {
        return mUrl + "resource/"
                + (mParentResourceId == null ? "-" : mParentResourceId)
                + "/child/";
    }

    public String getResourceArrayUrl() {
        return mUrl + "resource/"
                + (mResourceId == null ? "-" : mResourceId)
                + "/child/";
    }

    public String getChildObjectUrl() {
        return mUrl + "resource/"
                + (mResourceId == null ? "-" : mResourceId)
                + "/child/" + mChildId;
    }

    public String getGeoJsonUrl() {
        return mUrl + "resource/"
                + (mResourceId == null ? "-" : mResourceId)
                + "/geojson/";
    }

    public void setLoadParentArray(NgwResource resource) {
        mParentResourceId = getParentResourceIdFromResource(resource);
        mResourceId = getResourceIdFromResource(resource);
        mChildId = 0;
        mThatLoad = LOAD_PARENT;
    }

    public void setLoadResourceArray(NgwResource resource) {
        mParentResourceId = getParentResourceIdFromResource(resource);
        mResourceId = getResourceIdFromResource(resource);
        mChildId = 0;
        mThatLoad = LOAD_RESOURCE;
    }

    public void setLoadChildrenObject(NgwResource resource, Integer childId) {
        mParentResourceId = getParentResourceIdFromResource(resource);
        mResourceId = getResourceIdFromResource(resource);
        mChildId = childId;
        mThatLoad = LOAD_CHILDREN;
    }

    public void setLoadGeoJsonObject(NgwResource resource) {
        mParentResourceId = getParentResourceIdFromResource(resource);
        mResourceId = getResourceIdFromResource(resource);
        mChildId = 0;
        mThatLoad = LOAD_GEOJSON;
    }

    public boolean isForJsonArray() {
        switch (mThatLoad) {

            case LOAD_PARENT:
            case LOAD_RESOURCE:
            default:
                return true;

            case LOAD_CHILDREN:
            case LOAD_GEOJSON:
                return false;
        }
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
                int id = jsonConnection.getInt(JSON_ID_KEY);
                String name = jsonConnection.getString(JSON_NAME_KEY);
                String url = jsonConnection.getString(JSON_URL_KEY);
                String login = jsonConnection.getString(JSON_LOGIN_KEY);
                String password = jsonConnection.getString(JSON_PASSWORD_KEY);

                NgwConnection connection = new NgwConnection(id, name, url, login, password);
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

            for (NgwConnection ngwConnection : ngwConnections) {
                JSONObject jsonConnection = new JSONObject();
                jsonConnection.put(JSON_ID_KEY, ngwConnection.getId());
                jsonConnection.put(JSON_NAME_KEY, ngwConnection.getName());
                jsonConnection.put(JSON_URL_KEY, ngwConnection.getUrl());
                jsonConnection.put(JSON_LOGIN_KEY, ngwConnection.getLogin());
                jsonConnection.put(JSON_PASSWORD_KEY, ngwConnection.getPassword());

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
}
