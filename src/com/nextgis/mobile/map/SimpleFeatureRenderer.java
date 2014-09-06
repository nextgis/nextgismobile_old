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


import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.nextgis.mobile.datasource.Feature;
import com.nextgis.mobile.datasource.GeoEnvelope;
import com.nextgis.mobile.datasource.GeoGeometry;
import com.nextgis.mobile.display.GISDisplay;
import com.nextgis.mobile.display.Style;

import java.util.List;

import static com.nextgis.mobile.util.Constants.*;
import static com.nextgis.mobile.util.GeoConstants.*;

public class SimpleFeatureRenderer  extends Renderer{

    protected Style mStyle;
    protected GeoJsonLayer mGeoJsonLayer;

    public SimpleFeatureRenderer(Layer layer, Style style) {
        super(layer);
        mGeoJsonLayer = (GeoJsonLayer)layer;
        mStyle = style;
    }

    @Override
    public void draw() {
        final MapBase map = mLayer.getMap();
        final Handler handler = map.getMapEventsHandler();
        final GISDisplay display = map.getGISDisplay();
        GeoEnvelope env = display.getBounds();

        final List<Feature> features = mGeoJsonLayer.getFeatures(env);
        for(int i = 0; i < features.size(); i++){
            Feature feature = features.get(i);
            GeoGeometry geometry = feature.getGeometry();

            mStyle.onDraw(geometry, display);

            if(handler != null){
                Bundle bundle = new Bundle();
                bundle.putBoolean(BUNDLE_HASERROR_KEY, false);
                bundle.putInt(BUNDLE_TYPE_KEY, MSGTYPE_DRAWING_DONE);
                bundle.putFloat(BUNDLE_DONE_KEY, (float) i / features.size());

                Message msg = new Message();
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        }

        if(handler != null){
            Bundle bundle = new Bundle();
            bundle.putBoolean(BUNDLE_HASERROR_KEY, false);
            bundle.putInt(BUNDLE_TYPE_KEY, MSGTYPE_DRAWING_DONE);
            bundle.putFloat(BUNDLE_DONE_KEY, 100.0f);

            Message msg = new Message();
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    }
}
