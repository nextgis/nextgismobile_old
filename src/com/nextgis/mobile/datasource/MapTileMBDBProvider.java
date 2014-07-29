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
/*package com.nextgis.mobile.datasource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileRequestState;
import org.osmdroid.tileprovider.modules.MapTileFileStorageProviderBase;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.nextgis.mobile.MainActivity;


public class MapTileMBDBProvider extends MapTileFileStorageProviderBase {

	protected File m_sMBTilesDBPath;
	protected final SQLiteDatabase mDatabase;
	protected final AtomicReference<ITileSource> mTileSource = new AtomicReference<ITileSource>();

	//	TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB);
	public final static String TABLE_TILES = "tiles";
	public final static String COL_TILES_ZOOM_LEVEL = "zoom_level";
	public final static String COL_TILES_TILE_COLUMN = "tile_column";
	public final static String COL_TILES_TILE_ROW = "tile_row";
	public final static String COL_TILES_TILE_DATA = "tile_data";
	protected int m_nMaxZoomLevel = MAXIMUM_ZOOMLEVEL;
	protected int m_nMinZoomLevel = MINIMUM_ZOOMLEVEL;

	public MapTileMBDBProvider(File file, final IRegisterReceiver pRegisterReceiver) {
		super(pRegisterReceiver, NUMBER_OF_TILE_FILESYSTEM_THREADS / 2, TILE_FILESYSTEM_MAXIMUM_QUEUE_SIZE / 2);
		m_sMBTilesDBPath = file;
		mDatabase = SQLiteDatabase.openDatabase(m_sMBTilesDBPath.getAbsolutePath(), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
		
		final Cursor cur_min = mDatabase.rawQuery("SELECT min(" + COL_TILES_ZOOM_LEVEL + ") FROM " + TABLE_TILES, null);

		if(cur_min.getCount() != 0) {
			cur_min.moveToFirst();
			m_nMinZoomLevel = cur_min.getInt(0);
		}
		cur_min.close();
		
		final Cursor cur_max = mDatabase.rawQuery("SELECT max(" + COL_TILES_ZOOM_LEVEL + ") FROM " + TABLE_TILES, null);

		if(cur_max.getCount() != 0) {
			cur_max.moveToFirst();
			m_nMaxZoomLevel = cur_max.getInt(0);
		}
		cur_max.close();
	}
	
	@Override
	public int getMaximumZoomLevel() {
		//ITileSource tileSource = mTileSource.get();
		//return tileSource != null ? tileSource.getMaximumZoomLevel() : MAXIMUM_ZOOMLEVEL;
		return m_nMaxZoomLevel;
	}

	@Override
	public int getMinimumZoomLevel() {
		//ITileSource tileSource = mTileSource.get();
		//return tileSource != null ? tileSource.getMinimumZoomLevel() : MINIMUM_ZOOMLEVEL;
		return m_nMinZoomLevel;
	}

	@Override
	protected String getName() {
		return "NextGIS MBTiles Provider";
	}

	@Override
	protected String getThreadGroupName() {
		return  "mbtiles";
	}

	@Override
	protected Runnable getTileLoader() {
		return new TileLoader();
	}

	@Override
	public boolean getUsesDataConnection() {
		return false;
	}

	@Override
	public void setTileSource(ITileSource pTileSource) {
		mTileSource.set(pTileSource);
	}
	
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	protected class TileLoader extends MapTileModuleProviderBase.TileLoader {

		@Override
		public Drawable loadTile(final MapTileRequestState pState) throws CantContinueException {
			long startTime = System.currentTimeMillis();
			final MapTile tile = pState.getMapTile();
			try {
				InputStream ret = null;
				final String[] tile_data = { COL_TILES_TILE_DATA };
				final String[] xyz = {
						  Integer.toString(tile.getX())
						, Double.toString(Math.pow(2, tile.getZoomLevel()) - tile.getY() - 1)  // Use Google Tiling Spec
						, Integer.toString(tile.getZoomLevel())
				};

				final Cursor cur = mDatabase.query(TABLE_TILES, tile_data, "tile_column=? and tile_row=? and zoom_level=?", xyz, null, null, null);

				if(cur.getCount() != 0) {
					cur.moveToFirst();
					ret = new ByteArrayInputStream(cur.getBlob(0));
				}
				cur.close();
				if(ret != null) {
					long stopTime = System.currentTimeMillis();
				    long elapsedTime = stopTime - startTime;
				    
				    Log.w(MainActivity.TAG, "MBTiles load tile time: " + elapsedTime);
					return Drawable.createFromStream(ret, "tile");
				}
			} catch(final Throwable e) {
				Log.w(MainActivity.TAG, "Error getting db stream: " + tile, e);
			}
			
			long stopTime = System.currentTimeMillis();
		    long elapsedTime = stopTime - startTime;
		    
		    Log.w(MainActivity.TAG, "MBTiles load tile time: " + elapsedTime);

			return null;
		}
	}
}
*/