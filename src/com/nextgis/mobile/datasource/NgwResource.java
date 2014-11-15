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

public class NgwResource extends ArrayList<NgwResource> {
    protected NgwResource mParent;
    protected Integer mId;
    protected Integer mCls;
    protected String mDisplayName;

    protected boolean mIsSelected;


    public NgwResource() {
        super();

        mParent = null;
        mId = null;
        mCls = null;
        mDisplayName = null;
        mIsSelected = false;
    }

    public NgwResource(NgwResource parent, Integer id, Integer cls, String displayName) {
        super();

        mParent = parent;
        mId = id;
        mCls = cls;
        mDisplayName = displayName;
        mIsSelected = false;
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

    public boolean isRoot() {
        return mParent == null && mId == null;
    }

    public NgwResource getNgwResources(JSONArray jsonArray)
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
                cls = NgwJsonWorker.ngwClsToType(jsonObject.getString(Constants.JSON_CLS_KEY));
            } catch (JSONException e) {
                cls = Constants.NGWTYPE_UNKNOWN;
            }

            String displayName;
            try {
                displayName = jsonObject.getString(Constants.JSON_DISPLAY_NAME_KEY);
            } catch (JSONException e) {
                displayName = Constants.JSON_EMPTY_DISPLAY_NAME_VALUE;
            }

            NgwResource ngwResource = new NgwResource(this, id, cls, displayName);
            this.add(ngwResource);
        }

        return this;
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
}
