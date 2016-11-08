package com.bignerdranch.android.locatr;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

/**
 * Created by sredorta on 11/8/2016.
 */
public class LocatrFragment extends SupportMapFragment {
    // Create client for google API
    private GoogleApiClient mClient;
    private ProgressDialog mProgressDialog;
    private Bitmap mMapImage;
    private GalleryItem mMapItem;
    private Location mCurrentLocation;
    private GoogleMap mMap;


    public static LocatrFragment newInstance() {
        return new LocatrFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProgressDialog = new ProgressDialog(getContext(),0);
        mProgressDialog.setMax(100);
        mProgressDialog.setProgress(0);
        mProgressDialog.setTitle(R.string.dialog_title);
        setHasOptionsMenu(true);
        //Declare the client
        //mClient = new GoogleApiClient.Builder(getActivity()).addApi(LocationServices.API).build();
        mClient = new GoogleApiClient.Builder(getActivity()).addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        //Invalideate options menu when callback says connected so that button is updated
                        getActivity().invalidateOptionsMenu();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                updateUI();
            }
        });
    }



    //Connect the Google API client during onStart
    @Override
    public void onStart() {
        super.onStart();
        getActivity().invalidateOptionsMenu();
        mClient.connect();
    }

    //Disconnect the Google API client during onStop
    @Override
    public void onStop() {
        super.onStop();
        mClient.disconnect();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_locatr, menu);
        MenuItem searchItem = menu.findItem(R.id.action_locate);
        searchItem.setEnabled(mClient.isConnected());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_locate:
                Toast.makeText(getActivity(),"Finding image !",Toast.LENGTH_LONG).show();
                    mProgressDialog.show();
                    findImage();

                    return true;
            default:
                    return super.onOptionsItemSelected(item);
        }
    }

    private void findImage() {
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setNumUpdates(1);
        request.setInterval(0);
        LocationServices.FusedLocationApi
                .requestLocationUpdates(mClient, request, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        Toast.makeText(getActivity(),"Got current location:" + location,Toast.LENGTH_LONG).show();
                        new SearchTask().execute(location);
                    }
                });
    }

    private void updateUI() {
        if (mMap == null || mMapImage == null) {
            return;
        }
        LatLng itemPoint = new LatLng(mMapItem.getLat(),mMapItem.getLon());
        LatLng myPoint   = new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());
//        Toast.makeText(getActivity(),"item \nlat: " + mMapItem.getLat() + "\nlon: " + mMapItem.getLon(), Toast.LENGTH_LONG).show();
//        Toast.makeText(getActivity(),"curr\n lat: " + mCurrentLocation.getLatitude() + "\nlon: " + mCurrentLocation.getLongitude(), Toast.LENGTH_LONG).show();
        //Add markers on the two points
        BitmapDescriptor itemBitmap = BitmapDescriptorFactory.fromBitmap(mMapImage);
        MarkerOptions itemMarker = new MarkerOptions()
                .position(itemPoint)
                .icon(itemBitmap)
                .zIndex(10);
        MarkerOptions myMarker = new MarkerOptions()
                .position(myPoint);
        mMap.clear();
        mMap.addMarker(itemMarker);
        mMap.addMarker(myMarker);
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(itemPoint)
                .include(myPoint)
                .build();
        //int margin = getResources().getDimensionPixelSize(R.dimen.map_intersect_margin);
        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds,0);
        mMap.animateCamera(update);

    }

    private class SearchTask extends AsyncTask<Location,Void,Void> {
        private GalleryItem mGalleryItem;
        private Bitmap mBitmap;
        private Location mLocation;


        @Override
        protected Void doInBackground(Location... params) {
            mLocation = params[0];
            FlickrFetchr fetchr = new FlickrFetchr();
            List<GalleryItem> items = fetchr.searchPhotos(params[0]);
            if (items.size() == 0) {
                return null;
            } else {
                mGalleryItem = items.get(0);
 //               Toast.makeText(getActivity(),"Current Geo field is:\n" + "Lat : " + mGalleryItem.getLat() + "\nLon : " +mGalleryItem.getLon() , Toast.LENGTH_LONG).show();
                try {
                    byte[] bytes = fetchr.getURLBytes(mGalleryItem.getUrl());
                    mBitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                } catch (IOException ioe) {
                    Log.i("bck","Unable to download!");
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mProgressDialog.cancel();
            mMapImage = mBitmap;
            mMapItem = mGalleryItem;
            mCurrentLocation = mLocation;
            updateUI();
        }
    }
}
