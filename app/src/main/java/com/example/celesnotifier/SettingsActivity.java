package com.example.celesnotifier;

import static com.example.celesnotifier.R.string.service_status_key;
import static com.example.celesnotifier.R.string.sms_notif_key;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.Printer;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

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

    public static void setDebugPrefVal(Context appContxt , boolean val){
        SharedPreferences my_pref = appContxt.getSharedPreferences(appContxt.getString(R.string.debug_status_prefFileName)
            , MODE_PRIVATE);
        SharedPreferences.Editor editor = my_pref.edit();
        editor.putBoolean(appContxt.getString(R.string.debug_status_prefName), val);
        editor.apply();
        Log.i(TAG,"Saved debu_pref val as: "+val);
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
        //sendQueryBroadcast();
    }

    @Override
    public void onBackPressed() {
        //sendQueryBroadcast();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
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

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference svc_sw_pref = findPreference(getString(service_status_key));
            assert svc_sw_pref != null;
            svc_sw_cntxt = svc_sw_pref.getContext();
            svc_sw_pref.callChangeListener(getServiceStatus(svc_sw_cntxt)); //set current status
            svc_sw_cntxt = svc_sw_pref.getContext().getApplicationContext();
            svc_intent = new Intent(svc_sw_cntxt,FetcherService.class);
            Preference sms_notif_pref = findPreference(getString(R.string.sms_notif_key));
            assert sms_notif_pref != null;
            sms_notif_pref.setOnPreferenceChangeListener((preference, newValue) -> {
                setDebugPrefVal(svc_sw_cntxt, (boolean) newValue);
                return true;
            });
            svc_sw_pref.setOnPreferenceChangeListener((preference, newValue) -> {
                if((boolean) newValue) {
                    svc_sw_cntxt.startService(svc_intent);
                    Log.i(TAG, "Service Start Button Pressed...");
                }
                else{
                    svc_sw_cntxt.stopService(svc_intent);
                    Log.i(TAG, "Service Stop Button Pressed...");
                }

                return true;
            });
            SharedPreferences sms_notif_sharedPref = sms_notif_pref.getSharedPreferences();
            boolean val = sms_notif_sharedPref.getBoolean(getString(sms_notif_key), false);
            setDebugPrefVal(svc_sw_cntxt,val
                    );
        }



        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            Log.e(TAG," Selected Preference from Tree  >> "+ preference.getKey());
            return super.onPreferenceTreeClick(preference);
        }

        @Override
        public boolean onOptionsItemSelected(@NonNull MenuItem item) {
            Log.e("onOptionsItemSelected", item.toString());
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onPrimaryNavigationFragmentChanged(boolean isPrimaryNavigationFragment) {

            Log.e("onPrimaryNavigationFragmentChanged", String.valueOf(isPrimaryNavigationFragment));
            super.onPrimaryNavigationFragmentChanged(isPrimaryNavigationFragment);
        }

        @Override
        public boolean onContextItemSelected(@NonNull MenuItem item) {
            Log.e("onContextItemSelected", item.toString());
            return super.onContextItemSelected(item);
        }
    }


}