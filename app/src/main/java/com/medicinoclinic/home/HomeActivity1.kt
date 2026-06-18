package com.medicinoclinic.home


//import com.medicinoclinic.utils.ScrollTextView
import RetrofitClient
import RetrofitClient.ApiUtils.VIDEO_URL
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.util.Util
import com.medicinoclinic.App
import com.medicinoclinic.ContentModel
import com.medicinoclinic.R
import com.medicinoclinic.SettingsActivity
import com.medicinoclinic.adapter.DoctorAdapter
import com.medicinoclinic.adapter.SliderAdapter
import com.medicinoclinic.model.DoctorListingModel
import com.medicinoclinic.model.SliderDataModel
import com.medicinoclinic.model.VideoModel
import com.medicinoclinic.retrofit.APIService
import com.medicinoclinic.utils.BaseClass
import com.medicinoclinic.utils.ScrollTextView
import com.smarteist.autoimageslider.SliderView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.provider.Settings
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.medicinoclinic.adapter.CounterAdapter
import com.medicinoclinic.model.CounterListingModel
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.Channel
import com.pusher.client.channel.SubscriptionEventListener
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange

var doctor_selected_or_unselected = false

class HomeActivity1 : AppCompatActivity() {
    private lateinit var tts: TextToSpeech
    private var currentIndex = 0
    private var repeatCount = 0

    var videoView: VideoView? = null
    var lin_video: LinearLayout? = null
    var contents = arrayListOf<ContentModel>()
    var doctorsList = arrayListOf<DoctorListingModel>()
    var recyclerViewDoctor: RecyclerView? = null
    var btn_settings: Button? = null

    var scrollingtext: ScrollTextView? = null
    var title: String? = null
    var titles: String? = null
    var contentScroll: String? = null
    private lateinit var mediaSession: MediaSessionCompat
    val videoList = arrayListOf<VideoModel>()
    var video_counter = 0
    var token: String? = null
    var logged_clinic_id: String? = null
    var logged_doctor_id: String? = null
    var logged_lab_id: String? = null
    var logged_pharmacy_id: String? = null
    var type: String? = null
    var token_management_type: String? = null
    var todayString: String? = null
    var baseClass = BaseClass()
    lateinit var mainHandler: Handler
    var adapter: SliderAdapter? = null
    var sliderDataArrayList: ArrayList<SliderDataModel> = ArrayList()
    var sliderView: SliderView? = null

    lateinit var mPlayer: SimpleExoPlayer
    private lateinit var playerView: PlayerView
    val sliderHandler = Handler(Looper.getMainLooper())
    var count: Int? = 0
    var isVideoEnded = false
    private var mediaPlayer: MediaPlayer? = null
    var data_usage: String = "0 B"

    private var paginatedDoctors: List<List<DoctorListingModel>> = listOf()
    private var currentPage = 0
    private val pageSize = 4
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var doctorAdapter: DoctorAdapter

    var countersList = arrayListOf<CounterListingModel>()
    private lateinit var counterAdapter: CounterAdapter
    private var paginatedCounters: List<List<CounterListingModel>> = listOf()
    var recyclerViewCounter: RecyclerView? = null
    private val pagecounterSize = 5

    private lateinit var pusher: Pusher

    val runnable = object : Runnable {
        override fun run() {
            if (count != sliderView?.currentPagePosition) {
                runTvScrollingContent(sliderDataArrayList[count!!].id)
                count = sliderView?.currentPagePosition
            }
            sliderHandler.postDelayed(this, sliderDataArrayList[count!!].duration.toLong())
        }
    }


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        setContentView(R.layout.activity_home3)
        mainHandler = Handler(Looper.getMainLooper())
        videoView = findViewById(R.id.videoView_ID)
        sliderView = findViewById(R.id.slider)
        lin_video = findViewById(R.id.lin_video)
        scrollingtext = findViewById(R.id.scrollingtext)
        recyclerViewDoctor = findViewById(R.id.rv_doctors)
        recyclerViewCounter = findViewById(R.id.rv_counters)
        btn_settings = findViewById(R.id.btn_settings)
        playerView = findViewById(R.id.playerView)

        token = baseClass.getSharedPreferance(this@HomeActivity1, "token", "")
        logged_clinic_id = baseClass.getSharedPreferance(this@HomeActivity1, "clinic_id", "")
        logged_doctor_id = baseClass.getSharedPreferance(this@HomeActivity1, "doctor_id", "")
        logged_lab_id = baseClass.getSharedPreferance(this@HomeActivity1, "lab_id", "")
        logged_pharmacy_id = baseClass.getSharedPreferance(this@HomeActivity1, "pharmacy_id", "")
        type = baseClass.getSharedPreferance(this@HomeActivity1, "type", "")

