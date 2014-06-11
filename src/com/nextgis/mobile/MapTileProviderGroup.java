/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
*   Copyright (C) 2014 NextGIS
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.tileprovider.IMapTileProviderCallback;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.modules.ArchiveFileFactory;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.MapTileProviderArray;

import android.util.Log;

public class MapTileProviderGroup extends MapTileProviderArray  implements IMapTileProviderCallback {
	final static String META = "meta.json";
	
	public MapTileProviderGroup(final IRegisterReceiver pRegisterReceiver, final ITileSource pTileSource) {
		super(pTileSource, pRegisterReceiver);
		
		File f = new File(org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants.OSMDROID_PATH, "layers");
		if(!f.exists())
			return;
		
/*		final MapTileMBDBProvider tileProvider2 = new MapTileMBDBProvider(new File(f, "elista.mbtiles"), pRegisterReceiver);
		final ITileSource tileSource = new XYTileSource( new File(f, "elista.mbtiles").getName(), null, 3, 12, 256, ".png", new String[]{ });
		tileProvider2.setTileSource(tileSource);
		mTileProviderList.add(tileProvider2);
*/		
	
		File[] files = f.listFiles();
		for (File inFile : files) {
			boolean bHasMeta = false;
		    if (inFile.isDirectory()) {
		    	int nMaxLevel = 0;
		    	int nMinLevel = 512;
				final MapTileFolderProvider tileProvider = new MapTileFolderProvider(f.getPath(), pRegisterReceiver);
				
				File metaFile = new File(inFile, META);
		    	//get meta.json
		    	if(metaFile.exists()){
		    		String sJSON = readFromFile(metaFile);
		        	JSONObject oJSON;
					try {
						oJSON = new JSONObject(sJSON);
						nMaxLevel = oJSON.getInt("max_level");
						nMinLevel = oJSON.getInt("min_level");						
						
						final JSONArray jsonArray = oJSON.getJSONArray("levels");
						for(int i = 0; i < jsonArray.length(); i++) {
							JSONObject jsonLevel = jsonArray.getJSONObject(i);
							int nLevel = jsonLevel.getInt("level");
					    	int nMaxX = jsonLevel.getInt("maxX");
					    	int nMaxY = jsonLevel.getInt("maxY");
					    	int nMinX = jsonLevel.getInt("minX");
					    	int nMinY = jsonLevel.getInt("minY");
							
							tileProvider.setLimits(nLevel, nMaxX, nMinX, nMaxY, nMinY);
						}
			        	
						bHasMeta = true;
					} catch (JSONException e) {
						e.printStackTrace();
					}
		    	}

		    	if(!bHasMeta){			    	
		    		JSONObject oJSONRoot = new JSONObject();
		    		JSONArray jsonArray = new JSONArray();
		    		try {
						oJSONRoot.put("levels", jsonArray);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			    	//get cache levels
			    	
					
			    	File[] zoomLevels = inFile.listFiles();
			    	for (File zoomLevel : zoomLevels) {
			    		if(zoomLevel.getName().equals(META))
			    			continue;
				    	int nMaxX = 0;
				    	int nMinX = 10000000;
				    	int nMaxY = 0;
				    	int nMinY = 10000000;
			    		int nLevel = Integer.parseInt(zoomLevel.getName());
			    		if(nLevel > nMaxLevel)
			    			nMaxLevel = nLevel;
			    		if(nLevel < nMinLevel)
			    			nMinLevel = nLevel;
			    		File[] levelsX = zoomLevel.listFiles();
			    		
			    		boolean bFirstTurn = true;
				    	for (File inLevelX : levelsX) {
				    		int nX = Integer.parseInt(inLevelX.getName());
				    		if(nX > nMaxX)
				    			nMaxX = nX;
				    		if(nX < nMinX)
				    			nMinX = nX;
				    		
				    		File[] levelsY = inLevelX.listFiles();
				    		
				    		if(bFirstTurn){
						    	for (File inLevelY : levelsY) {
						    		String sLevelY = inLevelY.getName();
						    		int nY = Integer.parseInt(sLevelY.replace(".png", ""));
						    		if(nY > nMaxY)
						    			nMaxY = nY;
						    		if(nY < nMinY)
						    			nMinY = nY;
						    	}
						    	bFirstTurn = false;
				    		}
				    	}  
				    	
				    	JSONObject oJSONLevel = new JSONObject();
				    	try{
					    	oJSONLevel.put("level", nLevel);
					    	oJSONLevel.put("maxX", nMaxX);
					    	oJSONLevel.put("maxY", nMaxY);
					    	oJSONLevel.put("minX", nMinX);
					    	oJSONLevel.put("minY", nMinY);
					    	
					    	jsonArray.put(oJSONLevel);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

				    	tileProvider.setLimits(nLevel, nMaxX, nMinX, nMaxY, nMinY);
			    	}
			    	
			    	try{
			    		oJSONRoot.put("max_level", nMaxLevel);
			    		oJSONRoot.put("min_level", nMinLevel);
			    	} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			    	
			    	String sJSON = oJSONRoot.toString();
	                writeToFile(metaFile, sJSON);		    	
                }
		    	
               
				//new TilesOverlay <- MapTileProviderBase
				// Add tiles layer with custom tile source
		    	Log.d(MainActivity.TAG, "Load cache " + inFile.getPath() + ". Min: " + nMinLevel + " Max: " + nMaxLevel);
				final ITileSource tileSource = new XYTileSource( inFile.getName(), null, nMinLevel, nMaxLevel, 256, ".png", new String[]{ });//f.getPath()
				tileProvider.setTileSource(tileSource);
				
				mTileProviderList.add(tileProvider);
		    }
		}		
	}
	
	protected static boolean writeToFile(File filePath, String sData){
		try{
			FileOutputStream os = new FileOutputStream(filePath, false);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(os);
	        outputStreamWriter.write(sData);
	        outputStreamWriter.close();
	        return true;
		}
		catch(IOException e){
			return false;
		}		
	}

	public static String readFromFile(File filePath) {

	    String ret = "";

	    try {
	    	FileInputStream inputStream = new FileInputStream(filePath);

	        if ( inputStream != null ) {
	            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
	            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
	            String receiveString = "";
	            StringBuilder stringBuilder = new StringBuilder();

	            while ( (receiveString = bufferedReader.readLine()) != null ) {
	                stringBuilder.append(receiveString);
	            }

	            inputStream.close();
	            ret = stringBuilder.toString();
	        }
	    }
	    catch (FileNotFoundException e) {
	    	e.printStackTrace();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }

	    return ret;
	}
}
