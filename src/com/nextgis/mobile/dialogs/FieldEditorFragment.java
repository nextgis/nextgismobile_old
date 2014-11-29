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

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import com.nextgis.mobile.MainActivity;
import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.Feature;
import com.nextgis.mobile.datasource.Field;
import com.nextgis.mobile.map.LocalGeoJsonEditLayer;

import java.util.List;

public class FieldEditorFragment extends Fragment {

    protected LocalGeoJsonEditLayer mEditLayer;
    protected Feature mEditFeature;
    protected List<Field> mFields;

    protected int mMarginDP;
    protected int mMarginTop;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mEditLayer = ((MainActivity) getActivity()).getMap().getEditLayer();
        mEditFeature = mEditLayer.getEditFeature();
        mFields = mEditFeature.getFields();

        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        int actionBarHeight = actionBar.getHeight();

        mMarginDP = (int) (15 * getActivity().getResources().getDisplayMetrics().density);
        mMarginTop = mMarginDP + actionBarHeight;
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        MainActivity mainActivity = (MainActivity) getActivity();
        View view = inflater.inflate(R.layout.feature_field_editor, container, false);

        int marginBottom = mMarginDP;
        if (getActivity().getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT) marginBottom = mMarginTop;

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(mMarginDP, mMarginTop, mMarginDP, marginBottom);

        FrameLayout frame = (FrameLayout) view.findViewById(R.id.field_editor_frame);
        frame.setLayoutParams(layoutParams);

        ListView fieldListView = (ListView) view.findViewById(R.id.field_list_view);
        final FieldListAdapter fieldListAdapter = new FieldListAdapter(mainActivity, mFields);
        fieldListView.setAdapter(fieldListAdapter);

        fieldListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Field field = (Field) fieldListAdapter.getItem(position);

                FieldEditorDialog editDialog = new FieldEditorDialog();

                editDialog.setOnEditFieldListener(
                        new FieldEditorDialog.OnEditFieldListener() {
                            @Override
                            public void OnEditField(Object fieldValue) {
                                if (fieldValue == null) return;

                                field.setFieldValue(fieldValue);
                                fieldListAdapter.notifyDataSetChanged();
                            }
                        },
                        field);

                editDialog.show(getActivity().getSupportFragmentManager(), "FieldEditorDialog");
            }
        });

        return view;
    }

    public void saveEditedFields() {
        boolean isEdited = false;

        for (Field field : mFields) {
            if (field.isFieldValueEdited()) {
                isEdited = true;
                mEditFeature.setFieldValue(field.getFieldKey().getFieldName(), field.getFieldValue());
            }
        }

        if (isEdited) {
            mEditLayer.save();
        }
    }
}
