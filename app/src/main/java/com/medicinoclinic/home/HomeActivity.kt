package com.medicinoclinic.home

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medicinoclinic.R
import com.medicinoclinic.SettingsActivity
import com.medicinoclinic.adapter.DoctorAdapter
import com.medicinoclinic.ContentModel
import com.medicinoclinic.model.DoctorListingModel
import com.medicinoclinic.model.VideoModel
import com.medicinoclinic.retrofit.APIService
import com.medicinoclinic.utils.BaseClass
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class HomeActivity : AppCompatActivity() {
    // var recyclerView: InfiniteAutoScrollRecyclerView? = null
    var videoView: VideoView? = null
    var contents = arrayListOf<ContentModel>()
    var doctorsList = arrayListOf<DoctorListingModel>()
    var recyclerViewDoctor: RecyclerView? = null
    var btn_settings: Button? = null
    var scrollingtitle: TextView? = null
    var scrollingtext: TextView? = null
    var title: String? = null
    var titles: String? = null
    var contentScroll: String? = null
    private lateinit var mediaSession: MediaSessionCompat
    val videoList = arrayListOf<VideoModel>()
    var video_counter = 0
    var token: String? = null
    var type: String? = null
    var todayString: String? = null
    var baseClass = BaseClass()
    lateinit var mainHandler: Handler


    private val updateTextTask = object : Runnable {
        override fun run() {
            getDoctorDetails(type!!)
            mainHandler.postDelayed(this, 10000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fix: Only block touches if we are on a TV. On a phone, we need touches.
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
        }
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_home1)
        mainHandler = Handler(Looper.getMainLooper())

        videoView = findViewById(R.id.videoView_ID)
        scrollingtitle = findViewById(R.id.scrollingtitle)
        scrollingtext = findViewById(R.id.scrollingtext)
        recyclerViewDoctor = findViewById(R.id.rv_doctors)
        btn_settings = findViewById(R.id.btn_settings)

        token = baseClass.getSharedPreferance(this@HomeActivity, "token", "")
        type = baseClass.getSharedPreferance(this@HomeActivity, "type", "")
        RetrofitClient.bearer_token = token

        val dateFormat: DateFormat = SimpleDateFormat("dd-MM-yyyy")
        val date = Date()
        todayString = dateFormat.format(date)

        baseClass.setSharedPreferance(this@HomeActivity, "date", todayString);

        getHomeDetails(type!!)

        Log.e("Homeeee", "aaaaaa")

        val mediaController = MediaController(this@HomeActivity)
        mediaController.setAnchorView(videoView)
        mediaController.setMediaPlayer(videoView)
        btn_settings?.requestFocus()
        recyclerViewDoctor?.requestFocus()

        btn_settings!!.setOnClickListener(View.OnClickListener {

            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)

        })


    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(updateTextTask)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            videoView?.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        videoView?.stopPlayback();
    }

    private fun getHomeDetails(type: String) {
        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.getHome(type).enqueue(object :
            Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {

                    contents.clear()
                    videoList.clear()
                    doctorsList.clear()

                    var response_from_server = response.body()!!.string()
                    println(response_from_server)
                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(response_from_server)
                    var jObj_data = jsonObject.getJSONObject("data")
                    var jsonArry_doctors = jObj_data.getJSONArray("doctors")
                    var jsonArray_tvShows = jObj_data.getJSONArray("tv_shows")
                    var jsonArray_scrollingContents = jObj_data.getJSONArray("scrolling_contents")


                    if (jsonArry_doctors.length() > 0) {
                        for (k in 0 until jsonArry_doctors.length()) {
                            val jObjDoctors = jsonArry_doctors.getJSONObject(k)
                            val doctor_id = jObjDoctors.getString("doctor_id")
                            val name = jObjDoctors.getString("name")
                            var room = ""
                            if (jObjDoctors.has("room")) {
                                room = jObjDoctors.getString("room")
                            } else {
                                room = ""
                            }
                            val token = jObjDoctors.getString("token")
//                        val date = jObjDoctors.getString("date")

                            doctorsList.add(
                                DoctorListingModel(
                                    doctor_id,
                                    name,
                                    room,
                                    token,
                                    "",
                                    ""
                                )
                            )
                        }

                        if (doctorsList.size > 0) {
                            recyclerViewDoctor?.isVisible = true
                            btn_settings?.isVisible = false
                            recyclerViewDoctor?.layoutManager =
                                LinearLayoutManager(this@HomeActivity)
                            val adapter = DoctorAdapter(doctorsList)
                            recyclerViewDoctor?.adapter = adapter
                        } else {

                            recyclerViewDoctor?.isVisible = false
                            btn_settings?.isVisible = true

                        }
                    } else {
                        if (type.equals("1")) {
                            var savedDate =
                                baseClass.getSharedPreferance(this@HomeActivity, "date", "")
                            if (todayString == savedDate) {
                                var apiCall =
                                    baseClass.getSharedPreferance(this@HomeActivity, "apiCall", "")
                                if (apiCall == "true") {
                                    recyclerViewDoctor?.isVisible = false
                                    btn_settings?.isVisible = true
                                } else {

                                    selectDoctors()

                                }

                            }
                        }

                    }

                    for (i in 0 until jsonArray_tvShows.length()) {
                        val jObj = jsonArray_tvShows.getJSONObject(i)
                        val id = jObj.getString("id")
                        val type = jObj.getString("type")
                        val titles = jObj.getString("title")
                        val video = RetrofitClient.ApiUtils.VIDEO_URL + jObj.getString("video")

                        videoList.add(VideoModel(id, type, titles, video))

                    }
                    val sb = StringBuilder()
                    val sbContent = StringBuilder()

                    for (j in 0 until jsonArray_scrollingContents.length()) {
                        val jObj_scrollingContents = jsonArray_scrollingContents.getJSONObject(j)
                        val id = jObj_scrollingContents.getString("id")
                        val type = jObj_scrollingContents.getString("type")
                        titles = jObj_scrollingContents.getString("title")
                        val body = jObj_scrollingContents.getString("content")
                        titles = titles + "               "
                        contentScroll = body + "                        "

                        sb.append(titles)
                        sbContent.append(contentScroll)

                        contents.add(ContentModel(title, body, id, type))


                    }
                    title = sb.toString()
                    contentScroll = sbContent.toString()
                    scrollingtitle!!.setText(title)
                    scrollingtitle!!.setSelected(true);
                    scrollingtext!!.isSelected = true
                    scrollingtext!!.setSelected(true);
                    scrollingtext!!.setText(contentScroll)


                    if (videoList.isNotEmpty()) {
                        videoView!!.setVideoURI(Uri.parse(videoList[0].video))

                        videoView!!.setOnPreparedListener(object : MediaPlayer.OnPreparedListener {
                            override fun onPrepared(mp: MediaPlayer) {
                                mp.start();
                            }
                        })

                        videoView!!.setOnCompletionListener(OnCompletionListener {
                            if (videoList.isNotEmpty()) {
                                video_counter++
                                if (video_counter >= videoList.size) {
                                    video_counter = 0
                                }
                                videoView!!.setVideoURI(Uri.parse(videoList[video_counter].video))
                                videoView!!.start()
                            }
                        })
                    }


                } else {

                    if (response.code() == 401) {
                        Toast.makeText(
                            this@HomeActivity,
                            getString(R.string.unauthenticated),
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (response.code() == 500) {
                        Toast.makeText(
                            this@HomeActivity,
                            getString(R.string.api_error),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@HomeActivity,
                            response.message(),
                            Toast.LENGTH_LONG
                        ).show()

                    }

                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    this@HomeActivity,
                    getString(R.string.response_failed),
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
                    println("Response from Server...." + response_from_server)

                    recyclerViewDoctor?.isVisible = true
                    btn_settings?.isVisible = false
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // Handle failure
            }
        })
    }

    private fun getDoctorDetails(type: String) {
        // Implementation for periodic updates
    }
}