        // FIX 2: Load token_management_type immediately from SharedPreferences
        // so it is available when Pusher events fire before getHomeDetails returns
        token_management_type = baseClass.getSharedPreferance(this@HomeActivity1, "token_management_type", "")

        Log.d("DEBUG_STARTUP", "type=$type")
        Log.d("DEBUG_STARTUP", "token_management_type=$token_management_type")
        Log.d("DEBUG_STARTUP", "logged_pharmacy_id=$logged_pharmacy_id")
        Log.d("DEBUG_STARTUP", "logged_lab_id=$logged_lab_id")

        RetrofitClient.bearer_token = token

        Log.e("Token....", token.toString())

        val dateFormat: DateFormat = SimpleDateFormat("dd-MM-yyyy")
        val date = Date()
        todayString = dateFormat.format(date)

        mPlayer = SimpleExoPlayer.Builder(this@HomeActivity1).build()

        baseClass.setSharedPreferance(this@HomeActivity1, "date", todayString)
        getHomeDetails(type!!)

        btn_settings?.requestFocus()
        recyclerViewDoctor?.requestFocus()

        val options = PusherOptions()
        options.setCluster("ap2")

        // dev
        pusher = Pusher("e8a2220b4c88b31f047b", options)
        // live key
        // pusher = Pusher("3ca5933d5ea7c2a5dd5c", options)

        pusher.connect(object : ConnectionEventListener {
            override fun onConnectionStateChange(change: ConnectionStateChange) {
                Log.d("Pusher", "State changed from ${change.previousState} to ${change.currentState}")
            }

            override fun onError(message: String?, code: String?, e: Exception?) {
                Log.e("Pusher", "Error: $message, Code: $code, Exception: $e")
            }
        }, ConnectionState.ALL)

        // dev channel
        val channel: Channel = pusher.subscribe("medicino-tv-stagging")
        // live channel
        // val channel: Channel = pusher.subscribe("medicino-tv-live")

        channel.bind("token-called") { event ->
            Log.d("PUSHER_DEBUG", "--------------------")
            Log.d("PUSHER_DEBUG", "TOKEN-CALLED RECEIVED")
            Log.d("PUSHER_DEBUG", "RAW DATA: ${event.data}")
            Log.d("PUSHER_DEBUG", "--------------------")
            runOnUiThread {
                Log.d("DEBUG_FLOW", "Mode: $token_management_type")

                val jsonObject = JSONObject(event.data)

                if (type.equals("1") || type.equals("2")) {
                    var clinicId = 0
                    getDoctorDetails(type!!)

                    val doctorId = jsonObject.optInt("doctor_id", -1)
                    if (doctorId != -1) {
                        if (type.equals("1")) {
                            clinicId = jsonObject.getInt("clinic_id")
                        }
                        val tokenNumber = jsonObject.getString("token_number")
                        val roomNumber = jsonObject.getString("room_number")
                        if ((type.equals("1") && logged_clinic_id.equals(clinicId.toString())) ||
                            (type.equals("2") && doctorId.toString().equals(logged_doctor_id))
                        ) {
                            convertTextToSpeech(tokenNumber, roomNumber, doctorId.toString())
                        }
                    }
                } else {
                    if (token_management_type != "3") {
                        var lab_id = 0
                        var pharmacy_id = 0
                        getCountersDetails(type!!)
                        val jsonObject2 = JSONObject(event.data)

                        if (type.equals("3")) {
                            lab_id = jsonObject2.optInt("lab_id", -1)
                        } else {
                            pharmacy_id = jsonObject2.optInt("pharmacy_id", -1)
                        }
                        if ((type.equals("3") && lab_id != -1) || (type.equals("4") && pharmacy_id != -1)) {
                            val counterId = jsonObject2.optInt("counter_id", -1)
                            val tokenNumber = jsonObject2.optString("token_number", "")
                            val booking_id = jsonObject2.optInt("counter_booking_id", 0)
                            if ((type.equals("3") && logged_lab_id.equals(lab_id.toString())) ||
                                (type.equals("4") && pharmacy_id.toString().equals(logged_pharmacy_id))
                            ) {
                                convertTokenTextToSpeech(
                                    tokenNumber,
                                    counterId.toString(),
                                    booking_id.toString()
                                )
                            }
                        }
                    } else {
                        // token_management_type == "3": Just refresh UI, outsource speech handled separately
                        Log.d("DEBUG_FLOW", "token_management_type 3: Refreshing UI only")
                        getCountersDetails(type!!)
                    }
                }
            }
        }

