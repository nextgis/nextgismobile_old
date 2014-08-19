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

    public final List<TileItem> getTielsForBounds(GeoEnvelope bounds, float zoom) {

        int nZoom = (int) Math.floor(zoom);
        int tilesInMap = 1 << nZoom;
        double halfTilesInMap = tilesInMap / 2;
        GeoEnvelope fullBounds = mMap.getGISDisplay().getFullBounds();
        GeoPoint mapTileSize = new GeoPoint(fullBounds.width() / tilesInMap, fullBounds.height() / tilesInMap);

        List<TileItem> list = new ArrayList<TileItem>();
        int begX = (int) (bounds.getMinX() / mapTileSize.getX() - .5 + halfTilesInMap);
        int begY = (int) (bounds.getMinY() / mapTileSize.getY() - .5 + halfTilesInMap);
        int endX = (int) (bounds.getMaxX() / mapTileSize.getX() + .5 + halfTilesInMap);
        int endY = (int) (bounds.getMaxY() / mapTileSize.getY() + .5 + halfTilesInMap);

        if(begX < 0)
            begX = 0;
        if(begY < 0)
            begY = 0;
        if(endX > tilesInMap)
            endX = tilesInMap;
        if(endY > tilesInMap)
            endY = tilesInMap;

        //TODO: fill tiles on spiral
        //see http://www.cyberforum.ru/visual-cpp/thread3621.html
        //массив спираль java

        for(int x = begX; x < endX; x++){
            for(int y = begY; y < endY; y++){
                int realY = y;
                if(mTMSType == TMSTYPE_OSM){
                    realY = tilesInMap - y - 1;
                }

                final GeoPoint pt = new GeoPoint(fullBounds.getMinX() + x * mapTileSize.getX(), fullBounds.getMinY() + (y + 1) * mapTileSize.getY());
                TileItem item = new TileItem(x, realY, nZoom, pt);
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