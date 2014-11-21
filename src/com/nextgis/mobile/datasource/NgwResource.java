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

import com.nextgis.mobile.util.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

public class NgwResource extends ArrayList<NgwResource> implements Comparable<NgwResource> {

    protected Integer mConnectionId;

    protected NgwResource mParent;
    protected Integer mId;
    protected Integer mCls;
    protected String mDisplayName;

    protected boolean mIsSelected;


    public NgwResource(Integer connectionId) {
        super();
        Init(connectionId, null, null, null, null);
    }

    public NgwResource(Integer connectionId, NgwResource parent,
                       Integer id, Integer cls, String displayName) {
        super();
        Init(connectionId, parent, id, cls, displayName);
    }

    protected void Init(Integer connectionId, NgwResource parent,
                        Integer id, Integer cls, String displayName) {

        mConnectionId = connectionId;
        mParent = parent;
        mId = id;
        mCls = cls;
        mDisplayName = displayName;
        mIsSelected = false;
    }

    public void setConnectionId(Integer connectionId) {
        mConnectionId = connectionId;
    }

    public Integer getConnectionId() {
        return mConnectionId;
    }

    public NgwResource getParent() {
        return mParent;
    }

    public Integer getId() {
        return mId;
    }

    public Integer getCls() {
        return mCls;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean isSelected) {
        mIsSelected = isSelected;
    }

    public boolean isRoot() {
        return mParent == null && mId == null;
    }

    public void addNgwResourcesFromJSONArray(
            JSONArray jsonArray, Set<NgwResource> selectedResources)
            throws JSONException {

        this.clear();
        this.ensureCapacity(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); ++i) {
            // if error with JSON_RESOURCE_KEY -- throw JSONException
            JSONObject jsonObject =
                    jsonArray.getJSONObject(i).getJSONObject(Constants.JSON_RESOURCE_KEY);

            // if error with JSON_ID_KEY -- throw JSONException
            Integer id = jsonObject.getInt(Constants.JSON_ID_KEY);

            Integer cls;
            try {
                cls = ngwClsToType(jsonObject.getString(Constants.JSON_CLS_KEY));
            } catch (JSONException e) {
                cls = Constants.NGWTYPE_UNKNOWN;
            }

            String displayName;
            try {
                displayName = jsonObject.getString(Constants.JSON_DISPLAY_NAME_KEY);
            } catch (JSONException e) {
                displayName = Constants.JSON_EMPTY_DISPLAY_NAME_VALUE;
            }

            NgwResource ngwResource = new NgwResource(mConnectionId, this, id, cls, displayName);
            ngwResource.setSelected(selectedResources.contains(ngwResource));

            this.add(ngwResource);
        }
    }

    public NgwResource sort() {

        Collections.sort(this, new Comparator<NgwResource>() {
            @Override
            public int compare(NgwResource lhs, NgwResource rhs) {
                if (lhs.mCls == Constants.NGWTYPE_PARENT_RESOURCE_GROUP) {
                    return -1;
                } else if (rhs.mCls == Constants.NGWTYPE_PARENT_RESOURCE_GROUP) {
                    return 1;

                } else if (lhs.mCls == Constants.NGWTYPE_RESOURCE_GROUP) {

                    if (rhs.mCls == Constants.NGWTYPE_RESOURCE_GROUP) {
                        return lhs.mDisplayName.compareTo(rhs.mDisplayName);
                    } else {
                        return -1;
                    }

                } else {

                    if (rhs.mCls == Constants.NGWTYPE_RESOURCE_GROUP) {
                        return 1;
                    } else {
                        return lhs.mDisplayName.compareTo(rhs.mDisplayName);
                    }
                }
            }
        });

        return this;
    }

    public Integer ngwClsToType(String cls) {
        if (cls.equals(Constants.JSON_RESOURCE_GROUP_VALUE)) {
            return Constants.NGWTYPE_RESOURCE_GROUP;
        } else if (cls.equals(Constants.JSON_POSTGIS_LAYER_VALUE)) {
            return Constants.NGWTYPE_POSTGIS_LAYER;
        } else if (cls.equals(Constants.JSON_WMSSERVER_SERVICE_VALUE)) {
            return Constants.NGWTYPE_WMSSERVER_SERVICE;
        } else if (cls.equals(Constants.JSON_BASELAYERS_VALUE)) {
            return Constants.NGWTYPE_BASELAYERS;
        } else if (cls.equals(Constants.JSON_POSTGIS_CONNECTION_VALUE)) {
            return Constants.NGWTYPE_POSTGIS_CONNECTION;
        } else if (cls.equals(Constants.JSON_WEBMAP_VALUE)) {
            return Constants.NGWTYPE_WEBMAP;
        } else if (cls.equals(Constants.JSON_WFSSERVER_SERVICE_VALUE)) {
            return Constants.NGWTYPE_WFSSERVER_SERVICE;
        } else if (cls.equals(Constants.JSON_VECTOR_LAYER_VALUE)) {
            return Constants.NGWTYPE_VECTOR_LAYER;
        } else if (cls.equals(Constants.JSON_RASTER_LAYER_VALUE)) {
            return Constants.NGWTYPE_RASTER_LAYER;
        } else if (cls.equals(Constants.JSON_VECTOR_STYLE_VALUE)) {
            return Constants.NGWTYPE_VECTOR_STYLE;
        } else if (cls.equals(Constants.JSON_RASTER_STYLE_VALUE)) {
            return Constants.NGWTYPE_RASTER_STYLE;
        } else if (cls.equals(Constants.JSON_FILE_BUCKET_VALUE)) {
            return Constants.NGWTYPE_FILE_BUCKET;
        } else if (cls.equals(Constants.JSON_PARENT_RESOURCE_GROUP_VALUE)) {
            return Constants.NGWTYPE_PARENT_RESOURCE_GROUP;
        } else {
            return Constants.NGWTYPE_UNKNOWN;
        }
    }

    @Override
    public int compareTo(NgwResource resource) {
        int isConnEq = mConnectionId.compareTo(resource.mConnectionId);

        if (isConnEq != 0) return isConnEq;

        int isIdEq;

        if (this.mId == null && resource.mId == null)
            isIdEq = 0;
        else if (this.mId == null/* && resource.mId != null*/)
            isIdEq = -1;
        else if (/*this.mId != null &&*/ resource.mId == null)
            isIdEq = 1;
        else /*if (this.mId != null && resource.mId != null)*/
            isIdEq = this.mId.compareTo(resource.mId);

        return isIdEq;
    }
}
