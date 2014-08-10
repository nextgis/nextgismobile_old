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
import com.nextgis.mobile.datasource.GeoPoint;
import com.nextgis.mobile.datasource.TileItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.nextgis.mobile.util.Constants.*;

public abstract class TMSLayer extends Layer {
    protected int mTMSType;

    public TMSLayer() {
        super();
        mRenderer = new TMSRenderer(this);
    }

    public TMSLayer(MapBase map, File path, JSONObject config) {
        super(map, path, config);
        mRenderer = new TMSRenderer(this);
    }


    public int getTMSType() {
        return mTMSType;
    }

    public void setTMSType(int type) {
        mTMSType = type;
    }

    public final List<TileItem> getTielsForBounds(GeoEnvelope bounds, int zoom) {
        List<TileItem> list = new ArrayList<TileItem>();
        int maxY = 1 << zoom;
        int halfMax = maxY / 2;
        //1. get tile size in m for current zoom
        GeoPoint tileSize = mMap.getGISDisplay().getTileSize();
        //2. get bottom left tile
        int begX = (int) (bounds.getMinX() / tileSize.getX() - 1.5) + halfMax;
        int begY = (int) (bounds.getMinY() / tileSize.getY() - 1.5) + halfMax;
        int endX = (int) (bounds.getMaxX() / tileSize.getX() + 1.5) + halfMax;
        int endY = (int) (bounds.getMaxY() / tileSize.getY() + 1.5) + halfMax;
        //3.
        GeoEnvelope fullBounds = mMap.getGISDisplay().getFullBounds();
        //4.
        if(begX < 0)
            begX = 0;
        if(begY < 0)
            begY = 0;
        if(endX > maxY)
            endX = maxY;
        if(endY > maxY)
            endY = maxY;

        for(int x = begX; x < endX; x++){
            for(int y = begY; y < endY; y++){
                int realY = y;
                if(mTMSType == TMSTYPE_OSM){
                    realY = maxY - y - 1;
                }

                final GeoPoint pt = new GeoPoint(fullBounds.getMinX() + x * tileSize.getX(), fullBounds.getMinY() + (y + 1) * tileSize.getY());
                TileItem item = new TileItem(x, realY, zoom, pt);
                list.add(item);
            }
        }

        return list;
    }

    public abstract Bitmap getBitmap(TileItem tile);

    @Override
    protected void setDetailes(JSONObject config) {
        super.setDetailes(config);
        try {
            mTMSType = config.getInt(JSON_TMSTYPE_KEY);
        } catch (JSONException e){
            reportError(e.getLocalizedMessage());
        }
    }

    @Override
    protected JSONObject getDetailes() throws JSONException{
        JSONObject rootObject = super.getDetailes();
        rootObject.put(JSON_TMSTYPE_KEY, mTMSType);
        return rootObject;
    }
}