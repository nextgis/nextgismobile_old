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

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;

public class CompassActivity extends SherlockFragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
       	getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		setContentView(R.layout.compass);
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MainActivity.MENU_SETTINGS, Menu.NONE, R.string.sSettings)
        .setIcon(R.drawable.ic_action_settings)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);		
        menu.add(Menu.NONE, MainActivity.MENU_ABOUT, Menu.NONE, R.string.sAbout)
        .setIcon(R.drawable.ic_action_about)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);		
       return true;
	}
	
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case MainActivity.MENU_SETTINGS:
                // app icon in action bar clicked; go home
                Intent intentSet = new Intent(this, PreferencesActivity.class);
                intentSet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentSet);
                return true;
            case MainActivity.MENU_ABOUT:
                Intent intentAbout = new Intent(this, AboutActivity.class);
                intentAbout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentAbout);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
