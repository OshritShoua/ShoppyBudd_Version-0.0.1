package com.example.shoppybuddy;

import android.Manifest;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shoppybuddy.data.AppDataBase;
import com.example.shoppybuddy.entities.Cart;
import com.example.shoppybuddy.entities.Item;
import com.example.shoppybuddy.services.OCRServices;
import com.example.shoppybuddy.services.PricingServices;

import org.apache.commons.lang3.tuple.MutablePair;

import java.io.File;
import java.util.ArrayList;

public class CartReviewActivity extends AppCompatActivity implements RecaptureImageDialogFragment.RecaptureImageDialogListener, DescriptionDialogFragment.DescriptionDialogListener
{
    private static final int REQUEST_IMAGE_CAPTURE = 10;
    private static final int REQUEST_WRITE_PERMISSION = 20;
    private static final String SAVED_INSTANCE_URI = "uri";
    private static final String SAVED_INSTANCE_RESULT = "result";

    private PricingServices _pricingServices;
    private OCRServices _ocrServices;

    private Cart _cart;
    private double _originalPrice;
    private double _convertedPrice;
    private String _description;
    private AppDataBase _db;

    private Uri capturedImageUri;
    private TextView scanResults;

    @Override
    protected void onResume()
    {
        super.onResume();
        if(_cart.get_toCurrency() == 0)
        {
            _cart.set_toCurrency(SettingsPrefActivity.get_preferredTargetCurrencySymbol());
            _db.cartDao().updateCart(_cart);
            UpdateSumUI();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_review);
        scanResults = findViewById(R.id.scanResults);
        if (savedInstanceState != null) {
            capturedImageUri = Uri.parse(savedInstanceState.getString(SAVED_INSTANCE_URI));
            scanResults.setText(savedInstanceState.getString(SAVED_INSTANCE_RESULT));
        }

        initComponents(getIntent().getStringExtra("calling activity"));
    }

