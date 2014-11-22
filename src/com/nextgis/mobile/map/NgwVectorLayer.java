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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static com.nextgis.mobile.util.Constants.*;

public class NgwVectorLayer extends LocalGeoJsonLayer {

    protected Integer mConnectionId;
    protected Integer mResourceId;


    public NgwVectorLayer(Integer connectionId, Integer resourceId) {
        super();
        mConnectionId = connectionId;
        mResourceId = resourceId;
    }

    public NgwVectorLayer(MapBase map, File path, JSONObject config) {
        super(map, path, config);
    }

    public Integer getConnectionId() {
        return mConnectionId;
    }

    public Integer getResourceId() {
        return mResourceId;
    }

    @Override
    public Drawable getIcon() {
        // TODO: change icon
        return getContext().getResources().getDrawable(R.drawable.ic_local_json);
    }

    @Override
    public int getType() {
        return LAYERTYPE_NDW_VECTOR;
    }

    @Override
    protected void setDetails(JSONObject config) {
        super.setDetails(config);
        try {
            mConnectionId = config.getInt(JSON_CONNECTION_ID_KEY);

        } catch (JSONException e){
            reportError(e.getLocalizedMessage());
        }
    }

    @Override
    protected JSONObject getDetails() throws JSONException {
        JSONObject rootConfig = super.getDetails();
        rootConfig.put(JSON_CONNECTION_ID_KEY, mConnectionId);
        return rootConfig;
    }

    @Override
    protected JSONObject createDetails() throws JSONException{
        JSONObject rootConfig = super.createDetails();
        rootConfig.put(JSON_CONNECTION_ID_KEY, mConnectionId);
        return rootConfig;
    }
}
