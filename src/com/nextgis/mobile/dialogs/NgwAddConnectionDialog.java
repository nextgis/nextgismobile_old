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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.NgwConnection;
import com.nextgis.mobile.util.Constants;

public class NgwAddConnectionDialog extends DialogFragment {

    protected boolean mIsEditConnectionView = false;
    protected NgwConnection mNgwConnection;
    protected OnAddConnectionListener mOnAddConnectionListener;
    protected OnEditConnectionListener mOnEditConnectionListener;


    public interface OnAddConnectionListener {
        void onAddConnection(NgwConnection connection);
    }

    public void setOnAddConnectionListener(OnAddConnectionListener onAddConnectionListener) {
        mIsEditConnectionView = false;
        mOnAddConnectionListener = onAddConnectionListener;
        mOnEditConnectionListener = null;
    }

    public interface OnEditConnectionListener {
        void onEditConnection(NgwConnection connection);
    }

    public void setOnEditConnectionListener(OnEditConnectionListener onEditConnectionListener,
                                            NgwConnection connection) {
        mIsEditConnectionView = true;
        mOnEditConnectionListener = onEditConnectionListener;
        mNgwConnection = connection;
        mOnAddConnectionListener = null;
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

        getDialog().setTitle(getActivity().getString(mIsEditConnectionView
                ? R.string.edit_ngw_connection : R.string.add_ngw_connection));

        View view = inflater.inflate(R.layout.ngw_add_connection_dialog, container);
        final EditText edName = (EditText) view.findViewById(R.id.ed_name);
        final EditText edUrl = (EditText) view.findViewById(R.id.ed_url);
        final EditText edLogin = (EditText) view.findViewById(R.id.ed_login);
        final EditText edPassword = (EditText) view.findViewById(R.id.ed_password);

        if (mIsEditConnectionView) {
            edName.setText(mNgwConnection.getName());
            edUrl.setText(mNgwConnection.getUrl());
            edLogin.setText(mNgwConnection.getLogin());
            edPassword.setText(mNgwConnection.getPassword());
        }

        ImageButton btnOk = (ImageButton) view.findViewById(R.id.btn_ok_add_conn);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = edName.getText().toString();
                String url = edUrl.getText().toString();
                String login = edLogin.getText().toString();
                String password = edPassword.getText().toString();

                if (!url.substring(url.length()-1).equals("/")) {
                    url += "/";
                }

                if (name.length() == 0) {

                    if (url.length() > 0) {
                        name = url;
                    } else {
                        name = Constants.JSON_EMPTY_DISPLAY_NAME_VALUE;
                    }
                }


                if (mOnAddConnectionListener != null) {
                    NgwConnection connection = new NgwConnection(name, url, login, password);
                    mOnAddConnectionListener.onAddConnection(connection);
                }

                if (mOnEditConnectionListener != null) {
                    mNgwConnection.setName(name);
                    mNgwConnection.setUrl(url);
                    mNgwConnection.setLogin(login);
                    mNgwConnection.setPassword(password);

                    mOnEditConnectionListener.onEditConnection(mNgwConnection);
                }

                dismiss();
            }
        });

        ImageButton btnCancel = (ImageButton) view.findViewById(R.id.btn_cancel_add_conn);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return view;
    }
}
