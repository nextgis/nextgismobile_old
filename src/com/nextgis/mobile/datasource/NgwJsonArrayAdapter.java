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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.nextgis.mobile.R;
import com.nextgis.mobile.util.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NgwJsonArrayAdapter extends BaseAdapter {

    protected JSONArray mJSONArray;

    protected Context mContext;
    protected LayoutInflater mLayoutInflater;


    public NgwJsonArrayAdapter(Context context, JSONArray jsonArray) {
        super();

        this.mJSONArray = jsonArray;
        this.mContext = context;
        this.mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mJSONArray.length();
    }

    @Override
    public Object getItem(int position) {
        try {
            return mJSONArray.getJSONObject(position);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.ngw_connections_row, parent, false);
        }

        // TODO: design (icons)

        TextView tvJsonName = (TextView) convertView.findViewById(R.id.tv_connection_name);
        String displayName = "-----";

        try {
            JSONObject resource = mJSONArray.getJSONObject(position).getJSONObject(Constants.JSON_RESOURCE_KEY);
            displayName = resource.getString(Constants.JSON_DISPLAY_NAME_KEY);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        tvJsonName.setText(displayName);
        return convertView;
    }
}
