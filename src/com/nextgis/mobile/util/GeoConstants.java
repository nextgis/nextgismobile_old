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
package com.nextgis.mobile.util;

public interface GeoConstants {
    /**
     * DataSource type
     */
    public final static int DS_TYPE_ZIP = 1;
    public final static int DS_TYPE_TMS = 2;

    /**
     * TMS type
     */
    public static final int TMSTYPE_NORMAL = 1;
    public static final int TMSTYPE_OSM = 2;

    /**
     * geometry type
     */
    public static final int GEOMTYPE_Point = 1;
    public static final int GEOMTYPE_LineString = 2;
    public static final int GEOMTYPE_Polygon = 3;
    public static final int GEOMTYPE_MultiIPoint = 4;
    public static final int GEOMTYPE_MultiLineString = 5;
    public static final int GEOMTYPE_MultiPolygon = 6;
    public static final int GEOMTYPE_GeometryCollection = 7;
    public static final int GEOMTYPE_None = 100;

    /**
     * geojson
     * see http://geojson.org/geojson-spec.html
     */
    public static final String GEOJSON_TYPE = "type";
    public static final String GEOJSON_CRS = "crs";
    public static final String GEOJSON_NAME = "name";
    public static final String GEOJSON_PROPERTIES = "properties";
    public static final String GEOJSON_BBOX = "bbox";
    public static final String GEOJSON_TYPE_FEATURES = "features";
    public static final String GEOJSON_GEOMETRY = "geometry";
    public static final String GEOJSON_COORDINATES = "coordinates";
    public static final String GEOJSON_TYPE_Point = "Point";
    public static final String GEOJSON_TYPE_MultiPoint = "MultiPoint";
    public static final String GEOJSON_TYPE_LineString = "LineString";
    public static final String GEOJSON_TYPE_MultiLineString = "MultiLineString";
    public static final String GEOJSON_TYPE_Polygon = "Polygon";
    public static final String GEOJSON_TYPE_MultiPolygon = "MultiPolygon";
    public static final String GEOJSON_TYPE_GeometryCollection = "GeometryCollection";
    public static final String GEOJSON_TYPE_Feature = "Feature";
    public static final String GEOJSON_TYPE_FeatureCollection = "FeatureCollection";



}
