package com.example.fstoeber.possie;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

public class MyActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private LocationClient mLocationClient;
    private Location mCurrLocation;
    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        mLocationClient = new LocationClient(this, this, this);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(300);
        mLocationRequest.setFastestInterval(200);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Connect the LocationListener
        mLocationClient.connect();
    }

    @Override
    protected void onStop() {
        // Stop the LocationListener
        if (mLocationClient.isConnected())
        {
            mLocationClient.removeLocationUpdates(this);
        }
        mLocationClient.disconnect();
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshPosition(View view) {
        mCurrLocation = mLocationClient.getLastLocation();

        if(mCurrLocation != null){
            String timestamp = Calendar.getInstance().get(Calendar.HOUR) +":"+ Calendar.getInstance().get(Calendar.MINUTE) +":"+ Calendar.getInstance().get(Calendar.SECOND);
            TextView locationView = (TextView)view.findViewById(R.id.locationText);
            String text =  timestamp + " Lat: " + mCurrLocation.getLatitude() + " Lon: " + mCurrLocation.getLongitude()+ "\n" +
                    locationView.getText();
            locationView.setText(text);
        } else {
            Toast.makeText(this, "No Position available.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates",
                    "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            Toast.makeText(this, "Error while connecting", Toast.LENGTH_LONG).show();
            return false;
/*            // Get the error code
            int errorCode = ConnectionResult.getErrorCode();
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    errorCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {

            }*/
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        // Display the connection status
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
        Log.d("test", "connected");
    }

    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
               /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Toast.makeText(this, connectionResult.getErrorCode(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrLocation = location;
        Log.d("test", "new location acquired: lat(" + location.getLatitude() + ") lon(" + location.getLongitude() + ")");
        refreshPosition(findViewById(R.id.myactivityroot));
        sendHTTPGETRequest("http://kcdbox.hol.es/updatepos.php?lat=" + location.getLatitude() + "&lon=" + location.getLongitude());
    }

    public void sendHTTPGETRequest(String mUrl)
    {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new sendPosToServer().execute(mUrl);
        }
    }

    // Uses AsyncTask to create a task away from the main UI thread. This task takes a
    // URL string and uses it to create an HttpUrlConnection. Once the connection
    // has been established, the AsyncTask downloads the contents of the webpage as
    // an InputStream. Finally, the InputStream is converted into a string, which is
    // displayed in the UI by the AsyncTask's onPostExecute method.
    private class sendPosToServer extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                InputStream is = null;
                try
                {
                    URL url = new URL(urls[0]);
                    URLConnection ucon = url.openConnection();
                    ucon.setConnectTimeout(5000);
                    ucon.setReadTimeout(5000);
                    is = ucon.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(is, 8192);
                    ByteArrayBuffer baf = new ByteArrayBuffer(300);
                    int current = 0;
                    while ((current = bis.read()) != -1)
                    {
                        baf.append((byte) current);
                    }
                    String success = new String(baf.toByteArray());
                    Log.d("test", "sent Message to server!");
                }
                catch (Exception e)
                {
                }
                finally
                {
                    try
                    {
                        if (is != null)
                        {
                            is.close();
                        }
                    }
                    catch (Exception e)
                    {
                    }
                }
            }
            catch(Exception e){
            }
            return "foo";
        }
    }
}



