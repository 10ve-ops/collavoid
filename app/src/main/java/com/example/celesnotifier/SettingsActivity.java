package com.example.celesnotifier;

import static com.example.celesnotifier.R.string.logFileSize_key;
import static com.example.celesnotifier.R.string.service_status_key;
import static com.example.celesnotifier.R.string.sms_notif_key;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    private static final String TAG = "CelesNotifier";

    //getters
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
            Log.e("No data shared", e.toString());
        }
        return msg;
    }
    public static String getCurrentRngtone(Context context) throws PackageManager.NameNotFoundException {
        //Context con = createPackageContext("com.example.celesnotifier", 0);
        String key  = context.getString(R.string.ringtonePref_key);
        String result = context.getSharedPreferences(
                context.getPackageName() , Context.MODE_PRIVATE).getString(key,null);
        Log.e(TAG,"Got ringtone uri as: "+result);
        if(result!=null)
            return result;
        else{
            //get default ringtone
            return RingtoneManager.getValidRingtoneUri(context).toString();
        }
    }
    //

    //setters
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
    public static void setRingtonePref(Context context, String ringtone_URI){
        SharedPreferences ringtone_sp =
                context.getSharedPreferences
                        (context.getPackageName(),
                                Context.MODE_PRIVATE);
        ringtone_sp.edit().putString(context.getString(R.string.ringtonePref_key), ringtone_URI).apply();
        Log.e(TAG,"setting ringtone uri to: "+ringtone_URI + "from SettingsActivity");
    }
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        if(checkSelfPermission(Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_DENIED )
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.SEND_SMS }, 0);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG,"App destroyed");
        super.onDestroy();
            }

    @Override
    public void onLowMemory() {
        Log.e(TAG,"onLowMemory()");
        super.onLowMemory();
    }

    @Override
    protected void onPause() {
        Log.v(TAG,"App paused");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.v(TAG,"App resumed");
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }


    public static class SettingsFragment extends PreferenceFragmentCompat {

        private Context mSvc_sw_cntxt;
        private Intent mSvc_intent;
        ActivityResultLauncher<Intent> ringtoneSelectActivityLauncher;

        @SuppressWarnings("ConstantConditions")
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference latResPref = findPreference(getString(R.string.latest_result_key));
            ListPreference sms_send_pref = findPreference(getString(R.string.send_key));
            Preference signature = findPreference(getString(R.string.signature_key));
            Preference sms_notif_pref = findPreference(getString(R.string.sms_notif_key));
            SwitchPreference mSvc_sw_pref = findPreference(getString(service_status_key));
            Preference alertPref = findPreference(getString(R.string.ringtonePref_key));
            Preference logFileSize_pref = findPreference(getString(logFileSize_key));
            latResPref.setSummary(getLatRes(latResPref.getContext()));
            logFileSize_pref.setSummary(new DecimalFormat("0000.00").format(getLogFileSize
                    (logFileSize_pref.getContext()))+ "MB"+"    ("+getLogFileSize
                    (logFileSize_pref.getContext())/1024f+" GB)");
            mSvc_sw_cntxt = mSvc_sw_pref.getContext();
            mSvc_sw_pref.callChangeListener(getServiceStatus(mSvc_sw_cntxt)); //set current status
            mSvc_intent = new Intent(mSvc_sw_cntxt.getApplicationContext(),FetcherService.class);
            sms_notif_pref.setOnPreferenceChangeListener((preference, newValue) -> {
                set_Debug_sendAction_pref(mSvc_sw_cntxt, (boolean) newValue, null);
                return true;
            });
            mSvc_sw_pref.setOnPreferenceChangeListener((preference, newValue) -> {
                if((boolean) newValue) {
                    if(mSvc_sw_cntxt.checkSelfPermission(Manifest.permission.SEND_SMS) ==
                            PackageManager.PERMISSION_GRANTED )
                    ContextCompat.startForegroundService(mSvc_sw_cntxt, mSvc_intent);
                    else{
                        mSvc_sw_pref.setEnabled(false);
                    }

                    Log.i(TAG, "Service Start Button Pressed...");
                }
                else{
                    mSvc_sw_cntxt.stopService(mSvc_intent);
                }
                return true;
            });
            SharedPreferences sms_notif_sharedPref = sms_notif_pref.getSharedPreferences();
            boolean val = sms_notif_sharedPref.getBoolean(getString(sms_notif_key), false);
            set_Debug_sendAction_pref(mSvc_sw_cntxt,val, null);
            set_Debug_sendAction_pref(mSvc_sw_cntxt,null,sms_send_pref.getValue()); // redundant operation to be
            //replaced with original keys used by this pref
            SharedPreferences sign_sharedPref = signature.getSharedPreferences();
            SharedPreferences.Editor sign_editor = sign_sharedPref.edit();
            String sign_val = sign_sharedPref.getString(getString(R.string.signature_key), null);
            if(sign_val == null || sign_val.equals("") || sign_val.equals(" ")){
                sign_editor.putString(getString(R.string.signature_key), Parser.default_sign);
                sign_editor.apply();
                signature.callChangeListener(Parser.default_sign);
            }
            signature.setOnPreferenceChangeListener((preference, newValue) -> {
                //getting user modified signature
                //Log.e(TAG,"sign.changelistener val: "+(String) newValue);
                if(!Parser.getCurrentMsgSign().equals(Parser.default_sign))
                Parser.setdefault_sign((String) newValue);
                sign_editor.putString(getString(R.string.signature_key), Parser.getCurrentMsgSign());
                return true;
            });
            Context alertPref_cntxt = alertPref.getContext();
            Ringtone ringtone = null;
            try {
                ringtone = RingtoneManager
                        .getRingtone(alertPref_cntxt, Uri.parse(
                                getCurrentRngtone(alertPref_cntxt)));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if(ringtone!=null)
            alertPref.setSummary(ringtone.getTitle(alertPref_cntxt));
            ringtoneSelectActivityLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                            Intent data = result.getData();
                            Uri uri = null;
                            if(data != null)
                                uri = data
                                        .getParcelableExtra(RingtoneManager
                                                .EXTRA_RINGTONE_PICKED_URI);
                            if (uri != null) {
                                setRingtonePref(alertPref_cntxt,uri.toString());
                                Ringtone ringtonex = RingtoneManager
                                        .getRingtone(alertPref_cntxt, uri);
                                alertPref.setSummary(ringtonex.getTitle(alertPref_cntxt));
                                Log.i(TAG,"Ringtone title: "+ringtonex
                                        .getTitle(alertPref_cntxt));
                            }
                        }
                    });
            if(!getServiceStatus(mSvc_sw_cntxt))
                mSvc_sw_pref.setChecked(false);
                }

            @Override
        public void onStart() {
            super.onStart();
        }


        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
           Log.i(TAG," Selected Preference from Tree  >> "+ preference.getKey());
           if(preference.getKey().equals(getString(R.string.latest_result_key))){
               String result2bPopulated = getLatRes(preference.getContext());
               preference.setSummary(result2bPopulated);
               ClipboardManager clipboard = (ClipboardManager) preference
                       .getContext().getSystemService(CLIPBOARD_SERVICE);
               ClipData clip = ClipData.newPlainText("Celestrak Results", result2bPopulated);
               clipboard.setPrimaryClip(clip);
           }
           else if(preference.getKey().equals(getString(R.string.ringtonePref_key))){
               Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
               intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select ringtone for alerts:");
               intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
               intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
               intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_RINGTONE);
               ringtoneSelectActivityLauncher.launch(intent);
           }

           return super.onPreferenceTreeClick(preference);
        }

        @Override
        public boolean onOptionsItemSelected(@NonNull MenuItem item) {
            Log.v(TAG,"OnOptionsItemsSelected() with : " + item);
            return super.onOptionsItemSelected(item);
        }


        @Override
        public boolean onContextItemSelected(@NonNull MenuItem item) {
            Log.v("onContextItemSelected", item.toString());
            return super.onContextItemSelected(item);
        }
    }


}