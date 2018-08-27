package com.example.shoppybuddy;

import android.Manifest;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
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
import android.widget.Button;
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
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

public class CartReviewActivity extends AppCompatActivity implements RecaptureImageDialogFragment.RecaptureImageDialogListener, DescriptionDialogFragment.DescriptionDialogListener,
        PriceSelectionDialogFragment.PriceSelectionDialogListener, CurrencyConflictDialogFragment.CurrencyConflictDialogListener, CurrencySelectionDialogFragment.CurrencySelectionDialogListerner
{
    private static final int REQUEST_IMAGE_CAPTURE = 10;
    private static final int REQUEST_WRITE_PERMISSION = 20;
    private static final String SAVED_INSTANCE_URI = "uri";
    private static final String SAVED_INSTANCE_RESULT = "result";

    private PricingServices _pricingServices;
    private OCRServices _ocrServices;

    private Cart _cart;
    private double _originalAmount;
    private double _convertedAmount;
    private AppDataBase _db;
    private Uri capturedImageUri;
    private TextView scanResults;
    private Character _scannedSourceCurrency;

    @Override
    protected void onResume()
    {
        super.onResume();
        UpdateCurrenciesIfNeeded();
        UpdateUI();
    }

    private void UpdateCurrenciesIfNeeded()
    {
        if(_cart.get_fromCurrency() == 0 || _cart.get_toCurrency() == 0)
        {
            if(_cart.get_fromCurrency() == 0)
                _cart.set_fromCurrency(SettingsPrefActivity.get_preferredSourceCurrencySymbol());
            if(_cart.get_toCurrency() == 0)
                _cart.set_toCurrency(SettingsPrefActivity.get_preferredTargetCurrencySymbol());

            _db.cartDao().updateCart(_cart);
        }
    }

    private void UpdateUI()
    {
        UpdateCartNameTextView();
        InitializeItemListView();
        UpdateSourceSymbol();
        UpdateSumUI();
    }

    private void UpdateSourceSymbol()
    {
        char symbol = _cart.get_fromCurrency();
        Button symbolButton = findViewById(R.id.FromButton);
        if(symbol != 0)
            symbolButton.setText(getString(R.string.FromButtonText, Character.toString(symbol)));
        else
            symbolButton.setText(R.string.CurrencyNotSelectedString);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_review);
        //scanResults = findViewById(R.id.scanResults);
        if (savedInstanceState != null) {
            capturedImageUri = Uri.parse(savedInstanceState.getString(SAVED_INSTANCE_URI));
            //scanResults.setText(savedInstanceState.getString(SAVED_INSTANCE_RESULT));
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
        _cart = new Cart(SettingsPrefActivity.get_preferredSourceCurrencySymbol(),SettingsPrefActivity.get_preferredTargetCurrencySymbol());
        _db = Room.databaseBuilder(getApplicationContext(), AppDataBase.class, "userShoppings").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        if(callingActivity.equals(MainActivity.class.getSimpleName()))
        {
            if(SettingsPrefActivity.ShouldRequestCartDescription())
            {
                DescriptionDialogFragment cartDescriptionDialogFragment = DescriptionDialogFragment.newInstance("moo", DescriptionDialogFragment.DialogPurpose.cartDescription);
                cartDescriptionDialogFragment.show(getSupportFragmentManager(), "GetDescription");
            }
            else
                OnCartDescriptionDone(null);
        }
        else
        {
            int id = getIntent().getIntExtra("cart id", -1);
            _cart = _db.cartDao().getCartById(id);
            _cart.GetItems().addAll(_db.itemDao().getItemsByCartId(id));
            UpdateCurrenciesIfNeeded();
        }

        _pricingServices = new PricingServices();
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
            String totalPrice = String.format("%.2f%c", _cart.get_totalCost(), currencySymbol);
            totalSumTextView.setText(totalPrice);
        }
        else
            totalSumTextView.setText("");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            launchMediaScanIntent();
            _ocrServices.GetTextFromCapturedImage(getApplicationContext(), this, capturedImageUri);
            //scanResults.setText(_ocrServices.GetCurrentTextCaptured());
            handleCapturedImage();
        }
    }

    public void OnItemDescriptionDone(String description)
    {
        Item item = new Item(_originalAmount, _convertedAmount, description, _cart.getId(),
                _scannedSourceCurrency, _cart.get_toCurrency());
        _cart.AddItem(item);
        RefreshPricingUIDetails();
        _db.itemDao().insertAll(item);
        _db.cartDao().updateCart(_cart);

    }

    private void RefreshPricingUIDetails()
    {
        ListView view = (ListView)findViewById(R.id._dynamic_item_list);
        ArrayAdapter adapter = (ArrayAdapter<Item>)view.getAdapter();
        adapter.notifyDataSetChanged();
        UpdateSumUI();
    }

    @Override
    public void OnCartDescriptionDone(String description)
    {
        _cart.setId((int)(_db.cartDao().insert(_cart)));
        if(description == null)
            description = "My shopping cart #" + Integer.toString(_cart.getId() + 1);

        _cart.set_description(description);
        UpdateCartNameTextView();
        _db.cartDao().updateCart(_cart);
    }

    public void UpdateCartNameTextView()
    {
        TextView textView = findViewById(R.id.cartNameText);
        textView.setText(_cart.get_description());
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
//        if (capturedImageUri != null) {
//            outState.putString(SAVED_INSTANCE_URI, capturedImageUri.toString());
//            outState.putString(SAVED_INSTANCE_RESULT, scanResults.getText().toString());
//        }
        super.onSaveInstanceState(outState);
    }

    private void takePicture() {
        if(_cart.get_toCurrency() == 0 || _cart.get_fromCurrency() == 0)
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

        MutablePair<ArrayList<String>, Boolean> ocrResults = _ocrServices.getOCRResult(_pricingServices.getBaseCurrencyCode());
        if (ocrResults.getLeft().size() == 0)
        {
            RecaptureImageDialogFragment recaptureImageDialogFragment = new RecaptureImageDialogFragment();
            recaptureImageDialogFragment.show(getSupportFragmentManager(), "RetakeImage");
        }
        else
        {
            //todo :  what if couldn't convert
            List<Price> prices = getPricesFromOcrResults(ocrResults.getLeft());
            interactWithUserIfNeededAndDeterminePrice(prices, ocrResults.getRight());
        }
    }

    private void interactWithUserIfNeededAndDeterminePrice(List<Price> prices, Boolean foundWithHeuristics)
    {
        if(prices.size() == 1)
        {
            if(!foundWithHeuristics)
            {
                OnPriceSelected(prices.get(0));
            }
            else
            {
                PriceSelectionDialogFragment priceSelectionDialogFragment = PriceSelectionDialogFragment.newInstance(prices, "ConfirmPrice");
                priceSelectionDialogFragment.show(getSupportFragmentManager(), "ConfirmPrice");
            }
        }
        else
        {
            PriceSelectionDialogFragment priceSelectionDialogFragment = PriceSelectionDialogFragment.newInstance(prices, "SelectPrice");
            priceSelectionDialogFragment.show(getSupportFragmentManager(),"SelectPrice");
        }
    }

    private List<Price> getPricesFromOcrResults(List<String> identifiedPrices)
    {
        ArrayList<Price> res = new ArrayList<>();
        for(String currencyAndAmount : identifiedPrices)
        {
            if(currencyAndAmount.charAt(0) != '-')
                res.add(new Price(Double.parseDouble(currencyAndAmount.substring(1)), currencyAndAmount.charAt(0)));
            else
                res.add(new Price(Double.parseDouble(currencyAndAmount.substring(1)), _cart.get_fromCurrency()));
        }

        return res;
    }

    @Override
    public void OnRetakeImageClick(DialogFragment dialog)
    {
        takePicture();
    }

    @Override
    public void OnChangeSourceCurrenciesClick(Character newCurrency, boolean itemAdditionPending)
    {
        //update objects
        _cart.set_fromCurrency(newCurrency);
        List<Price> newPrices = new ArrayList<>();
        List<Item> items = _cart.GetItems();
        for(Item item : items)
        {
            item.set_fromCurrency(newCurrency);
            newPrices.add(new Price(item.getOriginalPrice(), newCurrency));
        }

        _pricingServices.ConvertPrices(newPrices, _cart.get_fromCurrencyCode(), _cart.get_toCurrencyCode());
        for(int i = 0; i < newPrices.size(); i++)
            items.get(i).setConvertedPrice(newPrices.get(i).getConvertedAmount());
        _cart.RecalculateTotalPrice();

        //save changes to database
        _db.cartDao().updateCart(_cart);
        _db.itemDao().updateItems(items);

        //update UI
        UpdateSourceSymbol();

        //handle description + add new item to cart
        if(itemAdditionPending)
            HandleItemDescription();
        else
            RefreshPricingUIDetails();
    }

    public void OnChangeSourceCurrenciesClick(View view)
    {
        new CurrencySelectionDialogFragment().show(getSupportFragmentManager(), "SelectCurrency" );
    }

    @Override
    public void OnCurrencySelected(Character currencySymbol)
    {
        OnChangeSourceCurrenciesClick(currencySymbol, false);
    }

    @Override
    public void OnPriceSelected(Price price)
    {   //todo - not important...but should change this to passing on the price to the dialogs.
        _pricingServices.ConvertPrices(Arrays.asList(price), price.get_fromCurrencyCode(), _cart.get_toCurrencyCode());
        _originalAmount = price.getOriginalAmount();
        _convertedAmount = price.getConvertedAmount();
        _scannedSourceCurrency = price.getFromCurrencySymbol();
        if(_scannedSourceCurrency != _cart.get_fromCurrency())
        {
            CurrencyConflictDialogFragment currencyConflictDialogFragment = CurrencyConflictDialogFragment.newInstance(_scannedSourceCurrency.toString(), Character.toString(_cart.get_fromCurrency()));
            currencyConflictDialogFragment.show(getSupportFragmentManager(), "ResolveConflict");
            return;
        }

        HandleItemDescription();
    }

    private void HandleItemDescription()
    {
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

