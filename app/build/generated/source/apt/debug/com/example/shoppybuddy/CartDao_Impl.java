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
public class CartDao_Impl implements CartDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter __insertionAdapterOfCart;

  private final EntityDeletionOrUpdateAdapter __deletionAdapterOfCart;

  public CartDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCart = new EntityInsertionAdapter<Cart>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `Cart`(`id`,`_description`) VALUES (nullif(?, 0),?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Cart value) {
        stmt.bindLong(1, value.getId());
        if (value.get_description() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.get_description());
        }
      }
    };
    this.__deletionAdapterOfCart = new EntityDeletionOrUpdateAdapter<Cart>(__db) {
      @Override
      public String createQuery() {
        return "DELETE FROM `Cart` WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Cart value) {
        stmt.bindLong(1, value.getId());
      }
    };
  }

  @Override
  public void insertAll(Cart... carts) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfCart.insert(carts);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public long insert(Cart cart) {
    __db.beginTransaction();
    try {
      long _result = __insertionAdapterOfCart.insertAndReturnId(cart);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(Cart cart) {
    __db.beginTransaction();
    try {
      __deletionAdapterOfCart.handle(cart);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<Cart> getAll() {
    final String _sql = "SELECT * FROM cart";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("id");
      final int _cursorIndexOfDescription = _cursor.getColumnIndexOrThrow("_description");
      final List<Cart> _result = new ArrayList<Cart>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final Cart _item;
        _item = new Cart();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        final String _tmp_description;
        _tmp_description = _cursor.getString(_cursorIndexOfDescription);
        _item.set_description(_tmp_description);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }
}
