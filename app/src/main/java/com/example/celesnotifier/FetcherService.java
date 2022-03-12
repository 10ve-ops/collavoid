package com.example.celesnotifier;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class FetcherService extends Service {
    public static String TAG = "Fetcher Service";
    static String updateTime;
    public static final String URL = "https://celestrak.com/SOCRATES/search-results.php?IDENT" +
            "=NAME&NAME_TEXT1=PRSS&NAME_TEXT2=&CATNR_TEXT1=&CATNR_TEXT2" +
            "=&ORDER=MAXPROB&MAX=25&B1=Submit";
    public static final Float PRSS_NORAD = 43530f;
    public Context This;
    private TimerTask timerTask, flushLog_timerTask;
    private Date date;
    private boolean timeHasChanged = false;
    public static final long logIntervals_inHours = 3;
    private final static float maxAllowedLogFileSize = 1024f; //1GB Log allowed
    //private static boolean activity_is_alive = false;
    private static boolean send_sms2all = false;
    private static final ArrayList<String> numbrs = new ArrayList<>();
    private static final long delay_bf_first_exec = 0;
    Map<String, ? super Object > hm;
    private static long delay_bw_queries = TimeUnit.MINUTES.toMillis(10);
    public static final float yello_warn_thresh = 3.0e-5f, red_warn_thresh = 1.0e-4f,
            yellow_min_range = 2.0f, red_min_range = 0.5f;
    SimpleDateFormat format_4DISP, format_4PARSNG;
    public static String msg = "*IMPORTANT MESSAGE*\n As of current celestrak data " +
            "(updated @ (UPDATE_TIME) UTC), this is to inform " +
            "(TYPE) warning of possible collision of PRSS with (DEB). Having probability = (PROB)" +
            ", minimum range = (RNG) km and impact time = (IM_TIME) UTC. \n Thanks & Regards, \n" +
            "M. Wajahat Qureshi\n AM(LSCS-K)";



/*
    BroadcastReceiver br = new MyBroadcastReceiver();
    public class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.celesnotifier.Query".equals(intent.getAction())) {
                //get notification sw status first
                String receivedText = intent.getStringExtra("notification_switch");
                Log.e(TAG,"In fetcher.service " +
                        "broadcast receiver Got sms_notif_status = "+receivedText);
                if(receivedText!=null)
                sms_notif_status = Boolean.parseBoolean(receivedText);
                activity_is_alive = true;
                String receivedText2 = intent.getStringExtra("dead");
                if(receivedText2 != null)
                activity_is_alive = false;
                Intent intent1 = new Intent();
                intent1.setAction("com.example.celesnotifier.FETCHER_SERVICE");
                intent1.putExtra("status", "true");
                intent1.setPackage("com.example.celesnotifier");
                sendBroadcast(intent1);
            }
        }
    }*/   //BRs commented out for future use


    public String getsmsPriorityPrefVal(){
        String priority = null;
        try {
            Context con = createPackageContext("com.example.celesnotifier", 0);
            SharedPreferences pref = con.getSharedPreferences(
                    getString(R.string.debug_status_prefFileName), Context.MODE_PRIVATE);
            priority = pref.getString(getString(R.string.send_sharedPref_key), null);

        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Not data shared", e.toString());
        }
        return priority;
    }
    public boolean getDebugPrefVal(){
        boolean debug = false;
        try {
            Context con = createPackageContext("com.example.celesnotifier", 0);
            SharedPreferences pref = con.getSharedPreferences(
                    getString(R.string.debug_status_prefFileName), Context.MODE_PRIVATE);
            debug = pref.getBoolean(getString(R.string.debug_status_prefName), false);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Not data shared", e.toString());
        }
        return debug;
    }
    public void setServiceStatus(boolean serviceStatus){
        SharedPreferences my_pref = getSharedPreferences(getString(R.string.svc_status_prefFileName)
                , MODE_PRIVATE);
        SharedPreferences.Editor editor = my_pref.edit();
        editor.putBoolean(getString(R.string.svc_status_prefName), serviceStatus);
        editor.apply();
        Log.i(TAG,"Saved debu_pref val as: "+serviceStatus);
    }
    public static void setLogFileSize(float size, Context context){
        SharedPreferences my_pref = context.getSharedPreferences(context.
                getString(R.string.svc_status_prefFileName)
                , MODE_PRIVATE);
        SharedPreferences.Editor editor = my_pref.edit();
        editor.putFloat(context.getString(R.string.logFileSize_key), size);
        editor.apply();
    }


    public static void printLog(Context context){
        boolean delete_successfull = false;
        String filename = context.getExternalFilesDir(null).getPath() + File.separator + "celesNotifier.log";
        String command = "logcat -d *:e";

        Log.d(TAG, "command: " + command);

        try{
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            try {
                File file = new File(filename);
                float file_size = (file.length()/1024f)/1024f;
                setLogFileSize(file_size, context);
                boolean sizeExceeded = file_size>maxAllowedLogFileSize;
                if(sizeExceeded){ //init force delete
                if(file.delete())
                Log.d(TAG,"File delete success in first attempt...");
                if(file.exists()){
                    Log.d(TAG,"File delete failed in first attempt...");
                    if (!file.getCanonicalFile().delete())
                        Log.d(TAG,"File delete failed in second attempt...");
                    if(file.exists()){
                        context.getApplicationContext().deleteFile(file.getName());
                        if(!file.exists())
                        Log.d(TAG,"File deleted successfully in third attempt...");
                    }
                }
                delete_successfull = !file.exists();
                }
                else {
                    FileWriter writer;
                    if (!file.createNewFile())//open file in append mode if it already exists
                        writer = new FileWriter(file,true);
                    else {
                        writer = new FileWriter(file);
                        Log.e(TAG, "Appending old Log file...");
                    }
                    while ((line = in.readLine()) != null) {
                        writer.write(line + "\n" + "\n");
                    }
                    writer.flush();
                    writer.close();
                    Log.i(TAG, "Log update to file done");

                }
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        if(delete_successfull)
            Log.e(TAG,"Old log file size exceeded "
                    +maxAllowedLogFileSize+"MBs, Hence deleted...");
    }

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
    private Date getLatestTime(){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(This);
        return new Date(pref.getLong(This.getString(R.string.lastUpdated_time), 0));
    }
    private boolean updateTimeAndCheck4Change(Date thisDate, Context This){
        //compare thisTime with oldTime if oldTime!=0 then return boolean
        // showing that this query is unique w.r.t time
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(This);
        Date prev_date = new Date(pref.getLong(This.getString(R.string.lastUpdated_time), 0));
        Log.e(TAG,"Last update time: "+ format_4DISP.format(prev_date));
        Log.e(TAG,"This query time: "+ format_4DISP.format(thisDate));
        if (thisDate.after(prev_date))
        {
            SharedPreferences.Editor editor = pref.edit();
            editor.putLong(This.getString(R.string.lastUpdated_time), thisDate.getTime());
            editor.apply();
            return true;
        }
        else
            return false;
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(getDebugPrefVal())
            delay_bw_queries = 20000;
        hm = new HashMap<>();
        new Timer().schedule(timerTask = new TimerTask() {
            @Override
            public void run() {
                /*
                Please Note: timerTask runnable is very unpredictable, it stops if certain types
                of operations are performed with-in this block; like reading/writing to a file!
                 */
                Log.d(TAG,"Running in the Thread " +
                        Thread.currentThread().getId());
                Document doc = jsoupConnector();
                if (doc != null) {
                    Elements links = doc.select("tr:not(.shade)");
                    links = links.select("tr:not(.header)");
                    Log.i(TAG, "Parsed html length = " + (links.indexOf(links.last()) + 1));
                    /*
                    for (Element link : links) { //get print of all data
                        Log.i(TAG, "Parsed html:" + link.text());
                    }*/
                    Element time_element = links.get(2); //get time element
                    //Log.i(TAG, "Time element: " + time_element.text());
                    Elements tds = time_element.getElementsByTag("td");
                    for (Element td : tds) {
                        updateTime = td.text().substring(td.text()
                                .indexOf(String.valueOf(Calendar.getInstance().
                                        get(Calendar.YEAR))), td.text().indexOf("UTC") - 1);
                        try {
                            Log.i(TAG,"Current Query Date String ="+updateTime);
                            date = format_4PARSNG.parse(updateTime);
                            if (date != null) {
                                timeHasChanged = updateTimeAndCheck4Change(date, This);
                                break;
                            }
                            break;
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                    }

                    Log.e(TAG,"timehaschanged and debug: "+timeHasChanged+" "+getDebugPrefVal());
                    if(timeHasChanged || getDebugPrefVal()){
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
                            else { //noinspection ConstantConditions
                                if ((Float) hm.get(getResources().getString(R.string.fetched_max_prob_k)) <= yellow_min_range
                                        && (Float) hm.get(getString(R.string.fetched_max_prob_k)) >= yello_warn_thresh)
                                    warn_type = "YELLOW";
                                else warn_type = "No";
                            }
                            msg = msg.replace( "(TYPE)", warn_type.equals("No") ? "that there is no" :
                                    warn_type).replace("(DEB)",
                                    String.valueOf(hm.get(getString(R.string.fetched_deb_name_k))))
                                    .replace("(PROB)",
                                            String.valueOf(hm.get(getString(R.string.fetched_max_prob_k))))
                                    .replace("(RNG)",
                                            String.valueOf(hm.get(getString(R.string.fetched_min_range_k))))
                                    .replace("(UPDATE_TIME)", format_4DISP.format(getLatestTime()))
                                    .replace("(IM_TIME)",
                                            String.valueOf(hm.get(getString(R.string.fetched_min_range_time_k))));

                            send_sms2all = getsmsPriorityPrefVal().equals(getString(R.string.send_all_key));
                            Log.e(TAG,"Send SMS action value retrieved as: "
                                    + (send_sms2all? "send to all" : "send to me only"));
                            Log.e(TAG, msg);
                            if (timeHasChanged && (!warn_type.equals("No") || getDebugPrefVal()))
                            {
                                Thread thread = new Thread(new SendSMS_Thread());
                                thread.start();
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
                    else if(!timeHasChanged){ //if this query time is same as last_updated
                        Log.i(TAG,"This query time is same as last updated_time..." +
                                "\n Retrying in "+delay_bw_queries+" mSec...");
                    }
                }
                else
                    Log.e(TAG, "Can't Connect... Please check internet " +
                            "Retrying after "+ delay_bw_queries+"....");
            }
        }, delay_bf_first_exec,delay_bw_queries);

        new Timer().schedule(flushLog_timerTask = new TimerTask() {
            @Override
            public void run() {
                printLog(This);
            }
        }, delay_bf_first_exec,TimeUnit.HOURS.toMillis(logIntervals_inHours));

        return START_STICKY;
    }


    @Override
    public void onCreate() {
        //read debug flag value from shared pref
        numbrs.add("+923342576758"); //my number
        numbrs.add("+923342576758"); //my number
        //IntentFilter filter = new IntentFilter("com.example.celesnotifier.Query");
        //this.registerReceiver(br, filter);
        //sendBroadcast("true");
        Log.e(TAG, "OnStartCommand()");
        Log.i(TAG,"Yellow & Red Warning prob threshold: "+yello_warn_thresh+" "+red_warn_thresh);
        Log.i(TAG,"Yellow & Red Warning range threshold: "+yellow_min_range+" "+red_min_range);
        This = this.getApplicationContext();
        setServiceStatus(true);
        format_4DISP = new SimpleDateFormat(This.getString(R.string.dateNTimeSMS_Format), Locale.US);
        format_4DISP.setTimeZone(TimeZone.getTimeZone("UTC"));
        format_4PARSNG = new SimpleDateFormat(This.getString(R.string.dateNTimePSRSE_Format), Locale.US);
        format_4PARSNG.setTimeZone(TimeZone.getTimeZone("UTC"));
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if(!flushLog_timerTask.cancel())
            Log.e(TAG,"ERROR! couldn't cancel flush_Log timer task..");
        if(!timerTask.cancel())
            Log.e(TAG,"ERROR! couldn't cancel main timer task..");
        //unregisterReceiver(br);
        //sendBroadcast("false");
        Log.e(TAG, "Service-OnDestroy()");
        setServiceStatus(false);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

   /* private void sendBroadcast(String status){ //status = "true" when service
        // is started and vice versa
        Intent intent1 = new Intent();
        intent1.setAction("com.example.celesnotifier.FETCHER_SERVICE");
        intent1.putExtra("status", status);
        intent1.setPackage("com.example.celesnotifier");
        sendBroadcast(intent1);
    }
*/
    public static class SendSMS_Thread implements Runnable {
        public void run() {

            SmsManager smsManager = SmsManager.getDefault();
            if (send_sms2all) {
                for (String number : numbrs) {
                    Log.e(TAG, "Sending sms...");
                    smsManager.sendMultipartTextMessage(number, null,
                            smsManager.divideMessage(msg), null, null);
                }
            }else {
                smsManager.sendMultipartTextMessage("+923342576758",
                        null, smsManager.divideMessage(msg), null,
                        null);
                Log.e(TAG, "Sending sms...");
            }
        }


}


}


