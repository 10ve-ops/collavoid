package com.example.celesnotifier;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class FetcherService extends Service {
    public static String TAG = "Fetcher Service";
    static String updateTime;
    public static final String URL = "https://celestrak.com/SOCRATES/search-results.php?IDENT=NAME&NAME_TEXT1=PRSS&NAME_TEXT2=&CATNR_TEXT1=&CATNR_TEXT2=&ORDER=MAXPROB&MAX=25&B1=Submit";
    public static final Float PRSS_NORAD = 43530f;
    public Context This;
    public static final float yello_warn_thresh = 3.0e-5f, red_warn_thresh = 1.0e-4f, yellow_min_range = 2.0f, red_min_range = 0.5f;
    public static String msg = "*IMPORTANT MESSAGE*\n As of current celestrak data, this is to inform " +
            "(COLOR) warning of possible collision of PRSS with (DEB). Having probability = (PROB)" +
            ", minimum range = (RNG) km and impact time = (IM_TIME) UTC. \n Thanks & Regards, \n" +
            "M. Wajahat Qureshi\n AM(LSCS-K)";


    BroadcastReceiver br = new MyBroadcastReceiver();
    public class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.celesnotifier.Query".equals(intent.getAction())) {
                Intent intent1 = new Intent();
                intent1.setAction("com.example.celesnotifier.FETCHER_SERVICE");
                intent1.putExtra("status", "true");
                intent1.setPackage("com.example.celesnotifier");
                sendBroadcast(intent1);
            }
        }
    }


    //new code
    int connection_out_times = 1;
    private Document jsoupConnector() {
        try {
            Connection connection =  Jsoup.connect(URL);
            return connection.get();
        } catch (IOException e) {
            if(connection_out_times<10) {
                Log.e(TAG, "Times: " + connection_out_times++ + " Connection Error, Retrying....");
                return jsoupConnector();
            }
            else return null;
        }
    }
    private boolean updateTimeAndCheck4Change(Date thisDate, Context This){ //compare thisTime with oldTime if oldTime!=0 then return boolean showing that this query is unique w.r.t time
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(This);
        SharedPreferences.Editor editor = pref.edit();
        Date prev_date = new Date(pref.getLong(This.getString(R.string.lastUpdated_time), 0));
        Log.e(TAG,"Last update time: "+ prev_date);
        Log.e(TAG,"This query time: "+ thisDate);
        if (thisDate.after(prev_date))
        {
            editor.putLong(This.getString(R.string.lastUpdated_time), thisDate.getTime());
            editor.apply();
            return true;
        }
        else
            return false;
    }
    //new code end
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = new IntentFilter("com.example.celesnotifier.Query");
        this.registerReceiver(br, filter);
        sendBroadcast("true");
        Log.e(TAG, "OnStartCommand()");
        Log.i(TAG,"Yellow & Red Warning prob threshold: "+yello_warn_thresh+" "+red_warn_thresh);
        Log.i(TAG,"Yellow & Red Warning range threshold: "+yellow_min_range+" "+red_min_range);
        This = this.getApplicationContext();
        Thread thread = new Thread(new SeparateThread());
            //thread.start();
        //new code starts here
        Map<String, ? super Object > hm
                = new HashMap<>();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG,"Running in the Thread " +
                        Thread.currentThread().getId());
                Document doc = jsoupConnector();
                if (doc != null) {
                    Elements links = doc.select("tr:not(.shade)");
                    links = links.select("tr:not(.header)");
                    Log.i(TAG, "Parsed html length = " + (links.indexOf(links.last()) + 1));
                    for (Element link : links) { //get print of all data
                        Log.i(TAG, "Parsed html:" + link.text());
                    }
                    Element time_element = links.get(2); //get time element
                    Log.i(TAG, "Time element: " + time_element.text());
                    Elements tds = time_element.getElementsByTag("td");
                    for (Element td : tds) {
                        updateTime = td.text().substring(td.text().indexOf(String.valueOf(Calendar.getInstance().get(Calendar.YEAR))), td.text().indexOf("UTC") - 1);
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat(This.getString(R.string.dateNTimeFormat));
                        try {
                            Date date = format.parse(updateTime);
                            if (date != null)
                                if (updateTimeAndCheck4Change(date, This)) {
                                    Log.i(TAG, "Saving data into database");
                                }

                            System.out.println(date);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                    }
                    Element el_1 = links.get(3); //get first row of results table
                    Element el_2 = links.get(4); //get second row of results table
                    Elements el1tds = el_1.getElementsByTag("td");
                    Elements el2tds = el_2.getElementsByTag("td");
                    hm.put(getString(R.string.fetched_prss_norad_k), Float.valueOf(el1tds.get(1).text()));
                    hm.put(getString(R.string.fetched_max_prob_k), Float.valueOf(el1tds.get(4).text()));
                    hm.put(getString(R.string.fetched_min_range_k), Float.valueOf(el1tds.get(6).text()));
                    hm.put(getString(R.string.fetched_rel_vel_k),  Float.valueOf(el1tds.get(7).text()));
                    hm.put(getString(R.string.fetched_deb_norad_k),  Float.valueOf(el2tds.get(0).text()));
                    hm.put(getString(R.string.fetched_deb_name_k), el2tds.get(1).text());
                    hm.put(getString(R.string.fetched_min_range_time_k), el2tds.get(5).text());

                    if (!PRSS_NORAD.equals(hm.get(getString(R.string.fetched_prss_norad_k))) )
                        Log.e(TAG,"PARSING ERROR! at table elements parsing");
                    else{
                        String warn_type;
                        //noinspection ConstantConditions
                        if ((Float) hm.get(getString(R.string.fetched_min_range_k)) <=
                                red_min_range && (Float) hm.get(getResources().getString(R.string.fetched_max_prob_k))>=red_warn_thresh)
                            warn_type = "RED";
                        else //noinspection ConstantConditions
                            if ((Float) hm.get(getResources().getString(R.string.fetched_max_prob_k)) <= yellow_min_range
                                    && (Float) hm.get(getString(R.string.fetched_max_prob_k)) >= yello_warn_thresh)
                                warn_type = "YELLOW";
                            else warn_type = "NONE";
                        String emsg = msg;
                        emsg = emsg.replace("(COLOR)", warn_type).replace("(DEB)",
                                String.valueOf(hm.get(getString(R.string.fetched_deb_name_k))))
                                .replace("(PROB)",
                                        String.valueOf(hm.get(getString(R.string.fetched_max_prob_k))))
                                .replace("(RNG)",
                                        String.valueOf(hm.get(getString(R.string.fetched_min_range_k))))
                                .replace("(IM_TIME)",
                                        String.valueOf(hm.get(getString(R.string.fetched_min_range_time_k))));
                        if (!warn_type.equals("NONE")) {
                            Log.e(TAG, emsg);
                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage("+923342576758", null, emsg, null, null);
                        }
                        else {
                            Log.e(TAG, "No celestrak warning");
                            Log.e(TAG, emsg);
                        }
                    }
                    /*
                    for (Map.Entry<String, String> me :
                            hm.entrySet()) {
                        System.out.print(me.getKey() + ":");
                        System.out.println(me.getValue());
                    }
                 */
                }
                else
                    Log.e(TAG, "Can't Connect... Please check internet " +
                            "connection then restart the service...");
            }
        }, 500,5000);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "OnCreate()");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(br);
        sendBroadcast("false");
        Log.e(TAG, "Service-OnDestroy()");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendBroadcast(String status){ //status = "true" when service
        // is started and vice versa
        Intent intent1 = new Intent();
        intent1.setAction("com.example.celesnotifier.FETCHER_SERVICE");
        intent1.putExtra("status", status);
        intent1.setPackage("com.example.celesnotifier");
        sendBroadcast(intent1);
    }

    public  class SeparateThread implements Runnable {
        int connection_out_times = 1;
        Map<String, ? super Object > hm
                = new HashMap<>();
        private Document jsoupConnector(){
            try {
                Connection connection =  Jsoup.connect(URL);
                return connection.get();
            } catch (IOException e) {
                if(connection_out_times<10) {
                    Log.e(TAG, "Times: " + connection_out_times++ + " Connection Error, Retrying....");
                    return jsoupConnector();
                }
                else return null;
            }
        }
        public void run(){
            Log.d(TAG,"Running in the Thread " +
                    Thread.currentThread().getId());
            Document doc = jsoupConnector();
            if (doc != null) {
                Elements links = doc.select("tr:not(.shade)");
                links = links.select("tr:not(.header)");
                Log.i(TAG, "Parsed html length = " + (links.indexOf(links.last()) + 1));
                for (Element link : links) { //get print of all data
                    Log.i(TAG, "Parsed html:" + link.text());
                }
                Element time_element = links.get(2); //get time element
                Log.i(TAG, "Time element: " + time_element.text());
                Elements tds = time_element.getElementsByTag("td");
                for (Element td : tds) {
                    updateTime = td.text().substring(td.text().indexOf(String.valueOf(Calendar.getInstance().get(Calendar.YEAR))), td.text().indexOf("UTC") - 1);
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat(This.getString(R.string.dateNTimeFormat));
                    try {
                        Date date = format.parse(updateTime);
                        if (date != null)
                            if (updateTimeAndCheck4Change(date, This)) {
                                Log.i(TAG, "Saving data into database");
                            }

                        System.out.println(date);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                }
                Element el_1 = links.get(3); //get first row of results table
                Element el_2 = links.get(4); //get second row of results table
                Elements el1tds = el_1.getElementsByTag("td");
                Elements el2tds = el_2.getElementsByTag("td");
                hm.put(getString(R.string.fetched_prss_norad_k), Float.valueOf(el1tds.get(1).text()));
                hm.put(getString(R.string.fetched_max_prob_k), Float.valueOf(el1tds.get(4).text()));
                hm.put(getString(R.string.fetched_min_range_k), Float.valueOf(el1tds.get(6).text()));
                hm.put(getString(R.string.fetched_rel_vel_k),  Float.valueOf(el1tds.get(7).text()));
                hm.put(getString(R.string.fetched_deb_norad_k),  Float.valueOf(el2tds.get(0).text()));
                hm.put(getString(R.string.fetched_deb_name_k), el2tds.get(1).text());
                hm.put(getString(R.string.fetched_min_range_time_k), el2tds.get(5).text());

                if (!PRSS_NORAD.equals(hm.get(getString(R.string.fetched_prss_norad_k))) )
                    Log.e(TAG,"PARSING ERROR! at table elements parsing");
                else{
                    String warn_type;
                    //noinspection ConstantConditions
                    if ((Float) hm.get(getString(R.string.fetched_min_range_k)) <=
                            red_min_range && (Float) hm.get(getResources().getString(R.string.fetched_max_prob_k))>=red_warn_thresh)
                        warn_type = "RED";
                    else //noinspection ConstantConditions
                        if ((Float) hm.get(getResources().getString(R.string.fetched_max_prob_k)) <= yellow_min_range
                            && (Float) hm.get(getString(R.string.fetched_max_prob_k)) >= yello_warn_thresh)
                        warn_type = "YELLOW";
                    else warn_type = "NONE";
                    String emsg = msg;
                    emsg = emsg.replace("(COLOR)", warn_type).replace("(DEB)",
                            String.valueOf(hm.get(getString(R.string.fetched_deb_name_k))))
                            .replace("(PROB)",
                                    String.valueOf(hm.get(getString(R.string.fetched_max_prob_k))))
                            .replace("(RNG)",
                                    String.valueOf(hm.get(getString(R.string.fetched_min_range_k))))
                            .replace("(IM_TIME)",
                                    String.valueOf(hm.get(getString(R.string.fetched_min_range_time_k))));
                    if (!warn_type.equals("NONE")) {
                        Log.e(TAG, emsg);
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage("+923342576758", null, emsg, null, null);
                    }
                    else {
                        Log.e(TAG, "No celestrak warning");
                        Log.e(TAG, emsg);
                    }
                }
                    /*
                    for (Map.Entry<String, String> me :
                            hm.entrySet()) {
                        System.out.print(me.getKey() + ":");
                        System.out.println(me.getValue());
                    }
                 */
                }
            else
                Log.e(TAG, "Can't Connect... Please check internet " +
                        "connection then restart the service...");
        }

        private boolean updateTimeAndCheck4Change(Date thisDate, Context This){ //compare thisTime with oldTime if oldTime!=0 then return boolean showing that this query is unique w.r.t time
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(This);
            SharedPreferences.Editor editor = pref.edit();
            Date prev_date = new Date(pref.getLong(This.getString(R.string.lastUpdated_time), 0));
            Log.e(TAG,"Last update time: "+ prev_date);
            Log.e(TAG,"This query time: "+ thisDate);
            if (thisDate.after(prev_date))
            {
                editor.putLong(This.getString(R.string.lastUpdated_time), thisDate.getTime());
                editor.apply();
                return true;
            }
            else
                return false;
        }
    }

}
