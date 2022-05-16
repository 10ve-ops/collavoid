package com.example.celesnotifier;

import static com.example.celesnotifier.App.CHANNEL_ID;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
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
    public Context This;
    private TimerTask timerTask, flushLog_timerTask;
    private static Date date;
    private static boolean timeHasChanged = false;
    public static final long logIntervals_inHours = 3;
    private final static float maxAllowedLogFileSize = 1024f; //1GB
    private static boolean send_sms2all = false;
    private static final long delay_bf_first_exec = 0;
    static Map<String, ? super Object > results;
    private static final int delay_bw_queries_inMinutes = 40, mNotifChannel_ID = 43;
    public static final float yello_warn_thresh = 3.0e-5f, red_warn_thresh = 1.0e-4f,
            yellow_min_range = 2.0f, red_min_range = 0.5f;
    static SimpleDateFormat format_4DISP, format_4PARSNG;
    public static String msg = "*IMPORTANT MESSAGE*\n As of current celestrak data " +
            "(updated @ (UPDATE_TIME)), this is to inform " +
            "(TYPE) warning of possible collision of (obj1) with (obj2). Having probability = (PROB)" +
            ", minimum range = (RNG) km and impact time = (IM_TIME) UTC. \n Thanks & Regards, \n" +
            "M. Wajahat Qureshi\n AM(LSCS-K)";

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
        Log.i(TAG,"setServiceStatus(): "+serviceStatus);
    }
    public static void setLogFileSize(float size, Context context){
        SharedPreferences my_pref = context.getSharedPreferences(context.
                getString(R.string.svc_status_prefFileName)
                , MODE_PRIVATE);
        SharedPreferences.Editor editor = my_pref.edit();
        editor.putFloat(context.getString(R.string.logFileSize_key), size);
        editor.apply();
    }
    public static void setLatRes(String msg, Context context){
        SharedPreferences my_pref = context.getSharedPreferences(context.
                        getString(R.string.latest_result_key)
                , MODE_PRIVATE);
        SharedPreferences.Editor editor = my_pref.edit();
        editor.putString(context.getString(R.string.latest_result_key), msg);
        editor.apply();
    }
    private Notification.Builder mBuilder;


    public static void printLog(Context context){
        boolean delete_successfull = false;
        String filename = context.getExternalFilesDir(null).getPath() + File.separator + "celesNotifier.log";
        String command = "logcat -d *:V";

        Log.d(TAG, "command: " + command);

        try{
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
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

    private static boolean check4Change(Date thisDate, Context This){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(This);
        Date prev_date = new Date(pref.getLong(This.getString(R.string.lastUpdated_time), 0));
        Log.e(TAG,"Last update time: "+ format_4DISP.format(prev_date));
        Log.e(TAG,"This query time: "+ format_4DISP.format(thisDate));
        return thisDate.after(prev_date);
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        mBuilder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("CelesNotifier")
                .setContentText("Logging results in every "+ delay_bw_queries_inMinutes + " minutes")
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentIntent(pendingIntent);
        Notification notification = mBuilder.build();

        startForeground(mNotifChannel_ID, notification);

        //do heavy work on a background thread
        //stopSelf();
        results = new HashMap<>();
        new Timer().schedule(timerTask = new TimerTask() {
            @Override
            public void run() {
                String warn_type;
                /*
                Please Note: timerTask runnable is very unpredictable, it stops if certain types
                of operations are performed with-in this block; like reading/writing to a file!
                 */
                Log.d(TAG,"Running in the Thread " +Thread.currentThread().getId());
                Document doc = jsoupConnector();
                if (doc != null) {
                    Elements links = doc.select("tr:not(.shade)");
                    links = links.select("tr:not(.header)");
                    Log.i(TAG, "Parsed html length = " + (links.indexOf(links.last()) + 1));
                    /*
                    for (Element link : links) { //get print of all data
                        Log.i(TAG, "Parsed html:" + link.text());
                    }*/

                    Elements time_element_paras = links.select("p"); //get time element paras
                    String time_el_txt = time_element_paras.get(2).text();
                    Log.i(TAG, "Time element: " + time_el_txt);
                        updateTime = time_el_txt.substring(time_el_txt
                                .indexOf(String.valueOf(Calendar.getInstance().
                                        get(Calendar.YEAR))), time_el_txt.indexOf("UTC")-1);
                        try {
                            Log.i(TAG,"Current Query Date String ="+updateTime);
                            date = format_4PARSNG.parse(updateTime);
                            //GMT+05 is not a property of Date obj.
                            //Log.e(TAG,"Parsed Date From Time Element:"+date);
                            //This print statement adds it

                            if (date != null) {
                                timeHasChanged = check4Change(date, This);
                                Log.e(TAG,"Parsed Date From Time Element:"+
                                        format_4DISP.format(date));
                            } else{
                                Log.e(TAG, "FATAL! Parse date was null! Could't parse said date..");
                            }
                        } catch (ParseException e) {
                            Log.e(TAG,"FATAL! updateTime parsing failed");
                            e.printStackTrace();
                        }

                    Log.e(TAG,"timehaschanged:  "+ timeHasChanged+" \n debug mode: "+ getDebugPrefVal());
                    if(timeHasChanged) {
                        Element el_1 = links.get(3); //get first row of results table
                        Element el_2 = links.get(4); //get second row of results table
                        Elements el1tds = el_1.getElementsByTag("td");
                        Elements el2tds = el_2.getElementsByTag("td");
                        results.put(getString(R.string.obj1_norad), Float.valueOf(el1tds.get(1).text()));
                        results.put(getString(R.string.obj1_name), el1tds.get(2).text());
                        results.put(getString(R.string.max_prob), Float.valueOf(el1tds.get(4).text()));
                        results.put(getString(R.string.min_range), Float.valueOf(el1tds.get(6).text()));
                        results.put(getString(R.string.rel_vel), Float.valueOf(el1tds.get(7).text()));
                        results.put(getString(R.string.obj2_norad), Float.valueOf(el2tds.get(0).text()));
                        results.put(getString(R.string.obj2_name), el2tds.get(1).text());
                        results.put(getString(R.string.TCA), el2tds.get(5).text());
                            //noinspection ConstantConditions
                            if ((Float) results.get(getString(R.string.min_range)) <=
                                    red_min_range && (Float) results.get(getResources().getString(R.string.max_prob)) >= red_warn_thresh)
                                warn_type = "RED";
                            else //noinspection ConstantConditions
                                if ((Float) results.get(getResources().getString(R.string.max_prob)) <= yellow_min_range
                                        && (Float) results.get(getString(R.string.max_prob)) >= yello_warn_thresh)
                                    warn_type = "YELLOW";
                                else warn_type = "No";
                            msg = msg.replace("(TYPE)", warn_type.equals("No") ? "that there is no" :
                                    warn_type).replace("(obj2)",
                                    String.valueOf(results.get(getString(R.string.obj1_name))))
                                    .replace("(PROB)",
                                            String.valueOf(results.get(getString(R.string.max_prob))))
                                    .replace("(RNG)",
                                            String.valueOf(results.get(getString(R.string.min_range))))
                                    .replace("(UPDATE_TIME)", format_4DISP.format(date))
                                    .replace("(IM_TIME)",
                                            String.valueOf(results.get(getString(R.string.TCA))))
                            .replace("(obj1)", String.valueOf(results.get(getString(R.string.obj2_name))));

                            Log.e(TAG, msg);
                            setLatRes(msg, This);
                            boolean debug_mode = getDebugPrefVal(),
                                    valid_warning = !warn_type.equals("No");
                            String smsPrefVal = getsmsPriorityPrefVal();

                            if(!smsPrefVal.equals(getString(R.string.dont_send_key))){
                            if (valid_warning || debug_mode)
                            {
                                send_sms2all = smsPrefVal.
                                        equals(getString(R.string.send_all_key));
                                Log.e(TAG,"Send SMS action value retrieved as: "
                                        + (send_sms2all? "send to all" : "send to me only"));
                                Thread sms_thread;
                                sms_thread = new Thread(new SendSMS_Thread(valid_warning
                                        && send_sms2all));
                                sms_thread.start();
                            }}
                        Log.i(TAG,"Printing all mapping entires.....");
                        for (Map.Entry<String, ? super Object> me :
                                results.entrySet()) {
                            System.out.print(me.getKey() + ":");
                            System.out.println(me.getValue());
                        }
                    }
                    else { //if this query time is same as last_updated
                        Log.i(TAG,"This query time is same as last updated_time..." +
                                "\n Retrying in "+ delay_bw_queries_inMinutes
                                +" minutes...");
                    }
                }
                else
                    Log.e(TAG, "Can't Connect... Please check internet " +
                            "\n Retrying in "+ delay_bw_queries_inMinutes
                            +" minutes...");

                long time = Calendar.getInstance().getTimeInMillis()
                        + (delay_bw_queries_inMinutes * 60 * 1000);
                mBuilder.setContentText("Next query at "+
                        new SimpleDateFormat(getString(R.string.timeDisp_Format),
                        Locale.getDefault()).format(time));
                getSystemService(NotificationManager.class).notify(mNotifChannel_ID,
                        mBuilder.build());
            }
        }, delay_bf_first_exec,TimeUnit.MINUTES.toMillis(delay_bw_queries_inMinutes));

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

    public void dispToastFrmSvc(String msg){
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(FetcherService.this.getApplicationContext()
                        ,msg,Toast.LENGTH_LONG).show());
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

    public  class SendSMS_Thread implements Runnable {
        boolean send_to_all;
       final ArrayList<String> all_numbrs = new ArrayList<>();
        public SendSMS_Thread(boolean send_to_all_val){
            all_numbrs.add("+923342576758"); //my number
            all_numbrs.add("+923342576758"); //my number
            send_to_all = send_to_all_val;
        }
        public void run() {
            SmsManager smsManager = SmsManager.getDefault();
            if (send_to_all) {
                for (String number : all_numbrs) {
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
            //edit changes after sms is sent so as to avoid storing times when sms fault occurred
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(This);
            SharedPreferences.Editor editor = pref.edit();
            editor.putLong(This.getString(R.string.lastUpdated_time), date.getTime());
            editor.apply();
        }



}


}


