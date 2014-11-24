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

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.nextgis.mobile.MainActivity;
import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.NgwConnection;
import com.nextgis.mobile.datasource.NgwConnectionList;
import com.nextgis.mobile.datasource.NgwConnectionWorker;
import com.nextgis.mobile.datasource.NgwResource;
import com.nextgis.mobile.map.MapBase;
import com.nextgis.mobile.map.NgwRasterLayer;
import com.nextgis.mobile.map.NgwVectorLayer;
import com.nextgis.mobile.util.Constants;
import com.nextgis.mobile.util.GeoConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class NgwResourcesDialog extends DialogFragment {

    protected MainActivity mMainActivity;
    protected MapBase mMap;

    protected NgwConnectionList mNgwConnections;
    protected NgwConnection mCurrConn;
    protected NgwConnectionWorker mNgwConnWorker;
    protected boolean mIsHttpRunning;

    protected NgwResourceRoots mNgwResRoots;
    protected NgwResource mCurrNgwRes;
    protected Set<NgwResource> mSelectedResources;
    protected Iterator<NgwResource> mSelResIterator;

    protected TextView mDialogTitleText;
    protected RelativeLayout mButtonBar;
    protected ImageButton mAddConnectionButton;
    protected ImageButton mOkButton;
    protected ImageButton mCancelButton;
    protected ProgressBar mHttpProgressBar;
    protected boolean mIsConnectionView;

    protected ListView mResourceList;
    protected NgwConnectionsListAdapter mConnectionsAdapter;
    protected AdapterView.OnItemClickListener mConnectionOnClickListener;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setStyle(STYLE_NO_TITLE, getTheme()); // remove title from DialogFragment

        mMainActivity = (MainActivity) getActivity();
        mMap = mMainActivity.getMap();
        mNgwConnections = mMap.getNgwConnections();

        mNgwResRoots = new NgwResourceRoots();
        for (int i = 0, connectionsSize = mNgwConnections.size(); i < connectionsSize; i++) {
            NgwConnection connection = mNgwConnections.get(i);
            mNgwResRoots.add(new NgwResource(connection.getId()));
        }

        mSelectedResources = new TreeSet<NgwResource>();
        mCurrNgwRes = null;

        mIsConnectionView = true;
        mIsHttpRunning = false;

        mConnectionsAdapter = new NgwConnectionsListAdapter(mMainActivity, mNgwConnections);
        mConnectionOnClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mCurrConn = mNgwConnections.get(position);
                mCurrNgwRes = mNgwResRoots.get(mCurrConn.getId());

                if (mCurrNgwRes.size() == 0) {
                    setHttpRunningView(true);
                    mCurrConn.setLoadResourceArray(mCurrNgwRes);
                    mNgwConnWorker.loadNgwJson(mCurrConn);

                } else {
                    setJsonView();
                }

            }
        };

        mNgwConnWorker = new NgwConnectionWorker();

        mNgwConnWorker.setJsonArrayLoadedListener(
                new NgwConnectionWorker.JsonArrayLoadedListener() {
                    @Override
                    public void onJsonArrayLoaded(final JSONArray jsonArray) {
                        if (jsonArray == null) {
                            Toast.makeText(mMap.getContext(),
                                    mMainActivity.getString(R.string.connection_error),
                                    Toast.LENGTH_LONG).show();
                            setHttpRunningView(false);
                            return;
                        }

                        try {
                            mCurrNgwRes.addNgwResourcesFromJSONArray(jsonArray, mSelectedResources);

                        } catch (JSONException e) {
                            String error = "Error by trying to add loaded resources to "
                                    + mCurrNgwRes.getDisplayName() + "\n" + e.getLocalizedMessage();
                            Log.w(Constants.TAG, error);
                            Toast.makeText(mMainActivity, error, Toast.LENGTH_LONG).show();
                        }

                        Integer cls = mCurrNgwRes.getCls();
                        if (cls == null) cls = Constants.NGWTYPE_RESOURCE_GROUP;

                        switch (cls) {
                            case Constants.NGWTYPE_RESOURCE_GROUP:
                                // Adding link to parent ("..")
                                // to position 0 (after sorting) of mResourceList
                                NgwResource ngwResource = new NgwResource(
                                        mCurrConn.getId(),
                                        mCurrNgwRes.getParent(),
                                        mCurrNgwRes.getId(),
                                        Constants.NGWTYPE_PARENT_RESOURCE_GROUP,
                                        Constants.JSON_PARENT_DISPLAY_NAME_VALUE);

                                mCurrNgwRes.add(ngwResource);
                                mCurrNgwRes.sort();

                                setHttpRunningView(false);
                                setJsonView();
                                break;

                            case Constants.NGWTYPE_RASTER_LAYER:
                                if (mCurrNgwRes.size() > 0) {
                                    String displayName = mCurrNgwRes.getDisplayName();
                                    mCurrNgwRes = mCurrNgwRes.get(0);

                                    try {
                                        new NgwRasterLayer(mCurrConn.getId(), mCurrNgwRes.getId())
                                                .create(mMap, displayName,
                                                        mCurrConn.getTmsUrl(mCurrNgwRes.getId()),
                                                        GeoConstants.TMSTYPE_OSM);

                                    } catch (JSONException e) {
                                        String error = "Error in " + mCurrNgwRes.getDisplayName()
                                                + "\n" + e.getLocalizedMessage();
                                        Log.w(Constants.TAG, error);
                                        Toast.makeText(mMainActivity, error, Toast.LENGTH_LONG).show();

                                    } catch (IOException e) {
                                        String error = "Error in " + mCurrNgwRes.getDisplayName()
                                                + "\n" + e.getLocalizedMessage();
                                        Log.w(Constants.TAG, error);
                                        Toast.makeText(mMainActivity, error, Toast.LENGTH_LONG).show();
                                    }
                                }

                                if (mSelResIterator.hasNext()) {
                                    loadNextSelectedResource();
                                } else {
                                    dismiss();
                                }

                                break;
                        }
                    }
                });

        mNgwConnWorker.setJsonObjectLoadedListener(
                new NgwConnectionWorker.JsonObjectLoadedListener() {
                    @Override
                    public void onJsonObjectLoaded(JSONObject jsonObject) {
                        if (jsonObject == null) {
                            Toast.makeText(mMap.getContext(),
                                    mMainActivity.getString(R.string.connection_error),
                                    Toast.LENGTH_LONG).show();
                            dismiss();
                            return;
                        }

                        // TODO: ProgressDialog with Fragment for screen rotation
                        ProgressDialog progressDialog = new ProgressDialog(mMainActivity);
                        progressDialog.setMessage(
                                mMainActivity.getString(R.string.message_loading_progress));
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        progressDialog.setCancelable(true);
                        progressDialog.show();

                        try {
                            switch (mCurrNgwRes.getCls()) {
                                case Constants.NGWTYPE_VECTOR_LAYER:
                                    new NgwVectorLayer(mCurrConn.getId(), mCurrNgwRes.getId())
                                            .create(mMap, mCurrNgwRes.getDisplayName(),
                                                    jsonObject, progressDialog);
                                    break;

                                case Constants.NGWTYPE_RASTER_LAYER:
                                    break;
                                case Constants.NGWTYPE_RASTER_STYLE:
                                    break;
                            }

                        } catch (JSONException e) {
                            String error = "Error in " + mCurrNgwRes.getDisplayName()
                                    + "\n" + e.getLocalizedMessage();
                            Log.w(Constants.TAG, error);
                            Toast.makeText(mMainActivity, error, Toast.LENGTH_LONG).show();

                        } catch (IOException e) {
                            String error = "Error in " + mCurrNgwRes.getDisplayName()
                                    + "\n" + e.getLocalizedMessage();
                            Log.w(Constants.TAG, error);
                            Toast.makeText(mMainActivity, error, Toast.LENGTH_LONG).show();
                        }

                        if (mSelResIterator.hasNext()) {
                            loadNextSelectedResource();
                        } else {
                            dismiss();
                        }
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
        View view = inflater.inflate(R.layout.ngw_resources_dialog, container);

        mDialogTitleText = (TextView) view.findViewById(R.id.dialog_title_text);
        mHttpProgressBar = (ProgressBar) view.findViewById(R.id.http_progress_bar);
        mButtonBar = (RelativeLayout) view.findViewById(R.id.button_nar);

        mAddConnectionButton = (ImageButton) view.findViewById(R.id.btn_add_connection);
        mAddConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NgwAddConnectionDialog dialog = new NgwAddConnectionDialog();

                dialog.setOnAddConnectionListener(
                        new NgwAddConnectionDialog.OnAddConnectionListener() {
                            @Override
                            public void onAddConnection(NgwConnection connection) {
                                mNgwConnections.add(connection);
                                NgwConnection.saveNgwConnections(mNgwConnections, mMap.getMapPath());
                                mNgwResRoots.add(new NgwResource(connection.getId()));
                                ((BaseAdapter) mResourceList.getAdapter()).notifyDataSetChanged();
                            }
                        });

                dialog.show(getActivity().getSupportFragmentManager(), "NgwAddConnectionDialog");
            }
        });

        mOkButton = (ImageButton) view.findViewById(R.id.btn_ok_res);
        mOkButton.setEnabled(!mSelectedResources.isEmpty());
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelResIterator = mSelectedResources.iterator();

                if (mSelResIterator.hasNext()) {
                    setLoadResourcesView(true);
                    loadNextSelectedResource();
                } else {
                    dismiss();
                }
            }
        });

        mCancelButton = (ImageButton) view.findViewById(R.id.btn_cancel_res);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNgwConnWorker.cancel();
                dismiss();
            }
        });

        mResourceList = (ListView) view.findViewById(R.id.ngw_resources_list);

        if (mIsConnectionView) {
            setConnectionView();
        } else {
            setJsonView();
        }

        setHttpRunningView(mIsHttpRunning);
        return view;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.ngw_connection_context_menu, menu);

        MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onContextItemSelected(item);
                return true;
            }
        };

        for (int i = 0, n = menu.size(); i < n; i++)
            menu.getItem(i).setOnMenuItemClickListener(listener);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case R.id.menu_edit_connection:
                NgwAddConnectionDialog editDialog = new NgwAddConnectionDialog();

                editDialog.setOnEditConnectionListener(
                        new NgwAddConnectionDialog.OnEditConnectionListener() {
                            @Override
                            public void onEditConnection(NgwConnection connection) {
                                NgwConnection.saveNgwConnections(mNgwConnections, mMap.getMapPath());
                                ((BaseAdapter) mResourceList.getAdapter()).notifyDataSetChanged();
                            }
                        },
                        mMap.getNgwConnections().get((int) info.id));

                editDialog.show(
                        getActivity().getSupportFragmentManager(), "NgwEditConnectionDialog");
                return true;

            case R.id.menu_delete_connection:
                NgwDeleteConnectionDialog deleteDialog =
                        NgwDeleteConnectionDialog.newInstance((int) info.id);

                deleteDialog.setOnDeleteConnectionListener(
                        new NgwDeleteConnectionDialog.OnDeleteConnectionListener() {
                            @Override
                            public void onDeleteConnection(int index) {
                                mNgwResRoots.remove(mNgwConnections.get(index).getId());
                                mNgwConnections.remove(index);
                                NgwConnection.saveNgwConnections(mNgwConnections, mMap.getMapPath());
                                ((BaseAdapter) mResourceList.getAdapter()).notifyDataSetChanged();
                            }
                        });

                deleteDialog.show(
                        getActivity().getSupportFragmentManager(), "NgwDeleteConnectionDialog");
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    public void loadNextSelectedResource() {
        mCurrNgwRes = mSelResIterator.next();
        mCurrConn = mNgwConnections.getByConnectionId(mCurrNgwRes.getConnectionId());

        switch (mCurrNgwRes.getCls()) {
            case Constants.NGWTYPE_VECTOR_LAYER:
                mCurrConn.setLoadGeoJsonObject(mCurrNgwRes);
                mNgwConnWorker.loadNgwJson(mCurrConn);
                break;

            case Constants.NGWTYPE_RASTER_LAYER:
                setHttpRunningView(true);
                mCurrConn.setLoadResourceArray(mCurrNgwRes);
                mNgwConnWorker.loadNgwJson(mCurrConn);
                break;
        }
    }

    protected void setHttpRunningView(boolean isRunning) {
        mIsHttpRunning = isRunning;
        mHttpProgressBar.setVisibility(mIsHttpRunning ? View.VISIBLE : View.INVISIBLE);
        mAddConnectionButton.setEnabled(!mIsHttpRunning);
        mOkButton.setEnabled(!mIsHttpRunning);
        mResourceList.setEnabled(!mIsHttpRunning);
    }

    protected void setLoadResourcesView(boolean isRunning) {
        mIsHttpRunning = isRunning;
        mHttpProgressBar.setVisibility(mIsHttpRunning ? View.VISIBLE : View.INVISIBLE);
        mAddConnectionButton.setEnabled(!mIsHttpRunning);
        mOkButton.setEnabled(!mIsHttpRunning);
        mResourceList.setVisibility(View.GONE);
    }

    protected void setConnectionView() {
        registerForContextMenu(mResourceList);

        mIsConnectionView = true;
        mDialogTitleText.setText(mMainActivity.getString(R.string.ngw_connections));

        mAddConnectionButton.setVisibility(View.VISIBLE);

        mResourceList.setAdapter(mConnectionsAdapter);
        mResourceList.setOnItemClickListener(mConnectionOnClickListener);
    }

    protected void setJsonView() {
        unregisterForContextMenu(mResourceList);

        mIsConnectionView = false;

        String titleText = mCurrNgwRes.getDisplayName() == null
                ? "" : "/" + mCurrNgwRes.getDisplayName();

        NgwResource parent = mCurrNgwRes.getParent();
        while (parent != null) {
            titleText = (parent.getDisplayName() == null
                    ? "" : "/" + parent.getDisplayName()) + titleText;
            parent = parent.getParent();
        }

        titleText = "/" + mCurrConn.getName() + titleText;
        mDialogTitleText.setText(titleText);

        mAddConnectionButton.setVisibility(View.GONE);

        NgwJsonArrayAdapter jsonArrayAdapter =
                new NgwJsonArrayAdapter(mMainActivity, mCurrNgwRes);

        jsonArrayAdapter.setOnItemCheckedChangeListener(
                new NgwJsonArrayAdapter.ItemCheckedChangeListener() {
                    @Override
                    public void onItemCheckedChange(NgwResource ngwResource, boolean isChecked) {
                        ngwResource.setSelected(isChecked);

                        if (isChecked) mSelectedResources.add(ngwResource);
                        else mSelectedResources.remove(ngwResource);

                        mOkButton.setEnabled(!mSelectedResources.isEmpty());
                    }
                });

        mResourceList.setAdapter(jsonArrayAdapter);

        mResourceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                NgwResource ngwResource = mCurrNgwRes.get(position);

                if (position == 0) {

                    if (ngwResource.isRoot()) {
                        mCurrNgwRes = ngwResource;
                        setConnectionView();

                    } else {
                        mCurrNgwRes = mCurrNgwRes.getParent();
                        setJsonView();
                    }

                } else {
                    mCurrNgwRes = ngwResource;

                    if (mCurrNgwRes.size() == 0) {
                        setHttpRunningView(true);
                        mCurrConn.setLoadResourceArray(mCurrNgwRes);
                        mNgwConnWorker.loadNgwJson(mCurrConn);

                    } else {
                        setJsonView();
                    }
                }
            }
        });
    }

    protected class NgwResourceRoots {
        private Set<NgwResource> mResourceRoots;

        public NgwResourceRoots() {
            mResourceRoots = new TreeSet<NgwResource>();
        }

        public boolean add(NgwResource resource) {
            return mResourceRoots.add(resource);
        }

        public boolean remove(int connectionId) {
            for (NgwResource resourceRoot : mResourceRoots) {
                if (resourceRoot.getConnectionId() == connectionId) {
                    return mResourceRoots.remove(resourceRoot);
                }
            }
            return false;
        }

        public NgwResource get(int connectionId) {
            for (NgwResource resourceRoot : mResourceRoots) {
                if (resourceRoot.getConnectionId() == connectionId) {
                    return resourceRoot;
                }
            }
            return null;
        }
    }
}
