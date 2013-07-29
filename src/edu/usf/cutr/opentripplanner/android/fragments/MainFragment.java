/*
 * Copyright 2012 University of South Florida
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package edu.usf.cutr.opentripplanner.android.fragments;

import static edu.usf.cutr.opentripplanner.android.OTPApp.CHOOSE_CONTACT_REQUEST_CODE;
import static edu.usf.cutr.opentripplanner.android.OTPApp.PREFERENCE_KEY_AUTO_DETECT_SERVER;
import static edu.usf.cutr.opentripplanner.android.OTPApp.PREFERENCE_KEY_CUSTOM_SERVER_BOUNDS;
import static edu.usf.cutr.opentripplanner.android.OTPApp.PREFERENCE_KEY_CUSTOM_SERVER_URL;
import static edu.usf.cutr.opentripplanner.android.OTPApp.PREFERENCE_KEY_GEOCODER_PROVIDER;
import static edu.usf.cutr.opentripplanner.android.OTPApp.PREFERENCE_KEY_MAP_TILE_SOURCE;
import static edu.usf.cutr.opentripplanner.android.OTPApp.PREFERENCE_KEY_MAX_WALKING_DISTANCE;
import static edu.usf.cutr.opentripplanner.android.OTPApp.PREFERENCE_KEY_SELECTED_CUSTOM_SERVER;
import static edu.usf.cutr.opentripplanner.android.OTPApp.PREFERENCE_KEY_SELECTED_SERVER;
import static edu.usf.cutr.opentripplanner.android.OTPApp.PREFERENCE_KEY_WHEEL_ACCESSIBLE;
import static edu.usf.cutr.opentripplanner.android.OTPApp.REFRESH_SERVER_LIST_REQUEST_CODE;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import maps.MyUrlTileProvider;

import org.miscwidgets.widget.Panel;
import org.opentripplanner.api.ws.Request;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.v092snapshot.api.model.Itinerary;
import org.opentripplanner.v092snapshot.api.model.Leg;
import org.osmdroid.util.GeoPoint;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.location.Address;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import edu.usf.cutr.opentripplanner.android.MyActivity;
import edu.usf.cutr.opentripplanner.android.OTPApp;
import edu.usf.cutr.opentripplanner.android.R;
import edu.usf.cutr.opentripplanner.android.SettingsActivity;
import edu.usf.cutr.opentripplanner.android.listeners.OTPGeocodingListener;
import edu.usf.cutr.opentripplanner.android.listeners.OTPGetCurrentLocationListener;
import edu.usf.cutr.opentripplanner.android.listeners.OnFragmentListener;
import edu.usf.cutr.opentripplanner.android.listeners.ServerSelectorCompleteListener;
import edu.usf.cutr.opentripplanner.android.listeners.TripRequestCompleteListener;
import edu.usf.cutr.opentripplanner.android.model.OTPBundle;
import edu.usf.cutr.opentripplanner.android.model.OptimizeSpinnerItem;
import edu.usf.cutr.opentripplanner.android.model.Server;
import edu.usf.cutr.opentripplanner.android.model.TraverseModeSpinnerItem;
import edu.usf.cutr.opentripplanner.android.overlays.MapOverlay;
import edu.usf.cutr.opentripplanner.android.overlays.OTPModeOverlay;
import edu.usf.cutr.opentripplanner.android.overlays.OTPPathOverlay;
import edu.usf.cutr.opentripplanner.android.sqlite.ServersDataSource;
import edu.usf.cutr.opentripplanner.android.tasks.MetadataRequest;
import edu.usf.cutr.opentripplanner.android.tasks.OTPGeocoding;
import edu.usf.cutr.opentripplanner.android.tasks.ServerChecker;
import edu.usf.cutr.opentripplanner.android.tasks.ServerSelector;
import edu.usf.cutr.opentripplanner.android.tasks.TripRequest;
import edu.usf.cutr.opentripplanner.android.util.LocationUtil;

/**
 * Main UI screen of the app, showing the map.
 * 
 * @author Khoa Tran
 */

