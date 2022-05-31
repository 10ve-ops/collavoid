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
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class FetcherService extends Service {
    public static String TAG = "Fetcher Service";
    public Context This;
    private TimerTask timerTask, flushLog_timerTask;
    private static boolean sms_send_Enabled;
    public static final long logIntervals_inHours = 3;
    private final static float maxAllowedLogFileSize = 1024f; //1GB
    private static boolean send_sms2all = false;
    private static final long delay_bf_first_exec = 0;
    private static final int delay_bw_queries_inMinutes = 40, mNotifChannel_ID = 43;
    private Notification.Builder mBuilder;
    private Ringtone alertTone;
    private Uri userRingtoneUri;



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
    private void updteNotifTime(){
        long time = Calendar.getInstance().getTimeInMillis()
                + (delay_bw_queries_inMinutes * 60 * 1000);
        mBuilder.setContentText("Next query at "+
                new SimpleDateFormat(getString(R.string.timeDisp_Format),
                        Locale.getDefault()).format(time));
        getSystemService(NotificationManager.class).notify(mNotifChannel_ID,
                mBuilder.build());
    }
    void setRingtonePref(String ringtone_URI){
        SharedPreferences ringtone_sp =
                getSharedPreferences
                        (this.getPackageName(),
                                Context.MODE_PRIVATE);
        ringtone_sp.edit().putString(getString(R.string.ringtonePref_key), ringtone_URI).apply();
        Log.e(TAG,"setting ringtone uri to: "+ringtone_URI+" from service");
    }
    public  String getRingtone_URI() {
            String key  = getString(R.string.ringtonePref_key);
            String result = getSharedPreferences(
                    this.getPackageName() , Context.MODE_PRIVATE).getString(key,null);
            Log.e(TAG,"Got ringtone uri as: "+result+ " from service");
            if(result!=null)
                return result;
            else{
                //get default ringtone
                String def= RingtoneManager.getValidRingtoneUri(this.getApplicationContext()).toString();
                setRingtonePref(def);
                return def;
        }
    }
    private void playRingtone() throws PackageManager.NameNotFoundException {
        userRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        //user_ringtone = RingtoneManager.getRingtone(This, userRingtoneUri);
        String ringtoneUri = getRingtone_URI();
        RingtoneManager.setActualDefaultRingtoneUri(
                This,
                RingtoneManager.TYPE_RINGTONE,
                ringtoneUri!=null?Uri.parse(ringtoneUri):userRingtoneUri);
        Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        alertTone = RingtoneManager.getRingtone(This.getApplicationContext(), alarm);
        alertTone.play();
    }
    private void stopRinging(){
        if(alertTone!=null)
        alertTone.stop();
        RingtoneManager.setActualDefaultRingtoneUri(
                This,
                RingtoneManager.TYPE_RINGTONE,
                userRingtoneUri);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE |
                        PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("CelesNotifier")
                .setContentText("Logging results in every "+ delay_bw_queries_inMinutes + " minutes")
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentIntent(pendingIntent);
        Notification notification = mBuilder.build();

        startForeground(mNotifChannel_ID, notification);
        new Timer().schedule(timerTask = new TimerTask() {
            @Override
            public void run() {
                Parser parser = new Parser(This);
                parser.init();
                boolean debug_mode = getDebugPrefVal(),
                                    valid_warning = parser.warning != Parser.NO_WARN;
               if(valid_warning) {
                   Log.e(TAG, "valid warning is executing");
                   try {
                       playRingtone();
                   } catch (PackageManager.NameNotFoundException e) {
                       e.printStackTrace();
                   }
               }
                String smsPrefVal = getsmsPriorityPrefVal();
                sms_send_Enabled = !smsPrefVal.equals(getString(R.string.dont_send_key));
                if(sms_send_Enabled){
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
                            /*else display notif or ring an alarm with alarm stop button*/

                updteNotifTime();

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
        This = this.getApplicationContext();
        setServiceStatus(true);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if(!flushLog_timerTask.cancel())
            Log.e(TAG,"ERROR! couldn't cancel flush_Log timer task..");
        if(!timerTask.cancel())
            Log.e(TAG,"ERROR! couldn't cancel main timer task..");
        Log.e(TAG, "Service-OnDestroy()");
        setServiceStatus(false);
        stopRinging();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



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
            String msg = SettingsActivity.getLatRes(This);
            if (send_to_all) {
                for (String number : all_numbrs) {
                    Log.e(TAG, "Sending sms...");
                    smsManager.sendMultipartTextMessage(number, null,
                            smsManager.divideMessage(msg),
                            null, null);
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


