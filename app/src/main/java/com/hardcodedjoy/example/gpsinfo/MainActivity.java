package com.hardcodedjoy.example.gpsinfo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvText;
    private MapView mvOsmMap;
    private GeoPoint myLocationPoint;
    private Marker currentLocationMarker;
    private RepeatingUiTask locationUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initGUI();
    }

    private void initGUI() {
        setContentView(R.layout.layout_main);
        tvText = findViewById(R.id.tv_text);
        mvOsmMap = findViewById(R.id.mv_osmmap);
        currentLocationMarker = new Marker(mvOsmMap);
        Drawable currentLocationDrawable = getResources().getDrawable(R.drawable.person);
        currentLocationMarker.setIcon(currentLocationDrawable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestPermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(locationUpdater != null) {
            locationUpdater.stop();
        }
    }

    private void requestPermissions() {
        if(Build.VERSION.SDK_INT < 23) {
            onPermissionsGranted();
            return;
        }

        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        for(String permission : permissions) {
            if(checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, 1);
                return;
            }
        }
        onPermissionsGranted();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        int n = permissions.length;
        for(int i=0; i<n; i++) {
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                finish();
                return;
            }
        }
        onPermissionsGranted();
    }

    @SuppressLint("MissingPermission")
    private void onPermissionsGranted() {

        tvText.setText(R.string.text_permissions_ganted);

        // without this, the map tiles will not load:
        Configuration.getInstance().setUserAgentValue(getPackageName());

        mvOsmMap.setTileSource(TileSourceFactory.MAPNIK);
        mvOsmMap.setMultiTouchControls(true);
        IMapController controller = mvOsmMap.getController();

        controller.setZoom(18.0);

        LocationManager locationManager;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // this keeps the location reading active:
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000, 0.5f, location -> {});

        locationUpdater = new RepeatingUiTask(() -> {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            onLocationChanged(location);
        }, 500);

        locationUpdater.start();
    }


    private void onLocationChanged(Location location) {
        if(location == null) { return; }
        int satellites = location.getExtras().getInt("satellites");
        float accuracy = location.getAccuracy(); // in meters
        double latitude = location.getLatitude(); // -90.0 to 90.0
        double longitude = location.getLongitude(); // -180.0 to 180.0
        double altitude = location.getAltitude(); // meters above the WGS84 reference ellipsoid
        double speed = location.getSpeed(); // meters/s
        speed *= 3.6; // km/h

        String s = "";
        s += "satellites: " + satellites + "\n";
        s += "latitude: " + String.format(Locale.US, "%.6f", latitude) + "\n";
        s += "longitude: " + String.format(Locale.US, "%.6f", longitude) + "\n";
        s += "accuracy: " + String.format(Locale.US, "%.1f", accuracy) + " m\n";
        s += "altitude: " + String.format(Locale.US, "%.2f", altitude) + " m\n";
        s += "speed: " + String.format(Locale.US, "%.2f", speed) + " km/h";

        tvText.setText(s);

        IMapController controller = mvOsmMap.getController();

        if(myLocationPoint == null) {
            myLocationPoint = new GeoPoint(latitude, longitude);
            controller.setCenter(myLocationPoint);
        } else {
            myLocationPoint = new GeoPoint(latitude, longitude);
        }

        mvOsmMap.getOverlayManager().remove(currentLocationMarker);
        currentLocationMarker.setPosition(myLocationPoint);
        mvOsmMap.getOverlayManager().add(currentLocationMarker);
        mvOsmMap.postInvalidate();
    }

    @Override
    public void onBackPressed() {
        myLocationPoint = null;
        super.onBackPressed();
    }
}