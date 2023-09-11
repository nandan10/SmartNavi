package khushboo.rohit.osmnavi;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.osmdroid.bonuspack.location.POI;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.overlay.Marker;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
//import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.HttpConnection;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.utils.HttpConnection;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int REQ_CODE_SPEECH_INPUT = 3;
    private static final int REQ_CODE_LOAD_ROUTE = 4;
    public static final int NAV_TYPE_LOAD_ROUTE = 2;
    public static final int NAV_TYPE_NAVIGATE = 0;
    public static final int NAV_TYPE_PREPLAN = 1;
    private static final String API_KEY = apikey;
    private static final String OSM_EDITING_URL = "https://api.openstreetmap.org/api/0.6//map?bbox=";
    private static final int RESULT_PICK_CONTACT = 1;
    private static final int REQ_CODE_PICK_CONTACT = 5;
    private final String TAG = MainActivity.class.getSimpleName();
    private final LinearLayout myLayout = null;
    MyItemizedOverlay myItemizedOverlay = null;
   // OSRMRoadManager roadManager;

    SQLiteDatabase db;
    MyApp app;

    private MediaRecorder myAudioRecorder;
    private String outputFile = null;
    boolean isNavigating = false;
    boolean isPreviewing = false;
    boolean isSelected = false;
    boolean isDebug = false;

    Button startVoiceRecording,stopVoiceRecording;
    Button startButton, saveButton;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    Location mLastLocation;
    AutocompleteSupportFragment endingDestination;
    TextToSpeech tts;

    Place destinationLatLng;
    View endingDestinationView;
    double currentLat, currentLong;
    private RoadNode node;
    private Marker nodeMarker;
    private OSRMRoadManager roadManager;
    private OverpassAPIProvider2 overpassProvider;

    int osmNumInstructions, osmNextInstruction, prefetch_nextInstruction, Instruction_progressTracker;

    String xingStr = "After ";



    ArrayList<GeoPoint> landmarks;
    ArrayList<GeoPoint> saved_landmarks;
    ArrayList<GeoPoint> tags;
    ArrayList<GeoPoint> poi_tags;
    ArrayList<String> instructions;
    ArrayList<String> saved_instructions;
    ArrayList<String> tagInstructions;
    ArrayList<Integer> tagCheck;
    ArrayList<Integer> landmarkCheck;

    ArrayList<Integer> tagLandmark;

    ArrayList<Long> timestamps;
    ArrayList<Long> tst;

    boolean isPreplanning = false;
    double current_lat, current_long;
    private MyLocationNewOverlay  myLocationOverlay;
    private MapView map;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (PackageManager.PERMISSION_GRANTED !=
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1339);
        }
        if (PackageManager.PERMISSION_GRANTED !=
                checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1340);
        }
        if (PackageManager.PERMISSION_GRANTED !=
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1341);
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
        MediaButtonIntentReceiver r = new MediaButtonIntentReceiver();
        filter.setPriority(10000); //this line sets receiver priority
        registerReceiver(r, filter);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_main);
        landmarks = new ArrayList<GeoPoint>();
        instructions = new ArrayList<String>();
        saved_landmarks = new ArrayList<GeoPoint>();

        saved_instructions = new ArrayList<String>();
        tags = new ArrayList<GeoPoint>();
        tagInstructions = new ArrayList<String>();
        tagCheck = new ArrayList<Integer>();
        landmarkCheck = new ArrayList<Integer>();
        tagLandmark = new ArrayList<Integer>();
        poi_tags = new ArrayList<GeoPoint>();

        timestamps = new ArrayList<Long>();
        tst = new ArrayList<Long>();
        roadManager = new OSRMRoadManager(this,"com.osm-navi.iit");
        //compass = new Compass(this);
        app = (MyApp) this.getApplicationContext();
        db = app.myDb;
        // button = (Button) findViewById(R.id.button8);
        // save_button = (Button) findViewById(R.id.button_save);
       // compass.start();

       // navType = getIntent().getIntExtra("Type",0);
       // System.out.println(navType);
       // MapView map = findViewById(R.id.map);
        startButton = findViewById(R.id.button8);
        saveButton = findViewById(R.id.button_save);
        roadManager = new OSRMRoadManager(this,"com.osm-navi.iit");
        tts = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if (status == TextToSpeech.SUCCESS) {
//                    int result=tts.setLanguage(Locale.US);
//                    if(result==TextToSpeech.LANG_MISSING_DATA ||
//                            result==TextToSpeech.LANG_NOT_SUPPORTED){
//                        Log.e("error", "This Language is not supported");
//                    }
                    if (!app.hasRefreshed) {
                        System.out.println("Starting app");
                        tts.speak("Welcome to OSM Navi.", TextToSpeech.QUEUE_ADD, null);
                        app.hasRefreshed = true;
                    }
//                    else{
//                        ConvertTextToSpeech();
//                    }
                } else
                    Log.e("error", "Initialization Failed!");
            }
        });

        buildGoogleApiClient();

        app = (MyApp) this.getApplicationContext();
        db = app.myDb;
        db.execSQL("CREATE TABLE IF NOT EXISTS myLocation(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat INT,long INT,description VARCHAR, timestamp INT, prev_id INT, next_id INT );");
        db.execSQL("CREATE TABLE IF NOT EXISTS myTags(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat INT,long INT, tag VARCHAR );");
        db.execSQL("CREATE TABLE IF NOT EXISTS locationByTag(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tag_id INTEGER, location_id INTEGER);");
        db.execSQL("CREATE TABLE IF NOT EXISTS trackData(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat DOUBLE, long DOUBLE, type INT, timestamp INT );");
        db.execSQL("CREATE TABLE IF NOT EXISTS trackData30(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat DOUBLE, long DOUBLE, type INT, timestamp INT );");
        db.execSQL("CREATE TABLE IF NOT EXISTS routes(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name VARCHAR, distance VARCHAR, start_lat INT, start_lon INT, end_lat INT, end_lon INT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS routebyinstructions(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routeid INTEGER, lat INT, long INT, description VARCHAR);");
        db.execSQL("CREATE TABLE IF NOT EXISTS tagroutebyinstructions(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routeid INTEGER, lat INT, long INT, description VARCHAR);");


        String apiKey = getString(R.string.places_api_key);

        /**
         * Initialize Places. For simplicity, the API key is hard-coded. In a production
         * environment we recommend using a secure mechanism to manage API keys.
         */
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
// Create a new Places client instance.
        PlacesClient placesClient = Places.createClient(this);

