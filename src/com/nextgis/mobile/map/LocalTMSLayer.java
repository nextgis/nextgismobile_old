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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.TileCacheLevelDescItem;
import com.nextgis.mobile.datasource.TileItem;
import com.nextgis.mobile.util.FileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.nextgis.mobile.util.Constants.*;

public class LocalTMSLayer extends TMSLayer{
    protected Map<Integer, TileCacheLevelDescItem> mLimits;

    public LocalTMSLayer(){
        super();
        mLimits = new HashMap<Integer, TileCacheLevelDescItem>();
    }

    public LocalTMSLayer(MapBase map, File path, JSONObject config){
        super(map, path, config);
    }

    @Override
    public Bitmap getBitmap(TileItem tile) {
        //check if present
        TileCacheLevelDescItem item = mLimits.get(tile.getZoomLevel());
        if(item != null && item.isInside(tile.getX(), tile.getY())){
            File tilePath = new File(mPath, tile.toString("{z}/{x}/{y}.tile"));
            if (tilePath.exists())
                return BitmapFactory.decodeFile(tilePath.getAbsolutePath());
        }
        return null;
    }

    @Override
    protected void setDetailes(JSONObject config){
        super.setDetailes(config);
        try {
            mLimits = new HashMap<Integer, TileCacheLevelDescItem>();
            final JSONArray jsonArray = config.getJSONArray(JSON_LEVELS_KEY);
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonLevel = jsonArray.getJSONObject(i);
                int nLevel = jsonLevel.getInt(JSON_LEVEL_KEY);
                int nMaxX = jsonLevel.getInt(JSON_MAXX_KEY);
                int nMaxY = jsonLevel.getInt(JSON_MAXY_KEY);
                int nMinX = jsonLevel.getInt(JSON_MINX_KEY);
                int nMinY = jsonLevel.getInt(JSON_MINY_KEY);

                mLimits.put(nLevel, new TileCacheLevelDescItem(nMaxX, nMinX, nMaxY, nMinY));
            }
        } catch (JSONException e){
            reportError(e.getLocalizedMessage());
        }
    }

    @Override
    protected JSONObject getDetailes() throws JSONException {
        JSONObject rootConfig = super.getDetailes();
        JSONArray jsonArray = new JSONArray();
        rootConfig.put(JSON_LEVELS_KEY, jsonArray);
        int nMaxLevel = 0;
        int nMinLevel = 512;
        for (Map.Entry<Integer, TileCacheLevelDescItem> entry : mLimits.entrySet()) {
            int nLevelZ = entry.getKey();
            TileCacheLevelDescItem item = entry.getValue();
            JSONObject oJSONLevel = new JSONObject();
            oJSONLevel.put(JSON_LEVEL_KEY, nLevelZ);
            oJSONLevel.put(JSON_MAXX_KEY, item.getMaxX());
            oJSONLevel.put(JSON_MAXY_KEY, item.getMaxY());
            oJSONLevel.put(JSON_MINX_KEY, item.getMinX());
            oJSONLevel.put(JSON_MINY_KEY, item.getMinY());

            jsonArray.put(oJSONLevel);

            if(nMaxLevel < nLevelZ)
                nMaxLevel = nLevelZ;
            if(nMinLevel > nLevelZ)
                nMinLevel = nLevelZ;
        }

        rootConfig.put(JSON_MAXLEVEL_KEY, nMaxLevel);
        rootConfig.put(JSON_MINLEVEL_KEY, nMinLevel);

        return rootConfig;
    }

    @Override
    public Drawable getIcon(){
        return getContext().getResources().getDrawable(R.drawable.ic_local_tms);
    }

    @Override
    public int getType(){
        return LAYERTYPE_LOCAL_TMS;
    }

    public static void create(final MapBase map, Uri uri){
        String sName = getFileNameByUri(map.getContext(), uri, "new layer.zip");
        sName = (String) sName.subSequence(0, sName.length() - 4);
        showPropertiesDialog(map, true, sName, TMSTYPE_OSM, uri, null);
    }

    protected static String getFileNameByUri(final Context context, Uri uri, String defaultName)
    {
        String fileName = defaultName;
        Uri filePathUri = uri;
        try {
            if (uri.getScheme().toString().compareTo("content") == 0) {
                Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor.moveToFirst()) {
                    int column_index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    //Instead of "MediaStore.Images.Media.DATA" can be used "_data"
                    filePathUri = Uri.parse(cursor.getString(column_index));
                    fileName = filePathUri.getLastPathSegment().toString();
                }
            } else if (uri.getScheme().compareTo("file") == 0) {
                fileName = filePathUri.getLastPathSegment().toString();
            } else {
                fileName = fileName + "_" + filePathUri.getLastPathSegment();
            }
        }
        catch (Exception e){
            //do nothing, only return default file name;
            Log.d(TAG, e.getLocalizedMessage());
        }
        return fileName;
    }


    @Override
    public void changeProperties(){
        showPropertiesDialog(mMap, false, mName, getTMSType(), null, this);
    }

    protected static void showPropertiesDialog(final MapBase map, final boolean bCreate, String layerName, int type, final Uri uri, final LocalTMSLayer layer){
        final LinearLayout linearLayout = new LinearLayout(map.getContext());
        final EditText input = new EditText(map.getContext());
        input.setText(layerName);

        final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(map.getContext(), android.R.layout.simple_spinner_item);
        final Spinner spinner = new Spinner(map.getContext());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        adapter.add(map.getContext().getString(R.string.tmstype_qtiles));
        adapter.add(map.getContext().getString(R.string.tmstype_osm));
        adapter.add(map.getContext().getString(R.string.tmstype_normal));
        adapter.add(map.getContext().getString(R.string.tmstype_ngw));

        if(type == TMSTYPE_OSM){
            spinner.setSelection(1);
        }
        else{
            spinner.setSelection(2);
        }

        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(input);
        linearLayout.addView(spinner);

        new AlertDialog.Builder(map.getContext())
                .setTitle(bCreate ? R.string.input_layer_name_and_type : R.string.change_layer_name_and_type)
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
                            create(map, input.getText().toString(), tmsType, uri);
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


    protected static void create(final MapBase map, String layerName, int tmsType, Uri uri) {
        String sErr = map.getContext().getString(R.string.error_occurred);
        try {
            InputStream inputStream = map.getContext().getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                ProgressDialog progressDialog = new ProgressDialog(map.getContext());
                progressDialog.setMessage(map.getContext().getString(R.string.message_zip_extract_progress));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(true);
                progressDialog.show();

                File outputPath = map.cretateLayerStorage();
                //create layer description file
                JSONObject oJSONRoot = new JSONObject();
                oJSONRoot.put(JSON_NAME_KEY, layerName);
                oJSONRoot.put(JSON_VISIBILITY_KEY, true);
                oJSONRoot.put(JSON_TYPE_KEY, LAYERTYPE_LOCAL_TMS);
                oJSONRoot.put(JSON_TMSTYPE_KEY, tmsType);

                new UnZipTask(map.getMapEventsHandler(), inputStream, outputPath, oJSONRoot, progressDialog).execute();
                return;
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, "Exception: " + e.getLocalizedMessage());
            sErr += ": " + e.getLocalizedMessage();
        }  catch (JSONException e) {
            Log.d(TAG, "Exception: " + e.getLocalizedMessage());
            sErr += ": " + e.getLocalizedMessage();
        }
        //if we here something wrong occurred
        Toast.makeText(map.getContext(), sErr, Toast.LENGTH_SHORT).show();
    }

    protected static class UnZipTask extends AsyncTask<String, Void, Boolean> {
        protected InputStream mInputStream;
        protected ProgressDialog mProgressDialog;
        protected File mOutputPath;
        protected JSONObject mLayerConfig;
        protected Handler mEventReceiver;

        public UnZipTask(Handler eventReceiver, InputStream inputStream, File outputPath, JSONObject layerConfig, ProgressDialog progressDialog) {
            super();
            mInputStream = inputStream;
            mProgressDialog = progressDialog;
            mOutputPath = outputPath;
            mLayerConfig = layerConfig;
            mEventReceiver = eventReceiver;
        }

        private void unzipEntry(ZipInputStream zis, ZipEntry entry, File outputDir) throws IOException {
            String entryName = entry.getName();
            //for backward capability where the zip haz root directory named "mapnik"
            entryName = entryName.replace("Mapnik/", "");
            entryName = entryName.replace("mapnik/", "");

            //for prevent searching by media library
            entryName = entryName.replace(".png", TILE_EXT);
            entryName = entryName.replace(".jpg", TILE_EXT);
            entryName = entryName.replace(".jpeg", TILE_EXT);

            if (entry.isDirectory()) {
                FileUtil.createDir(new File(outputDir, entryName));
                return;
            }
            File outputFile = new File(outputDir, entryName);
            if (!outputFile.getParentFile().exists()) {
                FileUtil.createDir(outputFile.getParentFile());
            }

            FileOutputStream fout = new FileOutputStream(outputFile);
            int nCount;
            byte[] buffer = new byte[1024];
            while ((nCount = zis.read(buffer)) > 0) {
                fout.write(buffer, 0, nCount);
            }
            //fout.flush();
            fout.close();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mProgressDialog.dismiss();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String sMsg = mProgressDialog.getContext().getString(R.string.error_occurred);
            try {
//                DeleteRecursive(mOutputPath);
                int nSize = mInputStream.available();
                ZipInputStream zis = new ZipInputStream(mInputStream);
                int nIncrement = 0;
                mProgressDialog.setMax(nSize);

                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    unzipEntry(zis, ze, mOutputPath);
                    nIncrement += ze.getSize();
                    zis.closeEntry();
                    mProgressDialog.setProgress(nIncrement);

                }
                zis.close();

                sMsg = mProgressDialog.getContext().getString(R.string.message_layer_added);
                getLocalCacheDetailes();
                File file = new File(mOutputPath, LAYER_CONFIG);
                FileUtil.writeToFile(file, mLayerConfig.toString());

                if(mEventReceiver != null){
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(BUNDLE_HASERROR_KEY, false);
                    bundle.putString(BUNDLE_MSG_KEY, sMsg);
                    bundle.putInt(BUNDLE_TYPE_KEY, DS_TYPE_ZIP);
                    bundle.putSerializable(BUNDLE_PATH_KEY, mOutputPath);

                    Message msg = new Message();
                    msg.setData(bundle);
                    mEventReceiver.sendMessage(msg);
                }
                return true;

            } catch (IOException e) {
                Log.d(TAG, "Exception: " + e.getLocalizedMessage());
                sMsg += ": " + e.getLocalizedMessage();
            } catch (JSONException e){
                Log.d(TAG, "Exception: " + e.getLocalizedMessage());
                sMsg += ": " + e.getLocalizedMessage();
            } catch (NumberFormatException e){
                Log.d(TAG, "Exception: " + e.getLocalizedMessage());
                sMsg += ": " + e.getLocalizedMessage();
            }

            //send message to handler to show error or add new layer

            if(mEventReceiver != null){
                Bundle bundle = new Bundle();
                bundle.putBoolean(BUNDLE_HASERROR_KEY, true);
                bundle.putString(BUNDLE_MSG_KEY, sMsg);
                bundle.putInt(BUNDLE_TYPE_KEY, DS_TYPE_ZIP);

                Message msg = new Message();
                msg.setData(bundle);
                mEventReceiver.sendMessage(msg);
            }

            return false;
        }

        protected void getLocalCacheDetailes() throws JSONException, NumberFormatException {
            int nMaxLevel = 0;
            int nMinLevel = 512;
            JSONArray jsonArray = new JSONArray();
            mLayerConfig.put(JSON_LEVELS_KEY, jsonArray);

            //get cache levels
            File[] zoomLevels = mOutputPath.listFiles();
            for (File zoomLevel : zoomLevels) {
                int nMaxX = 0;
                int nMinX = 10000000;
                int nMaxY = 0;
                int nMinY = 10000000;
                //Log.d(TAG, zoomLevel.getName());
                int nLevelZ = Integer.parseInt(zoomLevel.getName());
                if (nLevelZ > nMaxLevel)
                    nMaxLevel = nLevelZ;
                if (nLevelZ < nMinLevel)
                    nMinLevel = nLevelZ;
                File[] levelsX = zoomLevel.listFiles();

                boolean bFirstTurn = true;
                for (File inLevelX : levelsX) {
                    //Log.d(TAG, inLevelX.getName());
                    int nX = Integer.parseInt(inLevelX.getName());
                    if (nX > nMaxX)
                        nMaxX = nX;
                    if (nX < nMinX)
                        nMinX = nX;

                    File[] levelsY = inLevelX.listFiles();

                    if (bFirstTurn) {
                        for (File inLevelY : levelsY) {
                            String sLevelY = inLevelY.getName();
                            //Log.d(TAG, sLevelY);
                            int nY = Integer.parseInt(sLevelY.replace(TILE_EXT, ""));
                            if (nY > nMaxY)
                                nMaxY = nY;
                            if (nY < nMinY)
                                nMinY = nY;
                        }
                        bFirstTurn = false;
                    }
                }

                JSONObject oJSONLevel = new JSONObject();
                oJSONLevel.put(JSON_LEVEL_KEY, nLevelZ);
                oJSONLevel.put(JSON_MAXX_KEY, nMaxX);
                oJSONLevel.put(JSON_MAXY_KEY, nMaxY);
                oJSONLevel.put(JSON_MINX_KEY, nMinX);
                oJSONLevel.put(JSON_MINY_KEY, nMinY);

                jsonArray.put(oJSONLevel);
            }
            mLayerConfig.put(JSON_MAXLEVEL_KEY, nMaxLevel);
            mLayerConfig.put(JSON_MINLEVEL_KEY, nMinLevel);
        }
    }
}

