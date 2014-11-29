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
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.Field;
import com.nextgis.mobile.datasource.FieldKey;
import com.nextgis.mobile.util.GeoConstants;

public class FieldEditorDialog extends DialogFragment {

    protected Field mField;
    protected OnEditFieldListener mOnEditFieldListener;


    public interface OnEditFieldListener {
        void OnEditField(Object fieldValue);
    }

    public void setOnEditFieldListener(OnEditFieldListener onEditFieldListener, Field field) {
        mOnEditFieldListener = onEditFieldListener;
        mField = field;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setOnDismissListener(null);
        super.onDestroyView();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.field_editor_dialog, container);

        if (mField != null) {

            FieldKey fieldKey = mField.getFieldKey();
            Object fieldValue = mField.getFieldValue();

            getDialog().setTitle(getActivity().getString(R.string.field_value_editing));

            final TextView fieldKeyView = (TextView) view.findViewById(R.id.field_key_f_ed);
            fieldKeyView.setText(fieldKey.getFieldName() + ":");

            final EditText fieldValueEditor = (EditText) view.findViewById(R.id.field_value_f_ed);
            String fieldValueText = "";
            final int fieldType = fieldKey.getType();

            switch (fieldType) {
                case GeoConstants.FTInteger:
                    fieldValueText += ((Number) fieldValue).longValue();
                    fieldValueEditor.setInputType(
                            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    break;

                case GeoConstants.FTReal:
                    fieldValueText += ((Number) fieldValue).doubleValue();
                    fieldValueEditor.setInputType(
                            InputType.TYPE_CLASS_NUMBER
                                    | InputType.TYPE_NUMBER_FLAG_DECIMAL
                                    | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    break;

                case GeoConstants.FTString:
                    fieldValueText += (String) fieldValue;
                    fieldValueEditor.setInputType(InputType.TYPE_CLASS_TEXT);
                    break;

                case GeoConstants.FTDateTime:
//                    fieldValueText += ((Date) fieldValue).toString();
//                    fieldValueEditor.setInputType(InputType.TYPE_CLASS_DATETIME);
                    getDialog().setTitle(getActivity().getString(R.string.editing_is_not_possible));
                    fieldValueEditor.setEnabled(false);
                    break;

                case GeoConstants.FTIntegerList:
                case GeoConstants.FTRealList:
                case GeoConstants.FTStringList:
                case GeoConstants.FTBinary:
                    getDialog().setTitle(getActivity().getString(R.string.editing_is_not_possible));
                    fieldValueEditor.setEnabled(false);
                    break;
            }

            fieldValueEditor.setText(fieldValueText);

            ImageButton btnOk = (ImageButton) view.findViewById(R.id.btn_ok_field_ed);
            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnEditFieldListener != null) {
                        String valueText = fieldValueEditor.getText().toString();
                        Object newFieldValue = null;

                        switch (fieldType) {
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
                                dismiss();
                                return;

                            case GeoConstants.FTIntegerList:
                            case GeoConstants.FTRealList:
                            case GeoConstants.FTStringList:
                            case GeoConstants.FTBinary:
                                dismiss();
                                return;
                        }

                        mOnEditFieldListener.OnEditField(newFieldValue);
                    }

                    dismiss();
                }
            });

        } else getDialog().setTitle("ERROR!!!");

        ImageButton btnCancel = (ImageButton) view.findViewById(R.id.btn_cancel_field_ed);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return view;
    }
}
