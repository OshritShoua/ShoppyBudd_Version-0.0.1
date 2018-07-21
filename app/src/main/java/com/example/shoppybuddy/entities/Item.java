package com.example.shoppybuddy.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class Item
{
    @PrimaryKey(autoGenerate = true)
    private int _id;
    @ColumnInfo(name = "original price")
    private double _originalPrice;
    @ColumnInfo(name = "converted price")
    private double _convertedPrice;
    @ColumnInfo(name = "description")
    private String _description;
    @ColumnInfo(name = "cart id")
    private int _cart_id;

    public Item(double originalPrice, double convertedPrice, String description, int cart_id)
    {
        _originalPrice = originalPrice;
        _convertedPrice = convertedPrice;
        _description = description;
        _cart_id = cart_id;
    }

    @Override
    public String toString()
    {
        return _description + " " + Double.toString(_originalPrice) + " " + Double.toString(_convertedPrice);
    }

    public int get_id()
    {
        return _id;
    }

    public void set_id(int _id)
    {
        this._id = _id;
    }

    public double getOriginalPrice()
    {
        return _originalPrice;
    }

    public void setOriginalPrice(double price)
    {
        _originalPrice = price;
    }

    public double getConvertedPrice()
    {
        return _convertedPrice;
    }

    public void setConvertedPrice(double price)
    {
        _convertedPrice = price;
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
