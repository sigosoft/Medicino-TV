package com.medicinoclinic

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.medicinoclinic.utils.TvMessage
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import com.medicinoclinic.home.HomeActivity
import com.medicinoclinic.home.HomeActivity1
import com.medicinoclinic.retrofit.APIService
import com.medicinoclinic.utils.BaseClass
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Loads [MainFragment].
 */
class DoctorLoginActivity : AppCompatActivity() {
   var btn_login: Button? = null
    /** QA-16: true while a login request is running, so repeat presses send nothing. */
    private var loginInProgress = false
    var baseClass =  BaseClass()
    var  login_status: String? = "false"
    var drUsername: String? = ""
    var drPassword: String? = ""
    var fcm: String? = ""
    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isTv = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        
        // Fix: Only block touches if we are on a TV. On a phone, we need touches to enter credentials.
        if (isTv) {
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
        
        setContentView(R.layout.activity_doctorlogin)
        
        // Fix for phones: Adjust margins programmatically to ensure fields are clickable and visible
        if (!isTv) {
            adjustLayoutForDevice()
        }
        
        val edusername : EditText = findViewById(R.id.edUserName)
        val edPassword : EditText = findViewById(R.id.edPassword)

        // Ensure EditTexts are interactive on phone
        if (!isTv) {
            edusername.isFocusable = true
            edusername.isFocusableInTouchMode = true
            edusername.isEnabled = true
            edusername.isCursorVisible = true
            
            edPassword.isFocusable = true
            edPassword.isFocusableInTouchMode = true
            edPassword.isEnabled = true
            edPassword.isCursorVisible = true
        }

        btn_login = findViewById(R.id.btn_login)

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    fcm = task.result
                    Log.d("FCM_TOKEN1", fcm.toString())
                }
            }
        login_status = baseClass.getSharedPreferance(applicationContext,"login_status","false")

            if(login_status.equals("false")) {
                // QA-04: credentials are never stored or pre-filled; the user types them each time.
            }else{
                val intent = Intent(this, HomeActivity1::class.java)
                startActivity(intent)
                finish()
            }


        btn_login!!.setOnClickListener(View.OnClickListener {
            val userName = edusername.text.toString()
            val password = edPassword.text.toString()
            if (userName.trim().isEmpty()) {
                TvMessage.show(this@DoctorLoginActivity, getString(R.string.enter_username))

            } else if (password.trim().isEmpty()) {
                TvMessage.show(this@DoctorLoginActivity, getString(R.string.enter_password))

            } else if (password.trim().length < 6) {
                TvMessage.show(this@DoctorLoginActivity, getString(R.string.password_length))

            }
            else {
                getLogin(userName,password)
            }

        })

    }

    private fun adjustLayoutForDevice() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.post {
            try {
                findAndAdjustMargins(rootView as ViewGroup)
            } catch (e: Exception) {
                Log.e("DoctorLoginActivity", "Layout adjustment failed", e)
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

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(applicationContext, IntroductionActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun getLogin(userName: String, password: String) {
        // QA-20: with no network at all, say so straight away instead of waiting ~20 s for the
        // request to time out and then showing "Poor network connection".
        if (!TvMessage.isOnline(this)) {
            TvMessage.show(this, getString(R.string.no_internet))
            return
        }
        // QA-16: pressing Login again while waiting sent duplicate requests (and could open
        // the token screen more than once). Ignore repeat presses until this one finishes.
        if (loginInProgress) return
        loginInProgress = true


        val progressDialog = ProgressDialog(this@DoctorLoginActivity, R.style.MyTheme)
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Large)
        progressDialog.show()
        progressDialog.setCancelable(false)


        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService
        mAPIService.login(userName, password,"2",fcm.toString()).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                loginInProgress = false

                if (response.isSuccessful) {
                    progressDialog.dismiss()
                    try {
                        val bodyString = response.body()?.string() ?: ""
                        val jsonObject = JSONObject(bodyString)
                        val data: JSONObject = jsonObject.getJSONObject("data")
                        val doctorId = if (data.has("doctor_id")) data.opt("doctor_id").toString() else ""
                        baseClass.setSharedPreferance(applicationContext,"doctor_id",doctorId)
                        baseClass.setSharedPreferance(applicationContext,"token",data.getString("token"))
                        baseClass.setSharedPreferance(applicationContext,"type","2")
                        RetrofitClient.bearer_token = data.getString("token")

                        baseClass.setSharedPreferance(applicationContext,"login_status","true")
                        val intent = Intent(applicationContext, HomeActivity1::class.java)
                        startActivity(intent)
                        finishAffinity()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        TvMessage.show(this@DoctorLoginActivity, getString(R.string.invalid_credentials))
                    }


                } else {
                    progressDialog.dismiss()
                    try {
                        val errorBodyString = response.errorBody()?.string() ?: ""
                        val jsonObject = JSONObject(errorBodyString)

                        if (response.code() == 400) {
                            val message: String = jsonObject.getString("message")

                            TvMessage.show(this@DoctorLoginActivity, message)
                        }

                        else if (response.code() == 422) {
                            val message: JSONObject = jsonObject.getJSONObject("message")
                            if (message.has("username")) {
                                val username: String = message.getJSONArray("username").get(0).toString()
                                TvMessage.show(this@DoctorLoginActivity, username)
                            }else  if (message.has("password")) {
                                val username: String = message.getJSONArray("password").get(0).toString()
                                TvMessage.show(this@DoctorLoginActivity, username)
                            }
                            else  if (message.has("email")) {
                                val username: String = message.getJSONArray("email").get(0).toString()
                                TvMessage.show(this@DoctorLoginActivity, username)
                            }
                            else {
                                TvMessage.show(this@DoctorLoginActivity, message.toString())
                            }

                        } else if (response.code() == 500) {
                            TvMessage.show(this@DoctorLoginActivity, getString(R.string.api_error))
                        } else {
                            TvMessage.show(this@DoctorLoginActivity, getString(R.string.invalid_credentials))

                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (response.code() == 500) {
                            TvMessage.show(this@DoctorLoginActivity, getString(R.string.api_error))
                        } else {
                            TvMessage.show(this@DoctorLoginActivity, getString(R.string.invalid_credentials))
                        }
                    }

                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                loginInProgress = false
                TvMessage.show(this@DoctorLoginActivity, TvMessage.networkErrorText(this@DoctorLoginActivity, t))
                progressDialog.dismiss()
            }
        })
    }
}