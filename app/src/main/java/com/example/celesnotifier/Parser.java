package com.example.celesnotifier;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.ContentHandler;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class Parser {
    private static Map<String, ? super Object > results;
    private static String URL =  "https://celestrak.com/SOCRATES/search-results.php?IDENT" +
            "=NAME&NAME_TEXT1=PRSS&NAME_TEXT2=&CATNR_TEXT1=&CATNR_TEXT2" +
            "=&ORDER=MAXPROB&MAX=25&B1=Submit", TAG = "CelesNotifier_Parser";
    private String timeElement;
    private Date date;
    private Context This;
    private static boolean x;
    private boolean timeHasChanged;
    private static float z;
    private static int NONE_WARN = 0, RED_WARN = 1, YELLOW_WARN = 2, thisWarn,
            connection_out_times = 1;
    private static long l;
    private static SimpleDateFormat format_4DISP, format_4PARSNG;


    Parser(Context This){
        results = new HashMap<>();
        format_4DISP = new SimpleDateFormat(This.getString(R.string.dateNTimeSMS_Format), Locale.US);
        format_4DISP.setTimeZone(TimeZone.getTimeZone("UTC"));
        format_4PARSNG = new SimpleDateFormat(This.getString(R.string.dateNTimePSRSE_Format), Locale.US);
        format_4PARSNG.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void init(){
        Document doc = jsoupConnector();
        if (doc != null) {
            Elements links = doc.select("tr:not(.shade)");
            links = links.select("tr:not(.header)");
            //Log.i(TAG, "Parsed html length = " + (links.indexOf(links.last()) + 1));
                    /*
                    for (Element link : links) { //get print of all data
                        Log.i(TAG, "Parsed html:" + link.text());
                    }*/

            Elements time_element_paras = links.select("p"); //get time element paras
            String time_el_txt = time_element_paras.get(2).text();
            Log.i(TAG, "Time element: " + time_el_txt);
            timeElement = time_el_txt.substring(time_el_txt
                    .indexOf(String.valueOf(Calendar.getInstance().
                            get(Calendar.YEAR))), time_el_txt.indexOf("UTC")-1);
            try {
                Log.i(TAG,"Current Query Date String ="+timeElement);
                date = format_4PARSNG.parse(timeElement);
                //GMT+05 is not a property of Date obj.
                //Log.e(TAG,"Parsed Date From Time Element:"+date);
                //This print statement adds it

                if (date != null) {
                    timeHasChanged = check4Change(date, This);
                    Log.e(TAG,"Parsed Date From Time Element:"+
                            format_4DISP.format(date));
                } else{
                    Log.e(TAG, "FATAL! Parse date was null!");
                }
            } catch (ParseException e) {
                Log.e(TAG,"FATAL! timeElement parsing failed");
                e.printStackTrace();
            }
    }}


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
    private static boolean check4Change(Date thisDate, Context This){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(This);
        Date prev_date = new Date(pref.getLong(This.getString(R.string.lastUpdated_time), 0));
        Log.e(TAG,"Last update time: "+ format_4DISP.format(prev_date));
        Log.e(TAG,"This query time: "+ format_4DISP.format(thisDate));
        return thisDate.after(prev_date);
    }
    //getters
    public Date getQueryTime(){
        return date;
    }
    //

    //setters
    //
}
