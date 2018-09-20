package com.example.shoppybuddy;

import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 */
public class DescriptionDialogFragment extends DialogFragment implements TextView.OnEditorActionListener
{
    public enum DialogPurpose
    {
        cartDescription,
        itemDescription,
        itemDiscount,
        enterPriceManually
    }

    private static DialogPurpose _dialogPurpose;

    public interface DescriptionDialogListener
    {
         void OnItemDescriptionDone(String description);
         void OnCartDescriptionDone(String description);
         void OnItemDiscountDone(String discount, boolean isDiscountInPercents);
         void OnPriceEnterDone(String price);
    }

    private EditText mEditText;

    public DescriptionDialogFragment()
    {
        // Required empty public constructor
    }

    public static DescriptionDialogFragment newInstance(String dialogArg, DialogPurpose dialogPurpose) {
        _dialogPurpose = dialogPurpose;
        DescriptionDialogFragment frag = new DescriptionDialogFragment();
        Bundle args = new Bundle();
        if(dialogPurpose == DialogPurpose.itemDiscount)
            args.putString("discount", dialogArg);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view;
        TextView textView;
        if(_dialogPurpose == DialogPurpose.itemDiscount)
        {
            view = inflater.inflate(R.layout.fragment_item_discount_dialog, container,false );
        }
        else if (_dialogPurpose == DialogPurpose.enterPriceManually)
        {
            view = inflater.inflate(R.layout.fragment_item_price_enter, container, false);
        }
        else
        {
            view = inflater.inflate(R.layout.fragment_item_description_dialog, container, false);
            textView = view.findViewById(R.id.item_description_msg);
            if(_dialogPurpose == DialogPurpose.itemDescription)
                textView.setText(R.string.item_describe_dialog_text);
            else
                textView.setText(R.string.cart_describe_dialog_text);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get field from view
        if(_dialogPurpose == DialogPurpose.itemDiscount)
        {
            mEditText = view.findViewById(R.id.discount_edit_text);
            mEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER);
            String currentDiscount = getArguments().getString("discount");
            if(currentDiscount != null)
            {
                TextView discountTextView = view.findViewById(R.id.existing_discount_textview);
                discountTextView.setText(currentDiscount);
            }
        }
        else if (_dialogPurpose == DialogPurpose.enterPriceManually)
            mEditText = view.findViewById(R.id.price_enter_edittext);
        else
            mEditText = view.findViewById(R.id.item_description_editing);
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
            else if(_dialogPurpose == DialogPurpose.itemDiscount)
            {
                RadioGroup group = getDialog().findViewById(R.id.radio_group_id);
                int checkedButtonId = group.getCheckedRadioButtonId();
                RadioButton checkedButton = group.findViewById(checkedButtonId);
                if(checkedButton.getText().equals(getString(R.string.percents_text)))
                    listener.OnItemDiscountDone(mEditText.getText().toString(), true);
                else
                    listener.OnItemDiscountDone(mEditText.getText().toString(), false);
            }
            else if (_dialogPurpose == DialogPurpose.enterPriceManually)
            {
                listener.OnPriceEnterDone(mEditText.getText().toString());
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