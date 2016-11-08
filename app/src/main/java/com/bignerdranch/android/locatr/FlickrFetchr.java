package com.bignerdranch.android.locatr;

import android.location.Location;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sredorta on 10/13/2016.
 */
public class FlickrFetchr {
    private static final String TAG = "SERGI:FlickrFetchr";
    private static final String API_KEY ="ac13dca319e85342567b7ca58a42aaa4";
    private static final String FETCH_RECENT_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    public static final String PHOTOS_PER_PAGE = "30";
    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();


    //Get raw data from URL
    public byte[] getURLBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in            =  connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": width " + urlSpec);
            }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer))>0) {
                out.write(buffer,0,bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    //Get string data from URL
    public String getURLString(String urlSpec) throws IOException {
        return new String(getURLBytes(urlSpec));
    }

    public List<GalleryItem> fetchRecentPhotos(int pageNumber) {
        String url = buildUrl(FETCH_RECENT_METHOD, null, pageNumber);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query, int pageNumber) {
        String url = buildUrl(SEARCH_METHOD, query, pageNumber);
        return downloadGalleryItems(url);
    }

    //Search photos by location
    public List<GalleryItem> searchPhotos(Location location) {
        String url = buildUrl(location);
        return downloadGalleryItems(url);
    }

    //Use the Key to build valid URL
    public List<GalleryItem> downloadGalleryItems(String url) {
        List<GalleryItem> items = new ArrayList<>();
        try {
            String jsonString = getURLString(url);
            Log.i(TAG, "Received JSON:" + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items, jsonBody);

        } catch (JSONException je) {
                Log.e(TAG,"Failed to parse JSON", je);
        } catch (IOException ioe) {
            //Toast.makeText (mActivity,"Error JSON",Toast.LENGTH_LONG).show();
            Log.e(TAG,"Falied to fetch items !", ioe);
        }
        return items;
    }

    //Build http string besed on method and query
    private String buildUrl(String method, String query, int pageNumber) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method)
                .appendQueryParameter("page", String.valueOf(pageNumber));
        if (method.equals(SEARCH_METHOD)) {
            uriBuilder.appendQueryParameter("text", query);
        }
        return uriBuilder.build().toString();
    }
    //Add location on the URL
    private String buildUrl(Location location) {
        return ENDPOINT.buildUpon()
                .appendQueryParameter("method", SEARCH_METHOD)
                .appendQueryParameter("lat", "" + location.getLatitude())
                .appendQueryParameter("lon", "" + location.getLongitude())
                .build().toString();
    }


    //Parse JSON object
    private void parseItems(List<GalleryItem> items,JSONObject jsonBody) throws IOException, JSONException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");


        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
            GalleryItem item = GalleryItem.parseJSON(photoJsonObject.toString());
            items.add(item);
        }
    }

}
