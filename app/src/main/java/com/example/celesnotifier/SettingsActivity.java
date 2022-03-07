package com.example.celesnotifier;

import static com.example.celesnotifier.R.string.service_status_key;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
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
                service_status = Boolean.parseBoolean(receivedText);
                Log.e(TAG,"value of service_status in ...FETCHER_SERVICE broadcast receiver: " + service_status);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    Intent svc_intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        IntentFilter filter = new IntentFilter("com.example.celesnotifier.FETCHER_SERVICE");
        this.registerReceiver(br, filter);
        This = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(br);
        //svc_intent = new Intent(this, FetcherService.class);
        //if (service_status)
        //stopService(svc_intent);
        super.onDestroy();
    }

    public static boolean getSMSNotifStatus(){
            return sms_notif_status;
    }
    public static void setSMSNotifStatus(boolean status){ sms_notif_status = status;}

    public static class SettingsFragment extends PreferenceFragmentCompat {
        Context context1;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference service_onOff_pref = findPreference(getString(service_status_key));
            assert service_onOff_pref != null;
            context1 = service_onOff_pref.getContext();
            Preference sms_notif_pref = findPreference(getString(R.string.sms_notif_key));
            assert sms_notif_pref != null;
            SharedPreferences sms_notif_sharedPref = sms_notif_pref.getSharedPreferences();
            setSMSNotifStatus(sms_notif_sharedPref.getBoolean(sms_notif_pref.getKey(), false));
            Intent intent1 = new Intent(); //query fetcher service for its active status
            intent1.setAction("com.example.celesnotifier.Query");
            intent1.setPackage("com.example.celesnotifier");
            context1.sendBroadcast(intent1);
            Intent svc_intent;
            context1 = service_onOff_pref.getContext().getApplicationContext();
            svc_intent = new Intent(service_onOff_pref.getContext(), FetcherService.class);
            SharedPreferences service_onOff_pref_sharedPref = service_onOff_pref.getSharedPreferences();
            SharedPreferences.Editor editor = service_onOff_pref_sharedPref.edit();
            editor.putBoolean(getString(service_status_key), service_status);
            editor.apply();
            service_onOff_pref.setOnPreferenceChangeListener((preference, newValue) -> {
                if((boolean) newValue) {
                    context1.startService(svc_intent);
                    Log.i(TAG, "Service Start Button Pressed...");
                }
                else{
                    context1.stopService(svc_intent);
                    Log.i(TAG, "Service Stop Button Pressed...");
                }

                return true;
            });
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            Log.e(TAG," Key is >> "+ preference.getKey());
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