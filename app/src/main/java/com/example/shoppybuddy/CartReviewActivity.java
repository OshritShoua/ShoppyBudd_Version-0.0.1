package com.example.shoppybuddy;

import android.Manifest;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
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
import java.util.Locale;

public class CartReviewActivity extends AppCompatActivity implements RecaptureImageDialogFragment.RecaptureImageDialogListener, DescriptionDialogFragment.DescriptionDialogListener,
        PriceSelectionDialogFragment.PriceSelectionDialogListener, CurrencyConflictDialogFragment.CurrencyConflictDialogListener, CurrencySelectionDialogFragment.CurrencySelectionDialogListener, AdapterView.OnItemClickListener
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
    private static AppDataBase _db;
    private Uri capturedImageUri;
    private TextView scanResults;
    private Character _scannedSourceCurrency;
    private int _itemIdPendingDiscount;
    private boolean _activityInCreationProcess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _activityInCreationProcess = true;
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
                DescriptionDialogFragment cartDescriptionDialogFragment = DescriptionDialogFragment.newInstance("", DescriptionDialogFragment.DialogPurpose.cartDescription);
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

        registerForContextMenu(findViewById(R.id._dynamic_item_list));
        _pricingServices = new PricingServices();
        _ocrServices = new OCRServices();
    }

    public static AppDataBase GetAppDb(){return _db;}

    @Override
    protected void onResume()
    {
        if(!_activityInCreationProcess)
        {
            _cart = _db.cartDao().getCartById(_cart.getId());   //cart may have changed from other activity
            _cart.GetItems().clear();
            _cart.GetItems().addAll(_db.itemDao().getItemsByCartId(_cart.getId()));
        }

        _activityInCreationProcess = false;
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
            symbolButton.setText(Character.toString(symbol));
        else
            symbolButton.setText(R.string.CurrencyNotSelectedString);
    }

    private void addDiscount(long id)
    {
        _itemIdPendingDiscount = (int)id;
        Item item = _cart.GetItems().get(_itemIdPendingDiscount);
        DescriptionDialogFragment descriptionDialogFragment = DescriptionDialogFragment.newInstance(item.GetDiscountFormattedString(), DescriptionDialogFragment.DialogPurpose.itemDiscount );
        descriptionDialogFragment.show(getSupportFragmentManager(), "getDiscount");
    }

    @Override
    public void OnItemDiscountDone(String discountStr, boolean isDiscountInPercents)
    {
        Double discount = Double.parseDouble(discountStr);
        Item item = _cart.GetItems().get(_itemIdPendingDiscount);
        double prevAfterDiscountPrice = item.getAfterDiscountPrice();
        double prevConvertedPrice = item.getConvertedPrice();
        item.setDiscount(discount, isDiscountInPercents);
        _cart.set_totalDestCost(_cart.get_totalDestCost() - prevConvertedPrice + item.getConvertedPrice());
        _cart.set_totalSrcCost(_cart.get_totalSrcCost() - prevAfterDiscountPrice + item.getAfterDiscountPrice());
        _db.itemDao().updateItem(item);
        _db.cartDao().updateCart(_cart);
        RefreshPricingUIDetails();
    }

    private void deleteItem(long id)
    {
        ListView items = findViewById(R.id._dynamic_item_list);
        Item item = (Item)items.getAdapter().getItem((int)id);
        _cart.GetItems().remove((int)id);
        _cart.set_totalDestCost(_cart.get_totalDestCost() - item.getConvertedPrice());
        _cart.set_totalSrcCost(_cart.get_totalSrcCost() - item.getAfterDiscountPrice());
        _db.itemDao().delete(item);
        _db.cartDao().updateCart(_cart);
        RefreshPricingUIDetails();
    }

    private void InitializeItemListView()
    {
        ArrayAdapter<Item> adapter = new ArrayAdapter<Item>(this, android.R.layout.simple_list_item_1, _cart.GetItems())
        {
            @NonNull
            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                View view = super.getView(position, convertView, parent);
                if(position % 3 == 0)
                    view.setBackgroundColor(Color.parseColor("#efedf5"));
                else if(position % 3 == 1)
                    view.setBackgroundColor(Color.parseColor("#bcbddc"));
                else
                    view.setBackgroundColor(Color.parseColor("#756bb1"));

                return view;
            }
        };
        ListView itemListView = findViewById(R.id._dynamic_item_list);
        itemListView.setAdapter(adapter);
        itemListView.setOnItemClickListener(this);
        itemListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                showMenu(view, i);
                return true;
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
    {
        Intent intent = new Intent(CartReviewActivity.this, ItemDetailsActivity.class);
        intent.putExtra("selectedItemIndex", i);
        intent.putExtra("cart", _cart);
        startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void showMenu (View view, final int selectedItem)
    {
        PopupMenu menu = new PopupMenu (this, view);
        menu.setOnMenuItemClickListener (new PopupMenu.OnMenuItemClickListener ()
        {
            @Override
            public boolean onMenuItemClick (MenuItem item)
            {
                switch (item.getItemId()) {
                    case R.id.add_discount:
                        addDiscount(selectedItem);
                        return true;
                    case R.id.delete_item:
                        deleteItem(selectedItem);
                        return true;
                    default:
                        return true;
                }
            }
        });
        menu.inflate (R.menu.item_selection_menu);
        menu.show();
    }

    private void UpdateSumUI()
    {
        TextView totalDestSumTextView = findViewById(R.id.totalDestSumText);
        TextView totalSrcSumTextView = findViewById(R.id.totalSrcSumText);
        char srcCurrency = _cart.get_fromCurrency();
        char destCurrency = _cart.get_toCurrency();
        if(srcCurrency != 0)
        {
            String totalPrice = String.format(Locale.getDefault(),"%.2f%c", _cart.get_totalSrcCost(), srcCurrency);
            totalSrcSumTextView.setText(totalPrice);
        }
        else
            totalSrcSumTextView.setText("");

        if(destCurrency != 0)
        {
            String totalPrice = String.format(Locale.getDefault(), "%.2f%c", _cart.get_totalDestCost(), destCurrency);
            totalDestSumTextView.setText(totalPrice);
        }
        else
            totalDestSumTextView.setText("");
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
        ListView view = findViewById(R.id._dynamic_item_list);
        ArrayAdapter adapter = (ArrayAdapter<Item>)view.getAdapter();
        adapter.notifyDataSetChanged();
        UpdateSumUI();
    }

    @Override
    public void OnCartDescriptionDone(String description)
    {
        _cart.setId((int)(_db.cartDao().insert(_cart)));
        if(description == null)
            description = "My shopping cart #" + Integer.toString(_cart.getId());

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
    public void OnEnterPriceManuallyRequest()
    {
        DescriptionDialogFragment dialog = DescriptionDialogFragment.newInstance("", DescriptionDialogFragment.DialogPurpose.enterPriceManually);
        dialog.show(getSupportFragmentManager(), "PriceEnter");
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
    {
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
            DescriptionDialogFragment itemDescriptionDialogFragment = DescriptionDialogFragment.newInstance("", DescriptionDialogFragment.DialogPurpose.itemDescription);
            itemDescriptionDialogFragment.show(getSupportFragmentManager(), "GetDescription");
        }
        else
        {
            OnItemDescriptionDone("Item #" + Integer.toString(_cart.GetItems().size() + 1));
        }
    }

    @Override
    public void OnPriceEnterDone(String price)
    {
        double amount = Double.parseDouble(price);
        OnPriceSelected(new Price(amount, _cart.get_fromCurrency()));
    }
}