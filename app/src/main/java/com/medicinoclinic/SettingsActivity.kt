package com.medicinoclinic

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medicinoclinic.adapter.SettingsAdapter
import com.medicinoclinic.model.DoctorsModel
import com.medicinoclinic.retrofit.APIService
import com.medicinoclinic.utils.BaseClass
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsActivity :  FragmentActivity() {
    var btn_logout: Button? = null
    var recyclerview : RecyclerView?= null
    private lateinit var adapter : SettingsAdapter
    val doctorList = arrayListOf<DoctorsModel>()
    var baseClass =  BaseClass()
    var token : String ?= null
    var type: String ?= null
    var logoutClicked: Boolean? = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        setContentView(R.layout.activity_settings)
        btn_logout = findViewById(R.id.btn_logout)
        recyclerview = findViewById<RecyclerView>(R.id.recyclerView)

        recyclerview!!.layoutManager = LinearLayoutManager(this)

        token = baseClass.getSharedPreferance(this@SettingsActivity, "token", "")
        type = baseClass.getSharedPreferance(this@SettingsActivity, "type", "")
        RetrofitClient.bearer_token = token

        if(type.equals("1")||type.equals("2")) {
            getDoctors(type!!)
        }


        btn_logout!!.setOnClickListener(View.OnClickListener {
            if(logoutClicked==false) {
                logout()
            }
        })

       /* recyclerview!!.setOnFocusChangeListener(View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                (v as RecyclerView).getChildAt(1).requestFocus()
            } else {

            }
        })*/



    }

    private fun getDoctors(type : String) {

        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.getDoctors(type).enqueue(object :
            Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {

                    var response_from_server = response.body()!!.string()
                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(response_from_server)
                    print("Response");
                    print(jsonObject);
                    var jObj_data = jsonObject.getJSONObject("data")
                    var jsonArry_doctors = jObj_data.getJSONArray("doctors")
                    for (i in 0 until jsonArry_doctors.length()) {
                        val jObj = jsonArry_doctors.getJSONObject(i)
                        var id = jObj.getString("id")
                        var doctor_name = jObj.getString("doctor_name")
                        var select_status = jObj.getString("select_status")
                        doctorList.add(DoctorsModel(id, "", doctor_name, select_status.toBoolean()))

                    }
                    adapter = SettingsAdapter(doctorList)
                    recyclerview!!.layoutManager = LinearLayoutManager(this@SettingsActivity)
                    recyclerview!!.adapter = adapter




                } else {

                    if (response.code() == 401) {
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.unauthenticated),
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (response.code() == 500) {
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.api_error),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else {
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.server_error),
                            Toast.LENGTH_LONG
                        ).show()

                    }

                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    this@SettingsActivity,
                    getString(R.string.response_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun logout() {
        logoutClicked = true
        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.logout().enqueue(object :
            Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {

                    var response_from_server = response.body()!!.string()
                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(response_from_server)
                    print("Response");
                    Log.d("Response","logout: ${response_from_server}");
                   // var jObj_data = jsonObject.getJSONObject("data")
                    var message = jsonObject.getString("message")
                    var status = jsonObject.getBoolean("status")
                    if(status){
                        Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_LONG).show()
                        baseClass.setSharedPreferance(applicationContext,"login_status","false")
                        baseClass.setSharedPreferance(applicationContext,"date","");
                        baseClass.setSharedPreferance(applicationContext,"apiCall","false");

                        if(type.equals("1")) {
                            val intent = Intent(applicationContext, ClinicLoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }else if(type.equals("2")) {
                            val intent = Intent(applicationContext, DoctorLoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }else if(type.equals("3")) {
                            val intent = Intent(applicationContext, LabLoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        else {
                            val intent = Intent(applicationContext, PharmacyLoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }


                } else {

                    if (response.code() == 401) {
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.unauthenticated),
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (response.code() == 500) {
                        Log.d("Response","logout: ${response.errorBody()}");
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.api_error),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else {
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.server_error),
                            Toast.LENGTH_LONG
                        ).show()

                    }

                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    this@SettingsActivity,
                    getString(R.string.response_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

}
