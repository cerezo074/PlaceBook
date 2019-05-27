package com.example.eli.placebook.Repository

import android.arch.lifecycle.LiveData
import android.content.Context
import com.example.eli.placebook.DB.BookmarkDao
import com.example.eli.placebook.DB.PlaceBookDatabase
import com.example.eli.placebook.Model.Bookmark

class BookmarkRepo(context: Context) {

    private var db = PlaceBookDatabase.getInstance(context)
    private var bookmarkDao: BookmarkDao = db.bookmarkDao()

    fun addBookmark(bookmark: Bookmark): Long? {
        val newId = bookmarkDao.insertBookmark(bookmark)
        bookmark.id = newId
        return newId
    }

    fun createBookmark(): Bookmark {
        return Bookmark()
    }

    val allBookmarks: LiveData<List<Bookmark>>
    get() {
        return bookmarkDao.loadAll()
    }

}