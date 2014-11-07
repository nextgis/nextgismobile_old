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
import android.widget.AdapterView.OnItemClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import com.nextgis.mobile.MainActivity;
import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.NgwConnection;
import com.nextgis.mobile.datasource.NgwJsonWorker;
import com.nextgis.mobile.map.MapBase;
import org.json.JSONArray;

import java.util.List;

public class NgwConnectionsDialog extends DialogFragment {

    protected MainActivity mainActivity;
    protected MapBase map;
    protected static ListView connectionsList;
    protected NgwJsonWorker mNgwJsonWorker;

    // TODO: save pointers by screen rotation on http loading
    // TODO: dialog design


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        map = mainActivity.getMap();
        final List<NgwConnection> ngwConnections = map.getNgwConnections();

        getDialog().setTitle(mainActivity.getString(R.string.ngw_connections));
        View view = inflater.inflate(R.layout.ngw_connections_dialog, container);

        ImageButton btnAddConnection = (ImageButton) view.findViewById(R.id.btn_add_connection);
        btnAddConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NgwAddConnectionDialog dialog = new NgwAddConnectionDialog();
                dialog.show(getActivity().getSupportFragmentManager(), "NgwAddConnectionDialog");
            }
        });

        connectionsList = (ListView) view.findViewById(R.id.ngw_connections_list);
        connectionsList.setAdapter(new NgwConnectionsListAdapter(mainActivity, ngwConnections));
        connectionsList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: ProgressBar on
                mNgwJsonWorker.loadNgwRootJsonArrayString(ngwConnections.get(position));
            }
        });
        connectionsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ngwConnections.remove(position);
                NgwJsonWorker.saveNgwConnections(map.getNgwConnections(), map.getMapPath());
                ((BaseAdapter) connectionsList.getAdapter()).notifyDataSetChanged();
                return true;
            }
        });

        mNgwJsonWorker = new NgwJsonWorker();
        mNgwJsonWorker.setJsonArrayLoadedListener(new NgwJsonWorker.JsonArrayLoadedListener() {
            @Override
            public void onJsonArrayLoaded(JSONArray jsonArray) {
                // TODO: work with jsonArray

                getDialog().setTitle(mainActivity.getString(R.string.ngw_layers));
                // TODO: ProgressBar off
            }
        });

        return view;
    }


    public static class NgwAddConnectionDialog extends DialogFragment {

        // TODO: scrolling view for screen rotation
        // TODO: field verifications
        // TODO: dialog design

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final MainActivity mainActivity = (MainActivity) getActivity();
            final MapBase map = mainActivity.getMap();
            final List<NgwConnection> connections = map.getNgwConnections();

            getDialog().setTitle(mainActivity.getString(R.string.add_ngw_connection));

            View view = inflater.inflate(R.layout.ngw_add_connection_dialog, container);
            final EditText edName = (EditText) view.findViewById(R.id.edName);
            final EditText edUrl = (EditText) view.findViewById(R.id.edUrl);
            final EditText edLogin = (EditText) view.findViewById(R.id.edLogin);
            final EditText edPassword = (EditText) view.findViewById(R.id.edPassword);

            Button btnOk = (Button) view.findViewById(R.id.btnOk);
            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connections.add(new NgwConnection(
                            edName.getText().toString(),
                            edUrl.getText().toString(),
                            edLogin.getText().toString(),
                            edPassword.getText().toString()
                    ));

                    NgwJsonWorker.saveNgwConnections(map.getNgwConnections(), map.getMapPath());
                    ((BaseAdapter) connectionsList.getAdapter()).notifyDataSetChanged();
                    dismiss();
                }
            });

            Button btnCancel = (Button) view.findViewById(R.id.btnCancel);
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });

            return view;
        }
    }
}
