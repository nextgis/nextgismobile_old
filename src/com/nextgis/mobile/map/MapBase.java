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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.nextgis.mobile.R;
import com.nextgis.mobile.display.GISDisplay;
import com.nextgis.mobile.util.FileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.nextgis.mobile.util.Constants.*;

public class MapBase extends View {
    protected String mName;
    protected List<Layer> mLayers;
    protected List<MapEventListener> mListeners;
    protected GISDisplay mDisplay;
    protected File mMapPath;
    protected Handler mHandler;
    protected short mNewId;
    protected long mStartDrawTime;

    /**
     * The base map class
     */

    public MapBase(Context context) {
        super(context);

        mName = context.getString(R.string.default_map_name);
        mNewId = 0;
        mListeners = new ArrayList<MapEventListener>();
        mLayers = new ArrayList<Layer>();

        createHandler();

        //initialise display
        mDisplay = new GISDisplay(context);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        File defaultPath = context.getExternalFilesDir(PREFS_MAP);
        mMapPath = new File(sharedPreferences.getString(KEY_PREF_MAP_PATH, defaultPath.getPath()));
    }

    /**
     * Get map name
     *
     * @return map name
     */
    public String getName() {
        return mName;
    }

    /**
     * Set map name
     *
     * @param newName A new map name
     */
    public void setName(String newName) {
        this.mName = newName;
    }

    /**
     * Get the map layers
     *
     * @return map layers list
     */
    public List<Layer> getLayers() {
        return mLayers;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //super.onDraw(canvas);
        if(mDisplay != null){
            canvas.drawBitmap(mDisplay.getMainBitmap(), 0, 0, null);
        }
    }

    protected void runDrawThread(){
        mStartDrawTime = System.currentTimeMillis();
        for(Layer layer : mLayers) {
            layer.draw();
        }
    }

