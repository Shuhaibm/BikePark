package shuhaib.app.bikepark;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;



public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private LocationManager locationManager;
    private Double longitude, latitude;

    private AdView mAdView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);

        if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},1);
        }

        //Ads
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    //On click for refresh
    public void newLocation(View v){
        v.setEnabled(false);
        refreshLocation(v);
    }
    //For synchronization, goes after newLocation is done
    public void doRest(View v){
        getInfo(getLink());
        v.setEnabled(true);
    }

    public void refreshLocation(View v){
        //Get Location
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //Gets permission for location services
        if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},1);
        }

        //Makes call for location
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 100, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                longitude = location.getLongitude();
                latitude = location.getLatitude();

                //This fixes the asynchrony
                doRest(v);
            }
        });
    }

    public String getLink(){
        String newUrl = "https://bikewise.org:443/api/v2/incidents?incident_type=theft&proximity="+ String.valueOf(latitude) + "%2C" + String.valueOf(longitude) + "&proximity_square=1";
        return newUrl;
    }

    public void getInfo(String url){
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET,url, new Response.Listener<String>() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onResponse(String response) {
                processInfo(response);
            }
        },new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("CREATION", "That didn't work!");
            }
        });
        queue.add(stringRequest);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public void processInfo(String response){
        try {
            //Get JSON info
            JSONObject responseJSON = new JSONObject(response);
            JSONArray bikeArray = responseJSON.getJSONArray("incidents");

            //Get IDs + clear previous results from scrollview screen
            ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView);
            LinearLayout layout = (LinearLayout) scrollView.findViewById(R.id.layout);
            layout.removeAllViews();

            int theftsInPastYear = 0;

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.DATE, -365);
            Date oneYearAgo = cal.getTime();

            for (int i = 0; i < bikeArray.length();i++){
                //Process Info from JSON
                JSONObject curr = bikeArray.getJSONObject(i);

                String title = (String) curr.get("title");
                String address = (String) curr.get("address");
                Object htmlURL = curr.getJSONObject("source").get("html_url");

                String dateString = (String) curr.getString("occurred_at");
                dateString = dateString + "000";
                long dateInt = Long.parseLong(dateString);
                Date newDate= new Date(dateInt);

                //Total Thefts in Past year
                if (oneYearAgo.before(newDate)){
                    theftsInPastYear++;
                }

                //Create new cardview
                CardView tempCard = new CardView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                lp.setMargins(20, 20, 20, 20);
                tempCard.setLayoutParams(lp);
                TextView newText = new TextView(this);
                newText.setAutoLinkMask(Linkify.ALL);
                newText.setText("  " + title + "\n     " + "Date occurred:" + "  " + newDate + "\n  "+ String.valueOf(htmlURL));
                tempCard.addView(newText);
                //Add to scrollView
                layout.addView(tempCard);
            }

            CardView cardView = findViewById(R.id.cardView);
            TextView textView2 = findViewById(R.id.textView2);

            textView.setText(String.valueOf(theftsInPastYear) + " thefts in the past year");

            if (theftsInPastYear == 0){
                cardView.setCardBackgroundColor(Color.GREEN);
                textView2.setText("Insignificant Risk");
            } else if ((theftsInPastYear > 0) && (theftsInPastYear <=10)){
                cardView.setCardBackgroundColor(Color.parseColor("#48FFFF00"));
                textView2.setText("Minor Risk");
            } else if((theftsInPastYear > 10) && (theftsInPastYear <=30)){
                cardView.setCardBackgroundColor(Color.parseColor("#9CFFD740"));
                textView2.setText("Medium Risk");
            } else if (theftsInPastYear>30){
                cardView.setCardBackgroundColor(Color.parseColor("#95FF0000"));
                textView2.setText("Major Risk");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}

