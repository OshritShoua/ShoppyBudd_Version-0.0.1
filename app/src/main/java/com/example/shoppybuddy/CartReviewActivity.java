package com.example.shoppybuddy;

import android.arch.persistence.room.Room;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;

public class CartReviewActivity extends AppCompatActivity implements RecaptureImageDialogFragment.RecaptureImageDialogListener, DescriptionDialogFragment.DescriptionDialogListener
{
    Cart _cart;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private PricingServices _pricingServices;
    private double _convertedPrice;
    private String _description;
    private AppDataBase _db;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_review);
        init(getIntent().getStringExtra("calling activity"));
    }

    //todo - importnt. The switch to this CartReviewActivity will be from 2 places: 1. When starting a new cart.
    //todo 2. When entering an existing cart - in which case the items should be fetched from the db.
    //todo - This implementation is now for a new EMPTY cart only.
    private void init(String callingActivity)
    {
        _cart = new Cart();
        _db = Room.databaseBuilder(getApplicationContext(), AppDataBase.class, "userShoppings").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        if(callingActivity.equals(MainActivity.class.getSimpleName())) //todo - add a remark to notes about having had to move the "if else" with the database logic before creating the adapter
        {
            _cart.setId((int)(_db.cartDao().insert(_cart)));
        }
        else
        {
            //todo - this logic needs to change to getting the cart by the id from the db..that will give us the description too
            int id = getIntent().getIntExtra("cart id", -1);
            _cart.setId(id);
            _cart.items.addAll(_db.itemDao().getItemsByCartId(id));
        }

        _pricingServices = new PricingServices(this);
        ArrayAdapter<Item> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, _cart.items);
        ListView itemListView = findViewById(R.id._dynamic_item_list);
        itemListView.setAdapter(adapter);
        if(SettingsPrefActivity.ShouldRequestCartDescription())
        {
            DescriptionDialogFragment cartDescriptionDialogFragment = DescriptionDialogFragment.newInstance("moo", DescriptionDialogFragment.DialogPurpose.cartDescription);
            cartDescriptionDialogFragment.show(getSupportFragmentManager(), "GetDescription");
        }
    }

    public void OnCameraButtonClick(View view) throws InterruptedException, IOException
    {
        StartImageCapture();
    }

    private void StartImageCapture() {
        File imageFile = new File(PricingServices.GetImageFilePath());
        Uri outputFileUri = Uri.fromFile(imageFile);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); //create an intent to start an image capturing activity
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        if (intent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK)
        {
            handleCapturedImage();
        }
        else
        {
            return;
        }
    }

    private void handleCapturedImage()
    {
        double[] priceHolder = new double[1];
        if (!_pricingServices.TryGetConvertedPrice(priceHolder))
        {
            RecaptureImageDialogFragment recaptureImageDialogFragment = new RecaptureImageDialogFragment();
            recaptureImageDialogFragment.show(getSupportFragmentManager(), "RetakeImage");
        }
        else
        {
            _convertedPrice = priceHolder[0];
            if(SettingsPrefActivity.ShouldRequestItemDescription())
            {
                DescriptionDialogFragment itemDescriptionDialogFragment = DescriptionDialogFragment.newInstance("moo", DescriptionDialogFragment.DialogPurpose.itemDescription);
                itemDescriptionDialogFragment.show(getSupportFragmentManager(), "GetDescription");
            }
            else
                OnItemDescriptionDone("Item #" + Integer.toString(_cart.items.size() + 1));
        }
    }

    public void OnItemDescriptionDone(String description)
    {
        _description = description;
        Item item = new Item(_convertedPrice, _description, _cart.getId());
        _cart.AddItem(item);
        _db.itemDao().insertAll(item);
    }

    @Override
    public void OnCartDescriptionDone(String description)
    {
        TextView textView = findViewById(R.id.textview_cart_name);
        textView.setText(description);
    }

    @Override
    public void onRetakeImageClick(DialogFragment dialog) {
        StartImageCapture();
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
}


