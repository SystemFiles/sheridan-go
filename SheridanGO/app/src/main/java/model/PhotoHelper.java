package model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PhotoHelper {
    private final String TAG = "PHOTO_HELPER";
    private String photoID;
    private final String GOOGLE_BROWSER_API_KEY =
            "AIzaSyBj50PHW-4HWIoVv91Vlvt9VEuVcjOQr24"; // DO NOT SHARE!
    private String fullURL;

    /**
     * Init a PhotoHelper object.
     * @param photoID The ID corresponding to the photo we want.
     */
    public PhotoHelper(String photoID) {
        this.photoID = photoID;

        StringBuilder urlBuilder = new
                StringBuilder("https://maps.googleapis.com/maps/api/place/photo?");
        urlBuilder.append("maxwidth=400");
        urlBuilder.append("&photoreference=" + photoID);
        urlBuilder.append("&key=" + GOOGLE_BROWSER_API_KEY);

        fullURL = urlBuilder.toString();
    }

    /**
     * Returns a photo given a photoreference to googleapis
     * @return An image requested by API
     */
    public Bitmap getPhotoFromID() {
        return sendRequestForImage(fullURL);
    }

    /**
     * Returns the image response from the HTTP request to googleapis servers,
     * Keep private to avoid potential memory leaks, instead we use an accessor method to get
     * the image.
     * @param url The url for the request
     * @return The Image response from the HTTP request.
     */
    private Bitmap sendRequestForImage(String url) {
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
            InputStream is = con.getInputStream();
            Bitmap img = BitmapFactory.decodeStream(is);


            if (responseCode != HttpURLConnection.HTTP_BAD_GATEWAY) {
                // return the image bitmap
                return img;
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return null;
    }
}
