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

public class RecaptureImageDialogFragment extends DialogFragment
{
    public interface RecaptureImageDialogListener{
         void onRetakeImageClick(DialogFragment dialog);
         void onReturnToCartClick(DialogFragment dialog);
    }

    RecaptureImageDialogListener mListener;

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            mListener = (RecaptureImageDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

        @NonNull
        @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.retake_image_dialog_text)
                .setPositiveButton(R.string.retake_image_dialog_retry_button_text, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        mListener.onRetakeImageClick(RecaptureImageDialogFragment.this);
                    }
                })
                .setNegativeButton(R.string.retake_image_dialog_return_button_text, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        mListener.onReturnToCartClick(RecaptureImageDialogFragment.this);
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