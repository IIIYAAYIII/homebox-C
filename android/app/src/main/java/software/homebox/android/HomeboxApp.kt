package software.homebox.android

import android.app.Application
import software.homebox.android.data.prefs.AppPreferences

class HomeboxApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppPreferences.init(this)
    }
}
