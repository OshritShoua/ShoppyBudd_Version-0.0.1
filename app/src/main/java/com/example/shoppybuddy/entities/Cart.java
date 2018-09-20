package com.example.shoppybuddy.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import com.example.shoppybuddy.services.OCRServices;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Cart implements Serializable
{
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "description")
    private String _description;

    @ColumnInfo(name = "total dest cost")
    private double _totalDestCost;

    @ColumnInfo(name = "total src cost")
    private double _totalSrcCost;

    @ColumnInfo(name = "from currency")
    private char _fromCurrency;

    @ColumnInfo(name = "to currency")
    private char _toCurrency;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String get_description()
    {
        return _description;
    }

    public void set_description(String _description)
    {
        this._description = _description;
    }

    public Cart(char fromCurrency, char toCurrency)
    {
        _fromCurrency = fromCurrency;
        _toCurrency = toCurrency;
    }

    @Ignore
    List<Item> items = new ArrayList<>();

    public void AddItem(Item item)
    {
        items.add(item);
        _totalSrcCost += item.getOriginalPrice();
        _totalDestCost += item.getConvertedPrice();
    }

    public List<Item> GetItems()
    {
        return items;
    }

    public void RecalculateTotalPrice()
    {
        _totalSrcCost = 0;
        _totalDestCost = 0;
        for(Item item : items)
        {
            _totalSrcCost += item.getAfterDiscountPrice();
            _totalDestCost += item.getConvertedPrice();
        }

    }

    @Override
    public String toString()
    {
        String totalCost = String.format("%.2f", _totalDestCost);
        if(_description != null)
            return _description + "\n" + "Total: " + totalCost + Character.toString(_toCurrency);
        return super.toString();
    }

    public double get_totalDestCost()
    {
        return _totalDestCost;
    }

    public void set_totalDestCost(double _totalDestCost)
    {
        this._totalDestCost = _totalDestCost;
    }

    public double get_totalSrcCost()
    {
        return _totalSrcCost;
    }

    public void set_totalSrcCost(double _totalSrcCost)
    {
        this._totalSrcCost = _totalSrcCost;
    }

    public char get_toCurrency()
    {
        return _toCurrency;
    }

    public void set_toCurrency(char _toCurrency)
    {
        this._toCurrency = _toCurrency;
    }

    public String get_toCurrencyCode()
    {
        return OCRServices.getSymbolsToCodesMapping().get(_toCurrency);
    }

    public String get_fromCurrencyCode()
    {
        return OCRServices.getSymbolsToCodesMapping().get(_fromCurrency);
    }

    public char get_fromCurrency()
    {
        return _fromCurrency;
    }

    public void set_fromCurrency(char _fromCurrency)
    {
        this._fromCurrency = _fromCurrency;
    }
}