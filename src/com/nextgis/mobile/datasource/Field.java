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

import org.json.JSONException;
import org.json.JSONObject;

import static com.nextgis.mobile.util.Constants.*;

public class Field implements JSONStore{
    protected String mFieldName;
    protected String mFieldAlias;
    protected int mType;

    public Field(String fieldName, String fieldAlias, int type) {
        mFieldName = fieldName;
        if(fieldAlias.length() == 0)
            mFieldAlias = mFieldName;
        else
            mFieldAlias = fieldAlias;
        mType = type;
    }

    public String getFieldName() {
        return mFieldName;
    }

    public void setFieldName(String fieldName) {
        mFieldName = fieldName;
    }

    public String getFieldAlias() {
        return mFieldAlias;
    }

    public void setFieldAlias(String fieldAlias) {
        mFieldAlias = fieldAlias;
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        mType = type;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject oJSONBBox = new JSONObject();
        oJSONBBox.put(JSON_FIELD_NAME_KEY, getFieldName());
        oJSONBBox.put(JSON_FIELD_ALIAS_KEY, getFieldAlias());
        oJSONBBox.put(JSON_FIELD_TYPE_KEY, getType());
        return oJSONBBox;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONException{
        setFieldName(jsonObject.getString(JSON_FIELD_NAME_KEY));
        setFieldAlias(jsonObject.getString(JSON_FIELD_ALIAS_KEY));
        setType(jsonObject.getInt(JSON_FIELD_TYPE_KEY));
    }
}
