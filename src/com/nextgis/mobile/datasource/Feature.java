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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.nextgis.mobile.util.GeoConstants.*;
import static com.nextgis.mobile.util.Constants.*;

public class Feature implements JSONStore {
    protected GeoGeometry mGeometry;
    protected List<Object> mFieldData;
    protected List<Field> mFields;

    public Feature(List<Field> fields) {
        mFields = fields;
        mFieldData = new ArrayList<Object>(mFields.size());
    }

    public void setGeometry(GeoGeometry geometry){
        mGeometry = geometry;
    }

    public GeoGeometry getGeometry(){
        return mGeometry;
    }

    public boolean setField(int index, Object value){
        if(index < 0 || index >= mFields.size())
            return false;
        if(mFieldData.size() <= index){
            for(int i = mFieldData.size(); i <= index; i++){
                mFieldData.add(null);
            }
        }

        mFieldData.set(index, value);
        return true;
    }

    public boolean setField(String fieldName, Object value){
        int index = getFieldIndex(fieldName);
        return setField(index, value);
    }

    public int getFieldIndex(String fieldName){
        for(int i = 0; i < mFields.size(); i++){
            if(mFields.get(i).getFieldName().equals(fieldName))
                return i;
        }
        return NOT_FOUND;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject oJSONOut = new JSONObject();
        oJSONOut.put(GEOJSON_TYPE, GEOJSON_TYPE_Feature);
        oJSONOut.put(GEOJSON_GEOMETRY, mGeometry.toJSON());
        JSONObject oJSONProp = new JSONObject();

        for(int i = 0; i < mFieldData.size(); i++){
            String key = mFields.get(i).getFieldName();
            oJSONProp.put(key, mFieldData.get(i));
        }
        oJSONOut.put(GEOJSON_PROPERTIES, oJSONProp);
        return oJSONOut;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONException {
        if(!jsonObject.getString(GEOJSON_TYPE).equals(GEOJSON_TYPE_Feature))
            throw new JSONException("not valid geojson feature");
        JSONObject oJSONGeom = jsonObject.getJSONObject(GEOJSON_GEOMETRY);
        mGeometry = GeoGeometry.fromJson(oJSONGeom);
        JSONObject jsonAttributes = jsonObject.getJSONObject(GEOJSON_PROPERTIES);
        Iterator<String> iter = jsonAttributes.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            Object value = jsonAttributes.get(key);

            mFieldData.add(value);
        }
    }
}
