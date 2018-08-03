package com.example.shoppybuddy.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Cart
{
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "description")
    private String _description;

    @ColumnInfo(name = "total cost")
    private double _totalCost;

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

    public Cart(char toCurrency)
    {
        this._toCurrency = toCurrency;
    }

    @Ignore
    List<Item> items = new ArrayList<>();
    //List<String> items = Arrays.asList("Shirt   medium   5.7$", "Pants   large   10.50$", "Item3        112");//todo - this will obviously change to an array list of type Item

    public void AddItem(Item item)
    {
        items.add(item);
        _totalCost += item.getConvertedPrice();
    }

    public List<Item> GetItems()
    {
        return items;
    }

    @Override
    public String toString()
    {
        String totalCost = String.format("%.2f", _totalCost);
        if(_description != null)
            return _description + "\n" + "Total: " + totalCost + Character.toString(_toCurrency);
        return super.toString();
    }

    public double get_totalCost()
    {
        return _totalCost;
    }

    public void set_totalCost(double _totalCost)
    {
        this._totalCost = _totalCost;
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

