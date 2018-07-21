package com.example.shoppybuddy;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.MenuItem;

public class SettingsPrefActivity extends AppCompatPreferenceActivity {
    private static final String TAG = SettingsPrefActivity.class.getSimpleName();
    private static boolean _shouldRequestItemDescription = true;
    private static boolean _shouldRequestCartDescription = true;

    public static boolean ShouldRequestItemDescription()
    {
        return _shouldRequestItemDescription;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // load settings fragment
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    public static boolean ShouldRequestCartDescription()
    {
        return _shouldRequestCartDescription;
    }

    public static class MainPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);

            //todo: delete?
            // gallery EditText change listener
            //bindPreferenceSummaryToValue(findPreference(getString(R.string.key_gallery_name)));

            // notification preference change listener
            //bindPreferenceSummaryToValue(findPreference(getString(R.string.key_notifications_new_message_ringtone)));

            // feedback preference click listener
//            Preference myPref = findPreference(getString(R.string.key_send_feedback));
//            myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//                public boolean onPreferenceClick(Preference preference) {
//                    sendFeedback(getActivity());
//                    return true;
//                }
//            });

            findPreference(getString(R.string.key_request_description_for_item)).setOnPreferenceChangeListener(_prefChangeListener);
            findPreference(getString(R.string.key_request_description_for_cart)).setOnPreferenceChangeListener(_prefChangeListener);
            findPreference(getString(R.string.key_source_currency)).setOnPreferenceChangeListener(_prefChangeListener);
            findPreference(getString(R.string.key_target_currency)).setOnPreferenceChangeListener(_prefChangeListener);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String stringValue = newValue.toString();
            String key = preference.getKey();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(R.string.summary_choose_ringtone);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else if (preference instanceof EditTextPreference) {
                if (preference.getKey().equals("key_gallery_name")) {
                    // update the changed gallery name to summary filed
                    preference.setSummary(stringValue);
                }
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static Preference.OnPreferenceChangeListener _prefChangeListener = new Preference.OnPreferenceChangeListener()
    {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o)
        {
            switch (preference.getKey()) {
                case "key_request_description_for_item":
                    onItemDescriptionPrefChange(preference, o);
                case "key_request_description_for_cart":
                    onCartDescriptionPrefChange(preference, o);
                case "key_source_currency":
                    onSourceCurrencyPrefChange(preference, o );
                case "key_target_currency":
                    onTargetCurrencyPrefChange(preference,o );
            }
            return true;
        }
    };

    private static void onItemDescriptionPrefChange(Preference preference, Object o)
    {
        _shouldRequestItemDescription = (boolean)o;
        //todo: delete?
//        if(_shouldRequestItemDescription)
//            preference.setSummary(R.string.summary_request_item_description);
//        else
//            preference.setSummary(R.string.summary_auto_generate_item_description);
    }

    private static void onCartDescriptionPrefChange(Preference preference, Object o)
    {
        _shouldRequestCartDescription = (boolean)o;
        //todo: delete?
//        if(_shouldRequestItemDescription)
//            preference.setSummary(R.string.summary_request_item_description);
//        else
//            preference.setSummary(R.string.summary_auto_generate_item_description);
    }

    private static void onSourceCurrencyPrefChange(Preference preference, Object o)
    {
        //todo - decide how to take the currency from the object
        preference.setSummary(o.toString());
    }

    private static void onTargetCurrencyPrefChange(Preference preference, Object o)
    {
        preference.setSummary(o.toString());
    }

//    /**
//     * Email client intent to send support mail
//     * Appends the necessary device information to email body
//     * useful when providing support
//     */
//    public static void sendFeedback(Context context) {
//        String body = null;
//        try {
//            body = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
//            body = "\n\n-----------------------------\nPlease don't remove this information\n Device OS: Android \n Device OS version: " +
//                    Build.VERSION.RELEASE + "\n App Version: " + body + "\n Device Brand: " + Build.BRAND +
//                    "\n Device Model: " + Build.MODEL + "\n Device Manufacturer: " + Build.MANUFACTURER;
//        } catch (PackageManager.NameNotFoundException e) {
//        }
//        Intent intent = new Intent(Intent.ACTION_SEND);
//        intent.setType("message/rfc822");
//        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"contact@androidhive.info"});
//        intent.putExtra(Intent.EXTRA_SUBJECT, "Query from android app");
//        intent.putExtra(Intent.EXTRA_TEXT, body);
//        context.startActivity(Intent.createChooser(intent, context.getString(R.string.choose_email_client)));
//    }
}
