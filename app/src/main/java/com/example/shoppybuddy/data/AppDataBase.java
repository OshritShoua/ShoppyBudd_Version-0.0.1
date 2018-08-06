package com.example.shoppybuddy.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.example.shoppybuddy.entities.Cart;
import com.example.shoppybuddy.entities.Item;

@Database(entities = {Cart.class, Item.class}, version = 6, exportSchema = false)   //todo - I added this 'false' to get rid of a warning. make sure it doesn't break anything (ot shouldnt) and then remove this comment
public abstract class AppDataBase extends RoomDatabase
{
    abstract public CartDao cartDao();
    abstract public ItemDao itemDao();
}
