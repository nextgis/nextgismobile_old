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

    public final List<TileItem> getTielsForBounds(GeoEnvelope bounds, double zoom) {

        int nZoom = (int)zoom;
        int tilesInMap = 1 << nZoom;
        double halfTilesInMap = tilesInMap / 2;
        GeoEnvelope fullBounds = mMap.getGISDisplay().getFullBounds();
        GeoPoint mapTileSize = new GeoPoint(fullBounds.width() / tilesInMap, fullBounds.height() / tilesInMap);

        List<TileItem> list = new ArrayList<TileItem>();
        int begX = (int) (bounds.getMinX() / mapTileSize.getX() - .5 + halfTilesInMap);
        int begY = (int) (bounds.getMinY() / mapTileSize.getY() - .5 + halfTilesInMap);
        int endX = (int) (bounds.getMaxX() / mapTileSize.getX() + .5 + halfTilesInMap);
        int endY = (int) (bounds.getMaxY() / mapTileSize.getY() + .5 + halfTilesInMap);
        int addX = 0;

        if(begX < 0)
            begX = 0;
        if(begY < 0)
            begY = 0;
        if(endX > tilesInMap){
            addX = endX - tilesInMap;
            endX = tilesInMap;
        }
        if(endY > tilesInMap)
            endY = tilesInMap;

        //fill tiles from center

        int centerX = begX + (endX - begX) / 2;
        int centerY = begY + (endY - begY) / 2;
        int center = Math.max(centerX, centerY);

        //add center point
        addItemToList(fullBounds, mapTileSize, centerX, centerY, nZoom, tilesInMap, list);

        for(int k = 1; k < center + 2; k++){
            //1. top and bottom
            if(k + centerX < endX + 1) {
                int tileYBottom = centerY - k;
                int tileYTop = centerY + k;
                for (int i = centerX - k; i < centerX + k + 1; i++) {
                    addItemToList(fullBounds, mapTileSize, i, tileYTop, nZoom, tilesInMap, list);
                    addItemToList(fullBounds, mapTileSize, i, tileYBottom, nZoom, tilesInMap, list);
                }
            }

            //2. left and right
            if(k + centerY < endY + 1) {
                int tileLeft = centerX - k;
                int tileRight = centerX + k;
                for (int j = centerY - k + 1; j < centerY + k; j++) {
                    addItemToList(fullBounds, mapTileSize, tileLeft, j, nZoom, tilesInMap, list);
                    addItemToList(fullBounds, mapTileSize, tileRight, j, nZoom, tilesInMap, list);
                }
            }
        }

        if(addX > 0){
            for(int k = 1; k < center + 2; k++){
                if(k + centerY < endY + 1) {
                    for(int x = 0; x < addX; x++) {
                        for (int j = centerY - k + 1; j < centerY + k; j++) {
                            final GeoPoint pt = new GeoPoint(fullBounds.getMinX() + (x + tilesInMap) * mapTileSize.getX(), fullBounds.getMinY() + (j + 1) * mapTileSize.getY());
                            int realY = j;
                            if(mTMSType == TMSTYPE_OSM){
                                realY = tilesInMap - j - 1;
                            }

                            TileItem item = new TileItem(x, realY, nZoom, pt);
                            list.add(item);
                        }
                    }
                }
            }
        }

        /* normal fill from left bottom corner
        for(int x = begX; x < endX; x++){
            for(int y = begY; y < endY; y++){
                realY = y;
                if(mTMSType == TMSTYPE_OSM){
                    realY = tilesInMap - y - 1;
                }

                final GeoPoint pt = new GeoPoint(fullBounds.getMinX() + x * mapTileSize.getX(), fullBounds.getMinY() + (y + 1) * mapTileSize.getY());
                TileItem item = new TileItem(x, realY, nZoom, pt);
                list.add(item);
            }
        }*/

        return list;
    }

    protected void addItemToList(final GeoEnvelope fullBounds, final GeoPoint mapTileSize, int x, int y, int zoom, int tilesInMap, List<TileItem> list) {
        if(x < 0 || x >= tilesInMap)
            return;
        final GeoPoint pt = new GeoPoint(fullBounds.getMinX() + x * mapTileSize.getX(), fullBounds.getMinY() + (y + 1) * mapTileSize.getY());
        int realY = y;
        if(mTMSType == TMSTYPE_OSM){
            realY = tilesInMap - y - 1;
        }

        if(realY < 0 || realY >= tilesInMap)
            return;

        TileItem item = new TileItem(x, realY, zoom, pt);
        list.add(item);
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