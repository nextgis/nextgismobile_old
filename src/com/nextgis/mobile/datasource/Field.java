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

import com.nextgis.mobile.util.GeoConstants;

public class Field {

    protected FieldKey mFieldKey;
    protected Object mFieldValue;
    protected boolean mIsEdited = false;


    public Field(FieldKey fieldKey, Object fieldValue) {
        mFieldKey = fieldKey;
        mFieldValue = fieldValue;
    }

    public FieldKey getFieldKey() {
        return mFieldKey;
    }

    public Object getFieldValue() {
        return mFieldValue;
    }

    public void setFieldValue(Object fieldValue) {
        mIsEdited = true;
        mFieldValue = fieldValue;
    }

    public boolean isFieldValueEdited() {
        return mIsEdited;
    }

    public int getType() {
        return mFieldKey.getType();
    }

    public void setFieldValue(String valueText) {
            Object newFieldValue = null;

            switch (mFieldKey.getType()) {
                case GeoConstants.FTInteger:
                    newFieldValue = Long.valueOf(valueText);
                    break;

                case GeoConstants.FTReal:
                    newFieldValue = Double.valueOf(valueText);
                    break;

                case GeoConstants.FTString:
                    newFieldValue = valueText;
                    break;

                case GeoConstants.FTDateTime:
                    break;

                case GeoConstants.FTIntegerList:
                case GeoConstants.FTRealList:
                case GeoConstants.FTStringList:
                case GeoConstants.FTBinary:
                    break;
            }

            setFieldValue(newFieldValue);
    }

    public String getFieldValueText() {
        String fieldValueText = "";

        switch (mFieldKey.getType()) {
            case GeoConstants.FTInteger:
                fieldValueText += ((Number) mFieldValue).longValue();
                break;

            case GeoConstants.FTReal:
                fieldValueText += ((Number) mFieldValue).doubleValue();
                break;

            case GeoConstants.FTString:
                fieldValueText += (String) mFieldValue;
                break;

            case GeoConstants.FTDateTime:
//                fieldValueText += ((Date) mFieldValue).toString();
                break;

            case GeoConstants.FTIntegerList:
            case GeoConstants.FTRealList:
            case GeoConstants.FTStringList:
            case GeoConstants.FTBinary:
                break;
        }

        return fieldValueText;
    }
}
