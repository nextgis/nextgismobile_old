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

public class GeoRawPoint {

    public double x;
    public double y;

    public GeoRawPoint() {
        x = y = 0.0;
    }

    public GeoRawPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public GeoRawPoint(GeoRawPoint rpt) {
        this.x = rpt.x;
        this.y = rpt.y;
    }

    public GeoRawPoint(GeoPoint gpt) {
        this.x = gpt.getX();
        this.y = gpt.getY();
    }

    public boolean equals(GeoRawPoint grp) {
        return x == grp.x && y == grp.y;
    }
}
