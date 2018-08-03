package com.example.shoppybuddy;

import android.content.SharedPreferences;
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

import com.example.shoppybuddy.services.OCRServices;

public class SettingsPrefActivity extends AppCompatPreferenceActivity {
    private static final String TAG = SettingsPrefActivity.class.getSimpleName();
    private static boolean _shouldRequestItemDescription;
    private static boolean _shouldRequestCartDescription;
    private static String _preferredSourceCurrencyCode = "";
    private static Character _preferredSourceCurrencySymbol;
    private static String _preferredTargetCurrencyCode = "";
    private static Character _preferredTargetCurrencySymbol;

    public static boolean ShouldRequestItemDescription()
    {
        return _shouldRequestItemDescription;
    }

    public static String get_preferredSourceCurrencyCode()
    {
        return _preferredSourceCurrencyCode;
    }

    public static String get_preferredTargetCurrencyCode()
    {
        return _preferredTargetCurrencyCode;
    }

    public static Character get_preferredSourceCurrencySymbol()
    {
        return _preferredSourceCurrencySymbol;
    }

    public static Character get_preferredTargetCurrencySymbol()
    {
        return _preferredTargetCurrencySymbol;
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

    public static void InitValuesFromSharedPrefs(SharedPreferences sharedPreferences)
    {
        _shouldRequestItemDescription = sharedPreferences.getBoolean("key_request_description_for_item",true);
        _shouldRequestCartDescription = sharedPreferences.getBoolean("key_request_description_for_cart",true);
        String sourceCurrency = sharedPreferences.getString("key_source_currency", null);
        if(sourceCurrency != null)
        {
            _preferredSourceCurrencySymbol = sourceCurrency.charAt(0);
            _preferredSourceCurrencyCode = OCRServices.getSymbolsToCodesMapping().get(sourceCurrency.charAt(0));
        }

        String targetCurrency = sharedPreferences.getString("key_target_currency", null);
        if(targetCurrency != null)
        {
            _preferredTargetCurrencySymbol = targetCurrency.charAt(0);
            _preferredTargetCurrencyCode = OCRServices.getSymbolsToCodesMapping().get(targetCurrency.charAt(0));
        }
    }

    public static class MainPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);
//            CheckBoxPreference itemCheckbox = (CheckBoxPreference)findPreference(getString(R.string.key_request_description_for_item));
//            itemCheckbox.setChecked(true);
//            CheckBoxPreference cartCheckbox = (CheckBoxPreference)findPreference(getString(R.string.key_request_description_for_cart));
//            cartCheckbox.setChecked(true);
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
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String sourceCurrency = sharedPreferences.getString(getString(R.string.key_source_currency), "" );
            String targetCurrency = sharedPreferences.getString(getString(R.string.key_target_currency), "" );
            Preference sourceCurrencyPref = findPreference(getString(R.string.key_source_currency));
            Preference targetCurrencyPref = findPreference(getString(R.string.key_target_currency));
            sourceCurrencyPref.setOnPreferenceChangeListener(_prefChangeListener);
            sourceCurrencyPref.setSummary(sourceCurrency);
            targetCurrencyPref.setOnPreferenceChangeListener(_prefChangeListener);
            targetCurrencyPref.setSummary(targetCurrency);
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
    }

    private static void onCartDescriptionPrefChange(Preference preference, Object o)
    {
        _shouldRequestCartDescription = (boolean)o;
    }

    private static void onSourceCurrencyPrefChange(Preference preference, Object o)
    {
        String selectedEntryValue = o.toString();
        preference.setSummary(selectedEntryValue);
        char currencySymbol = selectedEntryValue.charAt(0);
        _preferredSourceCurrencySymbol = currencySymbol;
        _preferredSourceCurrencyCode = OCRServices.getSymbolsToCodesMapping().get(currencySymbol);
    }

    private static void onTargetCurrencyPrefChange(Preference preference, Object o)
    {
        String selectedEntryValue = o.toString();
        preference.setSummary(selectedEntryValue);
        char currencySymbol = selectedEntryValue.charAt(0);
        _preferredTargetCurrencySymbol = currencySymbol;
        _preferredTargetCurrencyCode = OCRServices.getSymbolsToCodesMapping().get(currencySymbol);
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
