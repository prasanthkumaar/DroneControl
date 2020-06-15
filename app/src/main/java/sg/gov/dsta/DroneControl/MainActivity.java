package sg.gov.dsta.DroneControl;

import android.Manifest;
import android.animation.LayoutTransition;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dji.dsta.DroneControl.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.common.error.DJIError;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class MainActivity extends FragmentActivity implements View.OnTouchListener, View.OnClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback {

    protected static final String TAG = "GSDemoActivity";

    private GoogleMap gMap;
    private SupportMapFragment mapFragment;

    private RelativeLayout waypointManagerRelativeLayout;


    private boolean cameraIsOverlayed;

    private Button addWaypoint, backToDefaultState, clearWaypoints, nextToConfigState, backToWaypointManager, startFlight, stopFlight;
    private LinearLayout clearNextButtons;
    private TextView panelTitle, panelSubtitle;

    private RelativeLayout cameraOverlayLayout;
    private RelativeLayout mapOverlayLayout;

    private boolean configSucess, uploadSuccess;

    private AlertDialog configDialog;

    private boolean isAdd = false;

    private double droneLocationLat = 181, droneLocationLng = 181;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Marker droneMarker = null;

    private float altitude = 100.0f;
    private float mSpeed = 10.0f;

    private List<Waypoint> waypointList = new ArrayList<>();

    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator waypointMissionOperator;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;

    //new1
    private WaypointMissionOperatorListener waypointMissionOperatorListener;

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
        removeListener();
        super.onDestroy();
    }

    /**
     * @Description : RETURN Button RESPONSE FUNCTION
     */
    public void onReturn(View view){
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void setResultToToast(final String string){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initUI() {

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(Color.BLACK);
        }


        addWaypoint = findViewById(R.id.btn_add_waypoint);
        backToDefaultState = findViewById(R.id.btn_back_to_default_state);
        clearWaypoints = findViewById(R.id.btn_clear_waypoint);
        nextToConfigState = findViewById(R.id.btn_next_configure);
        backToWaypointManager = findViewById(R.id.btn_back_to_waypoint_manager);
        startFlight = findViewById(R.id.btn_start_flight);
        stopFlight = findViewById(R.id.btn_stop_flight);

        addWaypoint.setOnClickListener(this);
        backToDefaultState.setOnClickListener(this);
        clearWaypoints.setOnClickListener(this);
        nextToConfigState.setOnClickListener(this);
        backToWaypointManager.setOnClickListener(this);
        startFlight.setOnClickListener(this);
        stopFlight.setOnClickListener(this);

        waypointManagerRelativeLayout = findViewById(R.id.waypoint_manager_relative_layout);
        panelTitle = findViewById(R.id.panel_title);
        panelSubtitle = findViewById(R.id.panel_subtitle);

        clearNextButtons = findViewById(R.id.clear_and_next_btns);

        cameraOverlayLayout = findViewById(R.id.camera_overlay_layout);
        LayoutTransition cameraLayoutTransition = cameraOverlayLayout.getLayoutTransition();
        cameraLayoutTransition.enableTransitionType(LayoutTransition.CHANGING);


        RelativeLayout maskView = findViewById(R.id.maskView);
        maskView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraIsOverlayed) {

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

                    resizeChildrenToMatchParent(cameraOverlayLayout);
                    waypointManagerRelativeLayout.setVisibility(View.GONE);
                    sendViewToBack(cameraOverlayLayout);
                    cameraIsOverlayed = false;

                }
            }
        });




