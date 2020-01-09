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

import at.floriantaurer.unitabeaconmodule.User;
import at.floriantaurer.unitasocialwallclient.R;

public class ContactAdapter extends ArrayAdapter<User> {

    public ContactAdapter(@NonNull Context context, ArrayList<User> resource) {
        super(context, R.layout.row_item, resource);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater storedInflater = LayoutInflater.from(getContext());

        View customView = storedInflater.inflate(R.layout.row_item,parent, false);

        User singleUserItem = getItem(position);

        TextView txtRowItemContactName = (TextView) customView.findViewById(R.id.rowitemContactname);
        TextView txtRowItemContactID = (TextView) customView.findViewById(R.id.rowitemContactid);

        txtRowItemContactName.setText(singleUserItem.getName());
        txtRowItemContactID.setText(String.format("(SocialWall-ID %d)", singleUserItem.getId()));

        return customView;
    }
}
