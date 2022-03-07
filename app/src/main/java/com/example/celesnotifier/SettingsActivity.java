package com.example.celesnotifier;

import static com.example.celesnotifier.R.string.service_status_key;

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
        sendQueryBroadcast();
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

    Intent svc_intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SmsManager smsManager = SmsManager.getDefault();
        Log.e(TAG,"sending SMS");
        smsManager.sendTextMessage("+923342576758", null, "A short message!", null,null);
        sendQueryBroadcast();
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
    public void onBackPressed() {
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

        sendQueryBroadcast();
        super.onLowMemory();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
    sendQueryBroadcast();
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {

        sendQueryBroadcast();
        super.onSaveInstanceState(outState, outPersistentState);
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
            sms_notif_pref.setOnPreferenceChangeListener((preference, newValue) -> {
                sms_notif_status = (boolean) newValue;
                return true;
            });
            SharedPreferences sms_notif_sharedPref = sms_notif_pref.getSharedPreferences();
            setSMSNotifStatus(sms_notif_sharedPref.getBoolean(sms_notif_pref.getKey(), false));
            Intent intent1 = new Intent(); //query fetcher service for its active status
            intent1.setAction("com.example.celesnotifier.Query");
            intent1.putExtra("notification_switch", Boolean.toString(getSMSNotifStatus()));
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
                    //SmsManager smsManager = SmsManager.getDefault();
                    //msManager.sendTextMessage("+923342576758", null, "Success!", null, null);

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
            Intent intent1 = new Intent(); //query fetcher service for its active status
            intent1.setAction("com.example.celesnotifier.Query");
            intent1.putExtra("notification_switch", Boolean.toString(sms_notif_status));
            intent1.setPackage("com.example.celesnotifier");
            context1.sendBroadcast(intent1);
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