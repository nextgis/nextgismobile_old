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
package com.nextgis.mobile.datasource;

import static com.nextgis.mobile.util.Constants.*;

public class GeoPoint extends GeoGeometry{
    protected double mX;
    protected double mY;

    public GeoPoint(){
        mX = mY = 0;
    }

    public GeoPoint(double x, double y){
        mX = x;
        mY = y;
    }

    public final double getX(){
        return mX;
    }

    public final double getY(){
        return mY;
    }

    public void setX(double x){
        mX = x;
    }

    public void setY(double y){
        mY = y;
    }

    public void setCoordinates(double x, double y){
        mX = x;
        mY = y;
    }

    public final int getType(){
        return GEOMTYPE_Point;
    }
}
