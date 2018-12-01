package ca.sykesdev.sheridango.model;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import ca.sykesdev.sheridango.R;

public class PlacesHelper {

    // MAIN PROPERTY
    private ArrayList<Property> properties;

    // Context of caller
    private Context context;

    // VARIABLES USED IN JSON PARSING
    private final String RESULTS = "results";
    private final String STATUS = "status";
    private final String STATUS_OK = "OK";
    private final String STATUS_ZERO_RESULTS = "ZERO_RESULTS";
    private final String STATUS_REQUEST_DENIED = "REQUEST_DENIED";
    private final String STATUS_OVER_USE_LIMIT = "OVER_QUERY_LIMIT";
    private final String GEOMETRY = "geometry";
    private final String LOCATION = "location";
    private final String ID = "id";
    private final String PLACE_NAME = "name";
    private final String PLACE_LAT = "lat";
    private final String PLACE_LONG = "lng";
    private final String PLACE_PHOTOS = "photos";
    private final String PLACE_PHOTO_REFERENCE = "photo_reference";
    private final String PLACE_TYPE = "types";

    // Constants needed to run!
    public static final String TAG = "PLACES_HELPER";
    private final String GOOGLE_BROWSER_API_KEY =
            "AIzaSyBj50PHW-4HWIoVv91Vlvt9VEuVcjOQr24"; // DO NOT SHARE!


    /**
     * Set context used to access elements from caller.
     * @param context The callers context...
     */
    public PlacesHelper(Context context) {
        this.context = context;
    }

    /**
     * Used by other classes to retrieve nearby place data based on their current location
     * or given Latitude/Longitude and radius rules. (Acts as an accessor to private functions)
     * @param latitude The latitude to ping
     * @param longitude The longitude to ping
     * @param radius The radius to scan for places
     * @return The arrayList of places
     */
    public ArrayList<Property> getProperties(double latitude, double longitude, double radius) {
        // Get places details given the current GPS Coordinates
        return getPlacesByLocation(latitude, longitude, (int) radius);
    }

    public void setProperties(ArrayList<Property> properties) {
        this.properties = properties;
    }

    /**
     * Returns place data given coordinates and a radius around them.
     * @param latitude The latitude coordinate
     * @param longitude The Longitude coordinate
     * @param radius The Radius around the phones current location to get place details from.
     * @return The places details in the form of an arrayList
     */
    private ArrayList<Property> getPlacesByLocation(double latitude, double longitude, int radius) {
        StringBuilder googlePlacesQuery =
                new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");

        // Build API Query
        googlePlacesQuery.append("location=").append(latitude).append(",").append(longitude);
        googlePlacesQuery.append("&radius=").append(radius);
        googlePlacesQuery.append("&key=" + GOOGLE_BROWSER_API_KEY);

        String url = googlePlacesQuery.toString();
        JSONObject placesObjectResponse = sendRequestForJSON(url);

        try {
            // Check for error in HTTPRequest
            if (placesObjectResponse == null || placesObjectResponse.getString(STATUS).
                    equalsIgnoreCase(STATUS_REQUEST_DENIED)) {
                Log.e(TAG, "Error requesting data from server resulting in Null JSONObject");
                return null; // Do not continue getting data...return control to user.
            } else {
                ArrayList<Property> nearbyPlaces = parsePlaceFromJSONObject(placesObjectResponse);

                // Return places location information...
                return nearbyPlaces;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error could not retrieve request data...");
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    /**
     * Parses the given JSON object which contains the request from Google Places web API
     * and then we return the list of PlaceObjects for a Nearby Places response
     * containing all relevant information about the place
     * @param results The JSON object to be parsed.
     * @return A list of Places Objects generated
     */
    private ArrayList<Property> parsePlaceFromJSONObject(JSONObject results) {
        String id, name=null, type, photoID;
        double cost=100000.00, investAmount, latitude, longitude;

        ArrayList<Property> placeObjects = new ArrayList<>();

        // Start parsing
        try {
            JSONArray resultsArray = results.getJSONArray(RESULTS);

            // Check response code status to make sure request was successful
            if (results.getString(STATUS).equalsIgnoreCase(STATUS_OK)) {
                for (int i = 0; i < resultsArray.length(); i++) {
                    JSONObject place = resultsArray.getJSONObject(i);

                    // Get basic information
                    id = place.getString(ID);
                    if (!place.isNull(PLACE_NAME)) {
                        name = place.getString(PLACE_NAME);
                    }

                    // Get all other information
                    type = place.getJSONArray(PLACE_TYPE).getString(0);
                    try {
                        photoID = place.getJSONArray(PLACE_PHOTOS).getJSONObject(0)
                                .getString(PLACE_PHOTO_REFERENCE);
                    } catch (JSONException e) {
                        Log.i(TAG, "parsePlaceFromJSONObject: Could not associate a photo with this place," +
                                "will add it anyway though. PhotoID = NULL;");
                        photoID = "NULL";
                    }
                    latitude = place.getJSONObject(GEOMETRY).getJSONObject(LOCATION)
                            .getDouble(PLACE_LAT);
                    longitude = place.getJSONObject(GEOMETRY).getJSONObject(LOCATION)
                            .getDouble(PLACE_LONG);


                    // Get values from stored values file
                    String[] specialTypes = context.getResources().
                            getStringArray(R.array.types);
                    int [] specialTypeValues = context.getResources().
                            getIntArray(R.array.types_values);

                    // Determine cost and invest amount initialization based on property type.
                    for (int j = 0; j < specialTypes.length; j++) {
                        if (type.equalsIgnoreCase(specialTypes[j])) {
                            cost = specialTypeValues[j];
                            break;
                        }
                    }

                    // Add Place Object to list (INIT)
                    placeObjects.add(new Property(id, name, photoID, type, cost,
                            0.00, latitude, longitude));
                    cost = 100000.00; // Reset The cost value after each place...
                }
            } else if (results.getString(STATUS)
                    .equalsIgnoreCase(STATUS_ZERO_RESULTS)) {
                Log.i(TAG,
                        "INFO: Did not find any places within set radius of your current location");
                return null;
            } else if (results.getString(STATUS).equalsIgnoreCase(STATUS_REQUEST_DENIED)) {
                Log.e(TAG, "ERROR: API Key Denied HTTP Access");
                return null;
            } else if (results.getString(STATUS).equalsIgnoreCase(STATUS_OVER_USE_LIMIT)) {
                Log.e(TAG, "ERROR: Over API Request limit! Cannot use for next while...");
                return null;
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage() + ". Cannot continue with this secondary error," +
                    "Ignoring property...");
        }

        return placeObjects; // Finally we return the list of nearby places
    }

    /**
     * Returns a response from an HTTP GET request to the URL specified in the parameters.
     * If no response is returned or an error in request then we simply return NULL.
     * @param url The url for the request
     * @return The JSONObject containing the response from the HTTP request.
     */
    private JSONObject sendRequestForJSON(String url) {
        /**
         * Use of normal HTTP request to request JSON data from Places API..
         */
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            // optional default is GET
            con.setRequestMethod("GET");
            //add request header
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            int responseCode = con.getResponseCode();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close(); // Close the BufferReader

            try {
                if (responseCode != HttpURLConnection.HTTP_BAD_GATEWAY) {
                    //Read JSON response and print
                    return new JSONObject(response.toString());
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return null;
    }

}
