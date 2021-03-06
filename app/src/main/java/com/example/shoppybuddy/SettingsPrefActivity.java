package com.example.shoppybuddy;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.example.shoppybuddy.services.OCRServices;

public class SettingsPrefActivity extends AppCompatPreferenceActivity {
    private static boolean _shouldRequestItemDescription;
    private static boolean _shouldRequestCartDescription;
    private static String _preferredSourceCurrencyCode = "";
    private static char _preferredSourceCurrencySymbol;
    private static String _preferredTargetCurrencyCode = "";
    private static char _preferredTargetCurrencySymbol;
    private static String _sourceSummary;
    private static String _targetSummary;

    public static boolean ShouldRequestItemDescription()
    {
        return _shouldRequestItemDescription;
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
            findPreference(getString(R.string.key_request_description_for_item)).setOnPreferenceChangeListener(_prefChangeListener);
            findPreference(getString(R.string.key_request_description_for_cart)).setOnPreferenceChangeListener(_prefChangeListener);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String sourceCurrency = sharedPreferences.getString(getString(R.string.key_source_currency), "" );
            String targetCurrency = sharedPreferences.getString(getString(R.string.key_target_currency), "" );
            _sourceSummary = getString(R.string.summary_source_currency);
            _targetSummary = getString(R.string.summary_target_currency);
            Preference sourceCurrencyPref = findPreference(getString(R.string.key_source_currency));
            Preference targetCurrencyPref = findPreference(getString(R.string.key_target_currency));
            sourceCurrencyPref.setOnPreferenceChangeListener(_prefChangeListener);
            sourceCurrencyPref.setSummary(String.format("%s:\n%s", _sourceSummary, sourceCurrency));
            targetCurrencyPref.setOnPreferenceChangeListener(_prefChangeListener);
            targetCurrencyPref.setSummary(String.format("%s\n%s", _targetSummary, targetCurrency));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    private static Preference.OnPreferenceChangeListener _prefChangeListener = new Preference.OnPreferenceChangeListener()
    {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o)
        {
            switch (preference.getKey()) {
                case "key_request_description_for_item":
                    onItemDescriptionPrefChange(o);
                    break;
                case "key_request_description_for_cart":
                    onCartDescriptionPrefChange(o);
                    break;
                case "key_source_currency":
                    onSourceCurrencyPrefChange(preference, o );
                    break;
                case "key_target_currency":
                    onTargetCurrencyPrefChange(preference,o );
                    break;
            }
            return true;
        }
    };

    private static void onItemDescriptionPrefChange(Object o)
    {
        _shouldRequestItemDescription = (boolean)o;
    }

    private static void onCartDescriptionPrefChange(Object o)
    {
        _shouldRequestCartDescription = (boolean)o;
    }

    private static void onSourceCurrencyPrefChange(Preference preference, Object o)
    {
        String selectedEntryValue = o.toString();
        preference.setSummary(String.format("%s\n%s", _sourceSummary, selectedEntryValue));
        char currencySymbol = selectedEntryValue.charAt(0);
        _preferredSourceCurrencySymbol = currencySymbol;
        _preferredSourceCurrencyCode = OCRServices.getSymbolsToCodesMapping().get(currencySymbol);
    }

    private static void onTargetCurrencyPrefChange(Preference preference, Object o)
    {
        String selectedEntryValue = o.toString();
        preference.setSummary(String.format("%s\n%s", _targetSummary, selectedEntryValue));
        char currencySymbol = selectedEntryValue.charAt(0);
        _preferredTargetCurrencySymbol = currencySymbol;
        _preferredTargetCurrencyCode = OCRServices.getSymbolsToCodesMapping().get(currencySymbol);
    }
}