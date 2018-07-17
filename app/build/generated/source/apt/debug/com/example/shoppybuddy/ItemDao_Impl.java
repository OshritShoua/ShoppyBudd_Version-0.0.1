package com.example.shoppybuddy;

import android.arch.persistence.db.SupportSQLiteStatement;
import android.arch.persistence.room.EntityDeletionOrUpdateAdapter;
import android.arch.persistence.room.EntityInsertionAdapter;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.RoomSQLiteQuery;
import android.database.Cursor;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class ItemDao_Impl implements ItemDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter __insertionAdapterOfItem;

  private final EntityDeletionOrUpdateAdapter __deletionAdapterOfItem;

  public ItemDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfItem = new EntityInsertionAdapter<Item>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `Item`(`_id`,`price`,`description`,`cart id`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Item value) {
        stmt.bindLong(1, value.get_id());
        stmt.bindDouble(2, value.getPrice());
        if (value.get_description() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.get_description());
        }
        stmt.bindLong(4, value.get_cart_id());
      }
    };
    this.__deletionAdapterOfItem = new EntityDeletionOrUpdateAdapter<Item>(__db) {
      @Override
      public String createQuery() {
        return "DELETE FROM `Item` WHERE `_id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Item value) {
        stmt.bindLong(1, value.get_id());
      }
    };
  }

  @Override
  public void insertAll(Item... items) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfItem.insert(items);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(Item item) {
    __db.beginTransaction();
    try {
      __deletionAdapterOfItem.handle(item);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<Item> getAll() {
    final String _sql = "SELECT * FROM item";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("_id");
      final int _cursorIndexOfPrice = _cursor.getColumnIndexOrThrow("price");
      final int _cursorIndexOfDescription = _cursor.getColumnIndexOrThrow("description");
      final int _cursorIndexOfCartId = _cursor.getColumnIndexOrThrow("cart id");
      final List<Item> _result = new ArrayList<Item>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final Item _item;
        final double _tmp_price;
        _tmp_price = _cursor.getDouble(_cursorIndexOfPrice);
        final String _tmp_description;
        _tmp_description = _cursor.getString(_cursorIndexOfDescription);
        final int _tmp_cart_id;
        _tmp_cart_id = _cursor.getInt(_cursorIndexOfCartId);
        _item = new Item(_tmp_price,_tmp_description,_tmp_cart_id);
        final int _tmp_id;
        _tmp_id = _cursor.getInt(_cursorIndexOfId);
        _item.set_id(_tmp_id);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<Item> getItemsByCartId(int id) {
    final String _sql = "SELECT * FROM item WHERE `cart id` = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("_id");
      final int _cursorIndexOfPrice = _cursor.getColumnIndexOrThrow("price");
      final int _cursorIndexOfDescription = _cursor.getColumnIndexOrThrow("description");
      final int _cursorIndexOfCartId = _cursor.getColumnIndexOrThrow("cart id");
      final List<Item> _result = new ArrayList<Item>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final Item _item;
        final double _tmp_price;
        _tmp_price = _cursor.getDouble(_cursorIndexOfPrice);
        final String _tmp_description;
        _tmp_description = _cursor.getString(_cursorIndexOfDescription);
        final int _tmp_cart_id;
        _tmp_cart_id = _cursor.getInt(_cursorIndexOfCartId);
        _item = new Item(_tmp_price,_tmp_description,_tmp_cart_id);
        final int _tmp_id;
        _tmp_id = _cursor.getInt(_cursorIndexOfId);
        _item.set_id(_tmp_id);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }
}
