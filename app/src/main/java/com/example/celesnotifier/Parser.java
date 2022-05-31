package com.example.celesnotifier;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
    private static final String TAG = "CelesNotifier_Parser";
    private String timeElement;
    private Date date;
    private Context This;
    private boolean timeHasChanged;
    public static final int NO_WARN = 0, RED_WARN = 1, YELLOW_WARN = 2;
    public int warning,
            connection_out_times = 1;
    private static long l;
    private static SimpleDateFormat format_4DISP, format_4PARSNG;
    public static final float yello_warn_thresh = 3.0e-5f, red_warn_thresh = 1.0e-4f,
            yellow_min_range = 2.0f, red_min_range = 0.5f;
    private static String msg = "*IMPORTANT MESSAGE*\n As of current celestrak data " +
            "(updated @ (UPDATE_TIME)), this is to inform " +
            "(TYPE) warning of possible collision of (obj1) with (obj2). Having probability = (PROB)" +
            ", minimum range = (RNG) km and impact time = (IM_TIME) UTC. \n Thanks & Regards, \n" +
            "M. Wajahat Qureshi\n AM(LSCS-K)";
    public static final String default_sign = "*IMPORTANT MESSAGE*\n As of current celestrak data " +
            "(updated @ (UPDATE_TIME)), this is to inform " +
            "(TYPE) warning of possible collision of (obj1) with (obj2). Having probability = (PROB)" +
            ", minimum range = (RNG) km and impact time = (IM_TIME) UTC. \n Thanks & Regards, \n" +
            "M. Wajahat Qureshi\n AM(LSCS-K)";


    Parser(Context context){
        This = context;
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
            if(timeHasChanged) {
                update_lastupdate();
                processLinks2Hashmap(links);
                composeNsaveResults();
            }
    }

    }

    private void processLinks2Hashmap(Elements links){
        Element el_1 = links.get(3); //get first row of results table
        Element el_2 = links.get(4); //get second row of results table
        Elements el1tds = el_1.getElementsByTag("td");
        Elements el2tds = el_2.getElementsByTag("td");
        results.put(This.getString(R.string.obj1_norad), Float.valueOf(el1tds.get(1).text()));
        results.put(This.getString(R.string.obj1_name), el1tds.get(2).text());
        results.put(This.getString(R.string.max_prob), Float.valueOf(el1tds.get(4).text()));
        results.put(This.getString(R.string.min_range), Float.valueOf(el1tds.get(6).text()));
        results.put(This.getString(R.string.rel_vel), Float.valueOf(el1tds.get(7).text()));
        results.put(This.getString(R.string.obj2_norad), Float.valueOf(el2tds.get(0).text()));
        results.put(This.getString(R.string.obj2_name), el2tds.get(1).text());
        results.put(This.getString(R.string.TCA), el2tds.get(5).text());
    }
    private void composeNsaveResults(){
        String warn_type = "No";
        this.warning = NO_WARN;
        //noinspection ConstantConditions
        if ((Float) results.get(This.getString(R.string.min_range)) <=
                red_min_range && (Float) results.get(This.getResources().getString(R.string.max_prob)) >= red_warn_thresh){
            warn_type = "RED"; warning = RED_WARN;}
        else //noinspection ConstantConditions
            if ((Float) results.get(This.getResources().getString(R.string.max_prob)) <= yellow_min_range
                    && (Float) results.get(This.getString(R.string.max_prob)) >= yello_warn_thresh)
            {warn_type = "YELLOW"; warning = YELLOW_WARN;}
        msg = msg.replace("(TYPE)", warn_type.equals("No") ? "that there is no" :
                        warn_type).replace("(obj2)",
                        String.valueOf(results.get(This.getString(R.string.obj1_name))))
                .replace("(PROB)",
                        String.valueOf(results.get(This.getString(R.string.max_prob))))
                .replace("(RNG)",
                        String.valueOf(results.get(This.getString(R.string.min_range))))
                .replace("(UPDATE_TIME)", format_4DISP.format(date))
                .replace("(IM_TIME)",
                        String.valueOf(results.get(This.getString(R.string.TCA))))
                .replace("(obj1)", String.valueOf(results.get(This.getString(R.string.obj2_name))));

        Log.e(TAG, msg);
        saveResults(msg, This);
        for (Map.Entry<String, ? super Object> me :
                results.entrySet()) {
            System.out.print(me.getKey() + ":");
            System.out.println(me.getValue());
        }
    }
    @Nullable
    private Document jsoupConnector(){
        try {
            String URL = "https://celestrak.com/SOCRATES/search-results.php?IDENT" +
                    "=NAME&NAME_TEXT1=PRSS&NAME_TEXT2=&CATNR_TEXT1=&CATNR_TEXT2" +
                    "=&ORDER=MAXPROB&MAX=25&B1=Submit";
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
    public static void saveResults(String msg, @NonNull Context context){
        SharedPreferences my_pref = context.getSharedPreferences(context.
                        getString(R.string.latest_result_key)
                , MODE_PRIVATE);
        SharedPreferences.Editor editor = my_pref.edit();
        editor.putString(context.getString(R.string.latest_result_key), msg);
        editor.apply();
    }
    private void update_lastupdate(){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(This);
        SharedPreferences.Editor editor = pref.edit();
        editor.putLong(This.getString(R.string.lastUpdated_time), date.getTime());
        editor.apply();
    }


    //getters
    public static String getCurrentMsgSign(){
        return msg;
    }
    //

    //setters
    public static void setdefault_sign(String sign){
        if(sign == null || sign.equals("") || sign.equals(" ")) {
            Log.e(TAG,"User has assigned an illegal/invalid msg signature...");
            Log.e(TAG,"User request discarded for sign change..");
            return;
        }
        msg = sign;
        Log.w(TAG,"Warning! Msg signature has been modified by the user");
    }
    //
}
