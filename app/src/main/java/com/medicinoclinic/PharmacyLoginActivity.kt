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
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import com.medicinoclinic.home.HomeActivity1
import com.medicinoclinic.retrofit.APIService
import com.medicinoclinic.utils.BaseClass
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PharmacyLoginActivity : AppCompatActivity() {
    var btn_login: Button? = null
    var baseClass = BaseClass()
    var login_status: String? = "false"
    var pharmacyUserName: String? = ""
    var pharmacyPassword: String? = ""
    var fcm: String? = ""

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }

        setContentView(R.layout.activity_pharmacylogin)
        adjustLayoutForDevice()

        val edusername: EditText = findViewById(R.id.edUserName)
        val edPassword: EditText = findViewById(R.id.edPassword)

        btn_login = findViewById(R.id.btn_login)

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    fcm = task.result
                    Log.d("FCM_TOKEN1", fcm.toString())
                }
            }

        login_status = baseClass.getSharedPreferance(applicationContext, "login_status", "false")

        if (login_status.equals("false")) {
            pharmacyUserName = baseClass.getSharedPreferance(applicationContext, "pharmacyUsername", "")
            pharmacyPassword = baseClass.getSharedPreferance(applicationContext, "pharmacyPassword", "")
            edusername.setText(pharmacyUserName)
            edPassword.setText(pharmacyPassword)
        } else {
            val intent = Intent(this, HomeActivity1::class.java)
            startActivity(intent)
            finish()
        }

        btn_login!!.setOnClickListener(View.OnClickListener {
            val userName = edusername.text.toString()
            val password = edPassword.text.toString()
            if (userName.trim().isEmpty()) {
                Toast.makeText(this@PharmacyLoginActivity, getString(R.string.enter_username), Toast.LENGTH_LONG).show()
            } else if (password.trim().isEmpty()) {
                Toast.makeText(this@PharmacyLoginActivity, getString(R.string.enter_password), Toast.LENGTH_LONG).show()
            } else if (password.trim().length < 6) {
                Toast.makeText(this@PharmacyLoginActivity, getString(R.string.password_length), Toast.LENGTH_LONG).show()
            } else {
                getLogin(userName, password)
            }
        })
    }

    private fun adjustLayoutForDevice() {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.currentModeType != Configuration.UI_MODE_TYPE_TELEVISION) {
            val rootView = findViewById<View>(android.R.id.content)
            rootView.post {
                try {
                    findAndAdjustMargins(rootView as ViewGroup)
                } catch (e: Exception) {
                    Log.e("PharmacyLoginActivity", "Layout adjustment failed", e)
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

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(applicationContext, IntroductionActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun getLogin(userName: String, password: String) {
        val progressDialog = ProgressDialog(this@PharmacyLoginActivity, R.style.MyTheme)
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Large)
        progressDialog.show()
        progressDialog.setCancelable(false)

        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService
        mAPIService.login(userName, password, "4", fcm.toString()).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    progressDialog.dismiss()
                    try {
                        val bodyString = response.body()?.string() ?: ""
                        val jsonObject = JSONObject(bodyString)
                        val data: JSONObject = jsonObject.getJSONObject("data")

                        val pharmacyId = if (data.has("pharmacy_id")) data.opt("pharmacy_id").toString() else ""
                        baseClass.setSharedPreferance(applicationContext, "pharmacy_id", pharmacyId)
                        baseClass.setSharedPreferance(applicationContext, "token", data.getString("token"))
                        baseClass.setSharedPreferance(applicationContext, "type", "4")
                        baseClass.setSharedPreferance(applicationContext, "pharmacyUsername", userName)
                        baseClass.setSharedPreferance(applicationContext, "pharmacyPassword", password)

                        // FIX 1: Save token_management_type at login so it is available
                        // immediately in HomeActivity1 before getHomeDetails API returns
                        val tokenManagementType = if (data.has("token_management_type")) data.opt("token_management_type").toString() else ""
                        baseClass.setSharedPreferance(applicationContext, "token_management_type", tokenManagementType)
                        Log.d("DEBUG_LOGIN", "Saved token_management_type=$tokenManagementType for pharmacy_id=$pharmacyId")

                        RetrofitClient.bearer_token = data.getString("token")
                        baseClass.setSharedPreferance(applicationContext, "login_status", "true")

                        val intent = Intent(applicationContext, HomeActivity1::class.java)
                        startActivity(intent)
                        finishAffinity()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@PharmacyLoginActivity, getString(R.string.invalid_credentials), Toast.LENGTH_LONG).show()
                    }
                } else {
                    progressDialog.dismiss()
                    try {
                        val errorBodyString = response.errorBody()?.string() ?: ""
                        val jsonObject = JSONObject(errorBodyString)

                        if (response.code() == 400) {
                            val message: String = jsonObject.getString("message")
                            Toast.makeText(this@PharmacyLoginActivity, message, Toast.LENGTH_LONG).show()
                        } else if (response.code() == 422) {
                            val message: JSONObject = jsonObject.getJSONObject("message")
                            if (message.has("username")) {
                                val username: String = message.getJSONArray("username").get(0).toString()
                                Toast.makeText(this@PharmacyLoginActivity, username, Toast.LENGTH_LONG).show()
                            } else if (message.has("password")) {
                                val username: String = message.getJSONArray("password").get(0).toString()
                                Toast.makeText(this@PharmacyLoginActivity, username, Toast.LENGTH_LONG).show()
                            } else if (message.has("email")) {
                                val username: String = message.getJSONArray("email").get(0).toString()
                                Toast.makeText(this@PharmacyLoginActivity, username, Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this@PharmacyLoginActivity, message.toString(), Toast.LENGTH_LONG).show()
                            }
                        } else if (response.code() == 500) {
                            Toast.makeText(this@PharmacyLoginActivity, getString(R.string.api_error), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@PharmacyLoginActivity, getString(R.string.invalid_credentials), Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (response.code() == 500) {
                            Toast.makeText(this@PharmacyLoginActivity, getString(R.string.api_error), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@PharmacyLoginActivity, getString(R.string.invalid_credentials), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@PharmacyLoginActivity, getString(R.string.response_failed), Toast.LENGTH_LONG).show()
                progressDialog.dismiss()
            }
        })
    }
}