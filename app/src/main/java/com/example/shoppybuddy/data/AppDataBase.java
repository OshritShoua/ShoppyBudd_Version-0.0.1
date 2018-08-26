package com.example.shoppybuddy.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.example.shoppybuddy.entities.Cart;
import com.example.shoppybuddy.entities.Item;

@Database(entities = {Cart.class, Item.class}, version = 7, exportSchema = false)
public abstract class AppDataBase extends RoomDatabase
{
    abstract public CartDao cartDao();
    abstract public ItemDao itemDao();
}
