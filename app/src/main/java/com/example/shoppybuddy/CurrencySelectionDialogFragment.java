package com.example.shoppybuddy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

public class CurrencySelectionDialogFragment extends DialogFragment
{
    interface CurrencySelectionDialogListerner
    {
        void OnCurrencySelected(Character currencySymbol);
    }
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private String mValue;
    private boolean mValueSet;
    SharedPreferences prefs;
    private int mClickedDialogEntryIndex;
    private CurrencySelectionDialogListerner mListener;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mEntries = getResources().getStringArray(R.array.pref_currency_entries);
        mEntryValues = getResources().getStringArray(R.array.pref_currency_values);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        dialog.setTitle(getString(R.string.CurrencySelection));
        dialog.setPositiveButton(null, null);
        mClickedDialogEntryIndex = getValueIndex();
        dialog.setSingleChoiceItems(mEntries, mClickedDialogEntryIndex, selectItemListener);
        return dialog.create();
    }

    private int getValueIndex() {
        return findIndexOfValue(mValue);
    }

    public int findIndexOfValue(String value) {
        if (value != null && mEntryValues != null) {
            for (int i = mEntryValues.length - 1; i >= 0; i--) {
                if (mEntryValues[i].equals(value)) {
                    return i;
                }
            }
        }
        return -1;
    }

    DialogInterface.OnClickListener selectItemListener = new DialogInterface.OnClickListener() {

        @Override public void onClick(DialogInterface dialog, int which) {
            if (mClickedDialogEntryIndex != which) {
                mClickedDialogEntryIndex = which;
                mValue = mEntryValues[mClickedDialogEntryIndex].toString();
                mListener.OnCurrencySelected(mValue.charAt(0));
            }
            dialog.dismiss();
        }
    };

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (CurrencySelectionDialogListerner) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }
}