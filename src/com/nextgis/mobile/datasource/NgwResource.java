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
    public Integer mId;
    public Integer mCls;
    public String mDisplayName;

    public NgwResource mParent;


    public NgwResource(NgwResource parent) {
        super();

        mParent = parent;

        if (parent == null) {
            mId = null;
        }
    }

    public boolean isRoot() {
        return mParent == null && mId == null;
    }

    public NgwResource getNgwResources(JSONArray jsonArray)
            throws JSONException {

        this.clear();

        for (int i = 0; i < jsonArray.length(); ++i) {
            // if error with JSON_RESOURCE_KEY -- throw JSONException
            JSONObject jsonObject =
                    jsonArray.getJSONObject(i).getJSONObject(Constants.JSON_RESOURCE_KEY);
            NgwResource ngwResource = new NgwResource(this);

            // if error with JSON_ID_KEY -- throw JSONException
            ngwResource.mId = jsonObject.getInt(Constants.JSON_ID_KEY);

            try {
                ngwResource.mCls =
                        NgwJsonWorker.ngwClsToType(jsonObject.getString(Constants.JSON_CLS_KEY));
            } catch (JSONException e) {
                ngwResource.mCls = Constants.NGWTYPE_UNKNOWN;
            }

            try {
                ngwResource.mDisplayName = jsonObject.getString(Constants.JSON_DISPLAY_NAME_KEY);
            } catch (JSONException e) {
                ngwResource.mDisplayName = Constants.JSON_EMPTY_DISPLAY_NAME_VALUE;
            }

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
