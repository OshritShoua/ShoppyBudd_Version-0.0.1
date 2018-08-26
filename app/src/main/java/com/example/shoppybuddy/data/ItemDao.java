package com.example.shoppybuddy.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.example.shoppybuddy.entities.Item;

import java.util.List;

@Dao
public interface ItemDao
{
    @Query("SELECT * FROM item")
    List<Item> getAll();

    @Query("SELECT * FROM item WHERE `cart id` = :id")
    List<Item> getItemsByCartId(int id);

    @Insert
    void insertAll(Item... items);

    @Delete
    void delete(Item item);

    @Update
    void updateItems(List<Item> items);
}

