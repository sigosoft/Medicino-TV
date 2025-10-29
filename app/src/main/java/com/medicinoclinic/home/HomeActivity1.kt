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
    // var recyclerView: InfiniteAutoScrollRecyclerView? = null
    private lateinit var tts: TextToSpeech
    private var currentIndex = 0
    private var repeatCount = 0 // Track repeat status


    var videoView: VideoView? = null
    var lin_video: LinearLayout? = null
    var contents = arrayListOf<ContentModel>()
    var doctorsList = arrayListOf<DoctorListingModel>()
    var recyclerViewDoctor: RecyclerView? = null
    var btn_settings: Button? = null

    //    var scrollingtitle:ScrollTextView? = null
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
    var todayString: String? = null
    var baseClass = BaseClass()
    lateinit var mainHandler: Handler
    var adapter: SliderAdapter? = null
    var sliderDataArrayList: ArrayList<SliderDataModel> = ArrayList()
    var sliderView: SliderView? = null

    //   lateinit var viewPager: ViewPager
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

//    private val updateTextTask = object : Runnable {
//        override fun run() {
//            if (type.equals("1") || type.equals("2")) {
//                getDoctorDetails(type!!)
//            } else {
//                getCountersDetails(type!!) // Re-fetch and update UI
//            }
//            mainHandler.postDelayed(this, 10000)
//        }
//    }

//    private val receiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            if (type.equals("1") || type.equals("2")) {
//                getDoctorDetails(type!!)
//            } else {
//                getCountersDetails(type!!) // Re-fetch and update UI
//            }
//        }
//    }
    val runnable = object : Runnable {

        override fun run() {

            /**
             * Calculate "scroll position" with
             * adapter pages count and current
             * value of scrollPosition.
             */

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
        );
        setContentView(R.layout.activity_home3)
        mainHandler = Handler(Looper.getMainLooper())
        videoView = findViewById(R.id.videoView_ID)
//        scrollingtitle = findViewById(R.id.scrollingtitle)
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
        RetrofitClient.bearer_token = token

        // Initialize TextToSpeech
//        tts = TextToSpeech(this, this)


        Log.e("Token....", token.toString());

        val dateFormat: DateFormat = SimpleDateFormat("dd-MM-yyyy")
        val date = Date()
        todayString = dateFormat.format(date)

        mPlayer = SimpleExoPlayer.Builder(this@HomeActivity1).build()


        baseClass.setSharedPreferance(this@HomeActivity1, "date", todayString);
//        settings()
        getHomeDetails(type!!)


//        val mediaController = MediaController(this@HomeActivity1)
//        mediaController.setAnchorView(videoView)
//        mediaController.setMediaPlayer(videoView)
        btn_settings?.requestFocus()
        recyclerViewDoctor?.requestFocus()

        val options = PusherOptions()
        options.setCluster("ap2") // change to your cluster

        //dev key
        //val pusher = Pusher("3552a5e736f71100d73f", options)
        //live key
        val pusher = Pusher("3ca5933d5ea7c2a5dd5c", options)

        // Connection logging
        pusher.connect(object : ConnectionEventListener {
            override fun onConnectionStateChange(change: ConnectionStateChange) {
                Log.d("Pusher", "State changed from ${change.previousState} to ${change.currentState}")
            }

            override fun onError(message: String?, code: String?, e: Exception?) {
                Log.e("Pusher", "Error: $message, Code: $code, Exception: $e")
            }
        }, ConnectionState.ALL)

        //dev channel
        //val channel: Channel = pusher.subscribe("medicino-tv")

        //live channel
        val channel: Channel = pusher.subscribe("medicino-tv-live")

        channel.bind("token-called") { event ->
            runOnUiThread {
                if (type.equals("1") || type.equals("2")) {
                    var clinicId = 0
                    getDoctorDetails(type!!)
                    val jsonObject = JSONObject(event.data)

                    val doctorId = jsonObject.getInt("doctor_id")
                    if(type.equals("1")){//clinic login
                     clinicId = jsonObject.getInt("clinic_id")
                    }
                    val tokenNumber = jsonObject.getString("token_number")
                    val roomNumber = jsonObject.getString("room_number")
                    if((type.equals("1") && logged_clinic_id.equals(clinicId.toString()))||(type.equals("2") && doctorId.toString().equals(logged_doctor_id))){
                        convertTextToSpeech(tokenNumber, roomNumber, doctorId.toString())
                    }
                } else {
                    var lab_id = 0
                    var pharmacy_id = 0
                    getCountersDetails(type!!) // Re-fetch and update UI
                    val jsonObject = JSONObject(event.data)

                    if(type.equals("3")){//lab login
                        lab_id = jsonObject.getInt("lab_id")
                    }else{
                        pharmacy_id = jsonObject.getInt("pharmacy_id")
                    }
                    val counter_id = jsonObject.getInt("counter_id")
                    val tokenNumber = jsonObject.getInt("token_number")
                    val booking_id = jsonObject.getInt("counter_booking_id")
                    if((type.equals("3") && logged_lab_id.equals(lab_id.toString()))||(type.equals("4") && pharmacy_id.toString().equals(logged_pharmacy_id))){
                        convertTokenTextToSpeech(tokenNumber.toString(),counter_id.toString(),booking_id.toString())
                    }

                }

                Log.d("PusherEvent", "Received: ${event.data}")
                //Toast.makeText(this, "Message: ${event.data}", Toast.LENGTH_SHORT).show()
            }
        }

//        pusher.connect()

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
    }


    override fun onStart() {
        super.onStart()


    }


    override fun onPause() {
        super.onPause()
       // mainHandler.removeCallbacks(updateTextTask)
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
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
//        mPlayer.setVolume(0.0f)
//        mPlayer!!.setMediaSource(buildMediaSource())
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
        // Create a data source factory.
        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()

        // Create a progressive media source pointing to a stream uri.
        val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoList[0].video))
        return mediaSource
    }

    private fun releasePlayer() {
        if (mPlayer == null) {
            return
        }
        //release player when done
        mPlayer!!.release()
        mPlayer.stop();
        mPlayer.setPlayWhenReady(false);
//        mPlayer = null
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


                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(response_from_server)
                    var jObj_data = jsonObject.getJSONObject("data")
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
                            } else {
                                room = ""
                            }
                            val tkObject =
                                jObjDoctors.optJSONObject("tk") // Use optJSONObject() to avoid crash

                            var token = tkObject?.optString(
                                "token",
                                ""
                            ) // Use optString() with a default value

                            if (token.isNullOrEmpty()) {
                                token = "";
                            }
                            //val token = jObjDoctors.getJSONObject("tk").getString("token")
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


