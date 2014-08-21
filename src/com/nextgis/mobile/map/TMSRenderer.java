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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.nextgis.mobile.datasource.GeoEnvelope;
import com.nextgis.mobile.datasource.TileItem;
import com.nextgis.mobile.display.GISDisplay;

import java.util.List;

import static com.nextgis.mobile.util.Constants.*;

public class TMSRenderer extends Renderer{


    public TMSRenderer(Layer layer) {
        super(layer);
    }

    @Override
    public void draw() throws NullPointerException {
        final MapBase map = mLayer.getMap();
        final Handler handler = map.getMapEventsHandler();
        final GISDisplay display = map.getGISDisplay();

        display.clearLayer(0);

        final double zoom = display.getZoomLevel();

        GeoEnvelope env = display.getBounds();
        //get tiled for zoom and bounds
        TMSLayer tmsLayer = (TMSLayer)mLayer;
        final List<TileItem> tiles = tmsLayer.getTielsForBounds(env, zoom);
        int counter = 0;
        float size = (float)tiles.size() / 100.0f;
        for(TileItem tile : tiles){
            if(tmsLayer.isDrawCanceled())
                break;
            final Bitmap bmp = tmsLayer.getBitmap(tile);
            if(bmp != null) {
                display.drawTile(bmp, tile.getPoint());
            }
            if(size != 0.0) {
                if(handler != null){
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(BUNDLE_HASERROR_KEY, false);
                    bundle.putInt(BUNDLE_TYPE_KEY, MSGTYPE_DRAWING_DONE);
                    bundle.putFloat(BUNDLE_DONE_KEY, (float) counter / size);

                    Message msg = new Message();
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                }
                counter++;
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
