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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;

import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.TileItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static com.nextgis.mobile.util.Constants.*;

public class RemoteTMSLayer extends TMSLayer {
    protected String mURL;

    public RemoteTMSLayer() {
        super();
    }

    public RemoteTMSLayer(MapBase map, File path, JSONObject config) {
        super(map, path, config);
    }

    @Override
    public Bitmap getBitmap(TileItem tile) {
        // try to get tile from local cache
        File tilePath = new File(mPath, tile.toString("{z}/{x}/{y}.tile"));
        if (tilePath.exists() && System.currentTimeMillis() - tilePath.lastModified() < DEFAULT_MAXIMUM_CACHED_FILE_AGE) {
            return BitmapFactory.decodeFile(tilePath.getAbsolutePath());
        }
        // try to get tile from remote

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

    @Override
    public void changeProperties() {

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
