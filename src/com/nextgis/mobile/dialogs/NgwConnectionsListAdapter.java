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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.NgwConnection;

import java.util.List;

public class NgwConnectionsListAdapter extends BaseAdapter {

    protected List<NgwConnection> mNgwConnections;

    protected Context mContext;
    protected LayoutInflater mLayoutInflater;


    public NgwConnectionsListAdapter(Context context, List<NgwConnection> connections) {
        super();

        this.mNgwConnections = connections;
        this.mContext = context;
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mNgwConnections.size();
    }

    @Override
    public Object getItem(int position) {
        return mNgwConnections.get(position);
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

        TextView tvConnectionName = (TextView) convertView.findViewById(R.id.tv_item_text);
        tvConnectionName.setText(mNgwConnections.get(position).getName());

        return convertView;
    }
}
