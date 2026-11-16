package com.aiutil.videoplayer

import android.app.Application
import com.aiutil.videoplayer.data.BookmarkStore
import com.aiutil.videoplayer.data.PlaybackHistoryStore
import com.aiutil.videoplayer.data.SettingsStore
import java.io.File

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsStore(this)
        history = PlaybackHistoryStore(this)
        bookmarks = BookmarkStore(File(filesDir, "bookmarks.json"))
    }

    companion object {
        lateinit var instance: App
            private set

        lateinit var settings: SettingsStore
            private set

        lateinit var history: PlaybackHistoryStore
            private set

        lateinit var bookmarks: BookmarkStore
            private set
    }
}
