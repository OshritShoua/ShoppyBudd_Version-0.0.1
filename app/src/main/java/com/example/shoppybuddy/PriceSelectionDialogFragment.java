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
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PriceSelectionDialogFragment extends DialogFragment
{
    public interface PriceSelectionDialogListener
    {
        void OnPriceSelected(Price price);
        void OnRetakeImageClick(DialogFragment dialog);
        void OnEnterPriceManuallyRequest();
    }

    public static PriceSelectionDialogFragment newInstance(List<Price> prices, String dialogPurpose) {
        PriceSelectionDialogFragment f = new PriceSelectionDialogFragment();

        Bundle args = new Bundle();
        args.putString("purpose",dialogPurpose);
        args.putSerializable("prices", (Serializable) prices);
        f.setArguments(args);
        return f;
    }

    PriceSelectionDialogFragment.PriceSelectionDialogListener mListener;

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            mListener = (PriceSelectionDialogFragment.PriceSelectionDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final List<Price> prices = (List<Price>) getArguments().getSerializable("prices");
        String dialogPurpose = getArguments().getString("purpose");
        int titleResId = dialogPurpose == "ConfirmPrice" ? R.string.price_confirmation_text : R.string.price_selection_text;
        List<CharSequence> temp = new ArrayList<>();
        for (Price p : prices)
            temp.add(Double.toString(p.getOriginalAmount()) + p.getFromCurrencySymbol());

        CharSequence[] priceStrings = temp.toArray(new CharSequence[temp.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        TextView textView = new TextView(getActivity());
        textView.setText(titleResId);
        builder.setCustomTitle(textView)
                .setItems(priceStrings, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        mListener.OnPriceSelected(prices.get(i));
                    }
                })
                .setPositiveButton(R.string.retake_image_dialog_retry_button_text, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        mListener.OnRetakeImageClick(PriceSelectionDialogFragment.this);
                    }
                })
                .setNegativeButton(R.string.enter_price_button_text, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        mListener.OnEnterPriceManuallyRequest();
                    }
                });
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