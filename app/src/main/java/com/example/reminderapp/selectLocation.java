package com.example.reminderapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class selectLocation extends AppCompatActivity implements OnMapReadyCallback {

    boolean isPermissionGranted;
    GoogleMap mGoogleMap;
    ImageView img_search_icon;
    EditText et_location, et_rem;
    Button btn_add_rem;
    double lat1, long1;
    String lat1s, long1s;
    private DBHandler dbHandler;
    Marker mHere;

    public void onBackPressed()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(selectLocation.this);
        builder.setMessage("You will have to re-fill the form");
        builder.setTitle("Are you sure?");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which){
                startActivity(new Intent(selectLocation.this,WelcomePage.class));
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog,int which){
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_location);

        img_search_icon = findViewById(R.id.img_search_icon);
        et_location = findViewById(R.id.et_location); //USER SPECIFIED LOCATION (dont store in db - only store long and lat)
        et_rem = findViewById(R.id.et_rem); //USER SPECIFIED REMINDER
        btn_add_rem = findViewById(R.id.btn_add_rem);

        dbHandler= new DBHandler(selectLocation.this);


        checkMyPermissions(); //location settings permission (access or deny)

        if(isPermissionGranted){
            if(checkGooglePlayServices()){
                Toast.makeText(this, "Google PlayServices are available", Toast.LENGTH_SHORT).show();
                SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.frag_map);
                supportMapFragment.getMapAsync(this);
            }else{
                Toast.makeText(this, "Google PlayServices are not available", Toast.LENGTH_SHORT).show();
            }
        }

        img_search_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String location = et_location.getText().toString();
                if(location == null){
                    Toast.makeText(selectLocation.this, "Type a valid location!", Toast.LENGTH_SHORT).show();
                }else{
                    Geocoder geocoder = new Geocoder(selectLocation.this, Locale.getDefault());
                    try {
                        List<Address> listAddress = geocoder.getFromLocationName(location,1 );
                        if(listAddress.size()>0){
                            LatLng latLng = new LatLng(listAddress.get(0).getLatitude(), listAddress.get(0).getLongitude());

                            lat1 = listAddress.get(0).getLatitude(); //LATITUDE OF THE REMINDER LOCATION
                            long1 = listAddress.get(0).getLongitude(); //LONGITUDE OF THE REMINDER LOCATION

                            lat1s = Double.toString(lat1); //for toast msg
                            long1s = Double.toString(long1); //for toast msg

                            Toast.makeText(selectLocation.this, lat1s + " & " + long1s , Toast.LENGTH_SHORT).show();

                            if(mHere!=null){
                                mHere.remove();
                            }
                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.title("Here");
                            markerOptions.position(latLng);
                            mHere = mGoogleMap.addMarker(markerOptions);
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 5);
                            mGoogleMap.animateCamera(cameraUpdate);
                        }
                    }catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        btn_add_rem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(et_rem.getText().toString().length() == 0){
                    Toast.makeText(selectLocation.this, "Enter valid reminder!", Toast.LENGTH_SHORT).show();
                }
                else if(lat1s==null || long1s==null){
                    Toast.makeText(selectLocation.this, "Please select a valid location", Toast.LENGTH_SHORT).show();
                }

                //code for adding details to database table - user's reminder
                else{
                    dbHandler.addReminderRecord(et_rem.getText().toString(),et_location.getText().toString(),lat1s,long1s);
                    Toast.makeText(selectLocation.this, "Record added ! thank you!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(selectLocation.this,WelcomePage.class));
                }
            }
        });

    }

    private boolean checkGooglePlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int result = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if(result == ConnectionResult.SUCCESS){
            return true;
        }else if(googleApiAvailability.isUserResolvableError(result)){
            Dialog dialog = googleApiAvailability.getErrorDialog(this, result, 201, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    Toast.makeText(selectLocation.this, "User cancelled", Toast.LENGTH_SHORT).show();
                }
            });
            dialog.show();
        }
        return false;
    }


    private void checkMyPermissions() {
        Dexter.withContext(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                isPermissionGranted = true;
                Toast.makeText(selectLocation.this, "Location Permission Granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Intent intent_settings = new Intent();
                intent_settings.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), "");
                intent_settings.setData(uri);
                startActivity(intent_settings);
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);


    }


}