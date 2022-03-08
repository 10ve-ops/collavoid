package com.example.celesnotifier;

import static com.example.celesnotifier.R.string.service_status_key;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {
    public static String TAG = "CelesNotifier";
    Context This;
    private static boolean sms_notif_status;
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

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        sendQueryBroadcast();
        super.onRestart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        sendQueryBroadcast();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        service_status = isMyServiceRunning(FetcherService.class);
        this.registerReceiver(br, new IntentFilter("com.example.celesnotifier.FETCHER_SERVICE"));
        This = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        sendQueryBroadcast();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        sendQueryBroadcast();
    }

    @Override
    public void onBackPressed() {
        sendQueryBroadcast();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(br);
        Intent intent1 = new Intent(); //query fetcher service for its active status
        intent1.setAction("com.example.celesnotifier.Query");
        intent1.putExtra("dead", Boolean.toString(true));
        intent1.setPackage("com.example.celesnotifier");
        sendBroadcast(intent1);
        //svc_intent = new Intent(this, FetcherService.class);
        //if (service_status)
        //stopService(svc_intent);
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        Log.e(TAG,"onLowMemory()");
        sendQueryBroadcast();
        super.onLowMemory();
    }

    @Override
    protected void onPause() {
        sendQueryBroadcast();
        Log.e(TAG,"onPause()");
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {

        sendQueryBroadcast();
        super.onSaveInstanceState(outState, outPersistentState);
    }


    public static class SettingsFragment extends PreferenceFragmentCompat {

        Context svc_sw_cntxt, sms_notif_cntxt;
        Intent svc_intent;

        private void toggleBoolPref(Preference pref){

        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            Intent queryIntent = new Intent(); //query fetcher service for its active status
            queryIntent.setAction("com.example.celesnotifier.Query");
            queryIntent.putExtra("notification_switch", Boolean.toString(sms_notif_status));
            queryIntent.setPackage("com.example.celesnotifier");
            Log.e(TAG,queryIntent.toString());


            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference svc_sw_pref = findPreference(getString(service_status_key));
            assert svc_sw_pref != null;
            svc_sw_cntxt = svc_sw_pref.getContext().getApplicationContext();
            svc_intent = new Intent(svc_sw_cntxt,FetcherService.class);

            Preference sms_notif_pref = findPreference(getString(R.string.sms_notif_key));
            assert sms_notif_pref != null;
            sms_notif_pref.setOnPreferenceChangeListener((preference, newValue) -> {
                Log.e(TAG,"debug button val change obsrved = "+ (boolean) newValue);
                sms_notif_status = (boolean) newValue;
                return true;
            });
            sms_notif_cntxt = sms_notif_pref.getContext().getApplicationContext();
            SharedPreferences sms_notif_sharedPref = sms_notif_pref.getSharedPreferences();
            sms_notif_status = sms_notif_sharedPref.getBoolean(sms_notif_pref.getKey(), false);
            boolean current_val = sms_notif_status;
            if(current_val){ //toggle the debug button for magic
                sms_notif_pref.callChangeListener(!current_val);
                sms_notif_pref.callChangeListener(current_val);
            }
            svc_sw_cntxt.sendBroadcast(queryIntent);

            svc_sw_pref.callChangeListener(service_status);

            svc_sw_pref.setOnPreferenceChangeListener((preference, newValue) -> {
                if((boolean) newValue) {
                    svc_sw_cntxt.startService(svc_intent);
                    Log.i(TAG, "Service Start Button Pressed...");
                    boolean current_val2 = sms_notif_status;
                    if(current_val2){ //toggle the debug button for magic
                        sms_notif_pref.callChangeListener(!current_val2);
                        sms_notif_pref.callChangeListener(current_val2);
                    }
                    //save value of debug flag in sharedpref
                }
                else{
                    svc_sw_cntxt.stopService(svc_intent);
                    Log.i(TAG, "Service Stop Button Pressed...");
                }

                return true;
            });
        }



        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            Intent intent1 = new Intent(); //query fetcher service for its active status
            intent1.setAction("com.example.celesnotifier.Query");
            intent1.putExtra("notification_switch", Boolean.toString(sms_notif_status));
            intent1.setPackage("com.example.celesnotifier");
            preference.getContext().getApplicationContext().sendBroadcast(intent1);

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