    private void initComponents(String callingActivity)
    {
        ImageButton captureImageButton = findViewById(R.id.cameraButton);
        captureImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(CartReviewActivity.this, new
                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, REQUEST_WRITE_PERMISSION);
            }
        });

        _db = Room.databaseBuilder(getApplicationContext(), AppDataBase.class, "userShoppings").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        if(callingActivity.equals(MainActivity.class.getSimpleName()))
        {
            _cart = new Cart(SettingsPrefActivity.get_preferredTargetCurrencySymbol());
            _cart.setId((int)(_db.cartDao().insert(_cart)));
            if(SettingsPrefActivity.ShouldRequestCartDescription())
            {
                DescriptionDialogFragment cartDescriptionDialogFragment = DescriptionDialogFragment.newInstance("moo", DescriptionDialogFragment.DialogPurpose.cartDescription);
                cartDescriptionDialogFragment.show(getSupportFragmentManager(), "GetDescription");
            }
            else
                OnCartDescriptionDone("My shopping cart #" + Integer.toString(_cart.getId() + 1));
        }
        else
        {
            int id = getIntent().getIntExtra("cart id", -1);
            _cart = _db.cartDao().getCartById(id);
            UpdateCartNameTextView(_cart.get_description());
            _cart.GetItems().addAll(_db.itemDao().getItemsByCartId(id));
            if(_cart.get_toCurrency()== 0) //note - this is for the carts that were created without a currency, and then one was selected which needs to be updated
            {
                _cart.set_toCurrency(SettingsPrefActivity.get_preferredTargetCurrencySymbol());
                _db.cartDao().updateCart(_cart);
            }
        }

        _pricingServices = new PricingServices();
        InitializeItemListView();
        UpdateSumUI();
        _ocrServices = new OCRServices();
    }

    private void InitializeItemListView()
    {
        ArrayAdapter<Item> adapter = new ArrayAdapter<Item>(this, android.R.layout.simple_list_item_1, _cart.GetItems())
        {
            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                View view = super.getView(position, convertView, parent);
                if(position % 2 == 0)
                    view.setBackgroundColor(Color.parseColor("#ff99ff"));
                else
                    view.setBackgroundColor(Color.parseColor("#ff9999"));

                return view;
            }
        };
        ListView itemListView = findViewById(R.id._dynamic_item_list);
        itemListView.setAdapter(adapter);
    }

    private void UpdateSumUI()
    {
        TextView totalSumTextView = findViewById(R.id.totalSumText);
        char currencySymbol = _cart.get_toCurrency();
        if(currencySymbol != 0)
        {
            String totalPrice = String.format("%.2f%c", _cart.get_totalCost(), _cart.get_toCurrency());
            totalSumTextView.setText(totalPrice);
        }
        else
            totalSumTextView.setText("Currency not selected");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            launchMediaScanIntent();
            _ocrServices.GetTextFromCapturedImage(getApplicationContext(), this, capturedImageUri);
            scanResults.setText(_ocrServices.GetCurrentTextCaptured());
            handleCapturedImage();
        }
    }

    public void OnItemDescriptionDone(String description)
    {
        _description = description;
        Item item = new Item(_originalPrice, _convertedPrice, _description, _cart.getId(),
                SettingsPrefActivity.get_preferredSourceCurrencySymbol(), SettingsPrefActivity.get_preferredTargetCurrencySymbol());
        _cart.AddItem(item);
        _db.itemDao().insertAll(item);
        _db.cartDao().updateCart(_cart);
        UpdateSumUI();
    }

    @Override
    public void OnCartDescriptionDone(String description)
    {
        _cart.set_description(description);
        _db.cartDao().updateCart(_cart);
        UpdateCartNameTextView(description);
    }

    public void UpdateCartNameTextView(String description)
    {
        TextView textView = findViewById(R.id.cartNameText);
        textView.setText(description);
    }

    @Override
    public void onRetakeImageClick(DialogFragment dialog) {
       takePicture();
    }

    @Override
    public void onReturnToCartClick(DialogFragment dialog) {
        return;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        finish();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePicture();
                } else {
                    Toast.makeText(CartReviewActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (capturedImageUri != null) {
            outState.putString(SAVED_INSTANCE_URI, capturedImageUri.toString());
            outState.putString(SAVED_INSTANCE_RESULT, scanResults.getText().toString());
        }
        super.onSaveInstanceState(outState);
    }

    private void takePicture() {
        if(_cart.get_toCurrency() == 0)
        {
            CurrencyNotSelectedDialogFragment fragment = new CurrencyNotSelectedDialogFragment();
            fragment.show(getSupportFragmentManager(), "Select currency");
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(), "captured_image.jpg");
        capturedImageUri = FileProvider.getUriForFile(CartReviewActivity.this,
                BuildConfig.APPLICATION_ID + ".provider", photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    private void launchMediaScanIntent() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(capturedImageUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void handleCapturedImage()
    {

        MutablePair<ArrayList<String>, Boolean> ocrResult = _ocrServices.getOCRResult(_pricingServices.getBaseCurrencyCode());
        if (ocrResult.getLeft().size() == 0)
        {
            RecaptureImageDialogFragment recaptureImageDialogFragment = new RecaptureImageDialogFragment();
            recaptureImageDialogFragment.show(getSupportFragmentManager(), "RetakeImage");
        }
        else
        {
            //todo :  what if couldn't convert
            _originalPrice = Double.parseDouble(_ocrServices.GetCurrentPriceCaptured());
            _pricingServices.ConvertPrice(_originalPrice);
            _convertedPrice = _pricingServices.GetConvertedPrice();

            if(SettingsPrefActivity.ShouldRequestItemDescription())
            {
                DescriptionDialogFragment itemDescriptionDialogFragment = DescriptionDialogFragment.newInstance("moo", DescriptionDialogFragment.DialogPurpose.itemDescription);
                itemDescriptionDialogFragment.show(getSupportFragmentManager(), "GetDescription");
            }
            else
            {
                OnItemDescriptionDone("Item #" + Integer.toString(_cart.GetItems().size() + 1));
            }
        }
    }
}