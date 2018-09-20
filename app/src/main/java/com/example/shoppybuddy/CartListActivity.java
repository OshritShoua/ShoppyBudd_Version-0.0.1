package com.example.shoppybuddy;

import android.arch.persistence.room.Room;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.example.shoppybuddy.data.AppDataBase;
import com.example.shoppybuddy.entities.Cart;
import java.util.ArrayList;
import java.util.List;

public class CartListActivity extends AppCompatActivity
{
    private AppDataBase _db;
    private List<Cart> _carts;
    ArrayAdapter<Cart> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_list);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        init();
    }

    private void init()
    {
        if(_db == null)
            _db = Room.databaseBuilder(getApplicationContext(), AppDataBase.class, "userShoppings").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        if(_carts == null)
            _carts = new ArrayList<>();
        _carts.clear();
        _carts.addAll(_db.cartDao().getAll());
        if(adapter == null)
            adapter = new ArrayAdapter<Cart>(this, android.R.layout.simple_list_item_1, _carts)
            {
                @NonNull
                @Override
                public View getView(int position, View convertView, ViewGroup parent)
                {
                    View view = super.getView(position, convertView, parent);
                    if(position % 3 == 0)
                        view.setBackgroundColor(Color.parseColor("#deebf7"));
                    else if(position % 3 == 1)
                        view.setBackgroundColor(Color.parseColor("#9ecae1"));
                    else
                        view.setBackgroundColor(Color.parseColor("#3182bd"));
                    return view;
                }
            };
        final ListView cartListView = findViewById(R.id._dynamic_cart_list);
        cartListView.setAdapter(adapter);
        cartListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l)
            {
                Cart selectedCart = (Cart)adapterView.getItemAtPosition(position);
                Intent intent = new Intent(CartListActivity.this, CartReviewActivity.class);
                intent.putExtra("calling activity", getLocalClassName());
                intent.putExtra("cart id", selectedCart.getId());
                intent.putExtra("num carts", _carts.size());
                startActivity(intent);
            }
        });
    }
}