//        String apiKey;
//        if (!Places.isInitialized()) {
//            Places.initialize(getApplicationContext(), API_KEY);
//            PlacesClient placesClient = Places.createClient(this);
//        }
//
        endingDestination = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.ending_destination_2);
        assert endingDestination != null;
        endingDestination.setPlaceFields(Arrays.asList(Place.Field.LAT_LNG, Place.Field.ID, Place.Field.NAME));
        endingDestinationView = endingDestination.getView();
        endingDestination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    isSelected = true;
                    // TODO: Get info about the selected place.
                    destinationLatLng = place;
                    Log.i("endingDestination ", "Place: " + destinationLatLng.getLatLng());
                    Log.i("endingDestination ", "Place: " + place.getName());
                    tts.speak("Destination successfully set to " + place.getName(), TextToSpeech.QUEUE_ADD, null);
                }
                @Override
                public void onError(@NonNull Status status) {
                    // TODO: Handle the error.
                    Log.i("endingDestination ", "An error occurred: " + status);
                    tts.speak("There was an error setting up the destination. Please try again.", TextToSpeech.QUEUE_ADD, null);
                }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mGoogleApiClient!=null){
            if(!mGoogleApiClient.isConnected()){
                mGoogleApiClient.connect();
            }
        }
        if(mGoogleApiClient==null){
            buildGoogleApiClient();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();

    }
    @Override
    protected void onResume() {
        super.onResume();
        if(mGoogleApiClient!=null){
            if(!mGoogleApiClient.isConnected()){
                mGoogleApiClient.connect();
            }
        }
        if(mGoogleApiClient==null){
            buildGoogleApiClient();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mGoogleApiClient!=null){

            if(mGoogleApiClient.isConnected()){

                mGoogleApiClient.disconnect();
            }

        }
        while(tts.isSpeaking()){}
        tts.stop();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                endingDestination.setText(result.get(0));
            }
        } else if (requestCode == REQ_CODE_LOAD_ROUTE) {
            if (resultCode == RESULT_OK) {
                int selectedRoute = data.getIntExtra("selectedRoute", 0);
                String navigatingDistance = data.getStringExtra("selectedRouteDistance");
                String routeName = data.getStringExtra("selectedRouteName");
                Intent intentNA = new Intent(getBaseContext(), NavigationActivity.class);
                intentNA.putExtra("Type", NAV_TYPE_LOAD_ROUTE);
                intentNA.putExtra("selectedRoute", selectedRoute);
                intentNA.putExtra("selectedRouteDistance", navigatingDistance);
                intentNA.putExtra("selectedRouteName", routeName);
                isNavigating = true;
                startActivity(intentNA);
            }
        }
    }

    public void promptSpeechInputView(View view) {
        promptSpeechInput();
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            if(!isNavigating) {
                startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
            }
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }


    public void changeLayout(View view) {
        Intent i = new Intent(getBaseContext(), AddButton.class);
        startActivityForResult(i, 1);
    }

    synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    public void setAudio(View view) {
        System.out.println(Environment.getExternalStorageDirectory().getAbsolutePath());
        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.3gp";

        myAudioRecorder = new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        myAudioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);

        myAudioRecorder.setOutputFile(outputFile);

        setContentView(R.layout.record_view);
        startVoiceRecording = findViewById(R.id.button6);
        stopVoiceRecording = findViewById(R.id.button7);
        startVoiceRecording.setEnabled(true);
        stopVoiceRecording.setEnabled(false);

    }

    public void start_audio(View view) {
        try {
            myAudioRecorder.prepare();
            myAudioRecorder.start();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (java.io.IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        stopVoiceRecording.setEnabled(true);
        startVoiceRecording.setEnabled(false);
        Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_LONG).show();
    }

    public void stop_audio(View view) {
        myAudioRecorder.stop();
        myAudioRecorder.release();
        myAudioRecorder = null;

        stopVoiceRecording.setEnabled(false);
        startVoiceRecording.setEnabled(false);

        Toast.makeText(getApplicationContext(), "Audio recorded successfully", Toast.LENGTH_LONG).show();


    }


    public void onImageButton(View view) {
        Intent i = new Intent(getBaseContext(), ImageActivity.class);
        startActivity(i);
    }

    public void whereAmI(View view){
        Address obj = getAddress(currentLat, currentLong);
        String fin_add = getNameFromAddress(obj, 4);
        if(fin_add == null)
            tts.speak("Could Not find location info", TextToSpeech.QUEUE_ADD, null);
        else
            tts.speak("You are at " + fin_add, TextToSpeech.QUEUE_ADD, null);

        System.out.println(fin_add);
    }

    public void shareloc(View view){

        Intent myIntent = new Intent(MainActivity.this, shareLocActivity.class);
        myIntent.putExtra("currentLat", currentLat);
        myIntent.putExtra("currentLong", currentLong);
        MainActivity.this.startActivity(myIntent);
    }

    public void onspoi(View view){

        Intent myIntent = new Intent(MainActivity.this, SearchPOI.class);
        myIntent.putExtra("currentLat", currentLat);
        myIntent.putExtra("currentLong", currentLong);
        MainActivity.this.startActivity(myIntent);
    }

    public String getNameFromAddress(Address obj, int ex){
        String add = "";
        String fin_add = "";
        for (int i = 0; i<= obj.getMaxAddressLineIndex(); i++){
            add = add + obj.getAddressLine(i);
            add = add + ", ";
        }
        int delimitCnt=0;
        for (int j = 0; j < add.length(); j++){
            if(add.charAt(j) == ',')
                delimitCnt++;
            if (delimitCnt == ex)
                break;
            fin_add = fin_add + add.charAt(j);
        }
        return fin_add;
    }

    public Address getAddress(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        Address obj = null;
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng,  1);
            obj = addresses.get(0);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return obj;
    }

    public void onStartButton(View view) {
        if (!isSelected) {
            tts.speak("Destination not set", TextToSpeech.QUEUE_ADD, null);
            Toast.makeText(getApplicationContext(), "Destination not set!", Toast.LENGTH_LONG).show();
            return;
        }
        else {
            try {
                double final_lat = destinationLatLng.getLatLng().latitude;
//            destinationLatLng.getLatLng().
                double final_long = destinationLatLng.getLatLng().longitude;
                CharSequence destinationAddress = destinationLatLng.getName();

                Intent intentNA = new Intent(getBaseContext(), NavigationActivity.class);
                intentNA.putExtra("Type", NAV_TYPE_NAVIGATE);
                intentNA.putExtra("startLat", currentLat);
                intentNA.putExtra("startLon", currentLong);
                intentNA.putExtra("endLat", final_lat);
                intentNA.putExtra("endLon", final_long);
                intentNA.putExtra("destination", destinationAddress);

                isNavigating = true;
                startActivity(intentNA);
            }
            catch (Exception e){
                Log.i("NAV start", e.getMessage());
            }
        }
    }

    public void onPrePlanButton(View view) {
        if (!isSelected) {
            tts.speak("Destination not set", TextToSpeech.QUEUE_ADD, null);
            Toast.makeText(getApplicationContext(), "Destination not set!", Toast.LENGTH_LONG).show();
            return;
        }
        else {

            double final_lat = Objects.requireNonNull(destinationLatLng.getLatLng()).latitude;
            double final_long = destinationLatLng.getLatLng().longitude;
            CharSequence destinationAddress = destinationLatLng.getName();

            Intent intentNA = new Intent(getBaseContext(), NavigationActivity.class);
            intentNA.putExtra("Type", NAV_TYPE_PREPLAN);
            intentNA.putExtra("startLat", currentLat);
            intentNA.putExtra("startLon", currentLong);
            intentNA.putExtra("endLat", final_lat);
            intentNA.putExtra("endLon", final_long);
            intentNA.putExtra("destination", destinationAddress);

            isNavigating = true;
            startActivity(intentNA);
        }
    }

    public void onLoadRouteButton(View view) {

        Intent i = new Intent(getBaseContext(), ShowRoutes.class);
        ArrayList<Integer> route_ids;
        route_ids = new ArrayList<Integer>();
        ArrayList<String> route_names;
        route_names = new ArrayList<String>();
        ArrayList<String> route_distances;
        route_distances = new ArrayList<String>();
        Cursor c = db.rawQuery("SELECT * FROM routes;", null);
        while (c.moveToNext()) {
            route_ids.add(Integer.parseInt(c.getString(0)));
            route_names.add(c.getString(1));
            route_distances.add(c.getString(2));
        }
        i.putExtra("route_ids", route_ids);
        i.putExtra("route_names", route_names);
        i.putExtra("route_distances", route_distances);
        startActivityForResult(i, REQ_CODE_LOAD_ROUTE);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(500); // Update location twice every second

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            currentLat = mLastLocation.getLatitude();
            currentLong = mLastLocation.getLongitude();
            Log.i("current_lat_lng", "Fetching Current LatLong  " + currentLat + ";" + currentLong);
        }
        Configuration.getInstance().setUserAgentValue("com.osm-navi.iit");
        // setContentView(R.layout.main);
        MapView map = findViewById(R.id.map);
        map.setMultiTouchControls(true);
        // OSRMRoadManager roadManager = new OSRMRoadManager(this, "com.osm-navi.iit");
        GeoPoint startPoint = new GeoPoint(currentLat, currentLong);
        IMapController mapController = map.getController();
        mapController.setZoom(9);
        mapController.setCenter(startPoint);
        Marker startMarker = new Marker(map);
        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(startMarker);
        map.invalidate();
        //  startMarker.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
        startMarker.setTitle("Start point");
        OSRMRoadManager roadManager;
        roadManager = new OSRMRoadManager(this, "com.osm-navi.iit");
        roadManager.setMean(OSRMRoadManager.MEAN_BY_FOOT);
        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
        waypoints.add(startPoint);
        if (!isSelected) {
            tts.speak("Destination not set", TextToSpeech.QUEUE_ADD, null);
            Toast.makeText(getApplicationContext(), "Destination not set!", Toast.LENGTH_LONG).show();
            return;
        } else {

            double final_lat = destinationLatLng.getLatLng().latitude;
//            destinationLatLng.getLatLng().
            double final_long = destinationLatLng.getLatLng().longitude;
            CharSequence destinationAddress = destinationLatLng.getName();

            GeoPoint endPoint = new GeoPoint(final_lat, final_long);
            waypoints.add(endPoint);
            map.getOverlays().clear();
            map.invalidate();
            // RoadManager roadManager = new OSRMRoadManager(this, "com.osm-navi.iit");
            Road road = roadManager.getRoad(waypoints);
            //  Road road = roadManager.getRoad(waypoints);
            Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
            map.getOverlays().add(roadOverlay);
            map.invalidate();

            //  Drawable nodeIcon = getResources().getDrawable(R.drawable.marker_node);
            //  Marker nodeMarker = null;
            //  RoadNode node = null;
            //  for (int i = 0; i < road.mNodes.size(); i++) {
            //    node = road.mNodes.get(i);
            overpassProvider = new OverpassAPIProvider2();
            for (int i = 0; i < road.mNodes.size(); i++) {
                node = road.mNodes.get(i);
                GeoPoint loc = road.mNodes.get(i).mLocation;
                BoundingBox bb = new BoundingBox(loc.getLatitude() + 0.0002, loc.getLongitude() + 0.0002, loc.getLatitude() - 0.0002, loc.getLongitude() - 0.0002);

                if (road.mNodes.get(i).mInstructions != null && !road.mNodes.get(i).mInstructions.isEmpty()) {
                    double nodelat_curr = road.mNodes.get(i).mLocation.getLatitude();
                    double nodelon_curr = road.mNodes.get(i).mLocation.getLongitude();
                    double nodelat_next = 0, nodelon_next = 0, nodelat_prev = 0, nodelon_prev = 0;

                    if (i != road.mNodes.size() - 1) {
                        nodelat_next = road.mNodes.get(i + 1).mLocation.getLatitude();
                        nodelon_next = road.mNodes.get(i + 1).mLocation.getLongitude();
                    }
                    if (landmarks.size() != 0) {
                        nodelat_prev = landmarks.get(landmarks.size() - 1).getLatitude();
                        nodelon_prev = landmarks.get(landmarks.size() - 1).getLongitude();
                    }
                    // url for highway type
                    String urlforpoirequest = overpassProvider.urlForPOISearch("\"highway\"", bb, 10, 50);
                    ArrayList<POI> points = overpassProvider.getPOIsFromUrlRoad(urlforpoirequest);
                    if (points == null) {
                        System.out.println("overpass returning nothing");
                        continue;
                    }

                    if (points.size() != 0 && points != null) {
                        System.out.println("Size is: " + points.size());

                        POI tagPOI = points.get(0);
                        if (points.size() > 1) {
                            for (int ptr = 0; ptr < points.size(); ptr++) {
                                if ((points.get(ptr).mLocation.getLatitude() - nodelat_curr) / (nodelat_next - nodelat_curr) > 0) {
                                    if ((points.get(ptr).mLocation.getLongitude() - nodelon_curr) / (nodelon_next - nodelon_curr) > 0) {
                                        tagPOI = points.get(ptr);
                                        break;
                                    }
                                }
                            }
                        }
                        String typeofhighway = tagPOI.mType;
                        String lanecount = tagPOI.mUrl;
                        String typeofsurface = tagPOI.mDescription;
                        String footway = tagPOI.mThumbnailPath;
                        tags.add(tagPOI.mLocation);
                        System.out.println("Tag Location " + tagPOI.mLocation);
                        typeofhighway = typeofhighway.replaceAll("_", " ");
                        typeofsurface = typeofsurface.replaceAll(",", "");
                        if (lanecount != null && (typeofsurface != null && !typeofsurface.isEmpty())) {
                            tagInstructions.add("This is " + typeofhighway + " with " + lanecount + " lanes and " + typeofsurface + " surface.");
                        } else if (lanecount == null && (typeofsurface != null && !typeofsurface.isEmpty())) {
                            tagInstructions.add("This is " + typeofhighway + " with " + typeofsurface + " surface.");
                        } else if (lanecount != null && (typeofsurface == null || typeofsurface.isEmpty())) {
                            tagInstructions.add("This is " + typeofhighway + " with " + lanecount + " lanes.");
                        } else if (lanecount == null && (typeofsurface == null || typeofsurface.isEmpty())) {
                            tagInstructions.add("This is " + typeofhighway + ".");
                        }
                        int len = (tagInstructions.size() - 1);
                        if (footway != null) {
                            tagInstructions.set(len, tagInstructions.get(len) + " Presence of " + footway + " footway.");
                        }
                        System.out.println(tagInstructions.get(len));
                        tagCheck.add(0);
                        if (i != 0) {
                            tagLandmark.add(landmarks.size());
                        } else if (i == 0) {
                            tagLandmark.add(-1);
                        }
                    }

                    if (i != 0) {
                        landmarks.add(new GeoPoint(loc.getLatitude(), loc.getLongitude()));
                        landmarkCheck.add(0);
                        //System.out.println("Added Instruction: " + loc.getLatitude() + ", " + loc.getLongitude() + " " + road.mNodes.get(i).mInstructions);
                        instructions.add(removeUnnamed(road.mNodes.get(i).mInstructions));
                        if (isPreplanning) {
                            preplanInstruction();
                            //preplanInstruction2();
                        }
                        timestamps.add(0L);
                        tst.add(0L);
                        osmNumInstructions = road.mNodes.size();
                        osmNextInstruction = 0;
                        prefetch_nextInstruction = 0;
                    }


                    nodeMarker = new Marker(map);
                    nodeMarker.setPosition(node.mLocation);
                    //nodeMarker.setIcon(nodeIcon);
                    nodeMarker.setTitle("Step " + i);
                    map.getOverlays().add(nodeMarker);

                    nodeMarker.setSnippet(node.mInstructions + tagInstructions);
                    nodeMarker.setSubDescription(Road.getLengthDurationText(this, node.mLength, node.mDuration));
                    map.getOverlays().add(roadOverlay);


                    MyLocationNewOverlay myLocationNewOverlay = new MyLocationNewOverlay(map);
                    myLocationNewOverlay.enableFollowLocation();
                    myLocationNewOverlay.enableMyLocation();
                    map.getOverlayManager().add(myLocationNewOverlay);

                }
            }

        }
        // setBoundsBias();
        // Log.i("current_lat_lng", "Connected!");



    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        buildGoogleApiClient();
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLat = location.getLatitude();
        currentLong = location.getLongitude();
    }



    public void onDebug(View view){
        Intent myIntent = new Intent(MainActivity.this, debugAcitivity.class);
        myIntent.putExtra("currentLat", currentLat);
        myIntent.putExtra("currentLong", currentLong);
        MainActivity.this.startActivity(myIntent);
    }

    public void setBoundsBias(){
        /*LatLng southWestBound = new LatLng( currentLat - 0.1, currentLong - 0.1);
        LatLng northEastBound = new LatLng(currentLat + 0.1, currentLong + 0.1);
        LatLngBounds customBounds = new LatLngBounds(southWestBound, northEastBound);
        endingDestination.setLocationBias(customBounds);
        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_NONE)
                .build();
        endingDestination.setFilter(typeFilter);*/
    }


    public static String requestStringFromUrl(String url, String userAgent) {
        HttpConnection connection = new HttpConnection();
        if (userAgent != null)
            connection.setUserAgent(userAgent);
        connection.doGet(url);
        String result = connection.getContentAsString();
        connection.close();
        return result;
    }

    /** sends an http request, and returns the whole content result in a String.
     * @param url
     * @return the whole content, or null if any issue.
     */
    public static String requestStringFromUrl(String url) {
        return requestStringFromUrl(url, null);
    }
    public String removeUnnamed(String instruction) {
//        String newinstruction = instruction.replaceAll("unnamed", "this");
        return instruction.replaceAll("(.*)waypoint(.*)", "You have arrived at your destination.");
//        return newinstruction;
    }
    public void preplanInstruction(){
        int lsize = landmarks.size();
        String insto;
        float[] res = new float[3];
        if (lsize > 1) {
            Location.distanceBetween(landmarks.get(lsize - 2).getLatitude(), landmarks.get(lsize - 2).getLongitude(), landmarks.get(lsize - 1).getLatitude(), landmarks.get(lsize - 1).getLongitude(), res);
        }
        else{
            Location.distanceBetween(current_lat,current_long, landmarks.get(lsize - 1).getLatitude(), landmarks.get(lsize - 1).getLongitude(), res);
        }
        int intres = (int) res[0];
        if (intres < 1000) {
            insto = "after " + intres + " metres " + instructions.get(lsize-1);
        } else {
            intres = (int)intres/100;
            insto = "after " + ((double)(intres/10)) + " kilometres " + instructions.get(lsize-1);
        }
        instructions.set((lsize-1),insto);
    }


}
