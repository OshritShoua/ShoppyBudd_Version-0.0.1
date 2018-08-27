package com.example.shoppybuddy;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Log;

public class CurrencyConflictDialogFragment extends DialogFragment
{
    public interface CurrencyConflictDialogListener
    {
        void OnRetakeImageClick(DialogFragment dialog);
        void OnChangeSourceCurrenciesClick(Character newCurrency, boolean itemAdditionPending);
    }

    public static CurrencyConflictDialogFragment newInstance(String scannedCurrency, String cartCurrency) {
        CurrencyConflictDialogFragment f = new CurrencyConflictDialogFragment();

        Bundle args = new Bundle();
        args.putString("scannedCurrency",scannedCurrency);
        args.putString("cartCurrency",cartCurrency);
        f.setArguments(args);

        return f;
    }

    CurrencyConflictDialogFragment.CurrencyConflictDialogListener mListener;

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (CurrencyConflictDialogFragment.CurrencyConflictDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String scannedCurrency = getArguments().getString("scannedCurrency");
        String cartCurrency = getArguments().getString("cartCurrency");
        String dialogText = getString(R.string.CurrencyConflictDialogText, scannedCurrency, cartCurrency, scannedCurrency);
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(dialogText)
                .setPositiveButton(R.string.retake_image_dialog_retry_button_text, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        mListener.OnRetakeImageClick(CurrencyConflictDialogFragment.this);
                    }
                })
                .setNegativeButton(R.string.Change_currencies_button_text, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        mListener.OnChangeSourceCurrenciesClick(scannedCurrency.charAt(0), true);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        try {
            FragmentTransaction ft = manager.beginTransaction();
            ft.add(this, tag);
            ft.commitAllowingStateLoss();
        } catch (IllegalStateException e) {
            Log.d("ABSDIALOGFRAG", "Exception", e);
        }
    }
}