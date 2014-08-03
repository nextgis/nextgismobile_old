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

import com.nextgis.mobile.datasource.GeoEnvelope;
import com.nextgis.mobile.datasource.TileItem;
import com.nextgis.mobile.display.GISDisplay;

import java.util.List;

public class TMSRenderer extends Renderer{


    public TMSRenderer(Layer layer) {
        super(layer);
    }

    @Override
    public void draw() throws NullPointerException {
        final MapBase map = mLayer.getMap();
        final GISDisplay display = map.getGISDisplay();
        final int zoom = display.getZoomLevel();
        GeoEnvelope env = display.getBounds();
        //get tiled for zoom and bounds
        TMSLayer tmsLayer = (TMSLayer)mLayer;
        final List<TileItem> tiles = tmsLayer.getTielsForBounds(env, zoom);
        int counter = 0;
        float size = tiles.size() / 100;
        for(TileItem tile : tiles){
            final Bitmap bmp = tmsLayer.getBitmap(tile);
            if(bmp != null)
                display.drawBitmap(bmp, tile.getPoint());
            map.onLayerDrawFinished(counter / size);
        }
        map.onLayerDrawFinished(100);
    }
}
