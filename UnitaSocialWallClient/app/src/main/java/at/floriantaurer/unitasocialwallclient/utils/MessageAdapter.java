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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import at.floriantaurer.unitabeaconmodule.User;
import at.floriantaurer.unitasocialwallclient.R;
import at.floriantaurer.unitasocialwallclient.TextMessage;

public class MessageAdapter extends ArrayAdapter<TextMessage> {

    ArrayList<User> contactList;

    public MessageAdapter(@NonNull Context context, ArrayList<TextMessage> resource, ArrayList<User> contactList) {
        super(context, R.layout.row_item, resource);
        this.contactList = contactList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater storedInflater = LayoutInflater.from(getContext());

        View customView = storedInflater.inflate(R.layout.row_message_item,parent, false);

        TextMessage singleMessageItem = getItem(position);

        TextView txtRowItemMessage = (TextView) customView.findViewById(R.id.rowitemMessage);
        TextView txtRowItemContactIDFrom = (TextView) customView.findViewById(R.id.rowitemContactidFrom);
        TextView txtRowItemContactIDTo = (TextView) customView.findViewById(R.id.rowitemContactidTo);

        txtRowItemMessage.setText(new String(singleMessageItem.getMessageBody().getMessageBodyRaw(), StandardCharsets.UTF_8));
        /*for(int i =0; i<contactList.size(); i++){
            if(singleMessageItem.getCommunicationPartner())
        }*/
        txtRowItemContactIDFrom.setText(String.format(getContext().getString(R.string.message_adapter_from), singleMessageItem.getHeader().getSender().getId()));
        if(singleMessageItem.getCommunicationPartner().getId() == 0){
            txtRowItemContactIDTo.setText(getContext().getString(R.string.message_adapter_to_all));
        }else{
            txtRowItemContactIDTo.setText(String.format(getContext().getString(R.string.message_adapter_to), singleMessageItem.getCommunicationPartner().getId()));
        }


        return customView;
    }
}
