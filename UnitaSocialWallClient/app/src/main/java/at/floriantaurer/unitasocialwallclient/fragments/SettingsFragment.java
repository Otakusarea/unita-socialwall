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

package at.floriantaurer.unitasocialwallclient.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import at.floriantaurer.unitabeaconmodule.utils.SettingsUtils;
import at.floriantaurer.unitabeaconmodule.utils.UnitaSettings;
import at.floriantaurer.unitasocialwallclient.R;

public class SettingsFragment extends Fragment {

    public static UnitaSettings unitaSettings = null;
    private ScrollView claSettings;

    private static EditText edtBaseFrequency;
    private static EditText edtBitperiod;
    private static EditText edtPausePeriod;
    private static EditText edtFrequencySpace;
    private static EditText edtMessageLength;
    private static TextView txtPreset;
    Button btnChangePreset;
    private static Spinner spnNumberOfFrequencies;

    private boolean isSettingsChanged = false;

    private TextView txtBasefrequencySetting;
    private TextView txtBitperiodSetting;
    private TextView txtPauseperiodSetting;
    private TextView txtFrequencyspaceSetting;
    private TextView txtMaxLengthSetting;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.settings_fragment_uneditable, null);
        claSettings = (ScrollView) rootView.findViewById(R.id.claSettings);
        edtBaseFrequency = (EditText) rootView.findViewById(R.id.edtBaseFrequency);
        edtBitperiod = (EditText) rootView.findViewById(R.id.edtBitperiod);
        edtPausePeriod = (EditText) rootView.findViewById(R.id.edtPausePeriod);
        edtFrequencySpace = (EditText) rootView.findViewById(R.id.edtFrequencySpace);
        edtMessageLength = (EditText) rootView.findViewById(R.id.edtMessageLength);

        txtBasefrequencySetting = (TextView) rootView.findViewById(R.id.txtBasefrequencySetting);
        txtBitperiodSetting = (TextView) rootView.findViewById(R.id.txtBitperiodSetting);
        txtPauseperiodSetting = (TextView) rootView.findViewById(R.id.txtPauseperiodSetting);
        txtFrequencyspaceSetting = (TextView) rootView.findViewById(R.id.txtFrequencyspaceSetting);
        txtMaxLengthSetting = (TextView) rootView.findViewById(R.id.txtMaxLengthSetting);


        txtPreset = (TextView) rootView.findViewById(R.id.txtPreset);
        btnChangePreset = (Button) rootView.findViewById(R.id.btnChangePreset);
        isSettingsChanged = true;
        btnChangePreset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAndSetPresets();
            }
        });
        spnNumberOfFrequencies = (Spinner) rootView.findViewById(R.id.spnNumberOfFrequencies);
        String[] numberOfFrequencyElements = new String[]{
                "8", "16", "24", "32", "40", "48"
        };
        spnNumberOfFrequencies.setEnabled(false);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, numberOfFrequencyElements);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnNumberOfFrequencies.setAdapter(adapter);
        spnNumberOfFrequencies.setSelection(1);

        setToPresetConfig(unitaSettings);

        return rootView;
    }

    public UnitaSettings setSettings(Context context){
        if(isSettingsChanged) {
            if (unitaSettings != null) {
                unitaSettings.setFrequencyZero(Integer.valueOf(edtBaseFrequency.getText().toString()));
                unitaSettings.setBitperiod(Integer.valueOf(edtBitperiod.getText().toString()));
                unitaSettings.setPauseperiod(Integer.valueOf(edtPausePeriod.getText().toString()));
                unitaSettings.setFrequencySpace(Integer.valueOf(edtFrequencySpace.getText().toString()));
                unitaSettings.setnMessageBlocks(Integer.valueOf(edtMessageLength.getText().toString()));
                unitaSettings.setnFrequencies(Integer.valueOf(spnNumberOfFrequencies.getSelectedItem().toString()));
            } else {
                unitaSettings = new UnitaSettings(Integer.valueOf(edtBaseFrequency.getText().toString()), Integer.valueOf(edtBitperiod.getText().toString()),
                        Integer.valueOf(edtPausePeriod.getText().toString()), Integer.valueOf(edtMessageLength.getText().toString()),
                        Integer.valueOf(spnNumberOfFrequencies.getSelectedItem().toString()), Integer.valueOf(edtFrequencySpace.getText().toString()), txtPreset.getText().toString());
            }
        }else{
            unitaSettings = SettingsUtils.getDefaultConfig(context);
        }
        return unitaSettings;
    }

    public void showAndSetPresets(){
        final String[] configList = SettingsUtils.getConfigList(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_title_set_preset));
        builder.setItems(configList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                unitaSettings = SettingsUtils.loadSettingsFromJson(configList[which], getActivity());
                if(unitaSettings != null){
                    setToPresetConfig(unitaSettings);
                }
            }
        });
        builder.show();
    }

    private void setToPresetConfig(UnitaSettings unitaSettings){
        edtBaseFrequency.setText(String.valueOf(unitaSettings.getFrequencyZero()));
        edtBitperiod.setText(String.valueOf(unitaSettings.getBitperiod()));
        edtPausePeriod.setText(String.valueOf(unitaSettings.getPauseperiod()));
        edtFrequencySpace.setText(String.valueOf(unitaSettings.getFrequencySpace()));
        edtMessageLength.setText(String.valueOf(unitaSettings.getnMessageBlocks()));

        txtBasefrequencySetting.setText(String.valueOf(unitaSettings.getFrequencyZero()));
        txtBitperiodSetting.setText(String.valueOf(unitaSettings.getBitperiod()));
        txtPauseperiodSetting.setText(String.valueOf(unitaSettings.getPauseperiod()));
        txtFrequencyspaceSetting.setText(String.valueOf(unitaSettings.getFrequencySpace()));
        txtMaxLengthSetting.setText(String.valueOf(unitaSettings.getnMessageBlocks()));

        spnNumberOfFrequencies.setSelection(checkFrequencySpinnerIndex(String.valueOf(unitaSettings.getnFrequencies())));
        txtPreset.setText(String.valueOf(unitaSettings.getSettingName()));
    }

    private int checkFrequencySpinnerIndex(String numberOfFrequencies){
        SpinnerAdapter adapter = spnNumberOfFrequencies.getAdapter();
        int n = adapter.getCount();
        int frequencySpinnerIndex = 0;
        for (int i = 0; i < n; i++) {
            if(numberOfFrequencies.equals(adapter.getItem(i).toString())){
                frequencySpinnerIndex = i;
            }
        }
        return frequencySpinnerIndex;
    }

}
