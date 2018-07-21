package com.example.shoppybuddy.entities;

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

    private String _description;

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

    @Ignore
    List<Item> items = new ArrayList<>();
    //List<String> items = Arrays.asList("Shirt   medium   5.7$", "Pants   large   10.50$", "Item3        112");//todo - this will obviously change to an array list of type Item

    public void AddItem(double originalPrice, double convertedPrice, String itemDesription, int cart_id)
    {
        items.add(new Item(originalPrice, convertedPrice, itemDesription, cart_id));
    }

    public void AddItem(Item item)
    {
        items.add(item);
    }

    public List<Item> GetItems()
    {
        return items;
    }
}

