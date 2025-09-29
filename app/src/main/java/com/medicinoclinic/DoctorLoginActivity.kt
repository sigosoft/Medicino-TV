package com.medicinoclinic

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
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
    var baseClass =  BaseClass()
    var  login_status: String? = "false"
    var drUsername: String? = ""
    var drPassword: String? = ""
    var fcm: String? = ""
    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        setContentView(R.layout.activity_doctorlogin)
        val edusername : EditText = findViewById(R.id.edUserName)
        val edPassword : EditText = findViewById(R.id.edPassword)

        btn_login = findViewById(R.id.btn_login)

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    fcm = task.result
                    Log.d("FCM_TOKEN1", fcm.toString())
                    // Send this token to your Laravel backend
                }
            }
        login_status = baseClass.getSharedPreferance(applicationContext,"login_status","false")

            if(login_status.equals("false")) {
                drUsername = baseClass.getSharedPreferance(applicationContext,"drUsername","")
                drPassword = baseClass.getSharedPreferance(applicationContext,"drPassword","")
               edusername.setText(drUsername)
               edPassword.setText(drPassword)
//                val intent = Intent(this, LoginActivity::class.java)
//                startActivity(intent)
//                finish()
            }else{
                val intent = Intent(this, HomeActivity1::class.java)
                startActivity(intent)
                finish()
            }


        btn_login!!.setOnClickListener(View.OnClickListener {
            val userName = edusername.text.toString()
            val password = edPassword.text.toString()
            if (userName.trim().isEmpty()) {
                Toast.makeText(
                    this@DoctorLoginActivity,
                    getString(R.string.enter_username),
                    Toast.LENGTH_LONG
                ).show()

            } else if (password.trim().isEmpty()) {
                Toast.makeText(
                    this@DoctorLoginActivity,
                    getString(R.string.enter_password),
                    Toast.LENGTH_LONG
                ).show()

            } else if (password.trim().length < 6) {
                Toast.makeText(
                    this@DoctorLoginActivity,
                    getString(R.string.password_length),
                    Toast.LENGTH_LONG
                ).show()

            }
            else {
                getLogin(userName,password)
            }

        })

    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(applicationContext, IntroductionActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun getLogin(userName: String, password: String) {

        val progressDialog = ProgressDialog(this@DoctorLoginActivity, R.style.MyTheme)
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Large)
        progressDialog.show()
        progressDialog.setCancelable(false)


        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService
        mAPIService.login(userName, password,"2",fcm.toString()).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

                if (response.isSuccessful) {
                    progressDialog.dismiss()
                    var response_from_server = response.body()
                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(response.body()!!.string())
                    val message: String = jsonObject.getString("message")
                    val data: JSONObject = jsonObject.getJSONObject("data")
                    baseClass.setSharedPreferance(applicationContext,"doctor_id",data.getInt("doctor_id").toString())
                    baseClass.setSharedPreferance(applicationContext,"token",data.getString("token"))
                    baseClass.setSharedPreferance(applicationContext,"type","2")
                    baseClass.setSharedPreferance(applicationContext,"drUsername",userName)
                    baseClass.setSharedPreferance(applicationContext,"drPassword",password)
                    RetrofitClient.bearer_token = data.getString("token")

                    baseClass.setSharedPreferance(applicationContext,"login_status","true")
                    val intent = Intent(applicationContext, HomeActivity1::class.java)
                    startActivity(intent)
                    finishAffinity()


                } else {
                    progressDialog.dismiss()
                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(response.errorBody()!!.string())

                    if (response.code() == 400) {
                        val message: String = jsonObject.getString("message")

                        Toast.makeText(
                            this@DoctorLoginActivity,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    else if (response.code() == 422) {
                        val message: JSONObject = jsonObject.getJSONObject("message")
                        if (message.has("username")) {
                            val username: String = message.getJSONArray("username").get(0).toString()
                            Toast.makeText(this@DoctorLoginActivity, username, Toast.LENGTH_LONG).show()
                        }else  if (message.has("password")) {
                            val username: String = message.getJSONArray("password").get(0).toString()
                            Toast.makeText(this@DoctorLoginActivity, username, Toast.LENGTH_LONG).show()
                        }
                        else  if (message.has("email")) {
                            val username: String = message.getJSONArray("email").get(0).toString()
                            Toast.makeText(this@DoctorLoginActivity, username, Toast.LENGTH_LONG).show()
                        }
                        else {
                            Toast.makeText(this@DoctorLoginActivity, message.toString(), Toast.LENGTH_LONG).show()
                        }

                    } else if (response.code() == 500) {
                        Toast.makeText(
                            this@DoctorLoginActivity,
                            getString(R.string.api_error),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@DoctorLoginActivity,
                            getString(R.string.invalid_credentials),
                            Toast.LENGTH_LONG
                        ).show()

                    }

                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    this@DoctorLoginActivity,
                    getString(R.string.response_failed),
                    Toast.LENGTH_LONG
                ).show()
                progressDialog.dismiss()
            }
        })
    }
}