package com.example.shoppybuddy;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class Item
{
    @PrimaryKey(autoGenerate = true)
    private int _id;
    @ColumnInfo(name = "price")
    private double _price;
    @ColumnInfo(name = "description")
    private String _description;
    @ColumnInfo(name = "cart id")
    private int _cart_id;

    public Item(double price, String description, int cart_id)
    {
        _price = price;
        _description = description;
        _cart_id = cart_id;
    }

    @Override
    public String toString()
    {
        return _description + " " + Double.toString(_price);
    }

    public int get_id()
    {
        return _id;
    }

    public void set_id(int _id)
    {
        this._id = _id;
    }

    public double getPrice()
    {
        return _price;
    }

    public void setPrice(double price)
    {
        _price = price;
    }

    public String get_description()
    {
        return _description;
    }

    public void set_description(String desc)
    {
        _description = desc;
    }

    public int get_cart_id()
    {
        return _cart_id;
    }

    public void set_cart_id(int _cart_id)
    {
        this._cart_id = _cart_id;
    }
}
