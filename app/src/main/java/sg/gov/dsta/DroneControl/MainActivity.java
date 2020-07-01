package sg.gov.dsta.DroneControl;
import static java.lang.Math.round;


import android.Manifest;
import android.animation.LayoutTransition;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.dji.dsta.DroneControl.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.flightcontroller.FlightControllerState;
import dji.common.gimbal.GimbalState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.flightcontroller.Compass;
import dji.sdk.flightcontroller.FlightController;
import dji.common.error.DJIError;
import dji.sdk.media.DownloadListener;
import dji.sdk.media.MediaFile;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainActivity extends FragmentActivity implements View.OnTouchListener, View.OnClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback {

    protected static final String TAG = "MainActivity";
    private static int deviceHeight, deviceWidth;
    private static final int maximumWaypoints = 5;


    //Map
    private static GoogleMap gMap;
    private SupportMapFragment mapFragment;

    //Target Marking Switch
    private LinearLayout targetMarkingLinearLayout;
    private TextView targetMarkingTextView;
    private Switch targetMarkingSwitch;

    //Locate Drone, Locate Me
    private LinearLayout locateButtonsLayout;
    private ImageButton locateDroneButton, locateMeButton;

    //Waypoint Flight Mission
    private RelativeLayout waypointManagerRelativeLayout;
    private Button addWaypoint, backToMissionSelectorButton, clearWaypoints, nextToConfigState, backToWaypointManager, startFlight, stopFlight;
    private LinearLayout clearAndNextButtonLayout;
    private TextView panelTitle, panelSubtitle;

    private List<Waypoint> waypointList = new ArrayList<>();
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();

    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator waypointMissionOperator;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;

    private boolean configSucess, uploadSuccess;

    private AlertDialog configDialog;

    private boolean isAdd = false;


    //Dashboard Widget
    private LinearLayout dashboardWidgetContainer;

    //Bottom Corner Overlay
    private static RelativeLayout cameraOverlayLayout;
    private RelativeLayout mapOverlayLayout;
    private boolean cameraIsOverlayed;

    //Target Marking Dialog
    private AlertDialog targetMarkingDialog;
    private boolean isTargetMarkingEnabled = false;
    private ImageView imageView;
    private Double imageHeight;

    //Download Screen
    private RelativeLayout downloadingScreenLayout;
    private ProgressBar downloadingProgressBar;
    private int currentDownloadProgress = -1;

    //Drone Stats
    public static double droneLocationLat = 181, droneLocationLng = 181;
    public static double droneLocationAltitude = 0.0;
    public static double droneCameraPitch = 0.0;
    private Marker droneMarker = null;

    private float altitude = 100.0f;
    private float mSpeed = 10.0f;
    private static Compass mCompass;

    //Storage of Image Taken
    public static File latestImageTaken;
    File destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/DroneControl/");

    //My Location
    private FusedLocationProviderClient fusedLocationClient;





    //Application State Methods

    @Override
    protected void onResume(){
        super.onResume();
        initFlightController();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        unregisterReceiver(mReceiver);
        removeWaypointMissionOperatorListener();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_main1);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);


        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);


        mapFragment.getMapAsync(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);




        addWaypointMissionOperatorListener();

        initUI();

        getDeviceWidthHeight();

        setMediaFileCallback();



    }




    //UI Setup Methods

    private void initUI() {

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(Color.BLACK);
        }


        addWaypoint = findViewById(R.id.btn_waypoint_mission);
        backToMissionSelectorButton = findViewById(R.id.btn_mission_selector);
        clearWaypoints = findViewById(R.id.btn_clear_waypoint);
        nextToConfigState = findViewById(R.id.btn_next_configure);
        backToWaypointManager = findViewById(R.id.btn_back_to_waypoint_selected_state);
        startFlight = findViewById(R.id.btn_start_flight);
        stopFlight = findViewById(R.id.btn_stop_flight);

        addWaypoint.setOnClickListener(this);
        backToMissionSelectorButton.setOnClickListener(this);
        clearWaypoints.setOnClickListener(this);
        nextToConfigState.setOnClickListener(this);
        backToWaypointManager.setOnClickListener(this);
        startFlight.setOnClickListener(this);
        stopFlight.setOnClickListener(this);

        locateDroneButton = findViewById(R.id.locate_drone_button);
        locateMeButton = findViewById(R.id.locate_me_button);
        locateDroneButton.setOnClickListener(this);
        locateMeButton.setOnClickListener(this);

        waypointManagerRelativeLayout = findViewById(R.id.waypoint_manager_relative_layout);
        panelTitle = findViewById(R.id.panel_title);
        panelSubtitle = findViewById(R.id.panel_subtitle);

        clearAndNextButtonLayout = findViewById(R.id.clear_and_next_btns);

        locateButtonsLayout = findViewById(R.id.locate_button_layout);

        cameraOverlayLayout = findViewById(R.id.camera_overlay_layout);
        LayoutTransition cameraLayoutTransition = cameraOverlayLayout.getLayoutTransition();
        cameraLayoutTransition.enableTransitionType(LayoutTransition.CHANGING);


        dashboardWidgetContainer = findViewById(R.id.dashboard_widget_container);

        RelativeLayout maskView = findViewById(R.id.maskView);
        maskView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraIsOverlayed) {

                    switchToCameraView();

                }
            }
        });



        mapOverlayLayout = findViewById(R.id.map_overlay_layout);
        LayoutTransition mapLayoutTransition = mapOverlayLayout.getLayoutTransition();
        mapLayoutTransition.enableTransitionType(LayoutTransition.CHANGING);

        targetMarkingLinearLayout = findViewById(R.id.target_marking_linearlayout);
        targetMarkingTextView = findViewById(R.id.target_marking_textview);
        targetMarkingSwitch = findViewById(R.id.target_marking_switch);

        targetMarkingLinearLayout.setVisibility(View.GONE);
        targetMarkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    enableTargetMarking();
                    isTargetMarkingEnabled = true;


                } else {
                    disableTargetMarking();
                    isTargetMarkingEnabled = false;


                }
            }
        });


        downloadingScreenLayout = findViewById(R.id.downloading_screen_layout);
        downloadingProgressBar = findViewById(R.id.downloading_progress_bar);



        cameraIsOverlayed = true;


    }

    private void getDeviceWidthHeight() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        deviceHeight = displayMetrics.heightPixels;
        deviceWidth = displayMetrics.widthPixels;

    }




    //Drone Setup Methods

    private void initFlightController() {

        final BaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();

                mCompass = mFlightController.getCompass();

                product.getGimbal().setStateCallback(new GimbalState.Callback() {
                    @Override
                    public void onUpdate(GimbalState gimbalState) {
                        droneCameraPitch = gimbalState.getAttitudeInDegrees().getPitch();
//                        setResultToToast(String.valueOf(droneCameraPitch));
                    }
                });


            }
        }

        if (mFlightController != null) {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {

                @Override
                public void onUpdate(FlightControllerState djiFlightControllerCurrentState) {
                    droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                    droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                    droneLocationAltitude = djiFlightControllerCurrentState.getAircraftLocation().getAltitude();
                    updateDroneLocation();


                }
            });
        }
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange()
    {
        initFlightController();
    }

    private void setMediaFileCallback() {

        BaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null) {

            Camera camera = product.getCamera();
            if (camera != null) {

                camera.setMediaFileCallback(new MediaFile.Callback() {
                    @Override
                    public void onNewFile(MediaFile mediaFile) {

                        String fileName = "Drone"+getTime();
                        saveHighResImage(mediaFile, destDir, fileName);

                    }
                });
            }

        }

