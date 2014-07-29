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


import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.nextgis.mobile.R;
import com.nextgis.mobile.util.FileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.nextgis.mobile.util.Constants.*;

public class MapView extends MapBase {

    public MapView(Context context) {
        super(context);
    }

    //create local TMS layer from zip
    public void CreateLocalTMSLayer(String layerName, int tmsType, Uri uri) {
        String sErr = getContext().getString(R.string.error_occurred);
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                ProgressDialog progressDialog = new ProgressDialog(getContext());
                progressDialog.setMessage(getContext().getString(R.string.message_zip_extract_progress));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(true);
                progressDialog.show();

                File outputPath = cretateLayerStorage();
                //create layer description file
                JSONObject oJSONRoot = new JSONObject();
                oJSONRoot.put(JSON_NAME_KEY, layerName);
                oJSONRoot.put(JSON_VISIBILITY_KEY, true);
                oJSONRoot.put(JSON_TYPE_KEY, LAYERTYPE_LOCAL_TMS);
                oJSONRoot.put(JSON_TMSTYPE_KEY, tmsType);

                new UnZipTask(mHandler, inputStream, outputPath, oJSONRoot, progressDialog).execute();
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
        Toast.makeText(getContext(), sErr, Toast.LENGTH_SHORT).show();
    }

    protected boolean CreateLocalJSONLayer(String layerName) {
        return false;
    }

    protected boolean CreateRemoteTMSLayer(String layerName) {
        return false;
    }

    @Override
    protected void processMessage(Bundle bundle){
        switch (bundle.getInt(BUNDLE_SRC_KEY)){
            case DS_TYPE_ZIP:
                File path = (File) bundle.getSerializable(BUNDLE_PATH_KEY);
                addLayer(path);
                break;
            default:
                super.processMessage(bundle);
        }
    }

    /*
        create new folder in map directory to store layer data
     */
    protected File cretateLayerStorage() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String layerDir = LAYER_PREFIX + sdf.format(new Date());
        return new File(mMapPath, layerDir);
    }

    protected class UnZipTask extends AsyncTask<String, Void, Boolean> {
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
            String sMsg = getContext().getString(R.string.error_occurred);
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

                sMsg = getContext().getString(R.string.message_layer_added);
                getLocalCacheDetailes();
                File file = new File(mOutputPath, LAYER_CONFIG);
                FileUtil.writeToFile(file, mLayerConfig.toString());

                if(mEventReceiver != null){
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(BUNDLE_HASERROR_KEY, false);
                    bundle.putString(BUNDLE_MSG_KEY, sMsg);
                    bundle.putInt(BUNDLE_SRC_KEY, DS_TYPE_ZIP);
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
                bundle.putInt(BUNDLE_SRC_KEY, DS_TYPE_ZIP);

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



