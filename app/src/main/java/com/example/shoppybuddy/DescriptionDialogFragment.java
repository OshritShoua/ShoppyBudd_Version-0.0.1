package com.example.shoppybuddy;


import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class DescriptionDialogFragment extends DialogFragment implements TextView.OnEditorActionListener
{
    public enum DialogPurpose
    {
        cartDescription,
        itemDescription
    }

    private static DialogPurpose _dialogPurpose;

    public interface DescriptionDialogListener
    {
         void OnItemDescriptionDone(String description);
         void OnCartDescriptionDone(String description);
    }

    private EditText mEditText;

    public DescriptionDialogFragment()
    {
        // Required empty public constructor
    }

    public static DescriptionDialogFragment newInstance(String title, DialogPurpose dialogPurpose) {
        _dialogPurpose = dialogPurpose;
        DescriptionDialogFragment frag = new DescriptionDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_item_description_dialog, container, false);
        TextView textView = view.findViewById(R.id.item_description_msg);
        if(_dialogPurpose == DialogPurpose.itemDescription)
            textView.setText(R.string.item_describe_dialog_text);
        else if(_dialogPurpose == DialogPurpose.cartDescription)
            textView.setText(R.string.cart_describe_dialog_text);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get field from view
        mEditText = (EditText) view.findViewById(R.id.item_description_editing);
        // Fetch arguments from bundle and set title
        String title = getArguments().getString("title", "Enter Name");
        getDialog().setTitle(title);
        // Show soft keyboard automatically and request focus to field
        mEditText.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mEditText.setOnEditorActionListener(this);
    }

    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            // Return input text back to activity through the implemented listener
            DescriptionDialogListener listener = (DescriptionDialogListener) getActivity();
            if(_dialogPurpose == DialogPurpose.itemDescription)
            {
                listener.OnItemDescriptionDone(mEditText.getText().toString());
            }
            else if(_dialogPurpose == DialogPurpose.cartDescription)
            {
                listener.OnCartDescriptionDone(mEditText.getText().toString());
            }

            // Close the dialog and return back to the parent activity
            dismiss();

            return true;
        }

        return false;
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
