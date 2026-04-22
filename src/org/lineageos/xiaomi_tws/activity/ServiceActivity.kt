package org.lineageos.xiaomi_tws.activity

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.collapsingtoolbar.R
import org.lineageos.xiaomi_tws.fragments.AutoConnectDeviceFragment
import org.lineageos.xiaomi_tws.fragments.AutoSwitchDeviceFragment
import org.lineageos.xiaomi_tws.fragments.DeviceConfigFragment
import org.lineageos.xiaomi_tws.fragments.DeviceListFragment
import org.lineageos.xiaomi_tws.fragments.ServiceFragment

class ServiceActivity : CollapsingToolbarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, createFragment(), TAG_EARBUDS).commit()
        }
    }

    private fun createFragment(): Fragment {
        val activityName = packageManager.resolveActivity(
            intent.apply { setPackage(packageName) },
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.name

        val fragmentClass: Class<*> =
            FRAGMENTS.getOrDefault(activityName, ServiceFragment::class.java)
        if (DEBUG) Log.d(TAG, "createFragment: class: $fragmentClass")

        try {
            return fragmentClass.getConstructor().newInstance() as Fragment
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create fragment: $fragmentClass", e)
        }
    }

    companion object {
        private val TAG = ServiceActivity::class.java.simpleName
        private const val DEBUG = true

        private val FRAGMENTS = mapOf<String, Class<out Fragment>>(
            "org.lineageos.xiaomi_tws.activity.AutoConnectDeviceActivity" to AutoConnectDeviceFragment::class.java,
            "org.lineageos.xiaomi_tws.activity.AutoSwitchDeviceActivity" to AutoSwitchDeviceFragment::class.java,
            "org.lineageos.xiaomi_tws.activity.DeviceConfigActivity" to DeviceConfigFragment::class.java,
            "org.lineageos.xiaomi_tws.activity.DeviceListActivity" to DeviceListFragment::class.java,
        )

        private const val TAG_EARBUDS = "earbuds"
    }
}
