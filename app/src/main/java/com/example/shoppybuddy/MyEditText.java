package com.example.shoppybuddy;

import android.content.Context;
import android.view.KeyEvent;
import android.widget.EditText;

public class MyEditText extends android.support.v7.widget.AppCompatEditText
{
    public MyEditText(Context context)
    {
        super(context);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event)
    {
        if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP)
        {

            return false;
        }
        return super.onKeyPreIme(keyCode, event);
    }
}
