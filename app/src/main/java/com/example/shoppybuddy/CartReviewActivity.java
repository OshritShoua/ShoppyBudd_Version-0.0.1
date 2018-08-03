package com.example.shoppybuddy;

import android.Manifest;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import org.w3c.dom.Text;

import java.io.File;

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

    /* todo - important.
    The switch to this CartReviewActivity will be from 2 places:
    1. When starting a new cart.
    2. When entering an existing cart - in which case the items should be fetched from the db.
    This implementation is now for a new EMPTY cart only.*/
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
        if(callingActivity.equals(MainActivity.class.getSimpleName())) //todo - add a remark to notes about having had to move the "if else" with the database logic before creating the adapter
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
            //todo - this logic needs to change to getting the cart by the id from the db..that will give us the description too
            int id = getIntent().getIntExtra("cart id", -1);
            _cart = _db.cartDao().getCartById(id);
            UpdateCartNameTextView(_cart.get_description());
            _cart.GetItems().addAll(_db.itemDao().getItemsByCartId(id));
        }

        _pricingServices = new PricingServices();
        InitializeItemListView();
        UpdateSumUI();
        _ocrServices = new OCRServices();
    }

    private void InitializeItemListView()
    {
        ArrayAdapter<Item> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, _cart.GetItems());
        ListView itemListView = findViewById(R.id._dynamic_item_list);
        itemListView.setAdapter(adapter);
    }

    private void UpdateSumUI()
    {
        TextView totalSumTextView = findViewById(R.id.totalSumText);
        String currencyCode = SettingsPrefActivity.get_preferredTargetCurrencyCode();
        String currencySymbol = currencyCode != null ? Character.toString(OCRServices.getCodesToSymbolsMapping().get(currencyCode)): "";
        String totalPrice = Double.toString(_cart.get_totalCost());
        totalSumTextView.setText(totalPrice);
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

    private void handleCapturedImage()
    {
        if (!_ocrServices.parsePriceFromTextSucceeded())
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

    private void takePicture() {
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (capturedImageUri != null) {
            outState.putString(SAVED_INSTANCE_URI, capturedImageUri.toString());
            outState.putString(SAVED_INSTANCE_RESULT, scanResults.getText().toString());
        }
        super.onSaveInstanceState(outState);
    }
}