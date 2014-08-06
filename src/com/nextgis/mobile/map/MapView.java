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
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.io.File;

import static com.nextgis.mobile.util.Constants.*;

public class MapView extends MapBase {

    public MapView(Context context) {
        super(context);
    }

    public void createLayer(Uri uri, int type){
        Log.d(TAG, "File Uri: " + uri.toString());
        switch (type) {
            case DS_TYPE_ZIP:
                LocalTMSLayer.create(this, uri);
                return;
        }
    }

    @Override
    protected void processMessage(Bundle bundle){
        switch (bundle.getInt(BUNDLE_TYPE_KEY)){
            case MSGTYPE_DS_TYPE_ZIP: //the new layer was create and need to be added on map
                File path = (File) bundle.getSerializable(BUNDLE_PATH_KEY);
                addLayer(path);
                break;
            default:
                super.processMessage(bundle);
        }
    }
}



