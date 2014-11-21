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

import com.nextgis.mobile.util.Constants;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static com.nextgis.mobile.util.Constants.*;

public class NgwRasterLayer extends RemoteTMSLayer {

    protected Integer mConnectionId;


    public NgwRasterLayer(Integer connectionId) {
        super();
        mConnectionId = connectionId;
    }

    public NgwRasterLayer(MapBase map, File path, JSONObject config) {
        super(map, path, config);
    }

    @Override
    public int getType() {
        return Constants.LAYERTYPE_NDW_RASTER;
    }

    @Override
    protected JSONObject createDetails() throws JSONException{
        JSONObject rootConfig = super.createDetails();
        rootConfig.put(JSON_CONNECTION_ID_KEY, mConnectionId);
        return rootConfig;
    }

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
}
