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
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import com.nextgis.mobile.MainActivity;
import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.NgwConnection;
import com.nextgis.mobile.datasource.NgwJsonArrayAdapter;
import com.nextgis.mobile.datasource.NgwJsonWorker;
import com.nextgis.mobile.map.MapBase;
import com.nextgis.mobile.util.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class NgwConnectionsDialog extends DialogFragment {

    protected MainActivity mMainActivity;
    protected MapBase mMap;

    protected NgwJsonWorker mNgwJsonWorker;
    protected List<NgwConnection> mNgwConnections;
    protected NgwConnection mCurrConn;
    protected JSONArray mCurrJsonArray = null;
    protected boolean mIsHttpRunning = false;

    protected ImageButton mAddConnectionButton;
    protected ImageButton mToParentButton;
    protected ProgressBar mHttpProgressBar;

    protected static ListView mConnectionsList;
    protected NgwConnectionsListAdapter mConnectionsAdapter;
    protected AdapterView.OnItemClickListener mConnectionOnClickListener;
    protected AdapterView.OnItemLongClickListener mConnectionOnLongClickListener;

    // TODO: dialog design
    // TODO: network cashing


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mMainActivity = (MainActivity) getActivity();
        mMap = mMainActivity.getMap();
        mNgwConnections = mMap.getNgwConnections();

        mConnectionsAdapter = new NgwConnectionsListAdapter(mMainActivity, mNgwConnections);
        mConnectionOnClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mHttpProgressBar.setVisibility(View.VISIBLE);
                mIsHttpRunning = true;

                mCurrConn = mNgwConnections.get(position);
                mNgwJsonWorker.loadNgwRootJsonArrayString(mCurrConn);
            }
        };
        mConnectionOnLongClickListener = new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: message "Are you sure to delete connection mMap.getNgwConnections().get(position).getName() ?"
                mNgwConnections.remove(position);
                NgwJsonWorker.saveNgwConnections(mMap.getNgwConnections(), mMap.getMapPath());
                ((BaseAdapter) mConnectionsList.getAdapter()).notifyDataSetChanged();
                return true;
            }
        };

        mNgwJsonWorker = new NgwJsonWorker();
        mNgwJsonWorker.setJsonArrayLoadedListener(new NgwJsonWorker.JsonArrayLoadedListener() {
            @Override
            public void onJsonArrayLoaded(final JSONArray jsonArray) {
                mHttpProgressBar.setVisibility(View.INVISIBLE);
                mIsHttpRunning = false;

                mCurrJsonArray = jsonArray;
                setJsonView();
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setOnDismissListener(null);
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ngw_connections_dialog, container);

        mAddConnectionButton = (ImageButton) view.findViewById(R.id.btn_add_connection);
        mAddConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NgwAddConnectionDialog dialog = new NgwAddConnectionDialog();
                dialog.show(getActivity().getSupportFragmentManager(), "NgwAddConnectionDialog");
            }
        });

        mToParentButton = (ImageButton) view.findViewById(R.id.btn_to_parent);

        mHttpProgressBar = (ProgressBar) view.findViewById(R.id.http_progress_bar);
        mHttpProgressBar.setVisibility(mIsHttpRunning ? View.VISIBLE : View.INVISIBLE);

        mConnectionsList = (ListView) view.findViewById(R.id.ngw_connections_list);

        if (mCurrJsonArray == null) {
            setConnectionView();
        } else {
            setJsonView();
        }

        return view;
    }

    protected void setConnectionView() {
        getDialog().setTitle(mMainActivity.getString(R.string.ngw_connections));

        mAddConnectionButton.setVisibility(View.VISIBLE);
        mToParentButton.setVisibility(View.GONE);

        mConnectionsList.setAdapter(mConnectionsAdapter);
        mConnectionsList.setOnItemClickListener(mConnectionOnClickListener);
        mConnectionsList.setOnItemLongClickListener(mConnectionOnLongClickListener);
    }

    protected void setJsonView() {
        // TODO: title as path
        getDialog().setTitle(mMainActivity.getString(R.string.ngw_layers));

        mAddConnectionButton.setVisibility(View.GONE);
        mToParentButton.setVisibility(View.VISIBLE);

        NgwJsonArrayAdapter jsonArrayAdapter = new NgwJsonArrayAdapter(mMainActivity, mCurrJsonArray);
        jsonArrayAdapter.getFilter().filter(null);
        mConnectionsList.setAdapter(jsonArrayAdapter);

        mConnectionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mHttpProgressBar.setVisibility(View.VISIBLE);
                mIsHttpRunning = true;

                try {
                    JSONObject resource = mCurrJsonArray.getJSONObject((int) id).getJSONObject(Constants.JSON_RESOURCE_KEY);
                    int resourceId = resource.getInt(Constants.JSON_ID_KEY);
                    mNgwJsonWorker.loadNgwJsonArrayString(mCurrConn, resourceId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mConnectionsList.setOnItemLongClickListener(null);

        mToParentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    JSONObject resource = mCurrJsonArray.getJSONObject(0).getJSONObject(Constants.JSON_RESOURCE_KEY);

                    try {
                        JSONObject jsonParent = resource.getJSONObject(Constants.JSON_PARENT_KEY);

                        try {
                            mHttpProgressBar.setVisibility(View.VISIBLE);
                            mIsHttpRunning = true;

                            jsonParent = jsonParent.getJSONObject(Constants.JSON_PARENT_KEY);

                            try {
                                Integer parentId = jsonParent.getInt(Constants.JSON_ID_KEY);
                                mNgwJsonWorker.loadNgwJsonArrayString(mCurrConn, parentId);
                            } catch (JSONException e) {
                                mNgwJsonWorker.loadNgwRootJsonArrayString(mCurrConn);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    } catch (JSONException e) {
                        mCurrJsonArray = null;
                        setConnectionView();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public static class NgwAddConnectionDialog extends DialogFragment {

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
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final MainActivity mainActivity = (MainActivity) getActivity();
            final MapBase map = mainActivity.getMap();
            final List<NgwConnection> connections = map.getNgwConnections();

            getDialog().setTitle(mainActivity.getString(R.string.add_ngw_connection));

            View view = inflater.inflate(R.layout.ngw_add_connection_dialog, container);
            final EditText edName = (EditText) view.findViewById(R.id.ed_name);
            final EditText edUrl = (EditText) view.findViewById(R.id.ed_url);
            final EditText edLogin = (EditText) view.findViewById(R.id.ed_login);
            final EditText edPassword = (EditText) view.findViewById(R.id.ed_password);

            Button btnOk = (Button) view.findViewById(R.id.btn_ok);
            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String name = edName.getText().toString();
                    String url = edUrl.getText().toString();

                    if (name.length() == 0) {

                        if (url.length() > 0) {
                            name = url;
                        } else {
                            name = "-----";
                        }
                    }

                    connections.add(new NgwConnection(
                            name,
                            url,
                            edLogin.getText().toString(),
                            edPassword.getText().toString()
                    ));

                    NgwJsonWorker.saveNgwConnections(map.getNgwConnections(), map.getMapPath());
                    ((BaseAdapter) mConnectionsList.getAdapter()).notifyDataSetChanged();
                    dismiss();
                }
            });

            Button btnCancel = (Button) view.findViewById(R.id.btn_cancel);
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
