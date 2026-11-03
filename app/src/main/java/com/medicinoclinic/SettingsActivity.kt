package com.medicinoclinic

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medicinoclinic.adapter.CounterSettingsAdapter
import com.medicinoclinic.adapter.SettingsAdapter
import com.medicinoclinic.model.CounterSettingsModel
import com.medicinoclinic.model.DoctorsModel
import com.medicinoclinic.retrofit.APIService
import com.medicinoclinic.utils.BaseClass
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsActivity : FragmentActivity() {
    var btn_logout: Button? = null
    var btn_submit_counter: Button? = null
    var recyclerview: RecyclerView? = null
    var recyclerViewCounters: RecyclerView? = null
    var tvChooseCounter: TextView? = null
    var tvNoCounters: TextView? = null
    private lateinit var adapter: SettingsAdapter
    private lateinit var counterAdapter: CounterSettingsAdapter
    val doctorList = arrayListOf<DoctorsModel>()
    val counterList = arrayListOf<CounterSettingsModel>()
    var baseClass = BaseClass()
    var token: String? = null
    var type: String? = null
    var logoutClicked: Boolean? = false
    private var submitInProgress = false

    companion object {
        const val SELECTED_COUNTER_IDS = "selected_counter_ids"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        btn_logout = findViewById(R.id.btn_logout)
        btn_submit_counter = findViewById(R.id.btn_submit_counter)
        recyclerview = findViewById(R.id.recyclerView)
        recyclerViewCounters = findViewById(R.id.recyclerViewCounters)
        tvChooseCounter = findViewById(R.id.tv_choose_counter)
        tvNoCounters = findViewById(R.id.tv_no_counters)

        recyclerview!!.layoutManager = LinearLayoutManager(this)
        recyclerViewCounters!!.layoutManager = LinearLayoutManager(this)

        token = baseClass.getSharedPreferance(this@SettingsActivity, "token", "")
        type = baseClass.getSharedPreferance(this@SettingsActivity, "type", "")
        RetrofitClient.bearer_token = token

        if (type.equals("1") || type.equals("2")) {
            getDoctors(type!!)
            hideCounterSetupUi()
        }

        if (type.equals("3") || type.equals("4")) {
            loadDisplayCounters()
        }

        btn_logout!!.setOnClickListener {
            if (logoutClicked == false) {
                logout()
            }
        }

        btn_submit_counter!!.setOnClickListener {
            submitCounterSelection()
        }
    }

    private fun hideCounterSetupUi() {
        tvChooseCounter?.visibility = View.GONE
        tvNoCounters?.visibility = View.GONE
        recyclerViewCounters?.visibility = View.GONE
        btn_submit_counter?.visibility = View.GONE
    }

    private fun getDoctors(type: String) {
        val mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.getDoctors(type).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val response_from_server = response.body()!!.string()
                    val jsonObject = JSONObject(response_from_server)
                    print("Response")
                    print(jsonObject)
                    val jObj_data = jsonObject.getJSONObject("data")
                    val jsonArry_doctors = jObj_data.getJSONArray("doctors")
                    for (i in 0 until jsonArry_doctors.length()) {
                        val jObj = jsonArry_doctors.getJSONObject(i)
                        val id = jObj.getString("id")
                        val doctor_name = jObj.getString("doctor_name")
                        val select_status = jObj.getString("select_status")
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
                    } else {
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
        val mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.logout().enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val response_from_server = response.body()!!.string()
                    val jsonObject = JSONObject(response_from_server)
                    print("Response")
                    Log.d("Response", "logout: $response_from_server")
                    val message = jsonObject.getString("message")
                    val status = jsonObject.getBoolean("status")
                    if (status) {
                        Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_LONG).show()
                        baseClass.setSharedPreferance(applicationContext, "login_status", "false")
                        baseClass.setSharedPreferance(applicationContext, "date", "")
                        baseClass.setSharedPreferance(applicationContext, "apiCall", "false")
                        baseClass.setSharedPreferance(applicationContext, "token", "")
                        baseClass.setSharedPreferance(applicationContext, "clinic_id", "")
                        baseClass.setSharedPreferance(applicationContext, "doctor_id", "")
                        baseClass.setSharedPreferance(applicationContext, "lab_id", "")
                        baseClass.setSharedPreferance(applicationContext, "pharmacy_id", "")
                        baseClass.setSharedPreferance(applicationContext, "type", "")
                        baseClass.setSharedPreferance(applicationContext, SELECTED_COUNTER_IDS, "")
                        RetrofitClient.bearer_token = ""

                        if (type.equals("1")) {
                            val intent = Intent(applicationContext, ClinicLoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else if (type.equals("2")) {
                            val intent = Intent(applicationContext, DoctorLoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else if (type.equals("3")) {
                            val intent = Intent(applicationContext, LabLoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            val intent = Intent(applicationContext, PharmacyLoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
                        Log.d("Response", "logout: ${response.errorBody()}")
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.api_error),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
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

    private fun loadDisplayCounters() {
        val mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.getDisplayCounters().enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (!response.isSuccessful) {
                    Toast.makeText(
                        this@SettingsActivity,
                        getString(R.string.api_error),
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val jsonObject = JSONObject(response.body()!!.string())
                val data = jsonObject.optJSONObject("data")
                val countersArray = data?.optJSONArray("counters") ?: JSONArray()
                val selectedFromIds = parseSelectedCounterIds(data?.optJSONArray("selected_counter_ids"))

                counterList.clear()
                for (i in 0 until countersArray.length()) {
                    val item = countersArray.getJSONObject(i)
                    val id = jsonFieldAsString(item, "id")
                    val name = jsonFieldAsString(item, "counter_name")
                    if (id.isEmpty() || id.equals("null", true)) continue
                    if (name.isEmpty() || name.equals("null", true)) continue
                    val selected = parseSelectedFlag(item) || selectedFromIds.contains(id)
                    counterList.add(CounterSettingsModel(id, name, selected))
                }

                if (counterList.isEmpty()) {
                    tvNoCounters?.visibility = View.VISIBLE
                    recyclerViewCounters?.visibility = View.GONE
                    btn_submit_counter?.isEnabled = false
                    return
                }

                tvNoCounters?.visibility = View.GONE
                recyclerViewCounters?.visibility = View.VISIBLE
                btn_submit_counter?.isEnabled = true
                counterAdapter = CounterSettingsAdapter(counterList)
                recyclerViewCounters?.adapter = counterAdapter
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

    private fun parseSelectedCounterIds(jsonArray: JSONArray?): Set<String> {
        if (jsonArray == null || jsonArray.length() == 0) return emptySet()
        val ids = HashSet<String>()
        for (i in 0 until jsonArray.length()) {
            val value = jsonArray.opt(i)?.toString()?.trim().orEmpty()
            if (value.isNotEmpty() && !value.equals("null", true)) {
                ids.add(value)
            }
        }
        return ids
    }

    private fun parseSelectedFlag(item: JSONObject): Boolean {
        val value = when {
            item.has("selected") && !item.isNull("selected") -> item.opt("selected")
            item.has("is_selected") && !item.isNull("is_selected") -> item.opt("is_selected")
            item.has("select_status") && !item.isNull("select_status") -> item.opt("select_status")
            else -> false
        }
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            else -> {
                val text = value.toString().trim()
                text.equals("true", ignoreCase = true) || text == "1"
            }
        }
    }

    private fun jsonFieldAsString(jsonObject: JSONObject, key: String): String {
        if (!jsonObject.has(key) || jsonObject.isNull(key)) return ""
        return jsonObject.opt(key).toString().trim()
    }

    private fun submitCounterSelection() {
        if (submitInProgress) return

        val selectedIds = counterList.filter { it.isSelected }.map { it.id }
        if (selectedIds.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.select_at_least_one_counter),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val selectedJson = JSONArray()
        selectedIds.forEach { id ->
            val asInt = id.toIntOrNull()
            if (asInt != null) selectedJson.put(asInt) else selectedJson.put(id)
        }

        val progressDialog = ProgressDialog(this, R.style.MyTheme)
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Large)
        progressDialog.setCancelable(false)
        progressDialog.show()
        submitInProgress = true

        val mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.saveDisplayCounters(selectedJson.toString())
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    submitInProgress = false
                    progressDialog.dismiss()
                    if (!response.isSuccessful) {
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.api_error),
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    val body = response.body()?.string().orEmpty()
                    val jsonObject = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                    val data = jsonObject.optJSONObject("data")
                    val isCountersSet = parseBooleanFlag(
                        data?.opt("is_counters_set") ?: jsonObject.opt("is_counters_set")
                    )
                    val statusOk = parseBooleanFlag(jsonObject.opt("status"))

                    // Same as initial setup: on success close and let home reload tvHome_new
                    if (isCountersSet || statusOk) {
                        baseClass.setSharedPreferance(
                            this@SettingsActivity,
                            SELECTED_COUNTER_IDS,
                            selectedIds.joinToString(",")
                        )
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.counter_selection_saved),
                            Toast.LENGTH_LONG
                        ).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(
                            this@SettingsActivity,
                            jsonObject.optString("message", getString(R.string.something_wrong)),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    submitInProgress = false
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@SettingsActivity,
                        getString(R.string.response_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun parseBooleanFlag(value: Any?): Boolean {
        return when (value) {
            null -> false
            is Boolean -> value
            is Number -> value.toInt() != 0
            else -> {
                val text = value.toString().trim()
                text.equals("true", ignoreCase = true) || text == "1"
            }
        }
    }
}
