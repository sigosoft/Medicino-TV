package com.medicinoclinic

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.medicinoclinic.home.HomeActivity
import com.medicinoclinic.home.HomeActivity1
import com.medicinoclinic.utils.BaseClass
import com.medicinoclinic.workmamager.DoctorWorker
import java.util.concurrent.TimeUnit
import android.provider.Settings
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging


class IntroductionActivity : AppCompatActivity() {
    var baseClass =  BaseClass()
    var  login_status: String? = "false"
    var btn_drlogin: Button? = null
    var btn_cliniclogin: Button? = null
    var btn_pharmacylogin: Button? = null
    var btn_lablogin: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_introduction)

        btn_drlogin = findViewById(R.id.btn_drlogin)
        btn_cliniclogin = findViewById(R.id.btn_cliniclogin)
        btn_pharmacylogin = findViewById(R.id.btn_pharmacylogin)
        btn_lablogin = findViewById(R.id.btn_lablogin)

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("FCM_TOKEN", token)
                    // Send this token to your Laravel backend
                }
            }

        scheduleWork("Doctors")

        AlertDialog.Builder(this@IntroductionActivity)
            .setTitle("Permission Required")
            .setMessage("To monitor your app's data usage, please grant Usage Access permission.")
            .setPositiveButton("Grant") { _, _ ->
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()

        btn_drlogin!!.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, DoctorLoginActivity::class.java)
            startActivity(intent)
            finish()

        })

        btn_cliniclogin!!.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, ClinicLoginActivity::class.java)
            startActivity(intent)
            finish()

        })

        btn_pharmacylogin!!.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, PharmacyLoginActivity::class.java)
            startActivity(intent)
            finish()

        })

        btn_lablogin!!.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, LabLoginActivity::class.java)
            startActivity(intent)
            finish()

        })

        login_status = baseClass.getSharedPreferance(applicationContext,"login_status","false")

        if(login_status.equals("false")) {
//                val intent = Intent(this, LoginActivity::class.java)
//                startActivity(intent)
//                finish()
        }else{
            val intent = Intent(this, HomeActivity1::class.java)
            startActivity(intent)
            finish()
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