public class MainFragment extends Fragment implements
		OnSharedPreferenceChangeListener, ServerSelectorCompleteListener,
		TripRequestCompleteListener, OTPGetCurrentLocationListener,
		OTPGeocodingListener {

	private GoogleMap mMap;
	private TileOverlay actualTileOverlay;
	private MenuItem mGPS;

	private EditText tbStartLocation;
	private EditText tbEndLocation;
	private ImageButton btnStartLocation;
	private ImageButton btnEndLocation;
	private Spinner ddlOptimization;
	private Spinner ddlTravelMode;
	private Button btnPlanTrip;
	private ImageView googlePlacesIcon;

	// private Spinner ddlGeocoder;

	private Panel tripPanel;
	Panel directionPanel;

	private ImageButton btnDisplayDirection;

	MapOverlay startMarker;
	MapOverlay endMarker;
	OTPPathOverlay routeOverlay;
	OTPModeOverlay modeOverlay;

	private SharedPreferences prefs;
	private OTPApp app;
	private static LocationManager locationManager;

	// private List<Itinerary> itineraries = null;

	ArrayList<String> directionText = new ArrayList<String>();

	private Boolean needToRunAutoDetect = false;

	private OnFragmentListener fragmentListener;

	private final GeoPoint defaultCenterLocation = new GeoPoint(40.5, -100);

	private final int defaultInitialZoomLevel = 12;

	private boolean isRealLostFocus = true;

	public static final String TAG = "OTP";

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			setFragmentListener((OnFragmentListener) activity);
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnFragmentListener");
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View mainView = inflater.inflate(R.layout.main, container, false);
		final Activity activity = this.getActivity();

		final OnFragmentListener ofl = this.getFragmentListener();

		prefs = PreferenceManager.getDefaultSharedPreferences(activity
				.getApplicationContext());
		prefs.registerOnSharedPreferenceChangeListener(this);

		app = ((OTPApp) activity.getApplication());
		
		setUpMapIfNeeded();
		
		String overlayString = prefs.getString(PREFERENCE_KEY_MAP_TILE_SOURCE, getResources().getString(R.string.map_tiles_default_server)); 
		updateOverlay(overlayString);
		
		UiSettings uiSettings = mMap.getUiSettings();
		mMap.setMyLocationEnabled(true);
		uiSettings.setMyLocationButtonEnabled(true);
		uiSettings.setCompassEnabled(true);
		uiSettings.setAllGesturesEnabled(true);
		uiSettings.setZoomControlsEnabled(true);

		locationManager = (LocationManager) activity
				.getSystemService(Context.LOCATION_SERVICE);

		btnStartLocation = (ImageButton) mainView
				.findViewById(R.id.btnStartLocation);
		btnEndLocation = (ImageButton) mainView
				.findViewById(R.id.btnEndLocation);
		tbStartLocation = (EditText) mainView
				.findViewById(R.id.tbStartLocation);
		tbEndLocation = (EditText) mainView.findViewById(R.id.tbEndLocation);
		
		btnPlanTrip = (Button) mainView.findViewById(R.id.btnPlanTrip);
		tripPanel = (Panel) mainView.findViewById(R.id.slidingDrawer1);
		ddlOptimization = (Spinner) mainView
				.findViewById(R.id.spinOptimization);
		ddlTravelMode = (Spinner) mainView.findViewById(R.id.spinTravelMode);

		btnDisplayDirection = (ImageButton) mainView
				.findViewById(R.id.btnDisplayDirection);

		googlePlacesIcon = (ImageView) mainView
				.findViewById(R.id.googlePlacesIcon);

		tripPanel.setOpen(true, true);

		tripPanel.setFocusable(true);
		tripPanel.setFocusableInTouchMode(true);

		ArrayAdapter<OptimizeSpinnerItem> optimizationAdapter = new ArrayAdapter<OptimizeSpinnerItem>(
				activity,
				android.R.layout.simple_spinner_item,
				new OptimizeSpinnerItem[] {
						new OptimizeSpinnerItem("Quickest", OptimizeType.QUICK),
						new OptimizeSpinnerItem("Safest", OptimizeType.SAFE),
						new OptimizeSpinnerItem("Fewest Transfers",
								OptimizeType.TRANSFERS) });

		optimizationAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		ddlOptimization.setAdapter(optimizationAdapter);

		ArrayAdapter<TraverseModeSpinnerItem> traverseModeAdapter = new ArrayAdapter<TraverseModeSpinnerItem>(
				activity, android.R.layout.simple_spinner_item,
				new TraverseModeSpinnerItem[] {
						new TraverseModeSpinnerItem("Transit",
								new TraverseModeSet(TraverseMode.TRANSIT,
										TraverseMode.WALK)),
						new TraverseModeSpinnerItem("Bus Only",
								new TraverseModeSet(TraverseMode.BUSISH,
										TraverseMode.WALK)),
						new TraverseModeSpinnerItem("Train Only",
								new TraverseModeSet(TraverseMode.TRAINISH,
										TraverseMode.WALK)), // not sure
						new TraverseModeSpinnerItem("Walk Only",
								new TraverseModeSet(TraverseMode.WALK)),
						new TraverseModeSpinnerItem("Bicycle",
								new TraverseModeSet(TraverseMode.BICYCLE)),
						new TraverseModeSpinnerItem("Transit and Bicycle",
								new TraverseModeSet(TraverseMode.TRANSIT,
										TraverseMode.BICYCLE)) });

		traverseModeAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		ddlTravelMode.setAdapter(traverseModeAdapter);

		GeoPoint currentLocation = LocationUtil.getLastLocation(activity);
		
		Server selectedServer;
		// if currentLocation is null
		if (currentLocation == null) {
			if ((selectedServer = app.getSelectedServer()) != null){
				GeoPoint serverCenterLocation = new GeoPoint(selectedServer.getCenterLatitude(), selectedServer.getCenterLongitude());
				currentLocation = serverCenterLocation;
			}
			else{
				currentLocation = defaultCenterLocation;
			}
		}

		OnClickListener ocl = new OnClickListener() {
			@Override
			public void onClick(View v) {
				// mBoundService.updateNotification();

				final int buttonID = v.getId();

				final CharSequence[] items = { "Current Location",
						"Contact Address", "Point on Map" };

				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				builder.setTitle("Choose Start Location");
				builder.setItems(items, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int item) {
						if (items[item].equals("Current Location")) {
							GeoPoint p = LocationUtil.getLastLocation(activity);

							if (buttonID == R.id.btnStartLocation) {
								tbStartLocation.setText("My Location");

								if (p != null) {
									startMarker.setLocation(p);
								}
							} else if (buttonID == R.id.btnEndLocation) {
								tbEndLocation.setText("My Location");
								if (p != null) {
									endMarker.setLocation(p);
								}
							}
						} else if (items[item].equals("Contact Address")) {
							Intent intent = new Intent(Intent.ACTION_PICK);
							intent.setType(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_TYPE);
							MyActivity myActivity = (MyActivity) activity;
							if (buttonID == R.id.btnStartLocation) {
								myActivity.setButtonStartLocation(true);
							} else if (buttonID == R.id.btnEndLocation) {
								myActivity.setButtonStartLocation(false);
							}
							activity.startActivityForResult(intent,
									CHOOSE_CONTACT_REQUEST_CODE);

						} else { // Point on Map
							if (buttonID == R.id.btnStartLocation) {
								tbStartLocation.setText(startMarker
										.getLocationFormatedString());
							} else if (buttonID == R.id.btnEndLocation) {
								tbEndLocation.setText(endMarker
										.getLocationFormatedString());
							}
						}
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
		};

		btnStartLocation.setOnClickListener(ocl);
		btnEndLocation.setOnClickListener(ocl);

		tbStartLocation.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		tbEndLocation.setImeOptions(EditorInfo.IME_ACTION_DONE);
		tbEndLocation.requestFocus();
		OnEditorActionListener tbLocationOnEditorActionListener = new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (v.getId() == R.id.tbStartLocation
						&& actionId == EditorInfo.IME_ACTION_NEXT
						|| (event != null
								&& event.getAction() == KeyEvent.ACTION_DOWN && event
								.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
					isRealLostFocus = false;
					processAddress(true, v.getText().toString());
				} else if (v.getId() == R.id.tbEndLocation
						&& actionId == EditorInfo.IME_ACTION_DONE
						|| (event != null
								&& event.getAction() == KeyEvent.ACTION_DOWN && event
								.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
					isRealLostFocus = false;
					processAddress(false, v.getText().toString());
				}
				return false;
			}
		};

		tbStartLocation
				.setOnEditorActionListener(tbLocationOnEditorActionListener);
		tbEndLocation
				.setOnEditorActionListener(tbLocationOnEditorActionListener);

		// Need to consider this case again
		OnFocusChangeListener tbLocationOnFocusChangeListener = new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!isRealLostFocus) {
					isRealLostFocus = true;
					return;
				}
				TextView tv = (TextView) v;
				if (!hasFocus) {
					if (v.getId() == R.id.tbStartLocation) {
						processAddress(true, tv.getText().toString());
					} else if (v.getId() == R.id.tbEndLocation) {
						processAddress(false, tv.getText().toString());
					}
				}
			}
		};
		tbStartLocation
				.setOnFocusChangeListener(tbLocationOnFocusChangeListener);
		tbEndLocation.setOnFocusChangeListener(tbLocationOnFocusChangeListener);

		

		
		