//        cameraOverlayLayout.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//
//
//                if (cameraIsOverlayed) {
//
//                    ViewGroup.LayoutParams layoutParams = cameraOverlayLayout.getLayoutParams();
//                    RelativeLayout.LayoutParams relativeLayoutParams =  new RelativeLayout.LayoutParams(layoutParams);
//                    relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//                    relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
//                    relativeLayoutParams.setMargins(16,16,16,16);
//
//                    cameraOverlayLayout.setLayoutParams(
//                            new RelativeLayout.LayoutParams(
//                                    // or ViewGroup.LayoutParams.WRAP_CONTENT
//                                    RelativeLayout.LayoutParams.MATCH_PARENT,
//                                    // or ViewGroup.LayoutParams.WRAP_CONTENT,
//                                    RelativeLayout.LayoutParams.MATCH_PARENT ) );
//
//                    mapOverlayLayout.setLayoutParams(relativeLayoutParams);
//                    sendViewToBack(cameraOverlayLayout);
//                    cameraIsOverlayed = false;
//
//                }
//                return true;
//            }
//        });

        mapOverlayLayout = findViewById(R.id.map_overlay_layout);
        LayoutTransition mapLayoutTransition = mapOverlayLayout.getLayoutTransition();
        mapLayoutTransition.enableTransitionType(LayoutTransition.CHANGING);


        cameraIsOverlayed = true;

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

        addListener();

        initUI();


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
        //loginAccount();
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        setResultToToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    private void initFlightController() {

        BaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {

                @Override
                public void onUpdate(FlightControllerState djiFlightControllerCurrentState) {
                    droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                    droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                    updateDroneLocation();
                }
            });
        }
    }

    //Add Listener for WaypointMissionOperator
    private void addListener() {
        if (getWaypointMissionOperator() != null){
            getWaypointMissionOperator().addListener(eventNotificationListener);
        }
    }

    private void removeListener() {
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

    private void setUpMap() {
        gMap.setOnMapClickListener(this);// add the listener for click for amap object

    }

    @Override
    public void onMapClick(LatLng point) {
        if (!cameraIsOverlayed) {

                Log.d(TAG, "MAP OVERLAY CLICKED");

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
                waypointManagerRelativeLayout.setVisibility(View.VISIBLE);
                sendViewToBack(mapOverlayLayout);

                cameraIsOverlayed = true;
        }


        if (isAdd == true){
            markWaypoint(point);
            Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);
            //Add Waypoints to Waypoint arraylist;
            if (waypointMissionBuilder != null) {

                String s = panelSubtitle.getText().toString();
                String newPoint = String.valueOf(round(point.latitude, 2)) + " " + String.valueOf(round(point.longitude, 2));
                panelSubtitle.setText(s + newPoint + "\n");

                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            } else
            {
                panelSubtitle.setText("");
                String s = panelSubtitle.getText().toString();
                String newPoint = String.valueOf(round(point.latitude, 2)) + " " + String.valueOf(round(point.longitude, 2));
                panelSubtitle.setText(s + newPoint + "\n");



                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }

            clearNextButtons.setVisibility(View.VISIBLE);
            backToDefaultState.setVisibility(View.GONE);


//            panelSubtitle.setText("Marker list will be here");

        }else{
            setResultToToast("Cannot Add Waypoint");
        }
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    // Update the drone location based on states from MCU.
    private void updateDroneLocation(){

        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

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

    private void markWaypoint(LatLng point){
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = gMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {


            case R.id.locate:{
                updateDroneLocation();
                cameraUpdate(); // Locate the drone's place
                break;
            }

            case R.id.btn_add_waypoint:{
                isAdd = true;
                panelSubtitle.setText(getResources().getString(R.string.subtitle_tap_map_message));
                addWaypoint.setVisibility(View.GONE);
                backToDefaultState.setVisibility(View.VISIBLE);

                break;
            }

            case R.id.btn_back_to_default_state:{

                isAdd = false;
                panelSubtitle.setText(getResources().getString(R.string.subtitle_no_waypoints_created));
                addWaypoint.setVisibility(View.VISIBLE);
                backToDefaultState.setVisibility(View.GONE);

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

                clearNextButtons.setVisibility(View.GONE);
                backToDefaultState.setVisibility(View.VISIBLE);
                panelSubtitle.setText(getResources().getString(R.string.subtitle_tap_map_message));


                break;
            }
            case R.id.btn_next_configure:{
                showSettingDialog();
                break;
            }


            case R.id.btn_start_flight:{
                startWaypointMission();

                break;
            }
            case R.id.btn_stop_flight:{
                stopWaypointMission();

                panelTitle.setText(getResources().getString(R.string.panel_title_flight_stopped));

                backToWaypointManager.setVisibility(View.VISIBLE);
                stopFlight.setVisibility(View.GONE);

                break;
            }
            case R.id.btn_back_to_waypoint_manager:{
                startFlight.setVisibility(View.GONE);
                backToWaypointManager.setVisibility(View.GONE);
                clearNextButtons.setVisibility(View.VISIBLE);
                panelSubtitle.setVisibility(View.VISIBLE);
                panelTitle.setText(getResources().getString(R.string.panel_title_waypoint_manager));

                break;
            }

//            case R.id.camera_overlay_layout: {
//
//                if (cameraIsOverlayed) {
//
//                    ViewGroup.LayoutParams layoutParams = cameraOverlayLayout.getLayoutParams();
//                    RelativeLayout.LayoutParams relativeLayoutParams =  new RelativeLayout.LayoutParams(layoutParams);
//                    relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//                    relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
//                    relativeLayoutParams.setMargins(16,16,16,16);
//
//                    cameraOverlayLayout.setLayoutParams(
//                            new RelativeLayout.LayoutParams(
//                                    // or ViewGroup.LayoutParams.WRAP_CONTENT
//                                    RelativeLayout.LayoutParams.MATCH_PARENT,
//                                    // or ViewGroup.LayoutParams.WRAP_CONTENT,
//                                    RelativeLayout.LayoutParams.MATCH_PARENT ) );
//
//                    mapOverlayLayout.setLayoutParams(relativeLayoutParams);
//                    sendViewToBack(cameraOverlayLayout);
//                    cameraIsOverlayed = false;
//
//                }
//
//
//                break;
//            }


            default:
                break;
        }
    }

    private void cameraUpdate(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        gMap.moveCamera(cu);

    }

    private void enableDisableAdd(){
        if (isAdd == false) {
            isAdd = true;
        }else{
            isAdd = false;
        }
    }

    private void showSettingDialog(){
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

                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //NOT BECOMING VISIBLE!!!!
                                backToWaypointManager.setVisibility(View.VISIBLE);
                                startFlight.setText(getResources().getString(R.string.button_start_flight));
                                startFlight.setVisibility(View.VISIBLE);
                                configDialog.dismiss();


                                //move on
                                panelTitle.setText(getResources().getString(R.string.panel_title_flight_plan_uploaded));
                                panelSubtitle.setVisibility(View.GONE);
                                clearNextButtons.setVisibility(View.GONE);
                            }
                        });




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

                if (error == null) {

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            panelTitle.setText(getResources().getString(R.string.panel_title_flight_ongoing));

                            backToWaypointManager.setVisibility(View.GONE);
                            startFlight.setVisibility(View.GONE);
                            stopFlight.setVisibility(View.VISIBLE);
                        }
                    });



                }
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



    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public static void sendViewToBack(final View child) {
        final ViewGroup parent = (ViewGroup)child.getParent();
        if (null != parent) {
            parent.removeView(child);
            parent.addView(child, 0);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {



            return false;
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
}
