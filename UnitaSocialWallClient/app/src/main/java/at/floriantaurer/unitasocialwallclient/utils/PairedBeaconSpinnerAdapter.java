/*
 * Copyright (c) 2019. Florian Taurer.
 *
 * This file is part of Unita SDK.
 *
 * Unita is free a SDK: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Unita is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Unita.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.floriantaurer.unitasocialwallclient.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import at.floriantaurer.unitabeaconmodule.Beacon;
import at.floriantaurer.unitasocialwallclient.R;

public class PairedBeaconSpinnerAdapter extends ArrayAdapter<Beacon> {

    private Context context;

    public PairedBeaconSpinnerAdapter(@NonNull Context context, ArrayList<Beacon> resource) {
        super(context, R.layout.spinner_item, resource);
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        //LayoutInflater storedInflater = LayoutInflater.from(getContext());
        //View customView = storedInflater.inflate(R.layout.row_item,parent, false);

        Beacon singleBeaconItem = getItem(position);

        LayoutInflater storedInflater = LayoutInflater.from(getContext());

        View customView = storedInflater.inflate(R.layout.spinner_item,parent, false);

        ((TextView) customView).setText(singleBeaconItem.getName());
        //txtRowItemContactName.setText(singleBeaconItem.getName());
        //txtRowItemContactID.setText(String.format("(SocialWall-ID %d)", singleUserItem.getId()));

        return customView;
    }

    @NonNull
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {

        Beacon singleBeaconItem = getItem(position);

        TextView textView = (TextView) View.inflate(context, R.layout.spinner_item, null);
        textView.setText(singleBeaconItem.getName());

        return textView;
    }
}
