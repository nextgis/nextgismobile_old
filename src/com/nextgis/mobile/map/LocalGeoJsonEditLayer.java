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

import android.graphics.Color;
import com.nextgis.mobile.datasource.Feature;
import com.nextgis.mobile.display.EditMarkerStyle;
import org.json.JSONException;
import org.json.JSONObject;

import static com.nextgis.mobile.util.Constants.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.nextgis.mobile.util.DisplayConstants.*;
import static com.nextgis.mobile.util.GeoConstants.GTPoint;

public class LocalGeoJsonEditLayer extends LocalGeoJsonLayer {

    protected Feature editFeature;

    public LocalGeoJsonEditLayer(MapBase map, File path, JSONObject config) {
        super(map, path, config);

        if(mGeometryType == GTPoint) {
            EditMarkerStyle style =
                    new EditMarkerStyle(Color.BLUE, Color.BLACK, mPointSize, MarkerEditStyleCircle);
            style.setWidth(2);
            mRenderer = new EditFeatureRenderer(this, style);
        }
    }

    @Override
    public int getType() {
        return LAYERTYPE_LOCAL_EDIT_GEOJSON;
    }

    /**
     * Create a LocalGeoJsonLayerEditor from the GeoJson data submitted by features.
     */
    public static void create(final MapBase map, String layerName, List<Feature> features)
            throws JSONException, IOException {

        create(map, layerName, features, LAYERTYPE_LOCAL_EDIT_GEOJSON, MSGTYPE_EDIT_LAYER_ADDED);
    }

    public Feature getEditFeature() {
        return editFeature;
    }

    public void setEditFeature(Feature editFeature) {
        this.editFeature = editFeature;
    }
}
