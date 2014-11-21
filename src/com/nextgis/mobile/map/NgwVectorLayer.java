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
import com.nextgis.mobile.datasource.NgwConnection;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static com.nextgis.mobile.util.Constants.*;

public class NgwVectorLayer extends LocalGeoJsonLayer {

    protected String mUrl;
    protected String mLogin;
    protected String mPassword;


    public NgwVectorLayer(NgwConnection connection) {
        mUrl = connection.getUrl();
        mLogin = connection.getLogin();
        mPassword = connection.getPassword();
    }

    public NgwVectorLayer(MapBase map, File path, JSONObject config) {
        super(map, path, config);
    }

    @Override
    public Drawable getIcon() {
        // TODO: change icon
        return getContext().getResources().getDrawable(R.drawable.ic_local_json);
    }

    @Override
    public int getType() {
        return LAYERTYPE_REMOTE_GEOJSON;
    }

    @Override
    protected void setDetails(JSONObject config) {
        super.setDetails(config);
        try {
            mUrl = config.getString(JSON_URL_KEY);
            mLogin = config.getString(JSON_LOGIN_KEY);
            mPassword = config.getString(JSON_PASSWORD_KEY);

        } catch (JSONException e){
            reportError(e.getLocalizedMessage());
        }
    }

    @Override
    protected JSONObject getDetails() throws JSONException {
        JSONObject rootConfig = super.getDetails();
        rootConfig.put(JSON_URL_KEY, mUrl);
        rootConfig.put(JSON_LOGIN_KEY, mLogin);
        rootConfig.put(JSON_PASSWORD_KEY, mPassword);
        return rootConfig;
    }

    @Override
    protected JSONObject createDetails() throws JSONException{
        JSONObject rootConfig = super.createDetails();
        rootConfig.put(JSON_URL_KEY, mUrl);
        rootConfig.put(JSON_LOGIN_KEY, mLogin);
        rootConfig.put(JSON_PASSWORD_KEY, mPassword);
        return rootConfig;
    }
}
