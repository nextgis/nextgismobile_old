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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
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
    protected static MapBase mMap;

    protected NgwJsonWorker mNgwJsonWorker;
    protected static List<NgwConnection> mNgwConnections;
    protected NgwConnection mCurrConn;
    protected JSONArray mCurrJsonArray = null;
    protected boolean mIsHttpRunning = false;

    protected TextView mDialogTitleText;
    protected LinearLayout mButtonBar;
    protected ImageButton mAddConnectionButton;
    protected ProgressBar mHttpProgressBar;

    protected static ListView mConnectionsList;
    protected NgwConnectionsListAdapter mConnectionsAdapter;
    protected AdapterView.OnItemClickListener mConnectionOnClickListener;
    protected AdapterView.OnItemLongClickListener mConnectionOnLongClickListener;

    // TODO: network cashing


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setStyle(STYLE_NO_TITLE, getTheme()); // remove title from DialogFragment

        mMainActivity = (MainActivity) getActivity();
        mMap = mMainActivity.getMap();
        mNgwConnections = mMap.getNgwConnections();

        mConnectionsAdapter = new NgwConnectionsListAdapter(mMainActivity, mNgwConnections);
        mConnectionOnClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mAddConnectionButton.setEnabled(false);
                mConnectionsList.setEnabled(false);

                mHttpProgressBar.setVisibility(View.VISIBLE);
                mIsHttpRunning = true;

                mCurrConn = mNgwConnections.get(position);
                mNgwJsonWorker.loadNgwRootJsonArrayString(mCurrConn);
            }
        };
        mConnectionOnLongClickListener = new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                NgwDeleteConnectionDialog.newInstance(position)
                        .show(getActivity().getSupportFragmentManager(), "NgwDeleteConnectionDialog");
                return true;
            }
        };

        mNgwJsonWorker = new NgwJsonWorker();
        mNgwJsonWorker.setJsonArrayLoadedListener(new NgwJsonWorker.JsonArrayLoadedListener() {
            @Override
            public void onJsonArrayLoaded(final JSONArray jsonArray) {
                mAddConnectionButton.setEnabled(true);
                mConnectionsList.setEnabled(true);

                mHttpProgressBar.setVisibility(View.INVISIBLE);
                mIsHttpRunning = false;

                mCurrJsonArray = jsonArray;

                try {
                    JSONObject jsonParentResource = new JSONObject();
                    jsonParentResource.put(Constants.JSON_CLS_KEY, Constants.JSON_PARENT_RESOURCE_GROUP_VALUE);
                    jsonParentResource.put(Constants.JSON_DISPLAY_NAME_KEY, "..");

                    JSONObject jsonParent = new JSONObject();
                    jsonParent.put(Constants.JSON_RESOURCE_KEY, jsonParentResource);
                    mCurrJsonArray.put(jsonParent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

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

        mDialogTitleText = (TextView) view.findViewById(R.id.dialog_title_text);

        mHttpProgressBar = (ProgressBar) view.findViewById(R.id.http_progress_bar);
        mHttpProgressBar.setVisibility(mIsHttpRunning ? View.VISIBLE : View.INVISIBLE);

        mButtonBar = (LinearLayout) view.findViewById(R.id.button_nar);

        mAddConnectionButton = (ImageButton) view.findViewById(R.id.btn_add_connection);
        mAddConnectionButton.setEnabled(!mIsHttpRunning);
        mAddConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NgwAddConnectionDialog dialog = new NgwAddConnectionDialog();
                dialog.show(getActivity().getSupportFragmentManager(), "NgwAddConnectionDialog");
            }
        });

        mConnectionsList = (ListView) view.findViewById(R.id.ngw_connections_list);
        mConnectionsList.setEnabled(!mIsHttpRunning);

        if (mCurrJsonArray == null) {
            setConnectionView();
        } else {
            setJsonView();
        }

        return view;
    }

    protected void setConnectionView() {
        mDialogTitleText.setText(mMainActivity.getString(R.string.ngw_connections));

        mButtonBar.setVisibility(View.VISIBLE);
        mAddConnectionButton.setVisibility(View.VISIBLE);

        mConnectionsList.setAdapter(mConnectionsAdapter);
        mConnectionsList.setOnItemClickListener(mConnectionOnClickListener);
        mConnectionsList.setOnItemLongClickListener(mConnectionOnLongClickListener);
    }

    protected void setJsonView() {
        // TODO: title as path
        mDialogTitleText.setText(mMainActivity.getString(R.string.ngw_layers));

        mButtonBar.setVisibility(View.GONE);
        mAddConnectionButton.setVisibility(View.GONE);

        NgwJsonArrayAdapter jsonArrayAdapter = new NgwJsonArrayAdapter(mMainActivity, mCurrJsonArray);
        jsonArrayAdapter.getFilter().filter(null);
        mConnectionsList.setAdapter(jsonArrayAdapter);

        mConnectionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                try {
                    if (position == 0) {
                        JSONObject resource = mCurrJsonArray.getJSONObject(0).getJSONObject(Constants.JSON_RESOURCE_KEY);

                        try {
                            JSONObject jsonParent = resource.getJSONObject(Constants.JSON_PARENT_KEY);

                            try {
                                jsonParent = jsonParent.getJSONObject(Constants.JSON_PARENT_KEY);

                                mConnectionsList.setEnabled(false);
                                mHttpProgressBar.setVisibility(View.VISIBLE);
                                mIsHttpRunning = true;

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

                    } else {
                        JSONObject resource = mCurrJsonArray.getJSONObject((int) id).getJSONObject(Constants.JSON_RESOURCE_KEY);

                        try {
                            int resourceId = resource.getInt(Constants.JSON_ID_KEY);

                            mConnectionsList.setEnabled(false);
                            mHttpProgressBar.setVisibility(View.VISIBLE);
                            mIsHttpRunning = true;

                            mNgwJsonWorker.loadNgwJsonArrayString(mCurrConn, resourceId);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mConnectionsList.setOnItemLongClickListener(null);
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

    public static class NgwDeleteConnectionDialog extends DialogFragment {

        int mIndex;


        static NgwDeleteConnectionDialog newInstance(int index) {
            NgwDeleteConnectionDialog dialog = new NgwDeleteConnectionDialog();

            // Supply index input as an argument.
            Bundle args = new Bundle();
            args.putInt("index", index);
            dialog.setArguments(args);

            return dialog;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            this.mIndex = getArguments().getInt("index");
        }

        @Override
        public void onDestroyView() {
            if (getDialog() != null && getRetainInstance())
                getDialog().setOnDismissListener(null);
            super.onDestroyView();
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.delete)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mNgwConnections.remove(mIndex);
                            NgwJsonWorker.saveNgwConnections(mMap.getNgwConnections(), mMap.getMapPath());
                            ((BaseAdapter) mConnectionsList.getAdapter()).notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .setMessage(String.format(getString(R.string.ngw_msg_delete_connection),
                            mNgwConnections.get(mIndex).getName()));
            return adb.create();
        }
    }
}
