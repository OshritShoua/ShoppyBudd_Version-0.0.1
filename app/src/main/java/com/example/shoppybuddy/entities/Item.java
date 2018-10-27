package com.example.shoppybuddy.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import java.io.Serializable;
import java.util.Locale;

@Entity
public class Item implements Serializable
{
    @PrimaryKey(autoGenerate = true)
    private int _id;
    @ColumnInfo(name = "original price")
    private double _originalPrice;
    @ColumnInfo(name = "after discount price")
    private double _afterDiscountPrice;
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
    @ColumnInfo(name = "discount")
    private double _discount;
    @ColumnInfo(name = "is discount in percents")
    private boolean _isDiscountInPercents;
    @ColumnInfo(name = "dest to src ratio")
    private double _destToSourceRatio;

    public Item(double originalPrice, double convertedPrice, String description, int cart_id, char fromCurrency, char toCurrency)
    {
        _originalPrice = originalPrice;
        _afterDiscountPrice = _originalPrice;
        _convertedPrice = convertedPrice;
        _destToSourceRatio = _afterDiscountPrice / _convertedPrice;
        _description = description;
        _cart_id = cart_id;
        _fromCurrency = fromCurrency;
        _toCurrency = toCurrency;
    }

    public double getDestToSourceRatio(){return _destToSourceRatio;}
    public void setDestToSourceRatio(double ratio){_destToSourceRatio = ratio;}

    @Override
    public String toString()
    {
        String afterDiscountPrice = String.format(Locale.getDefault(),"%.2f", _afterDiscountPrice);
        String convertedPrice = String.format(Locale.getDefault(),"%.2f", _convertedPrice);
        return _description + "\n" + "Paid: " + afterDiscountPrice + Character.toString(_fromCurrency) + "\n" +
                "Converted: " + convertedPrice + Character.toString(_toCurrency);
    }

    public String GetOriginalPriceFormattedString()
    {
        return String.format(Locale.getDefault(),"%.2f", _originalPrice);
    }

    public String GetAfterDiscountPriceFormattedString()
    {
        String AfterDiscountPrice = String.format(Locale.getDefault(),"%.2f", _afterDiscountPrice);
        return AfterDiscountPrice + Character.toString(_fromCurrency);
    }

    public String GetConvertedPriceFormattedString()
    {
        String convertedPrice = String.format(Locale.getDefault(),"%.2f", _convertedPrice);
        return convertedPrice + Character.toString(_toCurrency);
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

    public double getAfterDiscountPrice()
    {
        return _afterDiscountPrice;
    }

    public void setAfterDiscountPrice(double _afterDiscountPrice)
    {
        this._afterDiscountPrice = _afterDiscountPrice;
    }

    public double getDiscount()
    {
        return _discount;
    }

    public boolean getIsDiscountInPercents(){return _isDiscountInPercents;}

    public void setIsDiscountInPercents(boolean isInPercents){_isDiscountInPercents = isInPercents;}

    public String GetDiscountFormattedString()
    {
        if(_discount == 0)
            return null;

        double discountAmount, discountPercents;
        if(_isDiscountInPercents)
        {
            discountPercents = _discount;
            discountAmount = (discountPercents / 100) * _originalPrice;
        }
        else
        {
            discountAmount = _discount;
            discountPercents = (_discount / _originalPrice) * 100;
        }
        return String.format(Locale.getDefault(), "%.2f%c | %.2f%%", discountAmount, _fromCurrency, discountPercents);
    }

    public void setOriginalPrice(double price)
    {
        _originalPrice = price;
        UpdateDerivedPrices();
    }

    private void UpdateDerivedPrices()
    {
        double discountAmount;
        if(_isDiscountInPercents)
        {
            discountAmount = (_discount / 100) * _originalPrice;
        }
        else
            discountAmount = _discount;

        _afterDiscountPrice = _originalPrice - discountAmount;
        if(_afterDiscountPrice < 0)
        {
            _afterDiscountPrice = 0;
            _convertedPrice = 0;
        }
        else
            _convertedPrice = _afterDiscountPrice / _destToSourceRatio;
    }

    public void setDiscount(double discount, boolean isDiscountInPercents)
    {
        _isDiscountInPercents = isDiscountInPercents;
        _discount = discount;
        UpdateDerivedPrices();
    }

    public void setDiscount(double discount)
    {
        _discount = discount;
    }
}