        // FIX 3: outsource-token-called handler with full debug logging
        channel.bind("outsource-token-called") { event ->
            Log.d("PUSHER_DEBUG", "OUTSOURCE-TOKEN-CALLED RECEIVED")
            Log.d("PUSHER_DEBUG", "RAW DATA: ${event.data}")
            runOnUiThread {
                // Debug log to verify values at the time the event fires
                Log.d("DEBUG_OUTSOURCE", "token_management_type=$token_management_type")
                Log.d("DEBUG_OUTSOURCE", "type=$type")
                Log.d("DEBUG_OUTSOURCE", "logged_pharmacy_id=$logged_pharmacy_id")
                Log.d("DEBUG_OUTSOURCE", "logged_lab_id=$logged_lab_id")

                if (token_management_type == "3") {
                    val jsonObject = JSONObject(event.data)
                    val outsource_call_id = jsonObject.optString("outsource_call_id", "")
                    val counterId = jsonObject.optString("counter_id", "")
                    val tokenNumber = jsonObject.optString("token_number", "")
                    val lab_id = jsonObject.optInt("lab_id", -1)
                    val pharmacy_id = jsonObject.optInt("pharmacy_id", -1)

                    Log.d("DEBUG_OUTSOURCE", "event pharmacy_id=$pharmacy_id, event lab_id=$lab_id")

                    val isMatch = (type == "3" && logged_lab_id == lab_id.toString()) ||
                            (type == "4" && logged_pharmacy_id == pharmacy_id.toString())

                    Log.d("DEBUG_OUTSOURCE", "isMatch=$isMatch")

                    if (isMatch) {
                        Log.d("DEBUG_FLOW", "Match found. Refreshing UI and calling outsource TTS.")
                        getCountersDetails(type!!)
                        convertOutsourceTokenTextToSpeech(
                            outsource_call_id,
                            counterId,
                            tokenNumber
                        )
                    } else {
                        Log.d("DEBUG_OUTSOURCE", "ID mismatch. Skipping. Expected pharmacy=$logged_pharmacy_id got=$pharmacy_id")
                    }
                } else {
                    Log.d("DEBUG_OUTSOURCE", "BLOCKED: token_management_type=$token_management_type, expected 3")
                }
            }
        }

