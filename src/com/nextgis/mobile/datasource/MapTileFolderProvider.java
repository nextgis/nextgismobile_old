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

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileRequestState;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase.LowMemoryException;
import org.osmdroid.tileprovider.modules.MapTileFileStorageProviderBase;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;

import android.graphics.drawable.Drawable;
import android.util.Log;

import com.nextgis.mobile.MainActivity;

public class MapTileFolderProvider extends MapTileFileStorageProviderBase {
	
	protected final AtomicReference<ITileSource> mTileSource = new AtomicReference<ITileSource>();
	protected String m_sFolderPath;
	protected HashMap<Integer, TileCacheLevelDescItem> m_moLimits;

	public MapTileFolderProvider(String sFolderPath, final IRegisterReceiver pRegisterReceiver) {
		super(pRegisterReceiver, NUMBER_OF_TILE_FILESYSTEM_THREADS / 2, TILE_FILESYSTEM_MAXIMUM_QUEUE_SIZE / 2);
		m_sFolderPath = sFolderPath;
		
		m_moLimits = new HashMap<Integer, TileCacheLevelDescItem>();
		Log.d(MainActivity.TAG, "FolderPath: " + m_sFolderPath);

	}
	
	@Override
	public int getMaximumZoomLevel() {
		ITileSource tileSource = mTileSource.get();
		return tileSource != null ? tileSource.getMaximumZoomLevel() : MAXIMUM_ZOOMLEVEL;
	}

	@Override
	public int getMinimumZoomLevel() {
		ITileSource tileSource = mTileSource.get();
		return tileSource != null ? tileSource.getMinimumZoomLevel() : MINIMUM_ZOOMLEVEL;
	}

	@Override
	protected String getName() {
		return "NextGIS Folder Cache Provider";
	}

	@Override
	protected String getThreadGroupName() {
		return  "folder";//"filesystem"; //?
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
	
	public void setLimits(int nZoom, int nMaxX, int nMinX, int nMaxY, int nMinY){
		m_moLimits.put(nZoom, new TileCacheLevelDescItem(nMaxX, nMinX, nMaxY, nMinY));
	}
	
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	protected class TileLoader extends MapTileModuleProviderBase.TileLoader {

		@Override
		public Drawable loadTile(final MapTileRequestState pState) throws CantContinueException {

			long startTime = System.currentTimeMillis();
			
			ITileSource tileSource = mTileSource.get();
			if (tileSource == null) {
				return null;
			}

			final MapTile tile = pState.getMapTile();

			// if there's no sdcard then don't do anything
			if (!getSdCardAvailable()) {
				if (DEBUGMODE) {
					Log.d(MainActivity.TAG, "No sdcard - do nothing for tile: " + tile);
				}
				return null;
			}
			
			TileCacheLevelDescItem pLim = m_moLimits.get(tile.getZoomLevel());
			if(pLim == null){
				return null; 
			}
			
			if(!pLim.isInside(tile.getX(), tile.getY())){
				return null;
			}

			// Check the tile source to see if its file is available and if so, then render the
			// drawable and return the tile
			final File file = new File(m_sFolderPath, tileSource.getTileRelativeFilenameString(tile));
			if (DEBUGMODE) {
				Log.d(MainActivity.TAG, "FolderProvider: " + file.getPath());
			}			
				
			if (file.exists()) {


				try {
					final Drawable drawable = tileSource.getDrawable(file.getPath());
					
					long stopTime = System.currentTimeMillis();
				    long elapsedTime = stopTime - startTime;
				    
				    Log.w(MainActivity.TAG, "Filesystem load tile time: " + elapsedTime);

					return drawable;
				} catch (final LowMemoryException e) {
					// low memory so empty the queue
					Log.w(MainActivity.TAG, "LowMemoryException downloading MapTile: " + tile + " : " + e);
					throw new CantContinueException(e);
				}
			}
			long stopTime = System.currentTimeMillis();
		    long elapsedTime = stopTime - startTime;
		    
		    Log.w(MainActivity.TAG, "Filesystem load tile time: " + elapsedTime);

			// If we get here then there is no file in the file cache
			return null;
		}
	}
}
*/