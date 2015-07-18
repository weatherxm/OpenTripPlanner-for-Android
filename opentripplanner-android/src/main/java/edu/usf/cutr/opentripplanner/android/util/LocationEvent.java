package edu.usf.cutr.opentripplanner.android.util;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

public class LocationEvent {

    public String route;
    public double lat;
    public double lon;

    public LocationEvent() {
        super();
    }

    public String getRoute() {
        return route;
    }

    public LatLng getLatLng() {
        return new LatLng(lat, lon);
    }

    public static LocationEvent fromJson(String data) {
        LocationEvent event = new LocationEvent();
        try {
            JSONObject json = new JSONObject(data);
            event.route = json.optString("route");
            event.lat = json.optDouble("lat");
            event.lon = json.optDouble("lon");
        } catch (JSONException e) {
            Timber.e(e, "Could not create location event");
        }
        return event;
    }

    public String getLocationString() {
        return "(" + lat + ", " + lon + ")";
    }
}
