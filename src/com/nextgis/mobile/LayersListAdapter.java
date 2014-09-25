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
package com.nextgis.mobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nextgis.mobile.map.Layer;
import com.nextgis.mobile.map.MapBase;

import static com.nextgis.mobile.util.Constants.*;

/**
 * An adapter to show layers as list
 */
public class LayersListAdapter extends BaseAdapter {

    protected MapBase mMap;

    public LayersListAdapter(MapBase map) {
        mMap = map;
    }

    @Override
    public int getCount() {
        return mMap.getLayers().size();
    }

    @Override
    public Object getItem(int i) {
        int nIndex = getCount() - 1 - i;
        return mMap.getLayers().get(nIndex);
    }

    @Override
    public long getItemId(int i) {
        Layer layer = (Layer) getItem(i);
        return layer.getId();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final Layer layer = (Layer) getItem(i);
        switch (layer.getType()){
            case LAYERTYPE_LOCAL_TMS:
            case LAYERTYPE_TMS:
            default:
                return getStandardLayerView(layer, view);
        }
    }

    protected View getStandardLayerView(final Layer layer, View view){
        View v = view;
        if(v == null){
            LayoutInflater inflater = LayoutInflater.from(mMap.getContext());
            v = inflater.inflate(R.layout.layer_row_layout, null);
        }

        ImageView ivIcon = (ImageView)v.findViewById(R.id.ivIcon);
        ivIcon.setImageDrawable(layer.getIcon());

        TextView tvPaneName = (TextView)v.findViewById(R.id.tvLayerName);
        tvPaneName.setText(layer.getName());

        //final int id = layer.getId();

        ImageButton btShow = (ImageButton)v.findViewById(R.id.btShow);
        //Log.d(TAG, "Layer #" + id + " is visible " + layer.isVisible());
        btShow.setBackgroundResource(layer.isVisible() ? R.drawable.ic_brightness_high : R.drawable.ic_bightness_low);
        //btShow.refreshDrawableState();
        btShow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                MapBase map = getMap();
                //Layer layer = map.getLayerById(id);
                layer.setVisible(!layer.isVisible());
            }
        });

        ImageButton btSettings = (ImageButton)v.findViewById(R.id.btSettings);
        btSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                MapBase map = getMap();
                //Layer layer = map.getLayerById(id);
                layer.changeProperties();
            }
        });

        ImageButton btDelete = (ImageButton)v.findViewById(R.id.btDelete);
        btDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                MapBase map = getMap();
                map.deleteLayerById(layer.getId());//.deleteLayerById(id);
            }
        });

        return v;
    }

    public MapBase getMap(){
        return mMap;
    }

}
