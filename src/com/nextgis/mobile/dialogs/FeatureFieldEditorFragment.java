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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.nextgis.mobile.MainActivity;
import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.Feature;

public class FeatureFieldEditorFragment extends Fragment {

    protected Feature mEditFeature;
    protected int mMarginDP;
    protected int mMarginTop;


    protected OnEditFieldsListener mOnEditFieldsListener;


    public interface OnEditFieldsListener {
        void onEditFields();
    }

    public void setOnEditFieldsListener(OnEditFieldsListener onEditFieldsListener) {
        mOnEditFieldsListener = onEditFieldsListener;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

//        mEditFeature = ((MainActivity) getActivity()).getMap().getEditLayer().getEditFeature();
//        mEditFeature.getFieldKeys();

        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        int actionBarHeight = actionBar.getHeight();

        mMarginDP = (int) (15 * getActivity().getResources().getDisplayMetrics().density);
        mMarginTop = mMarginDP + actionBarHeight;
    }

/*
    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setOnDismissListener(null);
        super.onDestroyView();
    }
*/

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        MainActivity mainActivity = (MainActivity) getActivity();

        View view = inflater.inflate(R.layout.feature_field_editor, container, false);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        // TODO: instead mMarginTop use 0 for bottom in horizontal mode
        layoutParams.setMargins(mMarginDP, mMarginTop, mMarginDP, mMarginTop);

        ScrollView scrollView = (ScrollView) view.findViewById(R.id.field_editor_scroll);
        scrollView.setLayoutParams(layoutParams);

        LinearLayout fieldEditorLayout = (LinearLayout) view.findViewById(R.id.field_editor);

        for (int i = 0; i < 20; ++i) {
            final TextView fieldName = new TextView(mainActivity);
            fieldName.setText("Name:");

            final EditText fieldValue = new EditText(mainActivity);
            fieldValue.setText("Street Sea");

            fieldEditorLayout.addView(fieldName);
            fieldEditorLayout.addView(fieldValue);
        }

        return view;
    }
}
