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
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.nextgis.mobile.MainActivity;
import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.Feature;
import com.nextgis.mobile.datasource.Field;
import com.nextgis.mobile.datasource.FieldList;
import com.nextgis.mobile.map.GeoJsonLayer;
import com.nextgis.mobile.map.LocalGeoJsonEditLayer;
import com.nextgis.mobile.map.MapViewEditable;
import com.nextgis.mobile.util.Constants;
import com.nextgis.mobile.util.GeoConstants;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FieldEditorFragment extends Fragment {

    protected MainActivity mMainActivity;
    protected GeoJsonLayer mLayer;
    protected Feature mFeature;
    protected FieldList mFields;

    protected boolean mIsEditMode = false;

    protected TextView mTitle;
    protected LinearLayout mXmlFormLayout;

    protected int mMarginDP;
    protected int mMarginTop;

    protected File mXmlFormFile = null;
    protected boolean mHasXmlFormFile = false;


    protected List<EditorEventListener> mEditorEventListeners;

    public interface EditorEventListener {
        public abstract void onSetEditMode(boolean isEditMode);

        public abstract void onSaveEditedFields();
    }

    public void addFieldEventListener(EditorEventListener listener) {
        if (mEditorEventListeners != null) mEditorEventListeners.add(listener);
    }


    public boolean isEditMode() {
        return mIsEditMode;
    }

    public void setEditMode() {
        mIsEditMode = true;
        mTitle.setText(getActivity().getString(R.string.editing_of_feature_properties));

        if (mEditorEventListeners != null) {
            for (EditorEventListener listener : mEditorEventListeners) {
                listener.onSetEditMode(mIsEditMode);
            }
        }
    }

    public void saveEditedFields() {
        boolean isEdited = false;

        if (mEditorEventListeners != null) {
            for (EditorEventListener listener : mEditorEventListeners) {
                listener.onSaveEditedFields();
            }
        }

        for (Field field : mFields) {
            if (field.isFieldValueEdited()) {
                isEdited = true;
                mFeature.setFieldValue(field.getFieldKey().getFieldName(), field.getFieldValue());
            }
        }

        if (isEdited) {
            mLayer.save();
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        int actionBarHeight = actionBar.getHeight();

        mMarginDP = (int) (15 * getActivity().getResources().getDisplayMetrics().density);
        mMarginTop = mMarginDP + actionBarHeight;
        mEditorEventListeners = new ArrayList<EditorEventListener>();
    }

    public void setParams(MapViewEditable map, GeoJsonLayer layer,
                          Feature feature, boolean isForEditLayer) {

        String relativePath = "";

        if (isForEditLayer) {
            LocalGeoJsonEditLayer editLayer = map.getEditLayer();
            relativePath = map.getLayerByName(editLayer.getEditableLayerName()).getRelativePath();

            mIsEditMode = true;
            mLayer = editLayer;
            mFeature = editLayer.getEditFeature();

        } else {
            mIsEditMode = false;
            mLayer = layer;
            mFeature = feature;
            relativePath = mLayer.getRelativePath();
        }

        mFields = mFeature.getFields();

        String xmlFormPath = map.getMapPath() + File.separator
                + relativePath + File.separator + Constants.FORM_XML;
        mXmlFormFile = new File(xmlFormPath);

        if (mXmlFormFile.exists()) mHasXmlFormFile = true;
        else mXmlFormFile = null;
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mMainActivity = (MainActivity) getActivity();
        View view = inflater.inflate(R.layout.feature_field_editor, container, false);

        int marginBottom = mMarginDP;
        if (getActivity().getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT) marginBottom = mMarginTop;

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(mMarginDP, mMarginTop, mMarginDP, marginBottom);

        mTitle = (TextView) view.findViewById(R.id.field_editor_title);
        mTitle.setText(mIsEditMode
                ? mMainActivity.getString(R.string.editing_of_feature_properties)
                : mMainActivity.getString(R.string.feature_properties));

        FrameLayout frame = (FrameLayout) view.findViewById(R.id.field_editor_frame);
        frame.setLayoutParams(layoutParams);

        mXmlFormLayout = (LinearLayout) view.findViewById(R.id.field_form_xml_layout);

        if (mHasXmlFormFile) {
            xmlFormParser();

        } else {
            for (Field field : mFields) {
                TextView textView = new TextView(mXmlFormLayout.getContext());
                textView.setTextSize(16);
                textView.setText(field.getFieldKey().getFieldName());

                FormEditText editText = new FormEditText(mXmlFormLayout.getContext());
                editText.setField(field);
                editText.setTextSize(20);
                editText.setEnabled(mIsEditMode);
                editText.setText(field.getFieldValueText());

                int inputType = InputType.TYPE_CLASS_TEXT;

                switch (field.getType()) {
                    case GeoConstants.FTInteger:
                        inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
                        break;

                    case GeoConstants.FTReal:
                        inputType = InputType.TYPE_CLASS_NUMBER
                                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                                | InputType.TYPE_NUMBER_FLAG_SIGNED;
                        break;

                    case GeoConstants.FTString:
                        inputType = InputType.TYPE_CLASS_TEXT;
                        break;

                    case GeoConstants.FTDateTime:
                        editText.setEnabled(false);
                        break;

                    case GeoConstants.FTIntegerList:
                    case GeoConstants.FTRealList:
                    case GeoConstants.FTStringList:
                    case GeoConstants.FTBinary:
                        editText.setEnabled(false);
                        break;
                }

                editText.setInputType(inputType);

                mXmlFormLayout.addView(textView);
                mXmlFormLayout.addView(editText);
            }
        }

        return view;
    }

    public void xmlFormParser() {

        BufferedReader formReader;

        try {
            formReader = new BufferedReader(new FileReader(mXmlFormFile));
        } catch (FileNotFoundException e) {
//            e.printStackTrace();
            formReader = null;
        }


        // get factory
        //XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        // turn on support of namespace (default is off)
        //factory.setNamespaceAware(true);


        // create parser
        XmlPullParser xppForm = null;

        if (formReader != null) {

            try {
                xppForm = XmlPullParserFactory.newInstance().newPullParser();

                // reader to input of parser
                xppForm.setInput(formReader);

            } catch (XmlPullParserException e) {

                xppForm = null;

                // TODO
                //e.printStackTrace();
            }
        }


        if (xppForm != null) {

            try {
                TextView textView = null;
                FormEditText editText = null;
                int currentType = Constants.FORM_TYPE_TEXT_LABEL;

                while (xppForm.getEventType() != XmlPullParser.END_DOCUMENT) {
                    switch (xppForm.getEventType()) {

                        // begin of doc
                        //case XmlPullParser.START_DOCUMENT:
                        //    Log.d(LOG_TAG, "START_DOCUMENT");
                        //    break;

                        // begin of tag
                        case XmlPullParser.START_TAG:
                            switch (xppForm.getDepth()) {
                                case 1:
                                    if (xppForm.getName().equals("Form")) {

                                    }
                                    break;

                                case 2:
                                    if (xppForm.getName().equals("Tab")) {

                                    }
                                    break;

                                case 3:
                                    if (xppForm.getName().equals("Portrait")) {

                                    }
                                    break;

                                case 4:
                                    if (xppForm.getName().equals("Element")) {

                                        for (int i = 0; i < xppForm.getAttributeCount(); i++) {

                                            if (xppForm.getAttributeName(i).equals("Type")) {
                                                if (xppForm.getAttributeValue(i)
                                                        .equals("text_label")) {

                                                    currentType = Constants.FORM_TYPE_TEXT_LABEL;
                                                    textView = new TextView(
                                                            mXmlFormLayout.getContext());
                                                    textView.setTextSize(16);
                                                }

                                                if (xppForm.getAttributeValue(i)
                                                        .equals("text_edit")) {

                                                    currentType = Constants.FORM_TYPE_TEXT_EDIT;
                                                    editText = new FormEditText(
                                                            mXmlFormLayout.getContext());
                                                    editText.setEnabled(mIsEditMode);
                                                    editText.setTextSize(20);
                                                }
                                            }
                                        }
                                    }
                                    break;

                                case 5:
                                    if (xppForm.getName().equals("Property")) {
                                        String name = "", alias = "", value = "";

                                        for (int i = 0; i < xppForm.getAttributeCount(); i++) {

                                            if (xppForm.getAttributeName(i).equals("Name")) {
                                                name = xppForm.getAttributeValue(i);
                                            }

                                            if (xppForm.getAttributeName(i).equals("Alias")) {
                                                alias = xppForm.getAttributeValue(i);
                                            }

                                            if (xppForm.getAttributeName(i).equals("Value")) {
                                                value = xppForm.getAttributeValue(i);
                                            }
                                        }

                                        switch (currentType) {
                                            case Constants.FORM_TYPE_TEXT_LABEL:
                                                if (textView != null && name.equals("caption")) {
                                                    textView.setText(value);
                                                }
                                                break;

                                            case Constants.FORM_TYPE_TEXT_EDIT:
                                                if (editText != null) {
                                                    if (name.equals("field")) {
                                                        editText.setField(mFields.get(value));
                                                    }

                                                    if (name.equals("default")) {
                                                        editText.setText(value);
                                                    }

                                                    if (name.equals("only_figures")) {
                                                        editText.setInputType(
                                                                InputType.TYPE_CLASS_NUMBER
                                                                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
                                                                        | InputType.TYPE_NUMBER_FLAG_SIGNED);
                                                    }

                                                    if (name.equals("max_char_count")) {
                                                        // TODO: maxLength
                                                    }
                                                }
                                                break;
                                        }
                                    }
                                    break;

                                default:
                                    break;
                            }

                            break;

                        // end of tag
                        case XmlPullParser.END_TAG:
                            switch (xppForm.getDepth()) {
                                case 1:
                                    if (xppForm.getName().equals("Form")) {

                                    }
                                    break;

                                case 2:
                                    if (xppForm.getName().equals("Tab")) {

                                    }
                                    break;

                                case 3:
                                    if (xppForm.getName().equals("Portrait")) {

                                    }
                                    break;

                                case 4:
                                    if (xppForm.getName().equals("Element")) {

                                        switch (currentType) {
                                            case Constants.FORM_TYPE_TEXT_LABEL:
                                                if (textView != null)
                                                    mXmlFormLayout.addView(textView);
                                                break;

                                            case Constants.FORM_TYPE_TEXT_EDIT:
                                                if (editText != null)
                                                    mXmlFormLayout.addView(editText);
                                                break;
                                        }
                                    }
                                    break;

                                case 5:
                                    if (xppForm.getName().equals("Property")) {

                                    }
                                    break;

                                default:
                                    break;
                            }
                            break;

                        // content of tag
                        //case XmlPullParser.TEXT:
                        //    Log.d(LOG_TAG, "text = " + xppForm.getText());
                        //    break;

                        default:
                            break;
                    }

                    // next element
                    xppForm.next();
                }

                //Log.d(LOG_TAG, "END_DOCUMENT");

            } catch (XmlPullParserException e) {

                // TODO
                //e.printStackTrace();
            } catch (IOException e) {

                // TODO
                //e.printStackTrace();
            }
        }
    }


    protected class FormEditText extends EditText {

        protected Field mField;


        public FormEditText(Context context) {
            super(context);

            addFieldEventListener(new EditorEventListener() {
                @Override
                public void onSetEditMode(boolean isEditMode) {
                    setEditMode(isEditMode);
                }

                @Override
                public void onSaveEditedFields() {
                    setFieldValue();
                }
            });
        }

        public void setField(Field field) {
            mField = field;
        }

        @Override
        protected void onWindowVisibilityChanged(int visibility) {

            if (mField != null) {

                switch (visibility) {
                    case View.VISIBLE:
                        setText(mField.getFieldValueText());
                        setParameters();
                        break;

                    case View.INVISIBLE:
                    case View.GONE:
                        setFieldValue();
                        break;
                }
            }

            super.onWindowVisibilityChanged(visibility);
        }

        protected void setParameters() {

            if (mField == null) return;

            switch (mField.getType()) {
                case GeoConstants.FTInteger:
                    setInputType(
                            InputType.TYPE_CLASS_NUMBER
                                    | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    break;

                case GeoConstants.FTReal:
                    setInputType(
                            InputType.TYPE_CLASS_NUMBER
                                    | InputType.TYPE_NUMBER_FLAG_DECIMAL
                                    | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    break;

                case GeoConstants.FTString:
                    setInputType(InputType.TYPE_CLASS_TEXT);
                    break;

                case GeoConstants.FTDateTime:
//                    setInputType(InputType.TYPE_CLASS_DATETIME);
                    setEnabled(false);
                    break;

                case GeoConstants.FTIntegerList:
                case GeoConstants.FTRealList:
                case GeoConstants.FTStringList:
                case GeoConstants.FTBinary:
                    setEnabled(false);
                    break;
            }
        }

        public void setFieldValue() {
            if (mField != null) mField.setFieldValue(getText().toString());
        }

        public void setEditMode(boolean isEditMode) {
            if (mField != null) setEnabled(isEditMode);
        }
    }
}