    /**
     * Create handler for messages
     */
    protected void createHandler(){
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                Bundle resultData = msg.getData();
                boolean bHasErr = resultData.getBoolean(BUNDLE_HASERROR_KEY);
                if(bHasErr){
                    reportError(resultData.getString(BUNDLE_MSG_KEY));
                }
                else{
                    processMessage(resultData);
                }
            }
        };
    }

    /**
     * Standard error report function
     *
     * @param errMsg An error message
     */
    protected void reportError(String errMsg){
        Log.d(TAG, errMsg);
        Toast.makeText(getContext(), errMsg, Toast.LENGTH_SHORT).show();
    }

    /**
     * Process message received by handler
     *
     * @param bundle A message payload
     */
    protected void processMessage(Bundle bundle){
        //nothing to do now
    }

    /**
     * Create existed lyer from path and add it to the map
     *
     * @param path A path to layer directory
     */
    protected void addLayer(File path){
        File config_file = new File(path, LAYER_CONFIG);
        try {
            String sData = FileUtil.readFromFile(config_file);
            JSONObject rootObject = new JSONObject(sData);
            int nType = rootObject.getInt(JSON_TYPE_KEY);
            switch (nType){
                case LAYERTYPE_LOCAL_TMS:
                    Layer layer = new LocalTMSLayer(this, path, rootObject);
                    mLayers.add(layer);
                    onLayerAdded(layer);
                    break;
                case LAYERTYPE_LOCAL_GEOJSON:
                    break;
                case LAYERTYPE_LOCAL_RASTER:
                    break;
                case LAYERTYPE_TMS:
                    break;
                case LAYERTYPE_NGW:
                    break;
            }
        } catch (IOException e){
            reportError(e.getLocalizedMessage());
        } catch (JSONException e){
            reportError(e.getLocalizedMessage());
        }
    }

    /**
     * Load map properties and layers from map.json file
     */
    protected synchronized void loadMap(){
        Log.d(TAG, "load map");
        File config_file = new File(mMapPath, MAP_CONFIG);
        try {
            String sData = FileUtil.readFromFile(config_file);
            JSONObject rootObject = new JSONObject(sData);
            mName = rootObject.getString(JSON_NAME_KEY);
            final JSONArray jsonArray = rootObject.getJSONArray(JSON_LAYERS_KEY);
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonLayer = jsonArray.getJSONObject(i);
                String sPath = jsonLayer.getString(JSON_PATH_KEY);
                File inFile = new File(mMapPath, sPath);
                if(inFile.exists())
                    addLayer(inFile);
            }
        } catch (IOException e){
            reportError(e.getLocalizedMessage());
        } catch (JSONException e){
            reportError(e.getLocalizedMessage());
        }

        //load map without the order of layers
        /*File[] files = mMapPath.listFiles();
        for (File inFile : files) {
            if (inFile.isDirectory() && inFile.toString().startsWith(LAYER_PREFIX)) {
                addLayer(inFile);
            }
        }*/
    }

    /**
     * Save map properties and layers to map.json file
     */
    public synchronized void saveMap(){
        Log.d(TAG, "save map");
        try {
            JSONObject rootObject = new JSONObject();
            rootObject.put(JSON_NAME_KEY, mName);
            JSONArray jsonArray = new JSONArray();
            rootObject.put(JSON_LAYERS_KEY, jsonArray);
            for(Layer layer : mLayers){
                JSONObject layerObject = new JSONObject();
                layerObject.put(JSON_PATH_KEY, layer.getRelativePath());
                jsonArray.put(layerObject);
                layer.save();
            }

            File config_file = new File(mMapPath, MAP_CONFIG);
            FileUtil.writeToFile(config_file, rootObject.toString());
        } catch (IOException e){
            reportError(e.getLocalizedMessage());
        } catch (JSONException e){
            reportError(e.getLocalizedMessage());
        }
    }

    /**
     * The identificator generator
     *
     * @return new id
     */
    public short getNewId(){
        return mNewId++;
    }

    /**
     * Add new listener for map events
     *
     * @param listener A listener class implements MapEventListener
     */
    public void addListener(MapEventListener listener){
        if(mListeners != null){
            mListeners.add(listener);
        }
    }

    /**
     * Send layer added event to all listeners
     *
     * @param layer A new layer
     */
    protected void onLayerAdded(Layer layer){
        if(mListeners == null)
            return;
        for (MapEventListener listener : mListeners)
            listener.onLayerAdded(layer);
    }

    /**
     * Send layer changed event to all listeners
     *
     * @param layer A changed layer
     */
    protected void onLayerChanged(Layer layer){
        if(mListeners == null)
            return;
        for (MapEventListener listener : mListeners)
            listener.onLayerChanged(layer);
    }

    /**
     * Send layer delete event to all listeners
     *
     * @param id A deleted layer identificator
     */
    protected void onLayerDeleted(int id){
        if(mListeners == null)
            return;
        for (MapEventListener listener : mListeners)
            listener.onLayerDeleted(id);
    }

    /**
     *  onPauses should be called from parent activity
     */
    public void onPause(){

    }

    /**
     *  onResume should be called from parent activity
     */
    public void onResume(){
    }

    /**
     *  onStop should be called from parent activity
     */
    public void onStop(){
        saveMap();
        clearMap();
    }

    /**
     *  onStart should be called from parent activity
     */
    public void onStart(){
        loadMap();
    }

    /**
     * Remove all layers
     */
    protected void clearMap(){
        mLayers.clear(); //TODO: do we need onClearMap event?
    }

    /**
     * Delete layer by identifictor
     *
     * @param id An identificator
     * @return true on success or false
     */
    public boolean deleteLayerById(int id){
        boolean bRes = false;
        for(Layer layer : mLayers) {
            if (layer.getId() == id) {
                layer.delete();
                bRes = mLayers.remove(layer);
                if(bRes){
                    onLayerDeleted(id);
                }
                break;
            }
        }
        return bRes;
    }

    /**
     * Get layer by identificator
     *
     * @param id Layer identificator
     * @return Layer or null
     */
    public Layer getLayerById(int id){
        for(Layer layer : mLayers){
            if(layer.getId() == id)
                return layer;
        }
        return null;
    }

    /**
     *  Create new folder in map directory to store layer data
     */
    protected File cretateLayerStorage() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String layerDir = LAYER_PREFIX + sdf.format(new Date());
        return new File(mMapPath, layerDir);
    }

    /**
     * Get internal map events handler
     *
     * @return handler
     */
    public Handler getMapEventsHandler(){
        return mHandler;
    }

    protected void onLayerDrawFinished(float percent){
        if(System.currentTimeMillis() - mStartDrawTime > 500 || percent >= 100){
            mStartDrawTime = System.currentTimeMillis();
            invalidate();
        }
    }

    public final GISDisplay getGISDisplay(){
        return mDisplay;
    }
}
