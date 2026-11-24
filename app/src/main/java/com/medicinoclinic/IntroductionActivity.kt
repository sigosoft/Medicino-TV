package com.medicinoclinic

import android.app.AppOpsManager
import android.app.UiModeManager
import android.app.usage.UsageStatsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.medicinoclinic.home.HomeActivity1
import com.medicinoclinic.utils.BaseClass
import com.medicinoclinic.workmamager.DoctorWorker
import java.util.concurrent.TimeUnit

class IntroductionActivity : AppCompatActivity() {
    var baseClass = BaseClass()
    var login_status: String? = "false"
    var btn_drlogin: Button? = null
    var btn_cliniclogin: Button? = null
    var btn_pharmacylogin: Button? = null
    var btn_lablogin: Button? = null
    private var permissionDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_introduction)

        btn_drlogin = findViewById(R.id.btn_drlogin)
        btn_cliniclogin = findViewById(R.id.btn_cliniclogin)
        btn_pharmacylogin = findViewById(R.id.btn_pharmacylogin)
        btn_lablogin = findViewById(R.id.btn_lablogin)

        // Fix for blank screen on phone: Programmatically remove margins if not on TV
        adjustLayoutForDevice()

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("FCM_TOKEN", token)
                }
            }

        scheduleWork("Doctors")

        btn_drlogin!!.setOnClickListener {
            val intent = Intent(this, DoctorLoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        btn_cliniclogin!!.setOnClickListener {
            val intent = Intent(this, ClinicLoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        btn_pharmacylogin!!.setOnClickListener {
            val intent = Intent(this, PharmacyLoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        btn_lablogin!!.setOnClickListener {
            val intent = Intent(this, LabLoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        login_status = baseClass.getSharedPreferance(applicationContext, "login_status", "false")

        if (login_status != "false") {
            val intent = Intent(this, HomeActivity1::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Check permission in onResume so the dialog dismisses automatically when user returns from settings
        if (isUsageStatsPermissionGranted()) {
            permissionDialog?.dismiss()
        } else if (!usageAccessPromptDismissed()) {
            showPermissionDialog()
        }
    }

    /**
     * QA-02: the dialog is only for optional data-usage reporting. Once the user has said Cancel
     * (or Back), or this TV turned out to have no Usage Access screen, don't ask again on every
     * launch. Stored per device, so logging out doesn't bring it back.
     */
    private fun usageAccessPromptDismissed(): Boolean =
        baseClass.getSharedPreferance(applicationContext, PREF_USAGE_PROMPT_DISMISSED, "false") == "true"

    private fun rememberUsageAccessPromptDismissed() {
        baseClass.setSharedPreferance(applicationContext, PREF_USAGE_PROMPT_DISMISSED, "true")
    }

    private fun showPermissionDialog() {
        if (permissionDialog == null || !permissionDialog!!.isShowing) {
            permissionDialog = AlertDialog.Builder(this@IntroductionActivity)
                .setTitle("Permission Required")
                .setMessage("To monitor your app's data usage, please grant Usage Access permission.")
                // Back closes it, same as Cancel.
                .setCancelable(true)
                .setOnCancelListener { rememberUsageAccessPromptDismissed() }
                .setPositiveButton("Grant") { _, _ -> openUsageAccessSettings() }
                .setNegativeButton("Cancel") { _, _ -> rememberUsageAccessPromptDismissed() }
                .show()
            // Start with GRANT focused so the first OK on the remote does something.
            permissionDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.requestFocus()
        }
    }

    /**
     * QA-02: many Android TV / Google TV / Fire TV devices have no Usage Access screen, and
     * starting it there crashed the app. Open it where it exists; otherwise explain and carry on.
     */
    private fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w("USAGE", "No Usage Access screen on this device")
            rememberUsageAccessPromptDismissed()
            Toast.makeText(this, R.string.usage_access_unavailable, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("USAGE", "Could not open Usage Access settings", e)
            rememberUsageAccessPromptDismissed()
            Toast.makeText(this, R.string.usage_access_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val PREF_USAGE_PROMPT_DISMISSED = "usage_access_prompt_dismissed"
    }

    private fun adjustLayoutForDevice() {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.currentModeType != Configuration.UI_MODE_TYPE_TELEVISION) {
            val rootView = findViewById<View>(android.R.id.content)
            rootView.post {
                try {
                    findAndAdjustMargins(rootView as ViewGroup)
                } catch (e: Exception) {
                    Log.e("IntroductionActivity", "Layout adjustment failed", e)
                }
            }
        }
    }

    private fun findAndAdjustMargins(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            val params = child.layoutParams as? ViewGroup.MarginLayoutParams
            if (params != null && (params.marginStart > 100 || params.leftMargin > 100)) {
                params.setMargins(0, 0, 0, 0)
                child.layoutParams = params
            }
            if (child is ViewGroup) {
                findAndAdjustMargins(child)
            }
        }
    }

    private fun isUsageStatsPermissionGranted(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }

        if (mode == AppOpsManager.MODE_ALLOWED) return true

        // Definitive check: try to query usage stats. If it returns data, permission is granted.
        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 3600 * 24, time)
            !stats.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun scheduleWork(tag: String?) {
        val photoCheckBuilder = PeriodicWorkRequest.Builder(
            DoctorWorker::class.java, 30, TimeUnit.MINUTES
        )
        val request = photoCheckBuilder.build()
        WorkManager.getInstance()
            .enqueueUniquePeriodicWork(tag!!, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
