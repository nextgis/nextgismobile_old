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

package com.nextgis.mobile.display;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.WindowManager;
import android.view.Display;

import com.nextgis.mobile.R;

public class GISDisplay {
    protected Canvas mMainCanvas;
    protected Bitmap mMainBitmap;
    protected int mOrientation;
    protected Context mCtx;

    public GISDisplay(Context context) {
        this.mCtx = context;
        Display disp = ((WindowManager)mCtx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int width = disp.getWidth();
        int height = disp.getHeight();

        mMainBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mMainCanvas = new Canvas(mMainBitmap);
    }

    public Bitmap getMainBitmap() {
        //Test
        clearBackground();
        return mMainBitmap;
    }

    protected void clearBackground(){
        Bitmap bkBitmap = BitmapFactory.decodeResource(mCtx.getResources(), R.drawable.bk_tile);
        for(int i = 0; i < mMainBitmap.getWidth(); i += 256){
            for(int j = 0; j < mMainBitmap.getHeight(); j += 256){
                mMainCanvas.drawBitmap(bkBitmap, i, j, null);
            }
        }
    }
}