/*
		mv = (MapView) mainView.findViewById(R.id.mapview);
		mv.setBuiltInZoomControls(true);
		mv.setMultiTouchControls(true);

		mc = mv.getController();
		mc.setZoom(defaultInitialZoomLevel);
		
		mc.setCenter(currentLocation);

		mlo = new MyLocationOverlay(activity, mv);
		// mlo.enableCompass();
		mv.getOverlays().add(mlo);
*/
		startMarker = new MapOverlay(this, R.drawable.start, mainView);
		startMarker.setLocation(currentLocation);
/*		mv.getOverlays().add(startMarker);
*/
		endMarker = new MapOverlay(this, R.drawable.end, mainView);
		endMarker.setLocation(currentLocation);
/*		mv.getOverlays().add(endMarker);

		routeOverlay = new OTPPathOverlay(Color.DKGRAY, activity);
		mv.getOverlays().add(routeOverlay);
		
		modeOverlay = new OTPModeOverlay(this);
		mv.getOverlays().add(modeOverlay);
*/
		if (prefs.getBoolean(PREFERENCE_KEY_AUTO_DETECT_SERVER, true)) {
		
			if (app.getSelectedServer() == null) {
				processServerSelector(currentLocation, false, true);
			} else {
				Log.v(TAG, "Already selected a server!!");
			}
		}
		else {
			if (prefs.getBoolean(PREFERENCE_KEY_SELECTED_CUSTOM_SERVER, false)){
				String baseURL = prefs.getString(PREFERENCE_KEY_CUSTOM_SERVER_URL, "");
				Server s = new Server(baseURL);
				String bounds;
				if ((bounds = prefs.getString(PREFERENCE_KEY_CUSTOM_SERVER_BOUNDS, null)) != null){
					s.setBounds(bounds);
				}
				app.setSelectedServer(s);

				Log.v(TAG, "Now using custom OTP server: " + baseURL);
			}
			else{
				MyActivity myActivity = (MyActivity) this.getActivity();
				ServersDataSource dataSource = myActivity.getDatasource();
				long serverId = prefs.getLong(PREFERENCE_KEY_SELECTED_SERVER, 0);
				if (serverId != 0){
					dataSource.open();
					Server s = new Server(dataSource.getServer(prefs.getLong(PREFERENCE_KEY_SELECTED_SERVER, 0)));
					app.setSelectedServer(s);
					dataSource.close();
					Log.v(TAG, "Now using OTP server: " + s.getRegion());
				}
				dataSource.close();
			}
		}

		if (prefs.getString(PREFERENCE_KEY_GEOCODER_PROVIDER, "Google Places").equals(
				"Google Places")) {
			googlePlacesIcon.setVisibility(View.VISIBLE);
		} else {
			googlePlacesIcon.setVisibility(View.INVISIBLE);
		}

		// btnPlanTrip.setFocusable(true);
		// btnPlanTrip.setFocusableInTouchMode(true);

		btnPlanTrip.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				tripPanel.setOpen(false, true);

				Request request = new Request();
				request.setFrom(URLEncoder.encode(startMarker
						.getLocationFormatedString()));
				request.setTo(URLEncoder.encode(endMarker
						.getLocationFormatedString()));
				request.setArriveBy(false);

				request.setOptimize(((OptimizeSpinnerItem) ddlOptimization
						.getSelectedItem()).getOptimizeType());
				request.setModes(((TraverseModeSpinnerItem) ddlTravelMode
						.getSelectedItem()).getTraverseModeSet());

				try {
					Double maxWalk = Double.parseDouble(prefs.getString(
							PREFERENCE_KEY_MAX_WALKING_DISTANCE, "1600"));
					request.setMaxWalkDistance(maxWalk);
				} catch (NumberFormatException ex) {
					request.setMaxWalkDistance(new Double("1600"));
				}

				request.setWheelchair(prefs.getBoolean(PREFERENCE_KEY_WHEEL_ACCESSIBLE,
						false));

				request.setDateTime(
						DateFormat.format("MM/dd/yy",
								System.currentTimeMillis()).toString(),
						DateFormat
								.format("hh:mmaa", System.currentTimeMillis())
								.toString());

				request.setShowIntermediateStops(Boolean.TRUE);

				new TripRequest(MainFragment.this.getActivity(), app
						.getSelectedServer(), MainFragment.this)
						.execute(request);

				InputMethodManager imm = (InputMethodManager) activity
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(tbEndLocation.getWindowToken(), 0);
				imm.hideSoftInputFromWindow(tbStartLocation.getWindowToken(), 0);
			}
		});

		OnClickListener oclDisplayDirection = new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// Save states before leaving
				saveOTPBundle();

				ofl.onSwitchedToDirectionFragment();
			}
		};
		btnDisplayDirection.setOnClickListener(oclDisplayDirection);

		// Do NOT show direction icon if there is no direction yet
		if (ofl.getCurrentItinerary().isEmpty()) {
			btnDisplayDirection.setVisibility(View.INVISIBLE);
		} else {
			btnDisplayDirection.setVisibility(View.VISIBLE);
		}

		// get previous state if already exist
		OTPBundle otpBundle = ofl.getOTPBundle();
		if (otpBundle != null) {
			retrievePreviousState(otpBundle);
		}
		
		if (savedInstanceState != null){
			tbStartLocation.setText(savedInstanceState.getString("tbStartLocation"));
			tbEndLocation.setText(savedInstanceState.getString("tbEndLocation"));
			GeoPoint startMarkerLocation = new GeoPoint(savedInstanceState.getIntArray("startMarkerLocation")[0], savedInstanceState.getIntArray("startMarkerLocation")[1]);  
			startMarker.setLocation(startMarkerLocation);
			GeoPoint endMarkerLocation = new GeoPoint(savedInstanceState.getIntArray("endMarkerLocation")[0], savedInstanceState.getIntArray("endMarkerLocation")[1]);  
			endMarker.setLocation(endMarkerLocation);
			ddlOptimization.setSelection(savedInstanceState.getInt("ddlOptimization"));
			ddlTravelMode.setSelection(savedInstanceState.getInt("ddlTravelMode"));
		}
		
		Log.v(TAG, "finish onStart()");

		return mainView;
	}
	
	private void setUpMapIfNeeded() {
	    // Do a null check to confirm that we have not already instantiated the map.
	    if (mMap == null) {
	        mMap = ((SupportMapFragment) getFragmentManager().findFragmentById(R.id.map))
	                            .getMap();
	        // Check if we were successful in obtaining the map.
	        if (mMap == null) {
		        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getActivity().getApplicationContext());
		        
		        if(status!=ConnectionResult.SUCCESS){
		            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this.getActivity(), OTPApp.CHECK_GOOGLE_PLAY_REQUEST_CODE);
		            dialog.show();
		        }	        
		    }

	    }
	}
	
	public void onSaveInstanceState(Bundle bundle){
		super.onSaveInstanceState(bundle);
		bundle.putString("tbStartLocation", tbStartLocation.getText().toString());
		bundle.putString("tbEndLocation", tbEndLocation.getText().toString());
	//	bundle.putIntArray("startMarkerLocation", new int[]{startMarker.getLocation().getLatitudeE6(), startMarker.getLocation().getLongitudeE6()});
	//	bundle.putIntArray("endMarkerLocation", new int[]{endMarker.getLocation().getLatitudeE6(), endMarker.getLocation().getLongitudeE6()});
		bundle.putInt("ddlOptimization", ddlOptimization.getSelectedItemPosition());
		bundle.putInt("ddlTravelMode", ddlTravelMode.getSelectedItemPosition());
	}
	
	private void retrievePreviousState(OTPBundle bundle) {
		tbStartLocation.setText(bundle.getFromText());
		tbEndLocation.setText(bundle.getToText());
		startMarker.setLocation(bundle.getStartLocation());
		endMarker.setLocation(bundle.getEndLocation());
		ddlOptimization.setSelection(bundle.getOptimization());
		ddlTravelMode.setSelection(bundle.getTravelMode());

		this.showRouteOnMap(bundle.getCurrentItinerary());
	}

	private void saveOTPBundle() {
		OTPBundle bundle = new OTPBundle();
		bundle.setFromText(tbStartLocation.getText().toString());
		bundle.setToText(tbEndLocation.getText().toString());
		bundle.setStartLocation(startMarker.getLocation());
		bundle.setEndLocation(endMarker.getLocation());
		bundle.setOptimization(ddlOptimization.getSelectedItemPosition());
		bundle.setTravelMode(ddlTravelMode.getSelectedItemPosition());

		this.getFragmentListener().setOTPBundle(bundle);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.v(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);
	}

	public void processAddress(final boolean isStartTextBox, String address) {
		String[] availableGeocoderProviders = getResources().getStringArray(
				R.array.available_geocoder_providers);
		OTPGeocoding geocodingTask = new OTPGeocoding(this.getActivity(),
				isStartTextBox, app.getSelectedServer(), prefs.getString(
						PREFERENCE_KEY_GEOCODER_PROVIDER, availableGeocoderProviders[0]),
				this);
		geocodingTask.execute(address);
	}

	private void adjustFocusAfterSelectAddress(boolean isStartTextBox) {
		isRealLostFocus = false;
		if (isStartTextBox) {
			if (tbEndLocation.getText().toString().equals("")) {
				tbEndLocation.requestFocus();
			} else {
				tripPanel.requestFocus();
			}
		} else {
			if (tbStartLocation.getText().toString().equals("")) {
				tbStartLocation.requestFocus();
			} else {
				tripPanel.requestFocus();
			}
		}
	}

	/**
	 * Triggers the OTP server selection process.
	 * @param mustRefreshServerList True if the app should refresh the server list by downloading from the Google Doc, false if it shoudl not
	 */
	public void processServerSelector(boolean mustRefreshServerList) {
		boolean isAutoDetectEnabled = prefs.getBoolean(PREFERENCE_KEY_AUTO_DETECT_SERVER,
				true);
		GeoPoint currentLoc = LocationUtil.getLastLocation(this.getActivity());

		processServerSelector(currentLoc, mustRefreshServerList,
				isAutoDetectEnabled);
	}

	public void processServerSelector(GeoPoint currentLoc,
			boolean mustRefreshServerList, boolean isAutoDetectEnabled) {
		MyActivity myActivity = (MyActivity) this.getActivity();
		ServerSelector selector = new ServerSelector(myActivity,
				myActivity.getDatasource(), this, mustRefreshServerList,
				isAutoDetectEnabled);
		if (currentLoc == null) {
			currentLoc = LocationUtil.getLastLocation(this.getActivity());
		}

		selector.execute(currentLoc);
	}

	@Override
	public void onResume() {
		super.onResume();
		
		Log.v(TAG, "MainFragment onResume");

		if (needToRunAutoDetect) {
			GeoPoint currentLoc = LocationUtil.getLastLocation(this
					.getActivity());
			if (currentLoc != null) {
				Log.v(TAG, "Relaunching auto detection for server");
					
				processServerSelector(currentLoc, false, prefs.getBoolean(PREFERENCE_KEY_AUTO_DETECT_SERVER,
						true));
			}
			needToRunAutoDetect = false;
		}
	}

	@Override
	public void onPause() {

		// Save states before leaving
		this.saveOTPBundle();

		super.onPause();
	}

	@Override
	public void onDestroy() {
		// Release all map-related objects to make sure GPS is shut down when
		// the user leaves the app

		Log.d(TAG, "Released all map objects in MainFragment.onDestroy()");

		super.onDestroy();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key == null) {
			return;
		}
		Log.v(TAG, "A preference was changed: " + key);
		if (key.equals(PREFERENCE_KEY_MAP_TILE_SOURCE)) {
			String overlayString = prefs.getString(PREFERENCE_KEY_MAP_TILE_SOURCE, getResources().getString(R.string.map_tiles_default_server));
			updateOverlay(overlayString);
		} else if (key.equals(PREFERENCE_KEY_SELECTED_CUSTOM_SERVER)) {
			MyActivity myActivity = (MyActivity) this.getActivity();

			if (prefs.getBoolean(PREFERENCE_KEY_SELECTED_CUSTOM_SERVER, false)){
				app.setSelectedServer(new Server(prefs.getString(PREFERENCE_KEY_CUSTOM_SERVER_URL, "")));
				Log.v(TAG, "Now using custom OTP server: " + prefs.getString(PREFERENCE_KEY_CUSTOM_SERVER_URL, ""));
				MetadataRequest metaRequest = new MetadataRequest(myActivity);
				metaRequest.execute(prefs.getString(PREFERENCE_KEY_CUSTOM_SERVER_URL, ""));
			}
			else{
				long serverId = prefs.getLong(PREFERENCE_KEY_SELECTED_SERVER, 0);
				if (serverId != 0){
					ServersDataSource dataSource = myActivity.getDatasource();
					dataSource.open();
					Server s = new Server(dataSource.getServer(prefs.getLong(PREFERENCE_KEY_SELECTED_SERVER, 0)));
					app.setSelectedServer(s);
					dataSource.close();
				}
			}
			
		} else if (key.equals(PREFERENCE_KEY_AUTO_DETECT_SERVER)) {
			Log.v(TAG, "Detected change in auto-detect server preference. Value is now: " + prefs.getBoolean(PREFERENCE_KEY_AUTO_DETECT_SERVER, true));
			
			if (prefs.getBoolean(PREFERENCE_KEY_AUTO_DETECT_SERVER, true)) {
				needToRunAutoDetect = true;
			}
			else {
				needToRunAutoDetect = false;
			}
		} else if (key.equals(PREFERENCE_KEY_GEOCODER_PROVIDER)) {
			if (prefs.getString(PREFERENCE_KEY_GEOCODER_PROVIDER, "Google Places").equals(
					"Google Places")) {
				googlePlacesIcon.setVisibility(View.VISIBLE);
			} else {
				googlePlacesIcon.setVisibility(View.INVISIBLE);
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu pMenu, MenuInflater inflater) {
		// MenuInflater inflater = getMenuInflater();
		super.onCreateOptionsMenu(pMenu, inflater);
		inflater.inflate(R.menu.menu, pMenu);
		mGPS = pMenu.getItem(0);
	}

	public void onPrepareOptionsMenu(final Menu pMenu) {
		if (isGPSEnabled()) {
			mGPS.setTitle(R.string.disable_gps);
		} else {
			mGPS.setTitle(R.string.enable_gps);
		}
		super.onPrepareOptionsMenu(pMenu);
	}

	public boolean onOptionsItemSelected(final MenuItem pItem) {
		OTPApp app = ((OTPApp) this.getActivity().getApplication());
		switch (pItem.getItemId()) {
		case R.id.exit:
			this.getActivity().finish();
			return true;
		case R.id.gps_settings:
			Intent myIntent = new Intent(
					Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivity(myIntent);
			break;
		case R.id.my_location:
			// OTPGetCurrentLocation getCurrentLocation = new
			// OTPGetCurrentLocation(this.getActivity(), this);
			// getCurrentLocation.execute("");
			//TODO
			break;
		case R.id.settings:
			this.getActivity().startActivityForResult(
					new Intent(this.getActivity(), SettingsActivity.class),
					REFRESH_SERVER_LIST_REQUEST_CODE);
			break;
		case R.id.feedback:
			Server selectedServer = app.getSelectedServer();

			String[] recipients = { selectedServer.getContactEmail(),
					getString(R.string.email_otp_android_developer) };

			String uriText = "mailto:";
			for (int i = 0; i < recipients.length; i++) {
				uriText += recipients[i] + ";";
			}

			String subject = "";
			subject += "Android OTP user report OTP trip ";
			Date d = Calendar.getInstance().getTime();
			subject += "[" + d.toString() + "]";
			uriText += "?subject=" + subject;

			MyActivity myActivity = (MyActivity) this.getActivity();
			String content = myActivity.getCurrentRequestString();
			uriText += "&body=" + URLEncoder.encode(content);

			Uri uri = Uri.parse(uriText);

			Intent sendIntent = new Intent(Intent.ACTION_SENDTO);
			sendIntent.setData(uri);
			startActivity(Intent.createChooser(sendIntent, "Send email"));

			break;
		case R.id.server_info:
			Server server = app.getSelectedServer();
			
			if (server == null) {
				Log.w(TAG,
						"Tried to get server info when no server was selected");
				Toast.makeText(getActivity(), getResources().getString(R.string.info_server_no_server_selected), Toast.LENGTH_SHORT).show();
				break;
			}
		
			
			ServerChecker serverChecker = new ServerChecker(this.getActivity(), true);
			serverChecker.execute(server);
				

			break;
		default:
			break;
		}

		return false;
	}

	private Boolean isGPSEnabled() {
		return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	public void moveMarker(Boolean start, Address addr) {
		GeoPoint point = new GeoPoint(addr.getLatitude(), addr.getLongitude());
		if (start) {
			startMarker.setLocation(point);
			tbStartLocation.setText(addr.getAddressLine(addr
					.getMaxAddressLineIndex()));
		} else {
			endMarker.setLocation(point);
			tbEndLocation.setText(addr.getAddressLine(addr
					.getMaxAddressLineIndex()));
		}

	}

	public void zoomToCurrentLocation() {
		GeoPoint p = LocationUtil.getLastLocation(this.getActivity());

		if (p != null) {
			//TODO
		}
	}

	public void zoomToLocation(GeoPoint p) {
		if (p != null) {
			//TODO
		}
	}
	
	public void zoomToRegion(List<GeoPoint> items) {
		int minLat = Integer.MAX_VALUE;
		int maxLat = Integer.MIN_VALUE;
		int minLon = Integer.MAX_VALUE;
		int maxLon = Integer.MIN_VALUE;
		
		if (!items.isEmpty()){
			for (GeoPoint item : items) 
			{ 

			      int lat = item.getLatitudeE6();
			      int lon = item.getLongitudeE6();

			      maxLat = Math.max(lat, maxLat);
			      minLat = Math.min(lat, minLat);
			      maxLon = Math.max(lon, maxLon);
			      minLon = Math.min(lon, minLon);
			 }

			double fitFactor = 1.1;
			//TODO

		}
		
	}

	public void setMarker(GeoPoint p, boolean isStartMarker) {
		if (p == null)
			return;
		if (isStartMarker) {
			startMarker.setLocation(p);
		} else {
			endMarker.setLocation(p);
		}
	}

	public void setTextBoxLocation(String text, boolean isStartTextBox) {
		if (isStartTextBox) {
			tbStartLocation.setText(text);
		} else {
			tbEndLocation.setText(text);
		}
	}

	//
	// private GeoPoint getPoint(double lat, double lon) {
	// return (new GeoPoint((int) (lat * 1000000.0), (int) (lon * 1000000.0)));
	// }

	public void showRouteOnMap(List<Leg> itinerary) {
		Log.v(TAG,
				"(TripRequest) legs size = "
						+ Integer.toString(itinerary.size()));
		if (!itinerary.isEmpty()) {
			btnDisplayDirection.setVisibility(View.VISIBLE);
			routeOverlay.removeAllPath();
			modeOverlay.removeAllMode();
			List<GeoPoint> allGeoPoints = new ArrayList<GeoPoint>();
			int index = 0;
			for (Leg leg : itinerary) {
				int pathColor = getPathColor(leg.mode);
				routeOverlay.addPath(pathColor);
				List<GeoPoint> points = LocationUtil.decodePoly(leg.legGeometry
						.getPoints());
				modeOverlay.addLeg(points.get(0), leg.mode);
				for (GeoPoint geoPoint : points) {
					routeOverlay.addPoint(index, geoPoint);
				}
				index++;
				allGeoPoints.addAll(points);
			}
			zoomToRegion(allGeoPoints);
		}
	}

	private int getPathColor(String mode) {
		if (mode.equalsIgnoreCase("WALK")) {
			return Color.DKGRAY;
		} else if (mode.equalsIgnoreCase("BUS")) {
			return Color.RED;
		} else if (mode.equalsIgnoreCase("TRAIN")) {
			return Color.YELLOW;
		} else if (mode.equalsIgnoreCase("BICYCLE")) {
			return Color.BLUE;
		}
		return Color.WHITE;
	}

	/**
	 * @return the fragmentListener
	 */
	public OnFragmentListener getFragmentListener() {
		return fragmentListener;
	}

	/**
	 * @param fragmentListener
	 *            the fragmentListener to set
	 */
	public void setFragmentListener(OnFragmentListener fragmentListener) {
		this.fragmentListener = fragmentListener;
	}

	@Override
	public void onServerSelectorComplete(GeoPoint point, Server server) {
		//Update application server
		app.setSelectedServer(server);
		Log.v(TAG, "Automatically selected server: " + server.getRegion());
		MyActivity activity = (MyActivity) this.getActivity();

		activity.zoomToLocation(point);
		activity.setMarker(point, true);
		activity.setMarker(point, false);
	}

	@Override
	public void onTripRequestComplete(List<Itinerary> itineraries,
			String currentRequestString) {
		showRouteOnMap(itineraries.get(0).legs);
		OnFragmentListener ofl = getFragmentListener();

		// onItinerariesLoaded must be invoked before onItinerarySelected(0)
		ofl.onItinerariesLoaded(itineraries);
		ofl.onItinerarySelected(0);
		MyActivity myActivity = (MyActivity) this.getActivity();
		myActivity.setCurrentRequestString(currentRequestString);
	}

	@Override
	public void onOTPGetCurrentLocationComplete(GeoPoint point) {
		zoomToLocation(point);
	}

	@Override
	public void onOTPGeocodingComplete(final boolean isStartTextbox,
			ArrayList<Address> addressesReturn) {
		// isRealLostFocus = false;
		
		try{
			AlertDialog.Builder geocoderAlert = new AlertDialog.Builder(
					this.getActivity());
			geocoderAlert.setTitle(R.string.geocoder_results_title)
					.setMessage(R.string.geocoder_no_results_message)
					.setCancelable(false)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
						}
					});
	
			if (addressesReturn.isEmpty()) {
				AlertDialog alert = geocoderAlert.create();
				alert.show();
				return;
			} else if (addressesReturn.size() == 1) {
				moveMarker(isStartTextbox, addressesReturn.get(0));
				return;
			}
	
			adjustFocusAfterSelectAddress(isStartTextbox);
	
			AlertDialog.Builder geocoderSelector = new AlertDialog.Builder(
					this.getActivity());
			geocoderSelector.setTitle(R.string.choose_geocoder);
	
			final CharSequence[] addressesText = new CharSequence[addressesReturn
					.size()];
			for (int i = 0; i < addressesReturn.size(); i++) {
				Address addr = addressesReturn.get(i);
				addressesText[i] = addr.getAddressLine(0)
						+ "\n"
						+ addr.getAddressLine(1)
						+ ((addr.getAddressLine(2) != null) ? ", "
								+ addr.getAddressLine(2) : "");
				// addressesText[i] = addr.getAddressLine(0)+"\n"+
				// ((addr.getSubAdminArea()!=null) ? addr.getSubAdminArea()+", " :
				// "")+
				// ((addr.getAdminArea()!=null) ? addr.getAdminArea()+" " : "")+
				// ((addr.getPostalCode()!=null) ? addr.getPostalCode()+" " : "")+
				// ((addr.getCountryName()!=null) ? addr.getCountryName() : "");
				Log.v(TAG, addressesText[i].toString());
			}
	
			final ArrayList<Address> addressesTemp = addressesReturn;
			geocoderSelector.setItems(addressesText,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							Address addr = addressesTemp.get(item);
							String addressLine = addr.getAddressLine(0)
									+ "\n"
									+ addr.getAddressLine(1)
									+ ((addr.getAddressLine(2) != null) ? ", "
											+ addr.getAddressLine(2) : "");
							addr.setAddressLine(addr.getMaxAddressLineIndex() + 1,
									addressLine);
							moveMarker(isStartTextbox, addr);
							Log.v(TAG, "Chosen: " + addressesText[item]);
							adjustFocusAfterSelectAddress(isStartTextbox);
						}
					});
			AlertDialog alertGeocoder = geocoderSelector.create();
			alertGeocoder.show();
		}catch(Exception e){
			Log.e(TAG, "Error in Main Fragment Geocoding callback: " + e);
		}
	}
	
	private void updateOverlay(String overlayString){
		if (actualTileOverlay != null){
			actualTileOverlay.remove();
		}
		if (overlayString.startsWith(OTPApp.MAP_TILE_GOOGLE)){
			int mapType = GoogleMap.MAP_TYPE_NORMAL;
			
			if (overlayString.equals(OTPApp.MAP_TILE_GOOGLE_HYBRID)){
				mapType = GoogleMap.MAP_TYPE_HYBRID;
			}
			else if (overlayString.equals(OTPApp.MAP_TILE_GOOGLE_NORMAL)){
				mapType = GoogleMap.MAP_TYPE_NORMAL;	
			}
			else if (overlayString.equals(OTPApp.MAP_TILE_GOOGLE_TERRAIN)){
				mapType = GoogleMap.MAP_TYPE_TERRAIN;
			}
			else if (overlayString.equals(OTPApp.MAP_TILE_GOOGLE_SATELLITE)){
				mapType = GoogleMap.MAP_TYPE_SATELLITE;	
			}
			mMap.setMapType(mapType);
		}
		else{
			mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
			MyUrlTileProvider mTileProvider = new MyUrlTileProvider(256, 256, overlayString);
			actualTileOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mTileProvider));
		}
	}

}