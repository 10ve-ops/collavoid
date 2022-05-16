package com.example.celesnotifier;

import static com.example.celesnotifier.R.string.logFileSize_key;
import static com.example.celesnotifier.R.string.service_status_key;
import static com.example.celesnotifier.R.string.sms_notif_key;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import java.text.DecimalFormat;

public class SettingsActivity extends AppCompatActivity {
    public static String TAG = "CelesNotifier";
    Context This;
    //private static boolean sms_notif_status;
    /* //for future use
    public static boolean service_status = false;
    BroadcastReceiver br = new MyBroadcastReceiver();
    public static class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.celesnotifier.FETCHER_SERVICE".equals(intent.getAction())) {
                String receivedText = intent.getStringExtra("status");
                if(receivedText!=null)
                service_status = Boolean.parseBoolean(receivedText);
            }
        }
    }

    protected void sendQueryBroadcast(){
        Intent intent1 = new Intent(); //query fetcher service for its active status
        intent1.setAction("com.example.celesnotifier.Query");
        intent1.putExtra("notification_switch", Boolean.toString(sms_notif_status));
        intent1.setPackage("com.example.celesnotifier");
        sendBroadcast(intent1);
    }
*/

    /*
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
        */

    public static void set_Debug_sendAction_pref(Context appContxt , Boolean val, String send_sms_val){
        SharedPreferences my_pref = appContxt.getSharedPreferences(appContxt.getString(R.string.debug_status_prefFileName)
            , MODE_PRIVATE);
        SharedPreferences.Editor editor = my_pref.edit();
        if(val!=null)
        editor.putBoolean(appContxt.getString(R.string.debug_status_prefName), val);
        else if(send_sms_val!=null)
            editor.putString(appContxt.getString(R.string.send_sharedPref_key), send_sms_val);
        editor.apply();
        //Log.i(TAG,"Debug status & send_sms priority values = "+val + "  " +send_sms_val);
    }
    public static boolean getServiceStatus(Context cntxt){
        boolean debug = false;
        try {
            Context con = cntxt.createPackageContext("com.example.celesnotifier", 0);
            SharedPreferences pref = con.getSharedPreferences(
                    cntxt.getString(R.string.svc_status_prefFileName), Context.MODE_PRIVATE);
            debug = pref.getBoolean(cntxt.getString(R.string.svc_status_prefName), false);

        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Not data shared", e.toString());
        }
        return debug;
    }
    public static float getLogFileSize(Context cntxt){
        float size = 0f;
        try {
            Context con = cntxt.createPackageContext("com.example.celesnotifier", 0);
            SharedPreferences pref = con.getSharedPreferences(
                    cntxt.getString(R.string.svc_status_prefFileName), Context.MODE_PRIVATE);
            size = pref.getFloat(cntxt.getString(R.string.logFileSize_key), 0f);

        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Not data shared", e.toString());
        }
        return size;
    }
    public static String getLatRes(Context cntxt){
        String msg = " ";
        try {
            Context con = cntxt.createPackageContext("com.example.celesnotifier", 0);
            SharedPreferences pref = con.getSharedPreferences(
                    cntxt.getString(R.string.latest_result_key), Context.MODE_PRIVATE);
            msg = pref.getString(cntxt.getString(R.string.latest_result_key), " ");

        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Not data shared", e.toString());
        }
        return msg;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
       // this.registerReceiver(br, new IntentFilter("com.example.celesnotifier.FETCHER_SERVICE"));
        This = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        //sendQueryBroadcast();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        if(checkSelfPermission(Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_DENIED )
            ActivityCompat.requestPermissions(SettingsActivity.this,
                    new String[] { Manifest.permission.SEND_SMS }, 0);
        //sendQueryBroadcast();
    }

    @Override
    public void onBackPressed() {
        //sendQueryBroadcast();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG,"onDestroy()");
        /*
        //this.unregisterReceiver(br);
        Intent intent1 = new Intent(); //query fetcher service for its active status
        intent1.setAction("com.example.celesnotifier.Query");
        intent1.putExtra("dead", Boolean.toString(true));
        intent1.setPackage("com.example.celesnotifier");
        sendBroadcast(intent1);
        //svc_intent = new Intent(this, FetcherService.class);
        //if (service_status)
        //stopService(svc_intent);

        */
        super.onDestroy();
            }

    @Override
    public void onLowMemory() {
        Log.e(TAG,"onLowMemory()");
        //sendQueryBroadcast();
        super.onLowMemory();
    }

