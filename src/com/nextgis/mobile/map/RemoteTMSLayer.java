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
package com.nextgis.mobile.map;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.TileItem;
import com.nextgis.mobile.util.FileUtil;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.nextgis.mobile.util.Constants.*;
import static com.nextgis.mobile.util.GeoConstants.*;

public class RemoteTMSLayer extends TMSLayer {
    protected String mURL;
    protected final DefaultHttpClient mHTTPClient;

    public RemoteTMSLayer() {
        super();

        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, TIMEOUT_CONNECTION);
        // Set the default socket timeout (SO_TIMEOUT)
        // in milliseconds which is the timeout for waiting for data.
        HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT_SOKET);

        mHTTPClient = new  DefaultHttpClient(httpParameters);
        mHTTPClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, APP_USER_AGENT);
    }

    public RemoteTMSLayer(MapBase map, File path, JSONObject config) {
        super(map, path, config);
        mHTTPClient = new  DefaultHttpClient();
        mHTTPClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, APP_USER_AGENT);
    }

    @Override
    public Bitmap getBitmap(TileItem tile) {
        // try to get tile from local cache
        File tilePath = new File(mPath, tile.toString("{z}/{x}/{y}.tile"));
        if (tilePath.exists() && System.currentTimeMillis() - tilePath.lastModified() < DEFAULT_MAXIMUM_CACHED_FILE_AGE) {
            return BitmapFactory.decodeFile(tilePath.getAbsolutePath());
        }

        if(!mMap.isNetworkAvaliable())
            return null;
        // try to get tile from remote
        try {

            final HttpUriRequest head = new HttpGet(tile.toString(mURL));
            final HttpResponse response;
            response = mHTTPClient.execute(head);

            // Check to see if we got success
            final org.apache.http.StatusLine line = response.getStatusLine();
            if (line.getStatusCode() != 200) {
                Log.w(TAG, "Problem downloading MapTile: " + tile.toString(mURL) + " HTTP response: " + line);
                return null;
            }

            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                Log.w(TAG, "No content downloading MapTile: " + tile.toString(mURL));
                return null;
            }

            FileUtil.createDir(tilePath.getParentFile());

            InputStream input = entity.getContent();
            OutputStream output = new FileOutputStream(tilePath.getAbsolutePath());
            byte data[] = new byte[IO_BUFFER_SIZE];

            FileUtil.copyStream(input, output, data, IO_BUFFER_SIZE);

            output.close();
            input.close();
            return BitmapFactory.decodeFile(tilePath.getAbsolutePath());

        } catch (IOException e) {
            Log.w(TAG, e.getLocalizedMessage());
        }
        return null;
    }

    @Override
    public Drawable getIcon() {
        return getContext().getResources().getDrawable(R.drawable.ic_remote_tms);
    }

    @Override
    public int getType() {
        return LAYERTYPE_TMS;
    }

    public static void create(final MapBase map){
        String sName = "new TMS layer";
        showPropertiesDialog(map, true, sName, "http://tile.openstreetmap.org/{z}/{x}/{y}.png", TMSTYPE_OSM, null);
    }

    @Override
    public void changeProperties() {
        showPropertiesDialog(mMap, false, mName, mURL, getTMSType(), this);
    }

    protected static void showPropertiesDialog(final MapBase map, final boolean bCreate, String layerName, String layerUrl, int type, final RemoteTMSLayer layer){
        final LinearLayout linearLayout = new LinearLayout(map.getContext());
        final EditText input = new EditText(map.getContext());
        input.setText(layerName);

        final EditText url = new EditText(map.getContext());
        url.setText(layerUrl);

        final TextView stLayerName = new TextView(map.getContext());
        stLayerName.setText(map.getContext().getString(R.string.layer_name) + ":");

        final TextView stLayerUrl = new TextView(map.getContext());
        stLayerUrl.setText(map.getContext().getString(R.string.layer_url) + ":");

        final TextView stLayerType = new TextView(map.getContext());
        stLayerType.setText(map.getContext().getString(R.string.layer_type) + ":");

        final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(map.getContext(), android.R.layout.simple_spinner_item);
        final Spinner spinner = new Spinner(map.getContext());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        adapter.add(map.getContext().getString(R.string.tmstype_osm));
        adapter.add(map.getContext().getString(R.string.tmstype_normal));
        adapter.add(map.getContext().getString(R.string.tmstype_ngw));

        if(type == TMSTYPE_OSM){
            spinner.setSelection(0);
        }
        else{
            spinner.setSelection(1);
        }

        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(stLayerName);
        linearLayout.addView(input);
        linearLayout.addView(stLayerUrl);
        linearLayout.addView(url);
        linearLayout.addView(stLayerType);
        linearLayout.addView(spinner);

        new AlertDialog.Builder(map.getContext())
                .setTitle(bCreate ? R.string.input_layer_properties : R.string.change_layer_properties)
//                                    .setMessage(message)
                .setView(linearLayout)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        int tmsType = 0;
                        switch (spinner.getSelectedItemPosition()) {
                            case 0:
                            case 1:
                                tmsType = TMSTYPE_OSM;
                                break;
                            case 2:
                            case 3:
                                tmsType = TMSTYPE_NORMAL;
                                break;
                        }

                        if (bCreate) {
                            create(map, input.getText().toString(), url.getText().toString(), tmsType);
                        } else {
                            layer.setName(input.getText().toString());
                            layer.setTMSType(tmsType);
                            map.onLayerChanged(layer);
                        }
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
                Toast.makeText(map.getContext(), R.string.error_cancel_by_user, Toast.LENGTH_SHORT).show();
            }
        }).show();
    }

    protected static void create(final MapBase map, String layerName, String layerUrl, int tmsType) {
        String sErr = map.getContext().getString(R.string.error_occurred);
        try{
            File outputPath = map.cretateLayerStorage();
            //create layer description file
            JSONObject oJSONRoot = new JSONObject();
            oJSONRoot.put(JSON_NAME_KEY, layerName);
            oJSONRoot.put(JSON_URL_KEY, layerUrl);
            oJSONRoot.put(JSON_VISIBILITY_KEY, true);
            oJSONRoot.put(JSON_TYPE_KEY, LAYERTYPE_TMS);
            oJSONRoot.put(JSON_TMSTYPE_KEY, tmsType);

            //send message to handler to show error or add new layer

            File file = new File(outputPath, LAYER_CONFIG);
            FileUtil.createDir(outputPath);
            FileUtil.writeToFile(file, oJSONRoot.toString());

            if(map.getMapEventsHandler() != null){
                Bundle bundle = new Bundle();
                bundle.putBoolean(BUNDLE_HASERROR_KEY, false);
                bundle.putString(BUNDLE_MSG_KEY, map.getContext().getString(R.string.message_layer_added));
                bundle.putInt(BUNDLE_TYPE_KEY, MSGTYPE_LAYER_ADDED);
                bundle.putSerializable(BUNDLE_PATH_KEY, outputPath);

                Message msg = new Message();
                msg.setData(bundle);
                map.getMapEventsHandler().sendMessage(msg);
            }
            return;

        } catch (FileNotFoundException e) {
            Log.d(TAG, "Exception: " + e.getLocalizedMessage());
            sErr += ": " + e.getLocalizedMessage();
        }  catch (JSONException e) {
            Log.d(TAG, "Exception: " + e.getLocalizedMessage());
            sErr += ": " + e.getLocalizedMessage();
        }  catch (IOException e)    {
            Log.d(TAG, "Exception: " + e.getLocalizedMessage());
            sErr += ": " + e.getLocalizedMessage();
        }
        //if we here something wrong occurred
        Toast.makeText(map.getContext(), sErr, Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void setDetailes(JSONObject config) {
        super.setDetailes(config);
        try {
            mURL = config.getString(JSON_URL_KEY);
        } catch (JSONException e) {
            reportError(e.getLocalizedMessage());
        }
    }

    @Override
    protected JSONObject getDetailes() throws JSONException {
        JSONObject rootConfig = super.getDetailes();
        rootConfig.put(JSON_URL_KEY, mURL);
        return rootConfig;
    }
}
