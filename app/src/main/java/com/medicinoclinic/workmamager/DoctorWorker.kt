package com.medicinoclinic.workmamager

import android.content.Context
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.medicinoclinic.R
import com.medicinoclinic.model.DoctorListingModel
import com.medicinoclinic.retrofit.APIService
import com.medicinoclinic.utils.BaseClass
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    var loginStatus: String? = ""
    var token: String? = ""
    var type: String? = ""
    var baseClass = BaseClass()
    var doctorsList = arrayListOf<DoctorListingModel>()

    override fun doWork(): Result {
        print("Working....");
        loginStatus = baseClass.getSharedPreferance(applicationContext, "login_status", "false")
        token = baseClass.getSharedPreferance(applicationContext, "token", "")
        type = baseClass.getSharedPreferance(applicationContext, "type", "")
        if (loginStatus.equals("false")) {

        } else {
            getHomeDetails(type!!);
        }

        return Result.success()
    }

    private fun getHomeDetails(type: String) {

        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.getHome(type).enqueue(object :
            Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {

                    doctorsList.clear()

                    var response_from_server = response.body()!!.string()
                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(response_from_server)
                    var jObj_data = jsonObject.getJSONObject("data")
                    var jsonArry_doctors = jObj_data.getJSONArray("doctors")

                    val sb = StringBuilder()
                    val sbContent = StringBuilder()

                    if (jsonArry_doctors.length() > 0) {
                        for (k in 0 until jsonArry_doctors.length()) {
                            val jObjDoctors = jsonArry_doctors.getJSONObject(k)
                            val doctor_id = jObjDoctors.getString("doctor_id")
                            val name = jObjDoctors.getString("name")
                            val token = jObjDoctors.getString("token")
//                           val date = jObjDoctors.getString("date")

                            doctorsList.add(DoctorListingModel(doctor_id, name, "", token, "", ""))
                        }

                    } else {
                        if (type.equals("1")) {
                            selectDoctors()
                        }
                    }


                } else {

                    if (response.code() == 401) {
                        Toast.makeText(
                            applicationContext,
                            applicationContext.getString(R.string.unauthenticated),
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (response.code() == 500) {
                        Toast.makeText(
                            applicationContext,
                            applicationContext.getString(R.string.api_error),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            applicationContext.getString(R.string.server_error),
                            Toast.LENGTH_LONG
                        ).show()

                    }

                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    applicationContext,
                    applicationContext.getString(R.string.response_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun selectDoctors() {
        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.selectDoctors().enqueue(object :
            Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {

                    var response_from_server = response.body()!!.string()
                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(response_from_server)

                    print("JSON OBJECT...." + jsonObject)


                } else {

                    if (response.code() == 401) {
                        Toast.makeText(
                            applicationContext,
                            applicationContext.getString(R.string.unauthenticated),
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (response.code() == 500) {
                        Toast.makeText(
                            applicationContext,
                            applicationContext.getString(R.string.api_error),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            applicationContext.getString(R.string.server_error),
                            Toast.LENGTH_LONG
                        ).show()

                    }

                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    applicationContext,
                    applicationContext.getString(R.string.response_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        })

    }
}
