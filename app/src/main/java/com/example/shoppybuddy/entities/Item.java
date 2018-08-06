package com.example.shoppybuddy.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.text.DecimalFormat;

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
    @ColumnInfo(name = "from currency")
    private char _fromCurrency;
    @ColumnInfo(name = "to currency")
    private char _toCurrency;

    public Item(double originalPrice, double convertedPrice, String description, int cart_id, char fromCurrency, char toCurrency)
    {
        _originalPrice = originalPrice;
        _convertedPrice = convertedPrice;
        _description = description;
        _cart_id = cart_id;
        _fromCurrency = fromCurrency;
        _toCurrency = toCurrency;
    }

    @Override
    public String toString()
    {
        String originalPrice = String.format("%.2f", _originalPrice);
        String convertedPrice = String.format("%.2f", _convertedPrice);
        return _description + "\n" + "Original: " + originalPrice + Character.toString(_fromCurrency) + "\n" +
                "Converted: " + convertedPrice + Character.toString(_toCurrency);
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

    public char get_fromCurrency()
    {
        return _fromCurrency;
    }

    public void set_fromCurrency(char _fromCurrency)
    {
        this._fromCurrency = _fromCurrency;
    }

    public char get_toCurrency()
    {
        return _toCurrency;
    }

    public void set_toCurrency(char _toCurrency)
    {
        this._toCurrency = _toCurrency;
    }
}
