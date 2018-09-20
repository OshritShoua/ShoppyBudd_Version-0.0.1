package com.example.shoppybuddy;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.shoppybuddy.data.AppDataBase;
import com.example.shoppybuddy.entities.Cart;
import com.example.shoppybuddy.entities.Item;

import org.apache.commons.lang3.NotImplementedException;

import java.io.File;
import java.util.Locale;

public class ItemDetailsActivity extends AppCompatActivity implements TextView.OnEditorActionListener, View.OnFocusChangeListener,
        DescriptionDialogFragment.DescriptionDialogListener, View.OnClickListener
{
    private Cart _cart;
    private Item _item;
    private AppDataBase _db;
    EditText _description;
    EditText _originalPrice;
    EditText _discount;
    TextView _afterDiscountPrice;
    TextView _convertedPrice;
    TextView _originalPriceCurrency;
    TextView _discountCurrency;
    ImageView _itemPicture;
    private Uri _capturedImageUri;
    private static final int REQUEST_IMAGE_CAPTURE = 10;
    boolean _photoExists;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_details);
        _description = findViewById(R.id.item_details_description_text);
        _originalPrice = findViewById(R.id.item_details_original_price);
        _discount = findViewById(R.id.item_details_discount);
        _afterDiscountPrice = findViewById(R.id.item_details_after_discount);
        _convertedPrice = findViewById(R.id.item_details_converted_price);
        _originalPriceCurrency = findViewById(R.id.item_details_original_price_currency);
        _discountCurrency = findViewById(R.id.item_details_discount_currency);
        _itemPicture = findViewById(R.id.item_details_item_picture);
        _cart = (Cart)getIntent().getSerializableExtra("cart");
        int index = getIntent().getIntExtra("selectedItemIndex", -1);
        _item = _cart.GetItems().get(index);
        _db = CartReviewActivity.GetAppDb();
        _description.setOnEditorActionListener(this);
        _originalPrice.setOnEditorActionListener(this);
        _discount.setOnClickListener(this);
        _discount.setOnFocusChangeListener(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        UpdateUI();
    }

    private void UpdateUI()
    {
        _description.setText(_item.get_description());
        _originalPrice.setText(_item.GetOriginalPriceFormattedString());
        _originalPriceCurrency.setText(Character.toString(_item.get_fromCurrency()));
        _discountCurrency.setText(Character.toString(_item.get_fromCurrency()));
        if(_item.getDiscount() != 0)
            _discount.setText(_item.GetDiscountFormattedString());

        _afterDiscountPrice.setText(_item.GetAfterDiscountPriceFormattedString());
        _convertedPrice.setText(_item.GetConvertedPriceFormattedString());
        File photo = new File(Environment.getExternalStorageDirectory(), String.format(Locale.getDefault(), "cid_%d_%s.jpg", _cart.getId(), _item.get_description()));
        _capturedImageUri =FileProvider.getUriForFile(ItemDetailsActivity.this,
                BuildConfig.APPLICATION_ID + ".provider", photo);
        if(photo.exists())
        {
            _photoExists = true;
            _itemPicture.setImageURI(_capturedImageUri);
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
    {
        if (EditorInfo.IME_ACTION_DONE == actionId)
            switch (v.getId())
            {
                case R.id.item_details_description_text:
                    OnDescriptionEdited(v);
                    break;
                case R.id.item_details_original_price:
                    OnOriginalPriceEdited(v);
                    break;
            }

        return false;
    }

    private void OnOriginalPriceEdited(TextView originalPrice)
    {
        double prevAfterDiscountPrice = _item.getAfterDiscountPrice();
        double prevConverted = _item.getConvertedPrice();
        _item.setOriginalPrice(Double.parseDouble(originalPrice.getText().toString()));
        _cart.set_totalSrcCost(_cart.get_totalSrcCost() - prevAfterDiscountPrice + _item.getAfterDiscountPrice());
        _cart.set_totalDestCost(_cart.get_totalDestCost() - prevConverted + _item.getConvertedPrice());
        _db.itemDao().updateItem(_item);
        _db.cartDao().updateCart(_cart);
        _afterDiscountPrice.setText(_item.GetAfterDiscountPriceFormattedString());
        _convertedPrice.setText(_item.GetConvertedPriceFormattedString());
    }

    private void OnDescriptionEdited(TextView description)
    {
        File photo = new File(Environment.getExternalStorageDirectory(), String.format(Locale.getDefault(), "cid_%d_%s.jpg", _cart.getId(), _item.get_description()));
        _item.set_description(description.getText().toString());
        if(photo.exists())
            photo.renameTo(new File(Environment.getExternalStorageDirectory(), String.format(Locale.getDefault(), "cid_%d_%s.jpg", _cart.getId(), _item.get_description())));
        _capturedImageUri = FileProvider.getUriForFile(ItemDetailsActivity.this,
                BuildConfig.APPLICATION_ID + ".provider", photo);
        _db.itemDao().updateItem(_item);
    }

    @Override
    public void onFocusChange(View view, boolean receivedFocus)
    {
        if(!receivedFocus)
            return;

        DescriptionDialogFragment descriptionDialogFragment = DescriptionDialogFragment.newInstance(_item.GetDiscountFormattedString(), DescriptionDialogFragment.DialogPurpose.itemDiscount );
        descriptionDialogFragment.show(getSupportFragmentManager(), "getDiscount");
    }

    @Override
    public void OnItemDescriptionDone(String description){throw new NotImplementedException("Not implemented stub");}
    @Override
    public void OnCartDescriptionDone(String description){throw new NotImplementedException("Not implemented stub");}
    @Override
    public void OnPriceEnterDone(String price){throw new NotImplementedException("Not implemented stub");}
    @Override
    public void OnItemDiscountDone(String discountStr, boolean isDiscountInPercents)
    {
        Double discount = Double.parseDouble(discountStr);
        double prevAfterDiscountPrice = _item.getAfterDiscountPrice();
        double prevConvertedPrice = _item.getConvertedPrice();
        _item.setDiscount(discount, isDiscountInPercents);
        _cart.set_totalSrcCost(_cart.get_totalSrcCost() - prevAfterDiscountPrice + _item.getAfterDiscountPrice());
        _cart.set_totalDestCost(_cart.get_totalDestCost() - prevConvertedPrice + _item.getConvertedPrice());
        _db.itemDao().updateItem(_item);
        _db.cartDao().updateCart(_cart);
        _afterDiscountPrice.setText(_item.GetAfterDiscountPriceFormattedString());
        _discount.setText(_item.GetDiscountFormattedString());
        _convertedPrice.setText(_item.GetConvertedPriceFormattedString());
    }

    @Override
    public void onClick(View view)
    {
        onFocusChange(view, true);
    }

    public void OnItemPictureClick(View v)
    {
        if(!_photoExists)
        {
            _photoExists = true;
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, _capturedImageUri);
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
        else
        {
            Intent intent = new Intent(this, FullScreenImageActivity.class);
            intent.putExtra("path", String.format(Locale.getDefault(), "cid_%d_%s.jpg", _cart.getId(), _item.get_description()));
            startActivity(intent);
        }
    }
}