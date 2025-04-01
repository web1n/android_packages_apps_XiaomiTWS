package org.lineageos.xiaomi_tws.activity

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.collapsingtoolbar.R
import org.lineageos.xiaomi_tws.fragments.EarbudsInfoFragment
import org.lineageos.xiaomi_tws.fragments.EarbudsListFragment

class EarbudsActivity : CollapsingToolbarBaseActivity() {

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
            FRAGMENTS.getOrDefault(activityName, EarbudsListFragment::class.java)
        if (DEBUG) Log.d(TAG, "createFragment: class: $fragmentClass")

        try {
            return fragmentClass.getConstructor().newInstance() as Fragment
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create fragment: $fragmentClass", e)
        }
    }

    companion object {
        private val TAG = EarbudsActivity::class.java.simpleName
        private const val DEBUG = true

        private val FRAGMENTS = mapOf<String, Class<out Fragment>>(
            "org.lineageos.xiaomi_tws.activity.EarbudsInfoActivity" to EarbudsInfoFragment::class.java
        )

        private const val TAG_EARBUDS = "earbuds"
    }
}