//                        if (doctorsList.size > 0) {
//                            recyclerViewDoctor?.isVisible = true
//                            btn_settings?.isVisible = false
//                            recyclerViewDoctor?.layoutManager =
//                                LinearLayoutManager(this@HomeActivity1)
//                            val adapter = DoctorAdapter(doctorsList)
//                            recyclerViewDoctor?.adapter = adapter
//                        } else {
//
//                            recyclerViewDoctor?.isVisible = false
//                            btn_settings?.isVisible = true
//
//                        }
                        if (doctorsList.isNotEmpty()) {
                            recyclerViewDoctor?.isVisible = true
                            btn_settings?.isVisible = false

                            recyclerViewDoctor?.layoutManager =
                                LinearLayoutManager(this@HomeActivity1)
                            doctorAdapter = DoctorAdapter(doctorsList)// initially empty
                            recyclerViewDoctor?.adapter = doctorAdapter

                            paginatedDoctors = doctorsList.chunked(pageSize)

                            if (paginatedDoctors.isNotEmpty()) {
                                currentPage = 0
                                doctorAdapter.setDoctors(paginatedDoctors[currentPage])
                                Log.d("DoctorPaging", "Initial Page: 0")
                                handler.postDelayed({ updateDoctorListPage() }, 15_000)
                            }
                        } else {
                            recyclerViewDoctor?.isVisible = false
                            btn_settings?.isVisible = true
                        }

                    } else {
                        if (type.equals("1")) {
                            var savedDate =
                                baseClass.getSharedPreferance(this@HomeActivity1, "date", "")
                            if (todayString == savedDate) {
                                var apiCall =
                                    baseClass.getSharedPreferance(this@HomeActivity1, "apiCall", "")
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
                            val booking_date = jObjCounters.getString("booking_date")

                            countersList.add(
                                CounterListingModel(
                                    counter_id, token, call_status, counter_name, booking_date
                                )
                            )
                        }

                        if (countersList.isNotEmpty()) {
                            recyclerViewCounter?.isVisible = true
                            btn_settings?.isVisible = false

                            recyclerViewCounter?.layoutManager =
                                LinearLayoutManager(this@HomeActivity1)
                            counterAdapter = CounterAdapter(countersList)// initially empty
                            recyclerViewCounter?.adapter = counterAdapter

                            paginatedCounters = countersList.chunked(pagecounterSize)

                            if (paginatedCounters.isNotEmpty()) {
                                currentPage = 0
                                counterAdapter.setCounters(paginatedCounters[currentPage])
                                Log.d("CountersPaging", "Initial counter Page: 0")
                                handler.postDelayed({ updateCountersListPage() }, 15_000)
                            }
                        } else {
                            recyclerViewCounter?.isVisible = false
                            btn_settings?.isVisible = true
                        }

                    } else {
                        if (type.equals("3") || type.equals("4")) {//for lab and pharmacy
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
                            val outFile = File(cacheDir, "yourVideoName.mp4")

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
                    /* scrollingtitle!!.setText(title)
                     scrollingtitle!!.setTextColor(Color.WHITE);
                     scrollingtitle!!.startScroll();

                     scrollingtitle!!.setSelected(true)*/;
                    scrollingtext!!.isSelected = true
                    scrollingtext!!.setSelected(true);
                    scrollingtext!!.setTextColor(Color.WHITE);
                    scrollingtext!!.startScroll();
                    scrollingtext!!.setText(contentScroll)

                    if (!videoList.isEmpty()) {
                        val videoUri: Uri = Uri.parse(videoList[0].video)
                        val mediaItem: MediaItem = MediaItem.fromUri(videoUri)
                        val httpDataSourceFactory =
                            DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
                        val defaultDataSourceFactory: DataSource.Factory =
                            DefaultDataSourceFactory(applicationContext, httpDataSourceFactory)
                        val cacheDataSourceFactory = CacheDataSource.Factory()
                            .setCache(App.simpleCache)
                            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
                            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

                        val mediaSource: MediaSource =
                            ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                                .createMediaSource(mediaItem)
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
                                        runTvShows(videoList[video_counter].id, data_usage);
                                        isVideoEnded = true
                                    }
//
                                    if (video_counter + 1 <= videoList.size) {
//                                        video_counter++
                                        if (video_counter == 0) {
                                            val videoUri: Uri = Uri.parse(videoList[0].video)
                                            val mediaItem: MediaItem = MediaItem.fromUri(videoUri)
                                            val httpDataSourceFactory =
                                                DefaultHttpDataSource.Factory()
                                                    .setAllowCrossProtocolRedirects(true)
                                            val defaultDataSourceFactory: DataSource.Factory =
                                                DefaultDataSourceFactory(
                                                    applicationContext,
                                                    httpDataSourceFactory
                                                )
                                            val cacheDataSourceFactory = CacheDataSource.Factory()
                                                .setCache(App.simpleCache)
                                                .setUpstreamDataSourceFactory(
                                                    defaultDataSourceFactory
                                                )
                                                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

                                            val mediaSource: MediaSource =
                                                ProgressiveMediaSource.Factory(
                                                    cacheDataSourceFactory
                                                )
                                                    .createMediaSource(mediaItem)
                                            mPlayer!!.setMediaSource(mediaSource, true)
                                            mPlayer!!.prepare()
//                                            video_counter = 1


                                        } else {
                                            val videoUri: Uri =
                                                Uri.parse(videoList[video_counter].video)
                                            val mediaItem: MediaItem = MediaItem.fromUri(videoUri)
                                            val httpDataSourceFactory =
                                                DefaultHttpDataSource.Factory()
                                                    .setAllowCrossProtocolRedirects(true)
                                            val defaultDataSourceFactory: DataSource.Factory =
                                                DefaultDataSourceFactory(
                                                    applicationContext,
                                                    httpDataSourceFactory
                                                )
                                            val cacheDataSourceFactory = CacheDataSource.Factory()
                                                .setCache(App.simpleCache)
                                                .setUpstreamDataSourceFactory(
                                                    defaultDataSourceFactory
                                                )
                                                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

                                            val mediaSource: MediaSource =
                                                ProgressiveMediaSource.Factory(
                                                    cacheDataSourceFactory
                                                )
                                                    .createMediaSource(mediaItem)
                                            mPlayer!!.setMediaSource(mediaSource, true)
                                            mPlayer!!.prepare()
                                        }


//                              videoView!!.start()
//                        }
                                        // your code
                                    }
                                } else if (state == Player.STATE_READY) {
                                    isVideoEnded = false
                                }
                            }

                        }
                        );
                    }


