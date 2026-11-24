package com.medicinoclinic.utils

import RetrofitClient
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import com.medicinoclinic.ClinicLoginActivity
import com.medicinoclinic.DoctorLoginActivity
import com.medicinoclinic.LabLoginActivity
import com.medicinoclinic.PharmacyLoginActivity
import com.medicinoclinic.R
import com.medicinoclinic.SettingsActivity

/**
 * Handles an expired or rejected TV login (bug report L6).
 *
 * Any logged-in API call that comes back HTTP 401 ends up here (see RetrofitClient.getClient1).
 * The saved session is cleared and the TV goes to the login screen for its account type,
 * instead of sitting on a blank token screen.
 */
object SessionManager {

    private const val TAG = "SessionManager"

    /** Several calls usually fail together; handle the first and ignore the rest briefly. */
    private const val DEBOUNCE_MS = 5_000L

    @Volatile
    private var lastHandledAt = 0L

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Called from OkHttp's background thread. [rejectedToken] is the bearer the server refused:
     * if the TV has logged in again since that request was sent, the 401 is stale and ignored.
     */
    fun onUnauthorized(context: Context, rejectedToken: String?) {
        val appContext = context.applicationContext
        val baseClass = BaseClass()
        val currentToken = baseClass.getSharedPreferance(appContext, "token", "")
        val loginStatus = baseClass.getSharedPreferance(appContext, "login_status", "false")
        if (loginStatus != "true" || currentToken.isNullOrBlank()) return
        if (!rejectedToken.isNullOrBlank() && rejectedToken != currentToken) return

        synchronized(this) {
            val now = SystemClock.elapsedRealtime()
            if (lastHandledAt != 0L && now - lastHandledAt < DEBOUNCE_MS) return
            lastHandledAt = now
        }

        Log.w(TAG, "Server rejected the TV login (HTTP 401); returning to login")
        mainHandler.post { expireSession(appContext) }
    }

    private fun expireSession(context: Context) {
        val baseClass = BaseClass()
        // Read the account type before it is cleared, to pick the right login screen.
        val type = baseClass.getSharedPreferance(context, "type", "")
        clearSession(context)

        Toast.makeText(context, R.string.session_expired, Toast.LENGTH_LONG).show()
        openLoginScreen(context, type)
    }

    /**
     * Opens the login screen for account [type] ("1" clinic, "2" doctor, "3" lab, else pharmacy)
     * and closes everything else. CLEAR_TASK finishes the token screen, whose onDestroy
     * disconnects Pusher.
     */
    fun openLoginScreen(context: Context, type: String?) {
        val loginScreen = when (type) {
            "1" -> ClinicLoginActivity::class.java
            "2" -> DoctorLoginActivity::class.java
            "3" -> LabLoginActivity::class.java
            else -> PharmacyLoginActivity::class.java
        }
        val intent = Intent(context, loginScreen).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }

    /** Login usernames/passwords older builds saved in plain text (QA-04). Nothing writes them now. */
    private val LEGACY_CREDENTIAL_KEYS = listOf(
        "clinicUsername", "clinicPassword",
        "drUsername", "drPassword",
        "labUsername", "labPassword",
        "pharmacyUsername", "pharmacyPassword"
    )

    /**
     * QA-04: deletes any credentials an older build left on the TV. Called on every app start,
     * so an update cleans existing installs without the user having to log out.
     */
    fun removeLegacyStoredCredentials(context: Context) {
        try {
            val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
            if (LEGACY_CREDENTIAL_KEYS.none { prefs.contains(it) }) return
            val editor = prefs.edit()
            LEGACY_CREDENTIAL_KEYS.forEach { editor.remove(it) }
            editor.apply()
            Log.i(TAG, "Removed stored login credentials left by an older build")
        } catch (e: Exception) {
            Log.e(TAG, "Could not remove stored credentials", e)
        }
    }

    /** Same keys the Settings logout clears. */
    fun clearSession(context: Context) {
        val baseClass = BaseClass()
        baseClass.setSharedPreferance(context, "login_status", "false")
        baseClass.setSharedPreferance(context, "date", "")
        baseClass.setSharedPreferance(context, "apiCall", "false")
        baseClass.setSharedPreferance(context, "token", "")
        baseClass.setSharedPreferance(context, "clinic_id", "")
        baseClass.setSharedPreferance(context, "doctor_id", "")
        baseClass.setSharedPreferance(context, "lab_id", "")
        baseClass.setSharedPreferance(context, "pharmacy_id", "")
        baseClass.setSharedPreferance(context, "type", "")
        baseClass.setSharedPreferance(context, SettingsActivity.SELECTED_COUNTER_IDS, "")
        RetrofitClient.bearer_token = ""
    }
}
