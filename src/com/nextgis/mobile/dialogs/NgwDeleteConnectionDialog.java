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
package com.nextgis.mobile.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import com.nextgis.mobile.MainActivity;
import com.nextgis.mobile.R;
import com.nextgis.mobile.map.MapBase;

public class NgwDeleteConnectionDialog extends DialogFragment {

    protected OnDeleteConnectionListener mOnDeleteConnectionListener;

    public interface OnDeleteConnectionListener {
        void onDeleteConnection(int index);
    }

    public void setOnDeleteConnectionListener(OnDeleteConnectionListener onDeleteConnectionListener) {
        mOnDeleteConnectionListener = onDeleteConnectionListener;
    }


    protected int mIndex;

    public static NgwDeleteConnectionDialog newInstance(int index) {
        NgwDeleteConnectionDialog dialog = new NgwDeleteConnectionDialog();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("index", index);
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        this.mIndex = getArguments().getInt("index");
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setOnDismissListener(null);
        super.onDestroyView();
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MainActivity mainActivity = (MainActivity) getActivity();
        final MapBase map = mainActivity.getMap();

        AlertDialog.Builder adb = new AlertDialog.Builder(mainActivity)
                .setTitle(R.string.delete)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mOnDeleteConnectionListener != null) {
                            mOnDeleteConnectionListener.onDeleteConnection(mIndex);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setMessage(String.format(getString(R.string.ngw_msg_delete_connection),
                        map.getNgwConnections().get(mIndex).getName()));
        return adb.create();
    }
}