//                    videoView!!.setVideoURI(Uri.parse(videoList[0].video))


//                videoView!!.setOnPreparedListener(object : MediaPlayer.OnPreparedListener {
//                    override fun onPrepared(mp: MediaPlayer) {
//                        mp.start();
//
//                    }
//                })

//                    videoView!!.setOnCompletionListener(OnCompletionListener {
//
//                        if (video_counter + 1 <= videoList.size) {
//                            video_counter++
//                            if(video_counter==videoList.size){
//                                videoView!!.setVideoURI(Uri.parse(videoList[0].video))
//                                video_counter = 0
//                            }else {
//                                videoView!!.setVideoURI(Uri.parse(videoList[video_counter].video))
//                            }
//                            videoView!!.start()
//                        }
//
//                    })


                } else {

                    if (response.code() == 401) {
                        Toast.makeText(
                            this@HomeActivity1,
                            getString(R.string.unauthenticated),
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (response.code() == 500) {
                        Toast.makeText(
                            this@HomeActivity1,
                            response.message(),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
//                        Toast.makeText(
//                            this@HomeActivity,
//                            getString(R.string.server_error),
//                            Toast.LENGTH_LONG
//                        ).show()
                        Toast.makeText(
                            this@HomeActivity1,
                            response.message(),
                            Toast.LENGTH_LONG
                        ).show()

                    }

                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    this@HomeActivity1,
                    getString(R.string.response_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
//
    private fun updateDoctorListPage() {
        if (paginatedDoctors.isNotEmpty()) {
            currentPage = (currentPage + 1) % paginatedDoctors.size
            Log.d("DoctorPaging", "Showing page: $currentPage / ${paginatedDoctors.size}")
            doctorAdapter.setDoctors(paginatedDoctors[currentPage])
            handler.postDelayed({ updateDoctorListPage() }, 15_000)
        }
    }

    private fun updateCountersListPage() {
        if (paginatedCounters.isNotEmpty()) {
            currentPage = (currentPage + 1) % paginatedCounters.size
            Log.d("CountersPaging", "Showing page: $currentPage / ${paginatedCounters.size}")
            counterAdapter.setCounters(paginatedCounters[currentPage])
            handler.postDelayed({ updateCountersListPage() }, 15_000)
        }
    }

    private fun runTvShows(show_id: String, data_usage: String) {
        Log.e("Show ID: ", show_id)
        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.runTvShows(show_id, data_usage).enqueue(object :
            Callback<ResponseBody> {
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
                Toast.makeText(
                    this@HomeActivity1,
                    getString(R.string.response_failed),
                    Toast.LENGTH_LONG
                ).show()
            }

        })
    }


    fun runTvScrollingContent(content_id: String) {
        Log.e("Content ID: ", content_id)

        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.runScrollingContent(content_id).enqueue(object :
            Callback<ResponseBody> {
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
                Toast.makeText(
                    this@HomeActivity1,
                    getString(R.string.response_failed),
                    Toast.LENGTH_LONG
                ).show()
            }

        })
    }

    private fun settings() {
        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService
        mAPIService.settings().enqueue(object :
            Callback<ResponseBody> {

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
                        Toast.makeText(
                            this@HomeActivity1,
                            getString(R.string.unauthenticated),
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (response.code() == 500) {
                        Toast.makeText(
                            this@HomeActivity1,
                            response.message(),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@HomeActivity1,
                            response.message(),
                            Toast.LENGTH_LONG
                        ).show()

                    }

                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    this@HomeActivity1,
                    getString(R.string.response_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    fun updateDialog() {
        AlertDialog.Builder(this@HomeActivity1, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Newer version is available. Please update")
            .setCancelable(false)
            .setPositiveButton("UPDATE", DialogInterface.OnClickListener { dialog, which ->
                try {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=$packageName")
                        )
                    )
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                        )
                    )
                }
            })
            /*.setNegativeButton("NO", DialogInterface.OnClickListener { dialogInterface, i ->
              dialogInterface.dismiss()
            }
            )*/
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
        mAPIService.selectDoctors().enqueue(object :
            Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {

                    var response_from_server = response.body()!!.string()
                    println("Response from Server...." + response_from_server)

                    recyclerViewDoctor?.isVisible = true
                    btn_settings?.isVisible = false
                    baseClass.setSharedPreferance(applicationContext, "apiCall", "true");


                } else {
                    recyclerViewDoctor?.isVisible = false
                    btn_settings?.isVisible = true

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
                    } else if (response.code() == 422) {

                    } else {
                        Toast.makeText(
                            applicationContext,
                            response.message(),
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
                            doctorsList.add(
                                DoctorListingModel(
                                    doctor_id, name, room, token, "", token_call_status
                                )
                            )
                        }

                        if (doctorsList.isNotEmpty()) {
                            recyclerViewDoctor?.isVisible = true
                            btn_settings?.isVisible = false

                            recyclerViewDoctor?.layoutManager =
                                LinearLayoutManager(this@HomeActivity1)
                             doctorAdapter = DoctorAdapter(listOf()) // initially empty
                            recyclerViewDoctor?.adapter = doctorAdapter

                            paginatedDoctors = doctorsList.chunked(pageSize)

                            if (paginatedDoctors.isNotEmpty()) {
                                currentPage = 0
                                doctorAdapter.setDoctors(paginatedDoctors[currentPage])
                                Log.d("DoctorPaging1", "Initial Page: 0")
                                handler.postDelayed({ updateDoctorListPage() }, 15_000)
                            }
                            //speakOut()
                        } else {
                            recyclerViewDoctor?.isVisible = false
                            btn_settings?.isVisible = true
                        }
                    }
                } else {
                    when (response.code()) {
                        401 -> Toast.makeText(
                            this@HomeActivity1,
                            getString(R.string.unauthenticated),
                            Toast.LENGTH_LONG
                        ).show()

//                        500 -> Toast.makeText(
//                            this@HomeActivity1,
//                            getString(R.string.api_error) + "2 "+response.toString(),
//                            Toast.LENGTH_LONG
//                        ).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    this@HomeActivity1,
                    getString(R.string.response_failed),
                    Toast.LENGTH_LONG
                ).show()
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

                            countersList.add(
                                CounterListingModel(
                                    counter_id, token, call_status, counter_name, booking_id
                                )
                            )
                        }

                        if (countersList.isNotEmpty()) {
                            recyclerViewCounter?.isVisible = true
                            btn_settings?.isVisible = false

                            recyclerViewCounter?.layoutManager =
                                LinearLayoutManager(this@HomeActivity1)
                            counterAdapter = CounterAdapter(listOf()) // initially empty
                            recyclerViewCounter?.adapter = counterAdapter

                            paginatedCounters = countersList.chunked(pagecounterSize)

                            if (paginatedCounters.isNotEmpty()) {
                                currentPage = 0
                                counterAdapter.setCounters(paginatedCounters[currentPage])
                                Log.d("counters Paging1", "Initial Page: 0")
                                handler.postDelayed({ updateCountersListPage() }, 15_000)
                            }
                            //speakOutCounterToken()
                        } else {
                            if (type.equals("3") || type.equals("4")) {//for lab and pharmacy
                                recyclerViewCounter?.isVisible = false
                                btn_settings?.isVisible = true
                            }
                        }
                    }
                } else {
                    when (response.code()) {
                        401 -> Toast.makeText(
                            this@HomeActivity1,
                            getString(R.string.unauthenticated),
                            Toast.LENGTH_LONG
                        ).show()

//                        500 -> Toast.makeText(
//                            this@HomeActivity1,
//                            getString(R.string.api_error) + "2 "+response.toString(),
//                            Toast.LENGTH_LONG
//                        ).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    this@HomeActivity1,
                    getString(R.string.response_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

//    private fun getDoctorDetails1(type: String) {
//        currentIndex = 0
//        var mAPIService: APIService? = null
//        mAPIService = RetrofitClient.ApiUtils.apiService1
//        mAPIService.getHome(type).enqueue(object :
//            Callback<ResponseBody> {
//
//            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
//                if (response.isSuccessful) {
//                    doctorsList.clear()
//                    Log.d("FCM", "Message Received: ${response.code()}")
//                    var response_from_server = response.body()!!.string()
//                    var jsonObject: JSONObject? = null
//                    jsonObject = JSONObject(response_from_server)
//                    var jObj_data = jsonObject.getJSONObject("data")
//                    var jsonArry_doctors = jObj_data.getJSONArray("doctors")
//
//                    if (jsonArry_doctors.length() > 0) {
//                        for (k in 0 until jsonArry_doctors.length()) {
//                            val jObjDoctors = jsonArry_doctors.getJSONObject(k)
//                            val doctor_id = jObjDoctors.getString("doctor_id")
//                            val name = jObjDoctors.getString("name")
//                            val token_call_status = jObjDoctors.getString("call_status")
//                            var room = ""
//                            if (jObjDoctors.has("room")) {
//                                room = jObjDoctors.getString("room")
//                            } else {
//                                room = ""
//                            }
//                            val tkObject =
//                                jObjDoctors.optJSONObject("tk") // Use optJSONObject() to avoid crash
//
//                            var token = tkObject?.optString(
//                                "token",
//                                ""
//                            ) // Use optString() with a default value
//
//                            if (token.isNullOrEmpty()) {
//                                token = "";
//                            }
//                            // val token = jObjDoctors.getJSONObject("tk").getString("token")
//                            // val date = jObjDoctors.getString("date")
//
//                            doctorsList.add(
//                                DoctorListingModel(
//                                    doctor_id,
//                                    name,
//                                    room,
//                                    token,
//                                    "",
//                                    token_call_status
//                                )
//                            )
//
//                        }
//                        if (doctorsList.size > 0) {
//                            recyclerViewDoctor?.isVisible = true
//                            btn_settings?.isVisible = false
//                            recyclerViewDoctor?.layoutManager =
//                                LinearLayoutManager(this@HomeActivity1)
//                            val adapter = DoctorAdapter(doctorsList)
//                            recyclerViewDoctor?.adapter = adapter
//                            speakOut()
//
//                        } else {
//                            recyclerViewDoctor?.isVisible = false
//                            btn_settings?.isVisible = true
//
//                        }
//                    }
//
//
//                } else {
//
//                    if (response.code() == 401) {
//                        Toast.makeText(
//                            this@HomeActivity1,
//                            getString(R.string.unauthenticated),
//                            Toast.LENGTH_LONG
//                        ).show()
//                    } else if (response.code() == 500) {
//                        Toast.makeText(
//                            this@HomeActivity1,
//                            getString(R.string.api_error) + "2",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    } else {
////                        Toast.makeText(
////                            this@HomeActivity,
////                            getString(R.string.server_error),
////                            Toast.LENGTH_LONG
////                        ).show()
//
//                    }
//
//                }
//            }
//
//            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                Toast.makeText(
//                    this@HomeActivity1,
//                    getString(R.string.response_failed),
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        })
//    }


    override fun onResume() {
        super.onResume()
//        LocalBroadcastManager.getInstance(this)
//            .registerReceiver(receiver, IntentFilter("doctor_data_updated"))
        //mainHandler.post(updateTextTask)
        type = baseClass.getSharedPreferance(this@HomeActivity1, "type", "")
        mPlayer = SimpleExoPlayer.Builder(this@HomeActivity1).build()
        println(type)
        if (doctor_selected_or_unselected == true) {//if doctor is selected or unselected from settings page the home data should refresh when clicking back from settings
//            getHomeDetails(type!!)
            getDoctorDetails(type!!)
        }
    }

//    override fun onInit(status: Int) {
//        if (status == TextToSpeech.SUCCESS) {
//            val result = tts.setLanguage(Locale("ml", "IN")) // Set Malayalam Language
//            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                Log.e("TTS", "Malayalam not supported!")
//            }
////            tts.language = Locale.ENGLISH  // Set language
////            tts.setSpeechRate(0.65f)  // Normal speed (1.0), Increase for faster speech
////            tts.setPitch(0.8f)
//            tts.setSpeechRate(0.5f)  // Normal speed (1.0), Increase for faster speech
//            tts.setPitch(1.2f)
////            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
////                override fun onStart(utteranceId: String?) {}
////                override fun onDone(utteranceId: String?) {
////                    runOnUiThread {
////                        currentIndex++
////                        speakOut()
////                    }
////                }
//
////                override fun onError(utteranceId: String?) {}
////            })
//        }
//    }

//    private fun speakOut() {
//        if (currentIndex < doctorsList.size) {
//            val text = doctorsList[currentIndex]
////            println("Response code of token_call_status: " + currentIndex+ " "+text.token_call_status)
//            if (text.token_call_status == "false") {
//                // Change token_call_status to true
//                //changeTokenStatus(text.doctor_id)
//                convertTextToSpeech(text.token, text.room, text.doctor_id)
//            } else {
//                // Skip if the token has already been called
//                currentIndex++
//                speakOut() // Call the next token
//            }
//        }
//    }
//
//    private fun speakOutCounterToken() {
//        if (currentIndex < countersList.size) {
//            val text = countersList[currentIndex]
////            println("Response code of token_call_status: " + currentIndex+ " "+text.token_call_status)
////            if (text.call_status == "false") {
//            if(text.token!="null") {
//                convertTokenTextToSpeech(text.token, text.counter_id, text.booking_id)
//            }
////            } else {
//            // Skip if the token has already been called
//            currentIndex++
//            //speakOutCounterToken() // Call the next token
//            handler.postDelayed({ speakOutCounterToken() }, 4000)
//            // }
//        }
//    }

    fun isPlayerPlayingWithSound(context: Context, player: SimpleExoPlayer): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isMuted = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
        return player.audioAttributes != null && !isMuted
    }

    override fun onDestroy() {
        // Shutdown TTS to free resources
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    private fun changeTokenStatus(doctor_id: String) {

//        val progressDialog = ProgressDialog(context, R.style.MyTheme)
//        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Large)
//        progressDialog.show()
//        progressDialog.setCancelable(false)
//

        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.changeTokenStatus(doctor_id).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

//                println("Response code of change token status API: " + response.code())

                if (response.isSuccessful) {
//                    progressDialog.dismiss()
                    var response_from_server = response.body()
                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(response.body()!!.string())
                    val message: String = jsonObject.getString("message")

//                    Toast.makeText(context,message,Toast.LENGTH_LONG).show()

                } else {
//                    progressDialog.dismiss()
                    var jsonObject: JSONObject? = null


                    if (response.code() == 400) {
                        jsonObject = JSONObject(response.errorBody()!!.string())
                        val message: String = jsonObject.getString("message")

//                        Toast.makeText(
//                            context,
//                            message,
//                            Toast.LENGTH_LONG
//                        ).show()
                    } else if (response.code() == 500) {
//                        Toast.makeText(
//                            context,
//                            context.getString(R.string.api_error),
//                            Toast.LENGTH_LONG
//                        ).show()
                    } else {
//
//                        Toast.makeText(
//                            context,
//                            context.getString(R.string.something_wrong),
//                            Toast.LENGTH_LONG
//                        ).show()

                    }

                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                Toast.makeText(
//                    context,
//                    context.getString(R.string.response_failed),
//                    Toast.LENGTH_LONG
//                ).show()
//                progressDialog.dismiss()
            }
        })
    }

    private fun convertTextToSpeech(token_number: String, room_number: String, doctor_id: String) {

        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.convertTextToSpeech(token_number, room_number, doctor_id)
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
//                println("Response code of convertTextToSpeech API: " + response.code())
                    if (response.isSuccessful) {
                        val audioBytes = response.body()?.bytes()
                        if (audioBytes != null) {//to pause the media player if it is already playing(ad pausing with sound)
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
//                                    repeatCount++
//                                    if (repeatCount < 2) { // Play one more time
//                                        start()
//                                    } else {
                                    release()
                                    mPlayer.play()

//                                        repeatCount=0
//                                    }
                                }
                            }


                        }
                    } else {
//                    progressDialog.dismiss()
                        var jsonObject: JSONObject? = null


                        if (response.code() == 400) {
                            jsonObject = JSONObject(response.errorBody()!!.string())
                            val message: String = jsonObject.getString("message")

//                        Toast.makeText(
//                            context,
//                            message,
//                            Toast.LENGTH_LONG
//                        ).show()
                        } else if (response.code() == 500) {
//                        Toast.makeText(
//                            context,
//                            context.getString(R.string.api_error),
//                            Toast.LENGTH_LONG
//                        ).show()
                        } else {
//
//                        Toast.makeText(
//                            context,
//                            context.getString(R.string.something_wrong),
//                            Toast.LENGTH_LONG
//                        ).show()

                        }

                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                Toast.makeText(
//                    context,
//                    context.getString(R.string.response_failed),
//                    Toast.LENGTH_LONG
//                ).show()
//                progressDialog.dismiss()
                }
            })
    }

    private fun convertTokenTextToSpeech(
        token_number: String,
        counter_id: String,
        booking_id: String
    ) {

        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.convertTokenTextToSpeech(token_number, counter_id, booking_id)
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
//                println("Response code of convertTextToSpeech API: " + response.code())
                    if (response.isSuccessful) {
                        val audioBytes = response.body()?.bytes()
                        if (audioBytes != null) {//to pause the media player if it is already playing(ad pausing with sound)
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
//                                    repeatCount++
//                                    if (repeatCount < 2) { // Play one more time
//                                        start()
//                                    } else {
                                    release()
                                    mPlayer.play()

//                                        repeatCount=0
//                                    }
                                }
                            }


                        }
                    } else {
//                    progressDialog.dismiss()
                        var jsonObject: JSONObject? = null


                        if (response.code() == 400) {
                            jsonObject = JSONObject(response.errorBody()!!.string())
                            val message: String = jsonObject.getString("message")

//                        Toast.makeText(
//                            context,
//                            message,
//                            Toast.LENGTH_LONG
//                        ).show()
                        } else if (response.code() == 500) {
//                        Toast.makeText(
//                            context,
//                            context.getString(R.string.api_error),
//                            Toast.LENGTH_LONG
//                        ).show()
                        } else {
//
//                        Toast.makeText(
//                            context,
//                            context.getString(R.string.something_wrong),
//                            Toast.LENGTH_LONG
//                        ).show()

                        }

                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                Toast.makeText(
//                    context,
//                    context.getString(R.string.response_failed),
//                    Toast.LENGTH_LONG
//                ).show()
//                progressDialog.dismiss()
                }
            })
    }

    fun getDailyAppDataUsage(context: Context): Long {
        val networkStatsManager =
            context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
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
            // MOBILE data
            val mobileStats = networkStatsManager.queryDetailsForUid(
                ConnectivityManager.TYPE_MOBILE, null, startTime, currentTime, uid
            )
            val mobileBucket = NetworkStats.Bucket()
            while (mobileStats.hasNextBucket()) {
                mobileStats.getNextBucket(mobileBucket)
                totalBytes += mobileBucket.rxBytes + mobileBucket.txBytes
            }
            mobileStats.close()

            // WIFI data
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



