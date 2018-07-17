package com.example.shoppybuddy;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {Cart.class, Item.class}, version = 2)
public abstract class AppDataBase extends RoomDatabase
{
    abstract public CartDao cartDao();
    abstract public ItemDao itemDao();
}