    @Override
    protected void onPause() {
        //sendQueryBroadcast();
        Log.e(TAG,"onPause()");
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {

        //sendQueryBroadcast();
        super.onSaveInstanceState(outState, outPersistentState);
    }


    public static class SettingsFragment extends PreferenceFragmentCompat {

        Context svc_sw_cntxt;
        Intent svc_intent;
        SwitchPreference svc_sw_pref;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference latResPref = findPreference(getString(R.string.latest_result_key));
            assert latResPref != null;
            latResPref.setSummary(getLatRes(latResPref.getContext()));
            ListPreference sms_send_pref = findPreference(getString(R.string.send_key));
            Preference signature = findPreference(getString(R.string.signature_key));
            svc_sw_pref = findPreference(getString(service_status_key));
            Preference logFileSize_pref = findPreference(getString(logFileSize_key));
            assert logFileSize_pref != null;
            logFileSize_pref.setSummary(new DecimalFormat("0000.00").format(getLogFileSize
                    (logFileSize_pref.getContext()))+ "MB"+"    ("+getLogFileSize
                    (logFileSize_pref.getContext())/1024f+" GB)");
            assert svc_sw_pref != null;
            svc_sw_cntxt = svc_sw_pref.getContext();
            svc_sw_pref.callChangeListener(getServiceStatus(svc_sw_cntxt)); //set current status
            svc_sw_cntxt = svc_sw_pref.getContext().getApplicationContext();
            svc_intent = new Intent(svc_sw_cntxt.getApplicationContext(),FetcherService.class);
            Preference sms_notif_pref = findPreference(getString(R.string.sms_notif_key));
            assert sms_notif_pref != null;
            sms_notif_pref.setOnPreferenceChangeListener((preference, newValue) -> {
                set_Debug_sendAction_pref(svc_sw_cntxt, (boolean) newValue, null);
                return true;
            });
            svc_sw_pref.setOnPreferenceChangeListener((preference, newValue) -> {
                if((boolean) newValue) {
                    if(svc_sw_cntxt.checkSelfPermission(Manifest.permission.SEND_SMS) ==
                            PackageManager.PERMISSION_GRANTED )
                    ContextCompat.startForegroundService(svc_sw_cntxt, svc_intent);
                    else{
                        Log.e(TAG,"SMS_Permission denied at start service button");
                    }

                    Log.i(TAG, "Service Start Button Pressed...");
                }
                else{
                    svc_sw_cntxt.stopService(svc_intent);
                    Log.i(TAG, "Service Stop Button Pressed...");
                }

                return true;
            });
            SharedPreferences sms_notif_sharedPref = sms_notif_pref.getSharedPreferences();
            assert sms_notif_sharedPref != null;
            boolean val = sms_notif_sharedPref.getBoolean(getString(sms_notif_key), false);
            set_Debug_sendAction_pref(svc_sw_cntxt,val, null);

            assert sms_send_pref != null;
            set_Debug_sendAction_pref(svc_sw_cntxt,null,sms_send_pref.getValue()); // redundant operation to be
            //replaced with original keys used by this pref

            assert signature != null;
            SharedPreferences sign_sharedPref = signature.getSharedPreferences();
            assert sign_sharedPref != null;
            SharedPreferences.Editor sign_editor = sign_sharedPref.edit();
            if(sign_sharedPref.getString(getString(R.string.signature_key), null) == null
                    || sign_sharedPref.getString(getString(R.string.signature_key), null)
                    .equals("")
            || sign_sharedPref.getString(getString(R.string.signature_key), null).equals(" ")){
                sign_editor.putString(getString(R.string.signature_key), FetcherService.msg);
                //setting default signature
                sign_editor.apply();
                signature.callChangeListener(FetcherService.msg);
            }
            signature.setOnPreferenceChangeListener((preference, newValue) -> {
                //getting user modified signature
                //Log.e(TAG,"sign.changelistener val: "+(String) newValue);
                FetcherService.msg = (String) newValue;
                sign_editor.putString(getString(R.string.signature_key), FetcherService.msg);
                return true;
            });

        }

        @Override
        public void onStart() {
            if(!getServiceStatus(svc_sw_cntxt))
            svc_sw_pref.setChecked(false);
            super.onStart();
        }


        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
           Log.e(TAG," Selected Preference from Tree  >> "+ preference.getKey());
            return super.onPreferenceTreeClick(preference);
        }

        @Override
        public boolean onOptionsItemSelected(@NonNull MenuItem item) {
            //Log.e("onOptionsItemSelected", item.toString());
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onPrimaryNavigationFragmentChanged(boolean isPrimaryNavigationFragment) {

            //Log.e("onPrimaryNavigationFragmentChanged", String.valueOf(isPrimaryNavigationFragment));
            super.onPrimaryNavigationFragmentChanged(isPrimaryNavigationFragment);
        }

        @Override
        public boolean onContextItemSelected(@NonNull MenuItem item) {
            //Log.e("onContextItemSelected", item.toString());
            return super.onContextItemSelected(item);
        }
    }


}