        btn_settings!!.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        })

        if (!hasUsageStatsPermission(this)) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            Log.d("USAGE", "Permission already granted")
            val dailyUsageBytes = getDailyAppDataUsage(this@HomeActivity1)
            data_usage = formatDataUsage(dailyUsageBytes)
            Log.d("USAGE", "Today's usage: $data_usage")
        }

        pusher.connect()
    }


    override fun onStart() {
        super.onStart()
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT >= 24) {
            playerView!!.onPause()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    private fun initPlayer() {
        mPlayer = SimpleExoPlayer.Builder(this).build()
        playerView.player = mPlayer
        mPlayer!!.playWhenReady = true
        mPlayer!!.prepare()
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun formatDataUsage(bytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            bytes >= gb -> String.format("%.2f GB", bytes / gb)
            bytes >= mb -> String.format("%.2f MB", bytes / mb)
            bytes >= kb -> String.format("%.2f KB", bytes / kb)
            else -> "$bytes B"
        }
    }

    private fun buildMediaSource(): MediaSource {
        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
        val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoList[0].video))
        return mediaSource
    }

    private fun releasePlayer() {
        if (mPlayer == null) {
            return
        }
        mPlayer!!.release()
        mPlayer.stop()
        mPlayer.setPlayWhenReady(false)
    }

    private fun getHomeDetails(type: String) {
        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.getHome(type).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {

                    contents.clear()
                    videoList.clear()
                    doctorsList.clear()

                    var response_from_server = response.body()!!.string()
                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(response_from_server)
                    var jObj_data = jsonObject.getJSONObject("data")

                    if (jObj_data.has("token_management_type")) {
                        token_management_type = jObj_data.getString("token_management_type")
                        // Also persist in case it was not saved at login
                        baseClass.setSharedPreferance(
                            this@HomeActivity1,
                            "token_management_type",
                            token_management_type
                        )
                        Log.d("DEBUG_CONFIG", "token_management_type from getHomeDetails: $token_management_type")
                    }

                    var jsonArry_doctors = jObj_data.getJSONArray("doctors")
                    var jsonArray_counter_tokens = jObj_data.getJSONArray("counter_tokens")
                    var jsonArray_tvShows = jObj_data.getJSONArray("tv_shows")
                    var jsonArray_scrollingContents = jObj_data.getJSONArray("scrolling_contents")
                    var jsonArray_scrollingImages = jObj_data.getJSONArray("scrolling_images")

                    if (jsonArray_scrollingImages.length() > 0) {
                        sliderView!!.visibility = View.VISIBLE
                        for (p in 0 until jsonArray_scrollingImages.length()) {
                            val jObjImages = jsonArray_scrollingImages.getJSONObject(p)
                            val id = jObjImages.getString("id")
                            val image = VIDEO_URL + jObjImages.getString("image")
                            val duration = jObjImages.getString("duration")
                            sliderDataArrayList.add(SliderDataModel(image, id, duration))
                        }

                        adapter = SliderAdapter(this@HomeActivity1, sliderDataArrayList, sliderView)
                        sliderView?.setAutoCycleDirection(SliderView.LAYOUT_DIRECTION_LTR)
                        sliderView?.setSliderAdapter(adapter!!)
                        sliderView?.setAutoCycle(true)
                        sliderView?.startAutoCycle()
                        sliderHandler.post(runnable)
                    }

                    if (jsonArry_doctors.length() > 0) {
                        for (k in 0 until jsonArry_doctors.length()) {
                            val jObjDoctors = jsonArry_doctors.getJSONObject(k)
                            val doctor_id = jObjDoctors.getString("doctor_id")
                            val name = jObjDoctors.getString("name")
                            var room = ""
                            if (jObjDoctors.has("room")) {
                                room = jObjDoctors.getString("room")
                            }
                            val tkObject = jObjDoctors.optJSONObject("tk")
                            var token = tkObject?.optString("token", "")
                            if (token.isNullOrEmpty()) {
                                token = ""
                            }
                            doctorsList.add(DoctorListingModel(doctor_id, name, room, token, "", ""))
                        }

                        if (doctorsList.isNotEmpty()) {
                            recyclerViewDoctor?.isVisible = true
                            btn_settings?.isVisible = false
                            recyclerViewDoctor?.layoutManager = LinearLayoutManager(this@HomeActivity1)

                            if (recyclerViewDoctor?.adapter == null) {
                                doctorAdapter = DoctorAdapter(listOf())
                                recyclerViewDoctor?.adapter = doctorAdapter
                            } else {
                                doctorAdapter = recyclerViewDoctor?.adapter as DoctorAdapter
                            }

                            paginatedDoctors = doctorsList.chunked(pageSize)

                            if (paginatedDoctors.isNotEmpty()) {
                                handler.removeCallbacksAndMessages(null)
                                currentPage = 0
                                doctorAdapter.setDoctors(paginatedDoctors[currentPage])
                                handler.postDelayed({ updateDoctorListPage() }, 15_000)
                            }
                        } else {
                            recyclerViewDoctor?.isVisible = false
                            btn_settings?.isVisible = true
                        }
                    } else {
                        if (type.equals("1")) {
                            var savedDate = baseClass.getSharedPreferance(this@HomeActivity1, "date", "")
                            if (todayString == savedDate) {
                                var apiCall = baseClass.getSharedPreferance(this@HomeActivity1, "apiCall", "")
                                if (apiCall == "true") {
                                    recyclerViewDoctor?.isVisible = false
                                    btn_settings?.isVisible = true
                                } else {
                                    selectDoctors()
                                }
                            }
                        }
                    }

                    if (jsonArray_counter_tokens.length() > 0) {
                        for (k in 0 until jsonArray_counter_tokens.length()) {
                            val jObjCounters = jsonArray_counter_tokens.getJSONObject(k)
                            val counter_id = jObjCounters.getString("counter_id")
                            val token = jObjCounters.getString("token")
                            val call_status = jObjCounters.getString("call_status")
                            val counter_name = jObjCounters.getString("counter_name")
                            val booking_id = jObjCounters.getString("booking_id")
                            countersList.add(CounterListingModel(counter_id, token, call_status, counter_name, booking_id))
                        }

                        if (countersList.isNotEmpty()) {
                            recyclerViewCounter?.isVisible = true
                            btn_settings?.isVisible = false
                            recyclerViewCounter?.layoutManager = LinearLayoutManager(this@HomeActivity1)

                            if (recyclerViewCounter?.adapter == null) {
                                counterAdapter = CounterAdapter(listOf())
                                recyclerViewCounter?.adapter = counterAdapter
                            } else {
                                counterAdapter = recyclerViewCounter?.adapter as CounterAdapter
                            }

                            paginatedCounters = countersList.chunked(pagecounterSize)

                            if (paginatedCounters.isNotEmpty()) {
                                handler.removeCallbacksAndMessages(null)
                                if (currentPage >= paginatedCounters.size) currentPage = 0
                                counterAdapter.setCounters(paginatedCounters[currentPage])
                                handler.postDelayed({ updateCountersListPage() }, 15_000)
                            }
                        } else {
                            recyclerViewCounter?.isVisible = false
                            btn_settings?.isVisible = true
                        }
                    } else {
                        if (type.equals("3") || type.equals("4")) {
                            recyclerViewCounter?.isVisible = false
                            btn_settings?.isVisible = true
                        }
                    }

                    if (jsonArray_tvShows.length() > 0) {
                        for (i in 0 until jsonArray_tvShows.length()) {
                            val jObj = jsonArray_tvShows.getJSONObject(i)
                            val id = jObj.getString("id")
                            val type = jObj.getString("type")
                            val titles = jObj.getString("title")
                            val video = RetrofitClient.ApiUtils.VIDEO_URL + jObj.getString("video")
                            videoList.add(VideoModel(id, type, titles, video))
                        }
                        lin_video!!.visibility = View.GONE
                        playerView!!.visibility = View.VISIBLE
                    } else {
                        lin_video!!.visibility = View.VISIBLE
                        playerView!!.visibility = View.GONE
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
                    scrollingtext!!.isSelected = true
                    scrollingtext!!.setSelected(true)
                    scrollingtext!!.setTextColor(Color.WHITE)
                    scrollingtext!!.startScroll()
                    scrollingtext!!.setText(contentScroll)

                    if (!videoList.isEmpty()) {
                        val videoUri: Uri = Uri.parse(videoList[0].video)
                        val mediaItem: MediaItem = MediaItem.fromUri(videoUri)
                        val httpDataSourceFactory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
                        val defaultDataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(applicationContext, httpDataSourceFactory)
                        val cacheDataSourceFactory = CacheDataSource.Factory()
                            .setCache(App.simpleCache)
                            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
                            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

                        val mediaSource: MediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(mediaItem)
                        playerView.player = mPlayer
                        mPlayer!!.playWhenReady = true
                        mPlayer.setVolume(0.1f)
                        mPlayer!!.setMediaSource(mediaSource, true)
                        mPlayer!!.prepare()

                        mPlayer!!.addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(state: Int) {
                                if (state == Player.STATE_ENDED) {
                                    if (!isVideoEnded) {
                                        if (video_counter < videoList.size - 1) {
                                            video_counter++
                                        } else {
                                            video_counter = 0
                                        }
                                        runTvShows(videoList[video_counter].id, data_usage)
                                        isVideoEnded = true
                                    }

                                    if (video_counter + 1 <= videoList.size) {
                                        if (video_counter == 0) {
                                            val videoUri: Uri = Uri.parse(videoList[0].video)
                                            val mediaItem: MediaItem = MediaItem.fromUri(videoUri)
                                            val httpDataSourceFactory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
                                            val defaultDataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(applicationContext, httpDataSourceFactory)
                                            val cacheDataSourceFactory = CacheDataSource.Factory()
                                                .setCache(App.simpleCache)
                                                .setUpstreamDataSourceFactory(defaultDataSourceFactory)
                                                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                                            val mediaSource: MediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(mediaItem)
                                            mPlayer!!.setMediaSource(mediaSource, true)
                                            mPlayer!!.prepare()
                                        } else {
                                            val videoUri: Uri = Uri.parse(videoList[video_counter].video)
                                            val mediaItem: MediaItem = MediaItem.fromUri(videoUri)
                                            val httpDataSourceFactory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
                                            val defaultDataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(applicationContext, httpDataSourceFactory)
                                            val cacheDataSourceFactory = CacheDataSource.Factory()
                                                .setCache(App.simpleCache)
                                                .setUpstreamDataSourceFactory(defaultDataSourceFactory)
                                                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                                            val mediaSource: MediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(mediaItem)
                                            mPlayer!!.setMediaSource(mediaSource, true)
                                            mPlayer!!.prepare()
                                        }
                                    }
                                } else if (state == Player.STATE_READY) {
                                    isVideoEnded = false
                                }
                            }
                        })
                    }

                } else {
                    if (response.code() == 401) {
                        Toast.makeText(this@HomeActivity1, getString(R.string.unauthenticated), Toast.LENGTH_LONG).show()
                    } else if (response.code() == 500) {
                        Toast.makeText(this@HomeActivity1, response.message(), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@HomeActivity1, response.message(), Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@HomeActivity1, getString(R.string.response_failed), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun updateDoctorListPage() {
        handler.removeCallbacksAndMessages(null)
        if (paginatedDoctors.isNotEmpty()) {
            currentPage = (currentPage + 1) % paginatedDoctors.size
            doctorAdapter.setDoctors(paginatedDoctors[currentPage])
            handler.postDelayed({ updateDoctorListPage() }, 15_000)
        }
        pusher.connect()
    }

    private fun updateCountersListPage() {
        handler.removeCallbacksAndMessages(null)
        if (paginatedCounters.isNotEmpty()) {
            currentPage = (currentPage + 1) % paginatedCounters.size
            counterAdapter.setCounters(paginatedCounters[currentPage])
            handler.postDelayed({ updateCountersListPage() }, 15_000)
        }
        pusher.connect()
    }

    private fun runTvShows(show_id: String, data_usage: String) {
        Log.e("Show ID: ", show_id)
        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.runTvShows(show_id, data_usage).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.e("Status code of run TV shows API", response.code().toString())
                if (response.isSuccessful) {
                    var response_fromServer = response.body()!!.string()
                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(response_fromServer)
                    var status = jsonObject.getString("status")
                    if (status == "true") {
                        Log.e("Video Status...", " Updated")
                    } else {
                        Log.e("Video Status...", " Not updated")
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@HomeActivity1, getString(R.string.response_failed), Toast.LENGTH_LONG).show()
            }
        })
    }

    fun runTvScrollingContent(content_id: String) {
        Log.e("Content ID: ", content_id)
        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.runScrollingContent(content_id).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.e("Scrolling content", response.code().toString())
                if (response.isSuccessful) {
                    var response_fromServer = response.body()!!.string()
                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(response_fromServer)
                    var status = jsonObject.getString("status")
                    if (status == "true") {
                        Log.e("Video Status...", " Updated")
                    } else {
                        Log.e("Video Status...", " Not updated")
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@HomeActivity1, getString(R.string.response_failed), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun settings() {
        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService
        mAPIService.settings().enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    var response_from_server = response.body()!!.string()
                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(response_from_server)
                    var data = jsonObject.getJSONObject("data")
                    var settings = data.getJSONArray("settings")
                    val jObjSettings = settings.getJSONObject(0)
                    var android_tv_update = jObjSettings.getString("android_tv_update")
                    var maintenance = jObjSettings.getString("maintenance")
                    var maintenance_reason = jObjSettings.getString("maintenance_reason")
                    if (android_tv_update == "1") {
                        updateDialog()
                    } else if (maintenance == "1") {
                        maintenanceDialog(maintenance_reason)
                    }
                } else {
                    if (response.code() == 401) {
                        Toast.makeText(this@HomeActivity1, getString(R.string.unauthenticated), Toast.LENGTH_LONG).show()
                    } else if (response.code() == 500) {
                        Toast.makeText(this@HomeActivity1, response.message(), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@HomeActivity1, response.message(), Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@HomeActivity1, getString(R.string.response_failed), Toast.LENGTH_LONG).show()
            }
        })
    }

    fun updateDialog() {
        AlertDialog.Builder(this@HomeActivity1, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Newer version is available. Please update")
            .setCancelable(false)
            .setPositiveButton("UPDATE", DialogInterface.OnClickListener { dialog, which ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                } catch (e: ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                }
            })
            .create().show()
    }

    fun maintenanceDialog(reason: String) {
        AlertDialog.Builder(this@HomeActivity1, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(reason)
            .setCancelable(false)
            .setNegativeButton("Ok", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            }).create().show()
    }

    private fun selectDoctors() {
        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.selectDoctors().enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    var response_from_server = response.body()!!.string()
                    println("Response from Server...." + response_from_server)
                    recyclerViewDoctor?.isVisible = true
                    btn_settings?.isVisible = false
                    baseClass.setSharedPreferance(applicationContext, "apiCall", "true")
                } else {
                    recyclerViewDoctor?.isVisible = false
                    btn_settings?.isVisible = true
                    if (response.code() == 401) {
                        Toast.makeText(applicationContext, applicationContext.getString(R.string.unauthenticated), Toast.LENGTH_LONG).show()
                    } else if (response.code() == 500) {
                        Toast.makeText(applicationContext, applicationContext.getString(R.string.api_error), Toast.LENGTH_LONG).show()
                    } else if (response.code() == 422) {
                        // no action
                    } else {
                        Toast.makeText(applicationContext, response.message(), Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(applicationContext, applicationContext.getString(R.string.response_failed), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun getDoctorDetails(type: String) {
        currentIndex = 0
        val mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.getHome(type).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    doctorsList.clear()
                    val responseFromServer = response.body()!!.string()
                    val jsonObject = JSONObject(responseFromServer)
                    val jObjData = jsonObject.getJSONObject("data")
                    val jsonArrayDoctors = jObjData.getJSONArray("doctors")

                    if (jsonArrayDoctors.length() > 0) {
                        for (k in 0 until jsonArrayDoctors.length()) {
                            val jObjDoctor = jsonArrayDoctors.getJSONObject(k)
                            val doctor_id = jObjDoctor.getString("doctor_id")
                            val name = jObjDoctor.getString("name")
                            val token_call_status = jObjDoctor.getString("call_status")
                            val room = jObjDoctor.optString("room", "")
                            val tkObject = jObjDoctor.optJSONObject("tk")
                            var token = tkObject?.optString("token", "") ?: ""
                            doctorsList.add(DoctorListingModel(doctor_id, name, room, token, "", token_call_status))
                        }

                        if (doctorsList.isNotEmpty()) {
                            recyclerViewDoctor?.isVisible = true
                            btn_settings?.isVisible = false
                            recyclerViewDoctor?.layoutManager = LinearLayoutManager(this@HomeActivity1)

                            if (recyclerViewDoctor?.adapter == null) {
                                doctorAdapter = DoctorAdapter(listOf())
                                recyclerViewDoctor?.adapter = doctorAdapter
                            } else {
                                doctorAdapter = recyclerViewDoctor?.adapter as DoctorAdapter
                            }

                            paginatedDoctors = doctorsList.chunked(pageSize)

                            if (paginatedDoctors.isNotEmpty()) {
                                handler.removeCallbacksAndMessages(null)
                                if (currentPage >= paginatedDoctors.size) currentPage = 0
                                doctorAdapter.setDoctors(paginatedDoctors[currentPage])
                                handler.postDelayed({ updateDoctorListPage() }, 15_000)
                            }
                        } else {
                            recyclerViewDoctor?.isVisible = false
                            btn_settings?.isVisible = true
                        }
                    }
                } else {
                    when (response.code()) {
                        401 -> Toast.makeText(this@HomeActivity1, getString(R.string.unauthenticated), Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@HomeActivity1, getString(R.string.response_failed), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun getCountersDetails(type: String) {
        currentIndex = 0
        val mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.getHome(type).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    countersList.clear()
                    val responseFromServer = response.body()!!.string()
                    val jsonObject = JSONObject(responseFromServer)
                    val jObjData = jsonObject.getJSONObject("data")
                    val jsonArray_counter_tokens = jObjData.getJSONArray("counter_tokens")

                    if (jsonArray_counter_tokens.length() > 0) {
                        for (k in 0 until jsonArray_counter_tokens.length()) {
                            val jObjCounters = jsonArray_counter_tokens.getJSONObject(k)
                            val counter_id = jObjCounters.getString("counter_id")
                            val token = jObjCounters.getString("token")
                            val call_status = jObjCounters.getString("call_status")
                            val counter_name = jObjCounters.getString("counter_name")
                            val booking_id = jObjCounters.getString("booking_id")
                            countersList.add(CounterListingModel(counter_id, token, call_status, counter_name, booking_id))
                        }

                        if (countersList.isNotEmpty()) {
                            recyclerViewCounter?.isVisible = true
                            btn_settings?.isVisible = false
                            recyclerViewCounter?.layoutManager = LinearLayoutManager(this@HomeActivity1)

                            if (recyclerViewCounter?.adapter == null) {
                                counterAdapter = CounterAdapter(listOf())
                                recyclerViewCounter?.adapter = counterAdapter
                            } else {
                                counterAdapter = recyclerViewCounter?.adapter as CounterAdapter
                            }

                            paginatedCounters = countersList.chunked(pagecounterSize)

                            if (paginatedCounters.isNotEmpty()) {
                                handler.removeCallbacksAndMessages(null)
                                if (currentPage >= paginatedCounters.size) currentPage = 0
                                counterAdapter.setCounters(paginatedCounters[currentPage])
                                handler.postDelayed({ updateCountersListPage() }, 15_000)
                            }
                        } else {
                            if (type.equals("3") || type.equals("4")) {
                                recyclerViewCounter?.isVisible = false
                                btn_settings?.isVisible = true
                            }
                        }
                    }
                } else {
                    when (response.code()) {
                        401 -> Toast.makeText(this@HomeActivity1, getString(R.string.unauthenticated), Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@HomeActivity1, getString(R.string.response_failed), Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        type = baseClass.getSharedPreferance(this@HomeActivity1, "type", "")
        mPlayer = SimpleExoPlayer.Builder(this@HomeActivity1).build()
        println(type)
        if (doctor_selected_or_unselected == true) {
            getDoctorDetails(type!!)
        }
        pusher.connect()
    }

    fun isPlayerPlayingWithSound(context: Context, player: SimpleExoPlayer): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isMuted = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
        return player.audioAttributes != null && !isMuted
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    private fun changeTokenStatus(doctor_id: String) {
        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.changeTokenStatus(doctor_id).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(response.body()!!.string())
                    val message: String = jsonObject.getString("message")
                } else {
                    var jsonObject: JSONObject? = null
                    if (response.code() == 400) {
                        jsonObject = JSONObject(response.errorBody()!!.string())
                        val message: String = jsonObject.getString("message")
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            }
        })
    }

    private fun convertTextToSpeech(token_number: String, room_number: String, doctor_id: String) {
        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.convertTextToSpeech(token_number, room_number, doctor_id)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        val audioBytes = response.body()?.bytes()
                        if (audioBytes != null) {
                            if (isPlayerPlayingWithSound(this@HomeActivity1, mPlayer)) {
                                mPlayer.pause()
                            }
                            val tempMp3 = File.createTempFile("audio", ".mp3", cacheDir)
                            val fos = FileOutputStream(tempMp3)
                            fos.write(audioBytes)
                            fos.close()
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(tempMp3.absolutePath)
                                prepare()
                                start()
                                setOnCompletionListener {
                                    release()
                                    mPlayer.play()
                                }
                            }
                        }
                    } else {
                        if (response.code() == 400) {
                            val jsonObject = JSONObject(response.errorBody()!!.string())
                            val message: String = jsonObject.getString("message")
                        }
                    }
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                }
            })
    }

    private fun convertTokenTextToSpeech(token_number: String, counter_id: String, booking_id: String) {
        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.convertTokenTextToSpeech(token_number, counter_id, booking_id)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        val audioBytes = response.body()?.bytes()
                        if (audioBytes != null) {
                            if (isPlayerPlayingWithSound(this@HomeActivity1, mPlayer)) {
                                mPlayer.pause()
                            }
                            val tempMp3 = File.createTempFile("audio", ".mp3", cacheDir)
                            val fos = FileOutputStream(tempMp3)
                            fos.write(audioBytes)
                            fos.close()
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(tempMp3.absolutePath)
                                prepare()
                                start()
                                setOnCompletionListener {
                                    release()
                                    mPlayer.play()
                                }
                            }
                        }
                    } else {
                        if (response.code() == 400) {
                            val jsonObject = JSONObject(response.errorBody()!!.string())
                            val message: String = jsonObject.getString("message")
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                }
            })
    }

    private fun convertOutsourceTokenTextToSpeech(outsource_call_id: String, counter_id: String, token_number: String) {
        Log.d("DEBUG_API", "Outsource ID: $outsource_call_id, Counter: $counter_id, Token: $token_number")
        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.convertOutsourceTokenTextToSpeech(outsource_call_id, counter_id, token_number)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        val audioBytes = response.body()?.bytes()
                        if (audioBytes != null) {
                            if (isPlayerPlayingWithSound(this@HomeActivity1, mPlayer)) {
                                mPlayer.pause()
                            }
                            val tempMp3 = File.createTempFile("audio_outsource", ".mp3", cacheDir)
                            val fos = FileOutputStream(tempMp3)
                            fos.write(audioBytes)
                            fos.close()
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(tempMp3.absolutePath)
                                prepare()
                                start()
                                setOnCompletionListener {
                                    release()
                                    mPlayer.play()
                                }
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("DEBUG_API", "Outsource TTS API Fail: ${t.message}")
                }
            })
    }

    fun getDailyAppDataUsage(context: Context): Long {
        val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val currentTime = System.currentTimeMillis()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val uid = context.applicationInfo.uid
        var totalBytes = 0L

        try {
            val mobileStats = networkStatsManager.queryDetailsForUid(
                ConnectivityManager.TYPE_MOBILE, null, startTime, currentTime, uid
            )
            val mobileBucket = NetworkStats.Bucket()
            while (mobileStats.hasNextBucket()) {
                mobileStats.getNextBucket(mobileBucket)
                totalBytes += mobileBucket.rxBytes + mobileBucket.txBytes
            }
            mobileStats.close()

            val wifiStats = networkStatsManager.queryDetailsForUid(
                ConnectivityManager.TYPE_WIFI, null, startTime, currentTime, uid
            )
            val wifiBucket = NetworkStats.Bucket()
            while (wifiStats.hasNextBucket()) {
                wifiStats.getNextBucket(wifiBucket)
                totalBytes += wifiBucket.rxBytes + wifiBucket.txBytes
            }
            wifiStats.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return totalBytes
    }
}