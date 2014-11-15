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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.NgwResource;
import com.nextgis.mobile.util.Constants;

public class NgwJsonArrayAdapter extends BaseAdapter {

    protected NgwResource mNgwResources;

    protected Context mContext;
    protected LayoutInflater mLayoutInflater;


    protected ItemCheckedChangeListener mItemCheckedChangeListener;

    public interface ItemCheckedChangeListener {
        void onItemCheckedChange(NgwResource ngwResource, boolean isChecked);
    }

    public void setOnItemCheckedChangeListener(ItemCheckedChangeListener itemCheckedChangeListener) {
        mItemCheckedChangeListener = itemCheckedChangeListener;
    }


    public NgwJsonArrayAdapter(Context context, NgwResource ngwResources) {
        super();

        this.mNgwResources = ngwResources;
        this.mContext = context;
        this.mLayoutInflater =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mNgwResources.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.ngw_connections_row, parent, false);
        }

        ImageView ivJsonIcon = (ImageView) convertView.findViewById(R.id.iv_item_icon);
        TextView tvJsonName = (TextView) convertView.findViewById(R.id.tv_item_text);
        CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.check_box);

        NgwResource ngwResource = mNgwResources.get(position);

        if (position == 0) {
            ivJsonIcon.setImageResource(R.drawable.folder_up);
            checkBox.setVisibility(View.GONE);

        } else switch (ngwResource.getCls()) {

            case Constants.NGWTYPE_RESOURCE_GROUP:
                ivJsonIcon.setImageResource(R.drawable.folder);
                checkBox.setVisibility(View.GONE);
                break;
            case Constants.NGWTYPE_VECTOR_LAYER:
                ivJsonIcon.setImageResource(R.drawable.ngw);
                checkBox.setVisibility(View.VISIBLE);
                break;
            case Constants.NGWTYPE_RASTER_LAYER:
                ivJsonIcon.setImageResource(R.drawable.raster_bmp);
                checkBox.setVisibility(View.VISIBLE);
                break;
            default:
                ivJsonIcon.setImageResource(R.drawable.ic_action_cancel);
                checkBox.setVisibility(View.GONE);
                break;
        }

        tvJsonName.setText(ngwResource.getDisplayName());

        checkBox.setChecked(ngwResource.isSelected());
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemCheckedChangeListener != null) {
                    mItemCheckedChangeListener.onItemCheckedChange(
                            mNgwResources.get(position), ((CheckBox) v).isChecked());
                }
            }
        });

        return convertView;
    }
}
