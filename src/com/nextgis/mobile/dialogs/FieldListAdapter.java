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
package com.nextgis.mobile.dialogs;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.Field;
import com.nextgis.mobile.datasource.FieldKey;
import com.nextgis.mobile.util.GeoConstants;

import java.util.List;

public class FieldListAdapter extends BaseAdapter {

    protected List<Field> mFields;

    protected Context mContext;
    protected LayoutInflater mLayoutInflater;


    public FieldListAdapter(Context context, List<Field> fields) {
        super();

        mContext = context;
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mFields = fields;
    }

    @Override
    public int getCount() {
        return mFields.size();
    }

    @Override
    public Object getItem(int position) {
        return mFields.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.field_list_row, parent, false);
        }

        Field field = mFields.get(position);
        FieldKey fieldKey = field.getFieldKey();
        Object fieldValue = field.getFieldValue();

        TextView fieldKeyTextView = (TextView) convertView.findViewById(R.id.field_key);
        fieldKeyTextView.setText(fieldKey.getFieldName() + ":");

        String fieldValueText = "";

        switch (fieldKey.getType()) {
            case GeoConstants.FTInteger:
                fieldValueText += ((Number) fieldValue).longValue();
                break;

            case GeoConstants.FTReal:
                fieldValueText += ((Number) fieldValue).doubleValue();
                break;

            case GeoConstants.FTString:
                fieldValueText += (String) fieldValue;
                break;

            case GeoConstants.FTDateTime:
//                fieldValueText += ((Date) fieldValue).toString();
//                fieldValueTextView.setInputType(InputType.TYPE_CLASS_DATETIME);
                break;

            case GeoConstants.FTIntegerList:
            case GeoConstants.FTRealList:
            case GeoConstants.FTStringList:
            case GeoConstants.FTBinary:
                break;
        }

        TextView fieldValueTextView = (TextView) convertView.findViewById(R.id.field_value);
        fieldValueTextView.setText(fieldValueText);

        return convertView;
    }
}
