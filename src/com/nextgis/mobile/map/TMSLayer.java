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

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class TMSLayer extends Layer{
    protected int mTMSType;

    public TMSLayer(){
        super();
        mRenderer = new TMSRenderer(this);
    }

    public TMSLayer(MapBase map, File path, JSONObject config){
        super(map, path, config);
        mRenderer = new TMSRenderer(this);
    }


    public int getTMSType(){
        return mTMSType;
    }

    public void setTMSType(int type){
        mTMSType = type;
    }

    @Override
    public void draw() {
        //1. for each layer run draw thread but not more than CPU count - 1 and less than 1
        //2. wait until one thread exit and start new one
        //3. periodically invalidate the whole screen, i.e. every 0.5 sec
        //4. ?

        if(mRenderer != null){
            mRenderer.draw();
        }
    }

    public final List<TileItem> getTielsForBounds(GeoEnvelope bounds, int zoom){
        List<TileItem> list = new ArrayList<TileItem>();
        return list;
    }

    public abstract Bitmap getBitmap(TileItem tile);
}
