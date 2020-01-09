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

package at.floriantaurer.unitasocialwallclient;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import at.floriantaurer.unitabeaconmodule.User;
import at.floriantaurer.unitasocialwallclient.rest.RESTController;
import at.floriantaurer.unitasocialwallclient.rest.SocialWallAPI;
import at.floriantaurer.unitasocialwallclient.utils.LoginUtils;
import at.floriantaurer.unitasocialwallclient.utils.ServiceConstants;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

public class LoginActivity extends AppCompatActivity {

    Button btnLogin;
    EditText edtLogin;

    private ViewGroup rootViewGroup;

    private static final int REQUEST_INTERNET = 1337;
    private String [] PERMISSIONS = {Manifest.permission.INTERNET};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_view);

        rootViewGroup = (ViewGroup) ((ViewGroup) this
                .findViewById(android.R.id.content)).getChildAt(0);

        btnLogin = (Button) findViewById(R.id.btnLogin);
        edtLogin = (EditText) findViewById(R.id.edtLogin);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!edtLogin.getText().toString().equals("")) {
                    loginUser(edtLogin.getText().toString());
                    startService();
                }
            }
        });

        if(!hasPermissions(LoginActivity.this, PERMISSIONS)){
            requestInternetPermission();
        }

        if(LoginUtils.getLoggedInUser(LoginActivity.this) != null){
            Intent myIntent = new Intent(LoginActivity.this, MainActivity.class);
            startActivityForResult(myIntent, 0);
        }
    }

    private void startService() {
        if (!SocialWallService.IS_SERVICE_RUNNING) {
            Intent service = new Intent(this.getApplicationContext(), SocialWallService.class);
            service.setAction(ServiceConstants.ACTION.STARTFOREGROUND_ACTION);
            startService(service);
        }
    }

    private void loginUser(String userName){
        int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        final ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(NUMBER_OF_CORES + 1);

        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                final SocialWallAPI restService = RESTController.getRetrofitInstance().create(SocialWallAPI.class);

                restService.loginUser(userName).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            String detailsString = getStringFromRetrofitResponse(response);
                            Log.i("LoginActivity", "post submitted to API. " + detailsString);
                            JSONObject jObject = null;
                            try {
                                jObject = new JSONObject(detailsString);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Log.i("LoginActivity", "post submitted to API." + jObject.toString());
                            JSONObject jsonObject = null;
                            try {
                                 jsonObject = (JSONObject) jObject.getJSONArray("user").getJSONObject(0);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Log.i("LoginActivity", "post submitted to API." + jsonObject.toString());

                            onLoginResponse(jsonObject);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e("LoginActivity", "Unable to submit post to API." + t);
                    }
                });
            }
        });
    }

    public static String getStringFromRetrofitResponse(Response<ResponseBody> response) {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));

        String line;

        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();

    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void requestInternetPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this,
                Manifest.permission.RECORD_AUDIO)) {
            Snackbar.make(rootViewGroup, R.string.permissionRequestExplanation,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(LoginActivity.this,
                                    new String[]{/*Manifest.permission.ACCESS_FINE_LOCATION,*/ Manifest.permission.INTERNET},
                                    REQUEST_INTERNET);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(LoginActivity.this, PERMISSIONS, REQUEST_INTERNET);
        }
    }

    private void showRequestPermissionExplanation(int messageId) {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(LoginActivity.this);
        builder.setMessage(messageId);
        builder.setPositiveButton(R.string.permission_request_explanation_positive,new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                }
        );
        builder.setNegativeButton(R.string.permission_request_explanation_negative, null);
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0) {
            //we will show an explanation next time the user click on start
            showRequestPermissionExplanation(R.string.permissionRequestExplanation);
        }
        switch (requestCode){
            case REQUEST_INTERNET:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
                else if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    showRequestPermissionExplanation(R.string.permissionRequestExplanation);
                }
                break;
        }
    }

    public void onLoginResponse(JSONObject loginResponse) {
        int id = -1;
        String name = null;
        try {
            id = loginResponse.getInt("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            name = loginResponse.getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(id > (-1) && name != null){
            User loggedInUser = new User(id, name);
            LoginUtils.setLoggedInUser(loggedInUser, LoginActivity.this);
            Intent myIntent = new Intent(LoginActivity.this, MainActivity.class);
            startActivityForResult(myIntent, 0);
        } else{
        }


    }
}
