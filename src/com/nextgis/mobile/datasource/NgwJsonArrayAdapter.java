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
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import com.nextgis.mobile.R;
import com.nextgis.mobile.util.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NgwJsonArrayAdapter extends BaseAdapter implements Filterable {

    protected JSONArray mJSONArray;
    protected List<NgwJsonAttribute> mFilteredAttributeList;
    protected NgwJsonFilter mNgwJsonFilter;

    protected Context mContext;
    protected LayoutInflater mLayoutInflater;


    public NgwJsonArrayAdapter(Context context, JSONArray jsonArray) {
        super();

        this.mJSONArray = jsonArray;
        this.mContext = context;
        this.mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        getFilter();
    }

    @Override
    public int getCount() {
        if (mFilteredAttributeList != null) {
            return mFilteredAttributeList.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return mFilteredAttributeList.get(position).mJsonArrayIndex;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.ngw_connections_row, parent, false);
        }

        ImageView ivJsonIcon = (ImageView) convertView.findViewById(R.id.iv_item_icon);
        TextView tvJsonName = (TextView) convertView.findViewById(R.id.tv_item_text);
        CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.check_box);

        NgwJsonAttribute attribute = mFilteredAttributeList.get(position);

        if (position == 0) {
            ivJsonIcon.setImageResource(R.drawable.folder_up);
            checkBox.setVisibility(View.GONE);

        } else switch (attribute.mNgwJsonType) {

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

        tvJsonName.setText(attribute.mDisplayName);
        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (mNgwJsonFilter == null) {
            mNgwJsonFilter = new NgwJsonFilter();
        }

        return mNgwJsonFilter;
    }

    private class NgwJsonFilter extends Filter {

        //Invoked in a worker thread to filter the data according to the constraint.
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            List<NgwJsonAttribute> resultList = new ArrayList<NgwJsonAttribute>(mJSONArray.length());

            for (int i = 0; i < mJSONArray.length(); ++i) {
                NgwJsonAttribute attribute = new NgwJsonAttribute();

                JSONObject resource = null;
                try {
                    resource = mJSONArray.getJSONObject(i).getJSONObject(Constants.JSON_RESOURCE_KEY);
                } catch (JSONException e) {
                    e.printStackTrace();
                    results.count = 0;
                    results.values = null;
                    return results;
                }

                try {
                    attribute.mNgwJsonType =
                            NgwJsonWorker.ngwClsToType(resource.getString(Constants.JSON_CLS_KEY));
                } catch (JSONException e) {
                    attribute.mNgwJsonType = Constants.NGWTYPE_UNKNOWN;
                }

                try {
                    attribute.mDisplayName = resource.getString(Constants.JSON_DISPLAY_NAME_KEY);
                } catch (JSONException e) {
                    attribute.mDisplayName = "-----";
                }

                attribute.mJsonArrayIndex = i;

                resultList.add(attribute);
            }

            Collections.sort(resultList, new Comparator<NgwJsonAttribute>() {
                @Override
                public int compare(NgwJsonAttribute lhs, NgwJsonAttribute rhs) {
                    if (lhs.mNgwJsonType == Constants.NGWTYPE_PARENT_RESOURCE_GROUP) {
                        return -1;
                    } else if (rhs.mNgwJsonType == Constants.NGWTYPE_PARENT_RESOURCE_GROUP) {
                        return 1;

                    } else if (lhs.mNgwJsonType == Constants.NGWTYPE_RESOURCE_GROUP) {

                        if (rhs.mNgwJsonType == Constants.NGWTYPE_RESOURCE_GROUP) {
                            return lhs.mDisplayName.compareTo(rhs.mDisplayName);
                        } else {
                            return -1;
                        }

                    } else {

                        if (rhs.mNgwJsonType == Constants.NGWTYPE_RESOURCE_GROUP) {
                            return 1;
                        } else {
                            return lhs.mDisplayName.compareTo(rhs.mDisplayName);
                        }
                    }
                }
            });

            results.count = resultList.size();
            results.values = resultList;

            return results;
        }


        //Invoked in the UI thread to publish the filtering results in the user interface.
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mFilteredAttributeList = (List<NgwJsonAttribute>) results.values;
            notifyDataSetChanged();
        }
    }

    private class NgwJsonAttribute {
        public Integer mJsonArrayIndex;
        public Integer mNgwJsonType;
        public String mDisplayName;
    }
}
