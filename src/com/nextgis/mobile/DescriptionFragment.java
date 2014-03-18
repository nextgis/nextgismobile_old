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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;

import com.actionbarsherlock.app.SherlockFragment;

import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.os.Bundle;

public class DescriptionFragment extends SherlockFragment{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
    	this.setRetainInstance(true);

    	View view = inflater.inflate(R.layout.descriptfragment, container, false);

        final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(getSherlockActivity(), android.R.layout.simple_spinner_item);
        
        final Map<String, ArrayList<String>> mlCategories = new HashMap<String, ArrayList<String>>();
        //fill spinners from xml data
        
        XmlPullParser parser = Xml.newPullParser();
        try {
        	File file = new File(getSherlockActivity().getExternalFilesDir(null), "categories.xml");
            if (file != null) {
            	if(!file.exists())
            	{
            		createExternalStoragePrivateFile();
            		file = new File(getSherlockActivity().getExternalFilesDir(null), "categories.xml");
            	}
            	
            	InputStream in = new BufferedInputStream(new FileInputStream(file));
            	     
            	InputStreamReader isr = new InputStreamReader(in);
            	
                // auto-detect the encoding from the stream
                parser.setInput(isr);
                int eventType = parser.getEventType();       
                String sCatVal = null;
                while (eventType != XmlPullParser.END_DOCUMENT){                	
                    switch (eventType){
                        case XmlPullParser.START_DOCUMENT:
                            break;
                        case XmlPullParser.START_TAG:
                            String name = parser.getName();
                            if (name.equalsIgnoreCase("category"))
                            {
                            	sCatVal = parser.getAttributeValue(null, "name");
                            	adapter.add(sCatVal);
                            	mlCategories.put(sCatVal, new ArrayList<String>());
                            }
                            else if(name.equalsIgnoreCase("subcategory"))
                            {
                            	if(sCatVal != null)
                            	{
                            		String sSubCatVal = parser.getAttributeValue(null, "name");
                            		mlCategories.get(sCatVal).add(sSubCatVal);
                            	}
                            }
                            break;
                        }
                    eventType = parser.next();
                    }

	    	     if (in != null) {
	    	       in.close();
	    	     } 
            }
        } catch (IOException e) {
        	// TODO	         
        } catch (Exception e){
            // TODO
        }
        
        Spinner spinner = (Spinner) view.findViewById(R.id.spinner_cat);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            	final Spinner subspinner = (Spinner) getView().findViewById(R.id.spinner_subcat);
            	//subspinner
            	String sCat = adapter.getItem(arg2).toString();
            	TextView textview = (TextView) getView().findViewById(R.id.spinner_subcat_custom);
            	if(sCat.equalsIgnoreCase("custom"))
            	{
            		//enable text item
            		textview.setEnabled(true);
            	}
            	else
            	{
            		textview.setEnabled(false);
            	}
            	ArrayAdapter<String> subadapter = new ArrayAdapter<String>(getSherlockActivity(), android.R.layout.simple_spinner_item, mlCategories.get(sCat));
            	subadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            	subspinner.setAdapter(subadapter);
            }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });    
        
        onStoreValues();
       
        return view;
    }
    
    boolean hasExternalStoragePrivateFile() {
        // Get path for the file on external storage.  If external
        // storage is not currently mounted this will fail.
        File file = new File(getSherlockActivity().getExternalFilesDir(null), "categories.xml");
        if (file != null) {
            return file.exists();
        }
        return false;
    }    
    
    void createExternalStoragePrivateFile() {
        // Create a path where we will place our private file on external
        // storage.
        File file = new File(getSherlockActivity().getExternalFilesDir(null), "categories.xml");

        try {        	         
            FileOutputStream os = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(os);
            pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            pw.println("<categories>");
            pw.println("    <category name=\"first category\">");
            pw.println("        <subcategory name=\"subcategory 1\"/>");
            pw.println("        <subcategory name=\"subcategory 2\"/>");
            pw.println("        <subcategory name=\"subcategory 3\"/>");
            pw.println("        <subcategory name=\"subcategory 4\"/>");
            pw.println("    </category>");    
            pw.println("    <category name=\"second category\">");
            pw.println("        <subcategory name=\"subcategory 1\"/>");
            pw.println("        <subcategory name=\"subcategory 2\"/>");
            pw.println("        <subcategory name=\"subcategory 3\"/>");
            pw.println("        <subcategory name=\"subcategory 4\"/>");
            pw.println("    </category>");
            pw.println("        <!-- add button to add custom subcategory -->");
            pw.println("    <category name=\"custom\"/>");
            pw.println("</categories>");

            
            pw.flush();
            pw.close();
            os.close();
        } catch (IOException e) {
            Log.w("ExternalStorage", "Error writing " + file, e);
        } 
    }

    void deleteExternalStoragePrivateFile() {
        // Get path for the file on external storage.  If external
        // storage is not currently mounted this will fail.
        File file = new File(getSherlockActivity().getExternalFilesDir(null), "categories.xml");
        if (file != null) {
            file.delete();
        }
    }

	public void onStoreValues() {
		
		if(getView() != null)
		{
			InputPointActivity activity = (InputPointActivity) getSherlockActivity();
			if(activity == null)
				return;
			
			Spinner spinner_cat = (Spinner) getView().findViewById(R.id.spinner_cat);
			Spinner spinner_subcat = (Spinner) getView().findViewById(R.id.spinner_subcat);
			TextView textview = (TextView) getView().findViewById(R.id.spinner_subcat_custom);
			
			Object obj = spinner_cat.getSelectedItem();
			if(obj == null)
				return;
			
			String sCat = spinner_cat.getSelectedItem().toString();
			if(sCat.length() == 0)
				return;
			String sSubCat;
			
			if(sCat.equalsIgnoreCase("custom"))
	    	{
				sSubCat = textview.getText().toString();
	    	}
			else
			{
				if(spinner_subcat.getCount() > 0) {
					sSubCat = spinner_subcat.getSelectedItem().toString();
				}
				else {
					sSubCat = getView().getResources().getString(R.string.input_poi_subcatdefault);
				}
			}
			
			activity.SetDescription(sCat, sSubCat);
		}
	}   
}
