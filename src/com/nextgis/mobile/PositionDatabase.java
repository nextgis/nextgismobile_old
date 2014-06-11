/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
*   Copyright (C) 2012-2013 NextGIS
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
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

package com.nextgis.mobile;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class PositionDatabase extends SQLiteOpenHelper {
	
	private static final String TAG = "ngisdroid:PositionDatabase";
	
	public static final String TABLE_POS = "position";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_ACCURACY = "accuracy";
	public static final String COLUMN_ALTITUDE = "altitude";
	public static final String COLUMN_LAT = "lat";
	public static final String COLUMN_LON = "lon";
	public static final String COLUMN_PROVIDER = "provider";
	public static final String COLUMN_SPEED = "speed";
	public static final String COLUMN_DIRECTION = "direction";
	public static final String COLUMN_TIME_UTC = "time_utc";
	public static final String COLUMN_TIME = "datetime";
	
	private static final String DATABASE_NAME = "position.db";
	private static final int DATABASE_VERSION = 1;
	
	private static final String DATABASE_CREATE = "create table "
			+ TABLE_POS + "( " 
			+ COLUMN_ID + " integer primary key autoincrement, "
			+ COLUMN_TIME + " DEFAULT CURRENT_TIMESTAMP, "
			+ COLUMN_ACCURACY + " real, " 
			+ COLUMN_ALTITUDE + " real, " 
			+ COLUMN_LAT + " real," 
			+ COLUMN_LON + " real," 
			+ COLUMN_PROVIDER + " text, "
			+ COLUMN_SPEED + " real, "
			+ COLUMN_DIRECTION + " real, "
			+ COLUMN_TIME_UTC + " datetime );";

	
	public PositionDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// Create database
		db.execSQL(DATABASE_CREATE);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Logs that the database is being upgraded
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");

        // Kills the table and existing data
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POS);

        // Recreates the database with a new version
        onCreate(db);
	}

}
