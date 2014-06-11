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

import java.io.File;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.actionbarsherlock.app.SherlockFragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class CameraFragment extends SherlockFragment  {

	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 7;
	private String fileName; 
	private File imgFile;
	private SensorManager sensorManager;	
	private float declination;
	protected Location currentLocation;
	protected Map<Long, Float> mAngles = new HashMap<Long, Float>();
	
	private ArrayList <HashMap<String, Object>> listItems = new ArrayList<HashMap<String,Object>>();
	private static final String IMG_NAME = "image_name";
    private static final String IMG_ROT = "image_rotation";
    
    //DEFINING STRING ADAPTER WHICH WILL HANDLE DATA OF LISTVIEW
    private SimpleAdapter adapter;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {   
		
    	setRetainInstance(true);
    	
    	View view = inflater.inflate(R.layout.camfragment, container, false);

        Button button = (Button) view.findViewById(R.id.insert_take_photo);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	onCapturePhoto();
            }
        }); 
        
        ListView photoList = (ListView)view.findViewById(R.id.poi_photos_list);
        
        adapter = new SimpleAdapter(view.getContext(), 
        		listItems, 
                R.layout.row, new String[]{
                IMG_NAME, 
                IMG_ROT,
                }, new int[]{ 
                R.id.image_name,
                R.id.image_rotation});     
        
        photoList.setAdapter(adapter);
        
		final LocationManager locationManager = (LocationManager) getSherlockActivity().getSystemService(Context.LOCATION_SERVICE);
		
		if(currentLocation == null)
		{
			currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (currentLocation == null){
				currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}
		}
		long now = System.currentTimeMillis();
		declination = CompassFragment.getDeclination(currentLocation, now);
        
		sensorManager = (SensorManager) getSherlockActivity().getSystemService(Context.SENSOR_SERVICE);

        
        return view;
	}

	public void onStoreValues() {		
		if(getView() != null)
		{
			InputPointActivity activity = (InputPointActivity) getSherlockActivity();
			if(activity == null)
				return;
			
			//if (getView().findViewById(R.id.poi_notes_text) != null) {
			//	activity.SetNotes(((TextView) getView().findViewById(R.id.poi_notes_text)).getText().toString());
			//}
		}	
		//UpdateSummary();
	}
	
	private Uri getOutputMediaFileUri(){
	      return Uri.fromFile(getOutputMediaFile());
	}

	private File getOutputMediaFile(){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.
		File mediaStorageDir = new File(getSherlockActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "");
		
	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d(MainActivity.TAG, "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    fileName = "IMG_"+ timeStamp + ".jpg";
	    imgFile = new File(mediaStorageDir, fileName);

	    return imgFile;
	}
	
	private Date stringToDate(String aDate,String aFormat) {

	      if(aDate==null) return null;
	      ParsePosition pos = new ParsePosition(0);
	      SimpleDateFormat simpledateformat = new SimpleDateFormat(aFormat);
	      Date stringDate = simpledateformat.parse(aDate, pos);
	      return stringDate;            

	   }
	
	protected SensorEventListener sensorListener = new SensorEventListener() {

		public void onSensorChanged(SensorEvent event) {
			//long stamp = event.timestamp;
			long stamp = System.currentTimeMillis();
			float dfAz = event.values[0] + declination + CameraFragment.this.getDeviceRotation();
			mAngles.put(stamp, dfAz);
			
/*			if(mAngles.size() > 5000)
			{
				Set set = mAngles.keySet();
			    Iterator itr = set.iterator();
			    while(itr.hasNext()){
			    	if(mAngles.size() < 5000)
			    		break;
		    		itr.remove();
			    }
			}
			//bundle.putFloat("pitch", event.values[1]);
			//bundle.putFloat("roll", event.values[2]);		*/
			}
		public void onAccuracyChanged(Sensor arg0, int arg1) {			
		}
	};
	
	public int getDeviceRotation() {

		if(getSherlockActivity() == null)
			return 0;
		if(getSherlockActivity().getWindowManager() == null)
			return 0;
		Display display = getSherlockActivity().getWindowManager().getDefaultDisplay();
		if(display != null)
		{
			if (display.getRotation() == Surface.ROTATION_90) {
				return 90;
			} else if (display.getRotation() == Surface.ROTATION_180) {
				return 180;
			} else if (display.getRotation() == Surface.ROTATION_270) { 
				return 270; 
			}
		}
		return 0;
	}
	
	public void onCapturePhoto(){
    	Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    	Uri fileUri = getOutputMediaFileUri(); // create a file to save the image
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name

        mAngles.clear();
		sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);
        // start the image capture Intent
		startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);		
	}
	
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {  
		super.onActivityResult(requestCode, resultCode, data);
		sensorManager.unregisterListener(sensorListener);
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
         	//add angle
        	float dfAngle = 0;
        	try {
				ExifInterface exif = new ExifInterface(imgFile.getPath());
				String sDate = exif.getAttribute(ExifInterface.TAG_DATETIME);
				Date datetime = stringToDate(sDate, "yyyy:MM:dd HH:mm:ss");
				Log.d(MainActivity.TAG, "image date: " + datetime.toString());
				long testMilli = datetime.getTime();				
				
				long nDif = 1000000;
				for (long n : mAngles.keySet()) {
					long nDifTmp = Math.abs(testMilli - n);
					if(nDifTmp < nDif)
					{
						nDif = nDifTmp;
						dfAngle = (Float) mAngles.get(n);
					}
			    }
				Log.d(MainActivity.TAG, "image angle: " + dfAngle);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}       	
        	
        	String listItem;
        	if(data != null){
        		Uri outPath = data.getData();
        		listItem = outPath.getLastPathSegment();
        	}
        	else {
        		listItem = fileName;
        	}
        	HashMap<String, Object> hm = new HashMap<String, Object>();
        	hm.put(IMG_NAME, listItem);
        	String sAngle = CompassFragment.formatNumber(dfAngle, 0, 0) + CompassFragment.DEGREE_CHAR + " " + CompassFragment.getDirectionCode(dfAngle, getResources());
        	hm.put(IMG_ROT, sAngle);
         
            listItems.add(hm);    

            adapter.notifyDataSetChanged();
            
            InputPointActivity activity = (InputPointActivity) getSherlockActivity();
			if(activity == null)
				return;
			activity.AddImage(listItem, dfAngle);
        }  
    } 
	
}
