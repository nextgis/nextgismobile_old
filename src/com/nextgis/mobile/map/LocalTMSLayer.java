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


import android.graphics.drawable.Drawable;

import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.TileCacheLevelDescItem;
import com.nextgis.mobile.util.FileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.nextgis.mobile.util.Constants.*;

public class LocalTMSLayer extends Layer{

    protected Map<Integer, TileCacheLevelDescItem> mLimits;

    public LocalTMSLayer(){
        super();
        mLimits = new HashMap<Integer, TileCacheLevelDescItem>();
    }

    public LocalTMSLayer(MapBase map, short id, File path, JSONObject config){
        super(map, id, path, config);
    }

    @Override
    protected void fillDetails(JSONObject config){
        super.fillDetails(config);
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
    public Drawable getIcon(){
        return getContext().getResources().getDrawable(R.drawable.ic_local_tms);
    }

    @Override
    public int getType(){
        return LAYERTYPE_LOCAL_TMS;
    }

    @Override
    public void save(){
        try {
            JSONObject rootConfig = new JSONObject();
            rootConfig.put(JSON_NAME_KEY, mName);
            rootConfig.put(JSON_TYPE_KEY, getType());
            rootConfig.put(JSON_MAXLEVEL_KEY, mMaxZoom);
            rootConfig.put(JSON_MINLEVEL_KEY, mMinZoom);
            rootConfig.put(JSON_VISIBILITY_KEY, getVisible());

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

            File outFile = new File(mPath, LAYER_CONFIG);
            FileUtil.writeToFile(outFile, rootConfig.toString());
        } catch (JSONException e){
            reportError(e.getLocalizedMessage());
        } catch (IOException e){
            reportError(e.getLocalizedMessage());
        }
    }

    @Override
    public void changeProperties(){

    }
}