//        Camera camera = DJIDemoApplication.getProductInstance().getCamera();
//        camera.setMediaFileCallback(new MediaFile.Callback() {
//            @Override
//            public void onNewFile(MediaFile mediaFile) {
//
//                String fileName = "Drone"+getTime();
//                saveHighResImage(mediaFile, destDir, fileName);
//
//            }
//        });

    }

    public static Double getDroneHeading() {

        Double heading = 0.0;

        if (mCompass!=null) {
            heading = (double) mCompass.getHeading();
        }


//        DJIDemoApplication.getProductInstance().getGimbal().setStateCallback(new GimbalState.Callback() {
//            @Override
//            public void onUpdate(GimbalState gimbalState) {
//                droneCameraPitch = gimbalState.getAttitudeInDegrees().getPitch();
//            }
//        });

        return heading;

    }




    //Map Methods

    @Override
    public void onMapClick(LatLng point) {

        if (!cameraIsOverlayed) {

            switchToMapView();
        }


        if (isAdd == true && waypointList.size() < maximumWaypoints){

            backToMissionSelectorButton.setVisibility(View.GONE);
            clearAndNextButtonLayout.setVisibility(View.VISIBLE);


            markWaypoint(point);
            updateSubtitleWaypointList(point);
            addWaypointToArrayList(point);


        }else{
            setResultToToast("Cannot Add Waypoint");
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (gMap == null) {
            gMap = googleMap;
            setUpMap();
        }


        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }



        LatLng singapore = new LatLng(1.384057, 103.692025);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(singapore, 14));



        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                if (cameraIsOverlayed) {

                    LatLng newLatLng = getOffSetLatLng(marker.getPosition());
                    gMap.animateCamera(CameraUpdateFactory.newLatLng(newLatLng));

                }

                return true; // Consume the event since it was dealt with
            }
        });



    }

    private void setUpMap() {
        gMap.setOnMapClickListener(this);// add the listener for click for amap object
        gMap.setMyLocationEnabled(true);
        gMap.getUiSettings().setMyLocationButtonEnabled(false);

    }




    //OnClick & OnTouch

    @Override
    public void onClick(View v) {
        switch (v.getId()) {


            case R.id.btn_waypoint_mission:{

                isAdd = true;
                goToWaypointMissionBuilderState();

                break;
            }

            case R.id.btn_mission_selector:{

                isAdd = false;
                backToMissionSelectorState();

                break;
            }

            case R.id.btn_clear_waypoint:{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gMap.clear();
                    }

                });
                waypointList.clear();
                waypointMissionBuilder.waypointList(waypointList);
                waypointMissionBuilder = null;
                updateDroneLocation();

                backToWaypointMissionBuilderState();

                break;
            }

            case R.id.btn_next_configure:{
                goToWaypointConfigState();
                break;
            }

            case R.id.btn_start_flight:{
                startWaypointMission();

                break;
            }

            case R.id.btn_stop_flight:{
                stopWaypointMission();
//                goToMissionStoppedState();

                break;
            }
            case R.id.btn_back_to_waypoint_selected_state:{

                backToWaypointSelectedState();

                break;
            }

            case R.id.locate_drone_button: {

                updateCameraToDroneLocation();

                break;
            }

            case R.id.locate_me_button: {

                updateCameraToMyLocation();

                break;
            }

            default:
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        return false;
    }




    //Locating Me & Drone Methods

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    private void updateDroneLocation(){

        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.anchor(0.5f, 0.5f);
        Bitmap droneBitmap = getBitmapFromDrawable(R.drawable.drone_icon_map);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(droneBitmap));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = gMap.addMarker(markerOptions);
                }
            }
        });
    }

    private void updateCameraToMyLocation(){

        if (cameraIsOverlayed) {


            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {

                                LatLng offSetLatLng = getOffSetLatLng(new LatLng(location.getLatitude(), location.getLongitude()));

                                CameraPosition cameraPosition = new CameraPosition.Builder().target(offSetLatLng)// Sets the center of the map to location user
                                        .zoom(17)                   // Sets the zoom
                                        .bearing(0)                // Sets the orientation of the camera to east
                                        .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                                        .build();                   // Creates a CameraPosition from the builder

                                gMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                            }
                        }
                    });


        } else {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {

                                CameraPosition cameraPosition = new CameraPosition.Builder()
                                        .target(new LatLng(location.getLatitude(), location.getLongitude()))// Sets the center of the map to location user
                                        .zoom(17)                   // Sets the zoom
                                        .bearing(0)                // Sets the orientation of the camera to east
                                        .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                                        .build();                   // Creates a CameraPosition from the builder
                                gMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                            }
                        }
                    });

        }
    }

    private void updateCameraToDroneLocation(){

        updateDroneLocation();

        if (cameraIsOverlayed) {

            LatLng offSetLatLng = getOffSetLatLng(new LatLng(droneLocationLat, droneLocationLng));
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(offSetLatLng)// Sets the center of the map to location user
                    .zoom(17)                   // Sets the zoom
                    .bearing(0)                // Sets the orientation of the camera to east
                    .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            gMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        } else {

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(droneLocationLat, droneLocationLng))// Sets the center of the map to location user
                    .zoom(17)                   // Sets the zoom
                    .bearing(0)                // Sets the orientation of the camera to east
                    .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            gMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));


        }


    }

    private LatLng getOffSetLatLng(LatLng latLng) {
        // Calculate required horizontal shift for current screen density
        final int dX = getResources().getDimensionPixelSize(R.dimen.map_dx);
        // Calculate required vertical shift for current screen density
        final int dY = getResources().getDimensionPixelSize(R.dimen.map_dy);
        final Projection projection = gMap.getProjection();
        final Point markerPoint = projection.toScreenLocation(latLng);


        // Shift the point we will use to center the map
        markerPoint.offset(dX, dY);
        final LatLng newLatLng = projection.fromScreenLocation(markerPoint);

        return newLatLng;
    }




    //Waypoint Mission Methods

    private void markWaypoint(LatLng point){
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);

        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        Marker marker = gMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    private void addWaypointToArrayList(LatLng point) {

        Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);

        //Add Waypoints to Waypoint arraylist;
        if (waypointMissionBuilder != null) {


            waypointList.add(mWaypoint);
            waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());

        } else
        {

            waypointMissionBuilder = new WaypointMission.Builder();
            waypointList.add(mWaypoint);
            waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
        }
    }

    private void configWayPointMission(){

        configSucess = false;

        if (waypointMissionBuilder == null){

            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }else
        {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }

        if (waypointMissionBuilder.getWaypointList().size() > 0){

            for (int i=0; i< waypointMissionBuilder.getWaypointList().size(); i++){
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
            }

            setResultToToast("Set Waypoint attitude successfully");
        }

        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("loadWaypoint succeeded");
            configSucess = true;
        } else {
            setResultToToast("loadWaypoint failed " + error.getDescription());
            configSucess = false;
        }
    }

    private void uploadWayPointMission(){

        uploadSuccess = false;

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    setResultToToast("Mission upload successfully!");
                    uploadSuccess = true;

                    if (configSucess && uploadSuccess) {

                        goToMissionReadyState();
                        isAdd = false;

                    } else {
                        setResultToToast("Config success is "+ Boolean.toString(configSucess)+ "Upload success is "+Boolean.toString(uploadSuccess));
                    }


                } else {
                    setResultToToast("Mission upload failed, error: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                    uploadSuccess = false;

                }
            }
        });

    }

    private void startWaypointMission(){

        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));

                if (error == null) {goToMissionOngoingState();}
            }
        });
    }

    private void stopWaypointMission(){

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }




    //Waypoint Mission Operator Methods

    private void addWaypointMissionOperatorListener() {
        if (getWaypointMissionOperator() != null){
            getWaypointMissionOperator().addListener(eventNotificationListener);
        }
    }

    private void removeWaypointMissionOperatorListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
        }
    }

    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {

            setResultToToast("onDownloadUpdate");
        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {

            setResultToToast("onUploadUpdate");

        }

        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {

            if (executionEvent.getCurrentState() == WaypointMissionState.UNKNOWN) {
                setResultToToast("UKNOWNSTATE");
            } else if (executionEvent.getCurrentState() == WaypointMissionState.UPLOADING) {
                setResultToToast("UPLOADINGSTATE");
            } else if (executionEvent.getCurrentState() == WaypointMissionState.READY_TO_UPLOAD) {
                setResultToToast("READYTOUPLOADSTATE");
            } else if (executionEvent.getCurrentState() == WaypointMissionState.DISCONNECTED) {
                setResultToToast("DISCONNECTEDSTATE");
            } else if (executionEvent.getCurrentState() == WaypointMissionState.EXECUTING) {
                setResultToToast("EXECUTINGSTATE");
            } else if (executionEvent.getCurrentState() == WaypointMissionState.EXECUTION_PAUSED) {
                setResultToToast("EXECUTIONPAUSEDSTATE");
            } else if (executionEvent.getCurrentState() == WaypointMissionState.NOT_SUPPORTED) {
                setResultToToast("EXECUTIONNOTSUPPORTEDSTATE");
            } else if (executionEvent.getCurrentState() == WaypointMissionState.READY_TO_EXECUTE) {
                setResultToToast("READYTOEXECUTESTATE");
            } else if (executionEvent.getCurrentState() == WaypointMissionState.RECOVERING) {
                setResultToToast("RECOVERINGSTATE");
            }



        }

        @Override
        public void onExecutionStart() {

            setResultToToast("onExecutionStart");

        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
            setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));

            setResultToToast("onExecutionFinish");

            goToMissionStoppedState();



        }
    };

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (waypointMissionOperator == null) {
            if (DJISDKManager.getInstance().getMissionControl() != null){
                waypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
            }
        }
        return waypointMissionOperator;
    }




    //Waypoint Manager Panel UI Movements

    private void goToWaypointMissionBuilderState(){

        panelSubtitle.setText(getResources().getString(R.string.subtitle_tap_map_message));
        addWaypoint.setVisibility(View.GONE);
        backToMissionSelectorButton.setVisibility(View.VISIBLE);

    }

    private void backToMissionSelectorState() {

        panelSubtitle.setText(getResources().getString(R.string.subtitle_no_waypoints_created));
        backToMissionSelectorButton.setVisibility(View.GONE);
        addWaypoint.setVisibility(View.VISIBLE);

    }

    private void updateSubtitleWaypointList(LatLng point) {

        //Add Waypoints to Waypoint arraylist;
        if (waypointMissionBuilder != null) {

            String s = panelSubtitle.getText().toString();

//            String newPoint = String.valueOf(round(point.latitude, 2)) + " " + String.valueOf(round(point.longitude, 2));
            String newPoint = new Calculations().convertCoordToMGR(point.latitude, point.longitude);


            panelSubtitle.setText(s + "MGR: " + newPoint + "\n");

        } else
        {
            panelSubtitle.setText("");
            String s = panelSubtitle.getText().toString();

//            String newPoint = String.valueOf(round(point.latitude, 2)) + " " + String.valueOf(round(point.longitude, 2));
            String newPoint = new Calculations().convertCoordToMGR(point.latitude, point.longitude);

            panelSubtitle.setText(s + "MGR: " + newPoint + "\n");

        }

    }

    private void backToWaypointMissionBuilderState() {

        clearAndNextButtonLayout.setVisibility(View.GONE);
        backToMissionSelectorButton.setVisibility(View.VISIBLE);
        panelSubtitle.setText(getResources().getString(R.string.subtitle_tap_map_message));

    }

    private void goToWaypointConfigState() {
        showConfigDialog();
    }

    private void showConfigDialog(){
        RelativeLayout wayPointSettings = (RelativeLayout)getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);

        SeekBar altitude_seekbar = wayPointSettings.findViewById(R.id.altitude_seekbar);
        final TextView wpAltitude_TV = (TextView) wayPointSettings.findViewById(R.id.altitude);
        wpAltitude_TV.setText(String.valueOf(altitude_seekbar.getProgress()));
        RadioGroup speed_RG = (RadioGroup) wayPointSettings.findViewById(R.id.speed);
        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);


        altitude_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int seekBarProgress = seekBar.getProgress();
                wpAltitude_TV.setText(String.valueOf(seekBarProgress));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {


            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.lowSpeed){
                    mSpeed = 3.0f;
                } else if (checkedId == R.id.MidSpeed){
                    mSpeed = 5.0f;
                } else if (checkedId == R.id.HighSpeed){
                    mSpeed = 10.0f;
                }
            }

        });

        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select finish action");
                if (checkedId == R.id.finishNone){
                    mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                } else if (checkedId == R.id.finishGoHome){
                    mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                } else if (checkedId == R.id.finishAutoLanding){
                    mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                } else if (checkedId == R.id.finishToFirst){
                    mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        });

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select heading");

                if (checkedId == R.id.headingNext) {
                    mHeadingMode = WaypointMissionHeadingMode.AUTO;
                } else if (checkedId == R.id.headingInitDirec) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                } else if (checkedId == R.id.headingRC) {
                    mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                } else if (checkedId == R.id.headingWP) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                }
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(wayPointSettings);



        configDialog = builder.create();
        configDialog.show();


        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(configDialog.getWindow().getAttributes());
        float factor = getApplicationContext().getResources().getDisplayMetrics().density;
        lp.width = (int)(660 * factor);
        configDialog.getWindow().setLayout(lp.width, lp.height);




        Button uploadFlightConfiguration = wayPointSettings.findViewById(R.id.btn_upload_flight_configuration);
        uploadFlightConfiguration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String altitudeString = wpAltitude_TV.getText().toString();
                altitude = Integer.parseInt(nulltoIntegerDefalt(altitudeString));
                Log.e(TAG,"altitude "+altitude);
                Log.e(TAG,"speed "+mSpeed);
                Log.e(TAG, "mFinishedAction "+mFinishedAction);
                Log.e(TAG, "mHeadingMode "+mHeadingMode);
                configWayPointMission();
                uploadWayPointMission();
            }
        });

        Button cancelFlightConfiguration = wayPointSettings.findViewById(R.id.btn_cancel_flight_configuration);
        cancelFlightConfiguration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                configDialog.dismiss();

            }
        });

    }

    private void goToMissionReadyState() {

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                backToWaypointManager.setVisibility(View.VISIBLE);
                startFlight.setText(getResources().getString(R.string.button_start_flight));
                startFlight.setVisibility(View.VISIBLE);
                configDialog.dismiss();

                panelTitle.setText(getResources().getString(R.string.panel_title_mission_uploaded));
                panelSubtitle.setVisibility(View.GONE);
                clearAndNextButtonLayout.setVisibility(View.GONE);
            }
        });

    }

    private void goToMissionOngoingState(){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                panelTitle.setText(getResources().getString(R.string.panel_title_mission_ongoing));

                backToWaypointManager.setVisibility(View.GONE);
                startFlight.setVisibility(View.GONE);
                stopFlight.setVisibility(View.VISIBLE);
            }
        });
    }

    private void goToMissionStoppedState() {

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                panelTitle.setText(getResources().getString(R.string.panel_title_mission_over));
                backToWaypointManager.setVisibility(View.VISIBLE);
                stopFlight.setVisibility(View.GONE);
            }
        });


    }

    private void backToWaypointSelectedState() {

        startFlight.setVisibility(View.GONE);
        backToWaypointManager.setVisibility(View.GONE);
        clearAndNextButtonLayout.setVisibility(View.VISIBLE);
        panelSubtitle.setVisibility(View.VISIBLE);
        panelTitle.setText(getResources().getString(R.string.panel_title_waypoint_manager));

    }




    //Target Marking Methods

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void enableTargetMarking() {
        targetMarkingTextView.setText(getResources().getString(R.string.disable_target_marking));

        //hide bottom stuff
//        mapOverlayLayout.setVisibility(View.GONE);
        dashboardWidgetContainer.setVisibility(View.GONE);
        locateButtonsLayout.setVisibility(View.GONE);

        DrawingView drawingView = findViewById(R.id.drawing_view);
        drawingView.setVisibility(View.VISIBLE);
        drawingView.clearCanvas();

//        drawingView.imageHeight = (double) deviceHeight;
        imageHeight = (double) deviceHeight;




    }

    private void disableTargetMarking() {
        targetMarkingTextView.setText(getResources().getString(R.string.enable_target_marking));

        //show bottom stuff
        mapOverlayLayout.setVisibility(View.VISIBLE);
        dashboardWidgetContainer.setVisibility(View.VISIBLE);
        locateButtonsLayout.setVisibility(View.VISIBLE);

        DrawingView drawingView = findViewById(R.id.drawing_view);
        drawingView.setVisibility(View.GONE);

    }

    private void markTarget(final LatLng point){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Create MarkerOptions object
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(point);
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                Marker marker = gMap.addMarker(markerOptions);
            }
        });

    }

    private void showTargetMarkingDialog() {

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        RelativeLayout targetMarkingLayout = (RelativeLayout) inflater.inflate(R.layout.dialog_targetmarking, null);

        //Initialise dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(targetMarkingLayout);
        targetMarkingDialog = builder.create();
        targetMarkingDialog.show();


        //Set dimensions of dialog
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(targetMarkingDialog.getWindow().getAttributes());
        float factor = getResources().getDisplayMetrics().density;
        lp.width = (int)(660 * factor);
        targetMarkingDialog.getWindow().setLayout(lp.width, lp.height);


        final RadioGroup targetTypeRG = (RadioGroup) targetMarkingLayout.findViewById(R.id.target_type_radio_group);


        Button doneButton = targetMarkingLayout.findViewById(R.id.btn_done_dialog);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                double perspectiveHeightInM = getPerspectiveHeight(targetTypeRG.getCheckedRadioButtonId());


                double LOSDistanceInM = new Calculations().getDistanceFromDroneCamera(perspectiveHeightInM, imageHeight, DrawingView.objectHeight);
                double groundDistanceInM = new Calculations().getGroundDistancefromLOSDistance(droneLocationAltitude, droneCameraPitch, LOSDistanceInM);
                LatLng targetLatLng = new Calculations().getNewLocation(groundDistanceInM, getDroneHeading(), droneLocationLat, droneLocationLng);
                markTarget(targetLatLng);

                setResultToToast(
                        "\nHeading: "+ getDroneHeading() +
                                "\nAltitude: "+ droneLocationAltitude +
                                "\nCamera Pitch: "+ droneCameraPitch +

                                "\n\nPerspectiveHeight: "+ Math.round(perspectiveHeightInM) + "M" +
                                "\nLOS Distance: "+ Math.round(LOSDistanceInM) + "M" +
                                "\nGround Distance: "+ Math.round(groundDistanceInM) + "M"

                );

                targetMarkingDialog.dismiss();

            }
        });

        Button cancelButton = targetMarkingLayout.findViewById(R.id.btn_cancel_dialog);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                targetMarkingDialog.dismiss();

            }
        });

        imageView = targetMarkingLayout.findViewById(R.id.dialog_target_screenshot);

        if (MainActivity.latestImageTaken != null) {

            if (MainActivity.latestImageTaken.exists()) {

                Bitmap bitmapDecodedFromFile = BitmapFactory.decodeFile(MainActivity.latestImageTaken.getAbsolutePath());
                if (bitmapDecodedFromFile !=null) {

                    DrawingView.croppedRect.offset(0, 120);
                    Bitmap b = Bitmap.createScaledBitmap(bitmapDecodedFromFile, deviceWidth, deviceHeight, false);
                    Bitmap b1 = getCroppedBitmap(DrawingView.croppedRect, b);
                    imageView.setImageBitmap(b1);
                }
            } else {
                setResultToToast("new File does not exist");
            }

        } else {
            setResultToToast("new File is null");
        }


    }

    private double getPerspectiveHeight(int checkedRadioButtonId) {

        TargetTypes.TargetType targetType = TargetTypes.TargetType.Unknown;

        double perspectiveHeight = 0.0 ;

        switch (checkedRadioButtonId) {

            case R.id.target_type_personel: {
                targetType = TargetTypes.TargetType.Personnel;

                break;
            }

            case R.id.target_type_tank: {
                targetType = TargetTypes.TargetType.Tank;

                break;
            }

        }

        perspectiveHeight = new Calculations().getPerspectiveHeight(targetType.height, targetType.length, targetType.width, droneCameraPitch);

        return perspectiveHeight;

    }

    private Bitmap getCroppedBitmap(Rect cropRect, Bitmap orgBitmap) {

        cropRect.sort();

        if(cropRect.left < 0)
            cropRect.left = 0;
        if(cropRect.top < 0)
            cropRect.top = 0;
        if(cropRect.right > orgBitmap.getWidth())
            cropRect.right = orgBitmap.getWidth();
        if(cropRect.bottom > orgBitmap.getHeight())
            cropRect.bottom = orgBitmap.getHeight();

        Log.d("CropPoints", "(" + cropRect.left + "," + cropRect.top + ")" + " (" + cropRect.right + "," + cropRect.bottom + ")");
        Log.d("CropPoints", "Bitmap width: "+orgBitmap.getWidth()+ " Bitmap height: "+ orgBitmap.getHeight());


        int crop_width = Math.abs(cropRect.right - cropRect.left);
        int crop_height = Math.abs(cropRect.bottom - cropRect.top);

        return Bitmap.createBitmap(orgBitmap,cropRect.left,cropRect.top,crop_width,crop_height);
    }



    //Save Image File From Drone Methods

    static String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void saveHighResImage(MediaFile mediaFile, File destDir, final String fileName) {



        mediaFile.fetchFileData(destDir, fileName, new DownloadListener<String>() {

            @Override
            public void onStart() {
                showDownloadLayout();
                currentDownloadProgress = -1;
            }

            @Override
            public void onRateUpdate(long total, long current, long persize) {

                int tmpProgress = (int) (1.0 * current / total * 100);
                if (tmpProgress != currentDownloadProgress) {
                    downloadingProgressBar.setProgress(tmpProgress);
                    currentDownloadProgress = tmpProgress;
                }

            }

            @Override
            public void onProgress(long l, long l1) {

            }

            @Override
            public void onSuccess(String s) {

                hideDownloadLayout();
                currentDownloadProgress = -1;


                String newFileString = s + "/" + fileName+".jpg";
                latestImageTaken = new File(newFileString);
                setResultToToast("Successfully Saved"+newFileString);

                if (isTargetMarkingEnabled) {
                    showTargetMarkingDialog();
                }


            }

            @Override
            public void onFailure(DJIError djiError) {
                setResultToToast("Error Saving" + djiError.toString());
                currentDownloadProgress = -1;

            }
        });
    }

    private void hideDownloadLayout() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                downloadingScreenLayout.setVisibility(View.GONE);
                downloadingProgressBar.setProgress(0);

            }
        });
    }

    private void showDownloadLayout() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                downloadingScreenLayout.setVisibility(View.VISIBLE);
                downloadingProgressBar.setProgress(0);

            }
        });
    }




    //Map View / Camera View Toggle

    public static void sendViewToBack(final View child) {
        final ViewGroup parent = (ViewGroup)child.getParent();
        if (null != parent) {
            parent.removeView(child);
            parent.addView(child, 0);
        }
    }

    private void resizeChildrenToMatchParent(ViewGroup parent) {
        // size is in pixels so make sure you have taken device display density into account
        int childCount = parent.getChildCount();
        for(int i = 0; i < childCount; i++) {
            View v = parent.getChildAt(i);
            v.setLayoutParams(  new RelativeLayout.LayoutParams(
                    // or ViewGroup.LayoutParams.WRAP_CONTENT
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    // or ViewGroup.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT ) );

        }
    }

    private void switchToCameraView() {

        //expanding camera view
        ViewGroup.LayoutParams layoutParams = cameraOverlayLayout.getLayoutParams();
        RelativeLayout.LayoutParams relativeLayoutParams =  new RelativeLayout.LayoutParams(layoutParams);
        relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        float dpRatio = getApplicationContext().getResources().getDisplayMetrics().density;
        int pixelForDp = (int) (16 * dpRatio);
        relativeLayoutParams.setMargins(pixelForDp,pixelForDp,pixelForDp,pixelForDp);

        cameraOverlayLayout.setLayoutParams(
                new RelativeLayout.LayoutParams(
                        // or ViewGroup.LayoutParams.WRAP_CONTENT
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        // or ViewGroup.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT ) );


        mapOverlayLayout.setLayoutParams(relativeLayoutParams);
        dashboardWidgetContainer.setVisibility(View.VISIBLE);
        resizeChildrenToMatchParent(cameraOverlayLayout);
        waypointManagerRelativeLayout.setVisibility(View.GONE);
        sendViewToBack(cameraOverlayLayout);

        targetMarkingLinearLayout.setVisibility(View.VISIBLE);

        cameraIsOverlayed = false;

    }

    private void switchToMapView() {

        //Expanding map view
        ViewGroup.LayoutParams layoutParams = mapOverlayLayout.getLayoutParams();
        RelativeLayout.LayoutParams relativeLayoutParams =  new RelativeLayout.LayoutParams(layoutParams);
        relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        float dpRatio = getApplicationContext().getResources().getDisplayMetrics().density;
        int pixelForDp = (int) (16 * dpRatio);
        relativeLayoutParams.setMargins(pixelForDp,pixelForDp,pixelForDp,pixelForDp);

        mapOverlayLayout.setLayoutParams(
                new RelativeLayout.LayoutParams(
                        // or ViewGroup.LayoutParams.WRAP_CONTENT
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        // or ViewGroup.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT ) );

        cameraOverlayLayout.setLayoutParams(relativeLayoutParams);
        dashboardWidgetContainer.setVisibility(View.GONE);
        waypointManagerRelativeLayout.setVisibility(View.VISIBLE);
        sendViewToBack(mapOverlayLayout);

        targetMarkingLinearLayout.setVisibility(View.GONE);

        cameraIsOverlayed = true;

    }




    //Supporting Methods

    private void setResultToToast(final String string){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    String nulltoIntegerDefalt(String value){
        if(!isIntValue(value)) value="0";
        return value;
    }

    boolean isIntValue(String val)
    {
        try {
            val=val.replace(" ","");
            Integer.parseInt(val);
        } catch (Exception e) {return false;}
        return true;
    }

    private Bitmap getBitmapFromDrawable(int drawableRes) {
        Drawable drawable = getResources().getDrawable(drawableRes);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);

        return bitmap;
    }






}
