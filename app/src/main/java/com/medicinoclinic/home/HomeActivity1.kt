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
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
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
import com.medicinoclinic.BuildConfig
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
import com.medicinoclinic.utils.TokenAnnouncementQueue
import com.smarteist.autoimageslider.SliderView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
import com.pusher.client.channel.PrivateChannelEventListener
import com.pusher.client.channel.PusherEvent
import com.pusher.client.channel.SubscriptionEventListener
import com.pusher.client.util.HttpChannelAuthorizer
import org.json.JSONArray
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
    var todayString: String? = null
    var baseClass = BaseClass()
    lateinit var mainHandler: Handler

    private val counterSetupLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                type?.let { getHomeDetails(it) }
            }
        }
    var adapter: SliderAdapter? = null
    var sliderDataArrayList: ArrayList<SliderDataModel> = ArrayList()
    var sliderView: SliderView? = null

    lateinit var mPlayer: SimpleExoPlayer
    private lateinit var playerView: PlayerView
    val sliderHandler = Handler(Looper.getMainLooper())
    var count: Int? = 0
    var data_usage: String = "0 B"
    private var videoPlayerListener: Player.Listener? = null
    private lateinit var announcementQueue: TokenAnnouncementQueue
    private var videoVolumeBeforeAnnouncement = 0.1f
    private var isVideoPausedForAnnouncement = false

    private var paginatedDoctors: List<List<DoctorListingModel>> = listOf()
    private var currentPage = 0
    private var counterCurrentPage = 0
    private val pageSize = 4
    private val handler = Handler(Looper.getMainLooper())
    private val doctorPageRunnable = Runnable { updateDoctorListPage() }
    private val counterPageRunnable = Runnable { updateCountersListPage() }
    private lateinit var doctorAdapter: DoctorAdapter

    var countersList = arrayListOf<CounterListingModel>()
    private lateinit var counterAdapter: CounterAdapter
    private var paginatedCounters: List<List<CounterListingModel>> = listOf()
    var recyclerViewCounter: RecyclerView? = null
    private val pagecounterSize = 5
    private val sessionId: Long = System.currentTimeMillis()

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
        createStatusBanner()
        registerNetworkCallback()
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

        Log.d("DEBUG_STARTUP", "type=$type")
        Log.d("DEBUG_STARTUP", "logged_pharmacy_id=$logged_pharmacy_id")
        Log.d("DEBUG_STARTUP", "logged_lab_id=$logged_lab_id")

        RetrofitClient.bearer_token = token

        activeSessionId = sessionId
        disconnectGlobalPusher()

        announcementQueue = TokenAnnouncementQueue(
            context = this,
            apiService = RetrofitClient.ApiUtils.apiService1,
            onAnnouncementStart = { pauseVideoForAnnouncement() },
            onAllAnnouncementsFinished = { resumeVideoAfterAnnouncement() }
        )

        Log.e("Token....", token.toString())

        val dateFormat: DateFormat = SimpleDateFormat("dd-MM-yyyy")
        val date = Date()
        todayString = dateFormat.format(date)

        mPlayer = SimpleExoPlayer.Builder(this@HomeActivity1).build()
        attachVideoPlayerListener()

        baseClass.setSharedPreferance(this@HomeActivity1, "date", todayString)
        getHomeDetails(type!!)

        btn_settings?.requestFocus()
        recyclerViewDoctor?.requestFocus()

        setUpPusher()

        btn_settings!!.setOnClickListener(View.OnClickListener {
            openSettingsScreen()
        })

        if (!hasUsageStatsPermission(this)) {
            // QA-01: many Android TV / Google TV / Fire TV devices have no Usage Access screen.
            // Starting it there threw ActivityNotFoundException and crashed the app after every
            // login (and on every relaunch, since the session was already saved). Open it only
            // where it exists; otherwise just carry on without data-usage reporting.
            // (try/catch rather than resolveActivity(): on Android 11+ package visibility can make
            // resolveActivity() return null even when the screen exists.)
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.w("USAGE", "No Usage Access screen on this device; skipping data-usage reporting")
            } catch (e: Exception) {
                Log.e("USAGE", "Could not open Usage Access settings", e)
            }
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
    }

    override fun onStop() {
        // Do NOT disconnect Pusher or shut down announcement queue here.
        // Leaving Settings / brief onStop would kill token updates and TTS until restart.
        handler.removeCallbacks(doctorPageRunnable)
        handler.removeCallbacks(counterPageRunnable)
        stopListPolling()
        super.onStop()
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
        if (!::mPlayer.isInitialized) return
        videoPlayerListener?.let { mPlayer.removeListener(it) }
        videoPlayerListener = null
        mPlayer.playWhenReady = false
        mPlayer.stop()
        mPlayer.release()
    }

    private fun attachVideoPlayerListener() {
        if (videoPlayerListener != null) return
        videoPlayerListener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                    reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
                ) {
                    return
                }
                if (videoList.isNotEmpty()) {
                    runTvShows(videoList[video_counter].id, data_usage)
                    video_counter = (video_counter + 1) % videoList.size
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("PLAYER_ERROR", "Playback failed: ${error.errorCodeName}", error)
                if (mPlayer.hasNextMediaItem()) {
                    mPlayer.seekToNextMediaItem()
                } else {
                    mPlayer.seekTo(0, 0)
                }
                mPlayer.prepare()
                if (!isVideoPausedForAnnouncement) {
                    mPlayer.playWhenReady = true
                }
            }
        }
        mPlayer.addListener(videoPlayerListener!!)
    }

    private fun createMediaSourceForVideo(videoUrl: String): MediaSource {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
        val defaultDataSourceFactory: DataSource.Factory =
            DefaultDataSourceFactory(applicationContext, httpDataSourceFactory)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(App.simpleCache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return ProgressiveMediaSource.Factory(cacheDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(videoUrl)))
    }

    private fun startVideoPlayback() {
        if (videoList.isEmpty() || !::mPlayer.isInitialized) return

        val mediaSources = videoList.map { createMediaSourceForVideo(it.video) }

        playerView.player = mPlayer
        mPlayer.setMediaSources(mediaSources, true)
        mPlayer.repeatMode = Player.REPEAT_MODE_ALL
        mPlayer.volume = 0.1f
        if (!isVideoPausedForAnnouncement) {
            mPlayer.playWhenReady = true
        }
        mPlayer.prepare()
        video_counter = 0
    }

    private fun getHomeDetails(type: String) {
        homeLoadInFlight = true
        mainHandler.removeCallbacks(homeRetryRunnable)
        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.getHome(type).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                homeLoadInFlight = false
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful) {

                    contents.clear()
                    videoList.clear()
                    doctorsList.clear()
                    countersList.clear()

                    var response_from_server = response.body()!!.string()
                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(response_from_server)
                    var jObj_data = jsonObject.getJSONObject("data")

                    if (requiresCounterSetup(jObj_data) && !isCountersSet(jObj_data)) {
                        openCounterSetupScreen()
                        return
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
                            bindDoctorsToUi(resetPage = true)
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
                        bindCountersToUi(type, parseCounterTokens(jsonArray_counter_tokens))
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
                        lin_video!!.visibility = View.GONE
                        playerView!!.visibility = View.VISIBLE
                        startVideoPlayback()
                    }

                    // QA-07: the screen has its content now; stop retrying, clear the banner.
                    homeLoaded = true
                    homeRetryAttempt = 0
                    markSynced()

                } else {
                    if (response.code() == 401) {
                        // RetrofitClient sends the TV back to login; nothing to retry.
                        Toast.makeText(this@HomeActivity1, getString(R.string.unauthenticated), Toast.LENGTH_LONG).show()
                    } else if (response.code() == 500) {
                        Toast.makeText(this@HomeActivity1, response.message(), Toast.LENGTH_LONG).show()
                        if (!homeLoaded) scheduleHomeRetry()
                    } else {
                        Toast.makeText(this@HomeActivity1, response.message(), Toast.LENGTH_LONG).show()
                        if (!homeLoaded) scheduleHomeRetry()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                homeLoadInFlight = false
                if (isFinishing || isDestroyed) return
                // QA-07: a TV that starts before Wi-Fi is up used to stay blank forever. Show
                // why on screen (banner) and keep retrying until the first load succeeds.
                if (!homeLoaded) {
                    scheduleHomeRetry()
                } else {
                    Toast.makeText(this@HomeActivity1, getString(R.string.response_failed), Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    // ---------------------------------------------------------------------
    // QA-07: offline / stale-data state and recovery.
    //  - The first load (videos, ticker, doctors/counters) is retried with backoff until it works.
    //  - When the network comes back, Pusher reconnects and the screen reloads by itself.
    //  - If Pusher drops and gives up, it is reconnected with backoff.
    //  - A banner across the top says when the TV is offline, can't reach the server, or has
    //    no live connection, with the time of the last successful update.
    // ---------------------------------------------------------------------

    @Volatile private var homeLoaded = false
    @Volatile private var homeLoadInFlight = false
    private var homeRetryAttempt = 0
    private val homeRetryRunnable = Runnable {
        if (!homeLoaded && !homeLoadInFlight && isLoggedIn()) type?.let { getHomeDetails(it) }
    }

    @Volatile private var networkAvailable = true
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    @Volatile private var pusherConnected = false
    private var pusherDownSinceMs = 0L
    private var pusherReconnectAttempt = 0
    private val pusherReconnectRunnable = Runnable {
        if (isFinishing || isDestroyed || !isLoggedIn()) return@Runnable
        if (!::pusher.isInitialized || activePusher !== pusher) return@Runnable
        if (pusher.connection.state == ConnectionState.DISCONNECTED) {
            Log.d("Pusher", "Reconnecting (attempt ${pusherReconnectAttempt + 1})")
            try {
                pusher.connect()
            } catch (e: Exception) {
                Log.e("Pusher", "Reconnect failed", e)
            }
        }
    }

    private var lastSyncedAtMs = 0L
    private var statusBanner: TextView? = null
    private val bannerRefreshRunnable = Runnable { updateStatusBanner() }

    private fun backoffMs(attempt: Int): Long =
        (RETRY_BASE_MS shl attempt.coerceAtMost(4)).coerceAtMost(RETRY_MAX_MS)

    private fun scheduleHomeRetry() {
        val delay = backoffMs(homeRetryAttempt)
        homeRetryAttempt++
        Log.d("HomeLoad", "Home load failed, retrying in ${delay / 1000}s")
        mainHandler.removeCallbacks(homeRetryRunnable)
        mainHandler.postDelayed(homeRetryRunnable, delay)
        updateStatusBanner()
    }

    /** Called after any successful load from the server. */
    private fun markSynced() {
        lastSyncedAtMs = System.currentTimeMillis()
        updateStatusBanner()
    }

    private fun schedulePusherReconnect() {
        val delay = backoffMs(pusherReconnectAttempt)
        pusherReconnectAttempt++
        mainHandler.removeCallbacks(pusherReconnectRunnable)
        mainHandler.postDelayed(pusherReconnectRunnable, delay)
    }

    /** Pusher state changes arrive on Pusher's thread; this runs on the main thread. */
    private fun onPusherStateChanged(state: ConnectionState) {
        if (isFinishing || isDestroyed) return
        when (state) {
            ConnectionState.CONNECTED -> {
                pusherConnected = true
                pusherDownSinceMs = 0L
                pusherReconnectAttempt = 0
                mainHandler.removeCallbacks(pusherReconnectRunnable)
            }
            ConnectionState.DISCONNECTED -> {
                markPusherDown()
                // The client gives up after a few tries; keep trying while we're logged in
                // (only if this is still the TV's own connection, not one being shut down).
                if (activePusher === pusher && isLoggedIn()) schedulePusherReconnect()
            }
            else -> markPusherDown()
        }
        updateStatusBanner()
    }

    private fun markPusherDown() {
        pusherConnected = false
        if (pusherDownSinceMs == 0L) pusherDownSinceMs = System.currentTimeMillis()
        // Re-check after the grace period, so a short reconnect doesn't flash the banner.
        mainHandler.removeCallbacks(bannerRefreshRunnable)
        mainHandler.postDelayed(bannerRefreshRunnable, PUSHER_BANNER_GRACE_MS + 500)
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkAvailable = cm.activeNetwork != null
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                mainHandler.post {
                    val wasOffline = !networkAvailable
                    networkAvailable = true
                    updateStatusBanner()
                    if (wasOffline) onNetworkRestored()
                }
            }

            override fun onLost(network: android.net.Network) {
                mainHandler.post {
                    // Another network (e.g. Ethernet) may still be up.
                    networkAvailable = cm.activeNetwork != null
                    updateStatusBanner()
                }
            }
        }
        try {
            cm.registerDefaultNetworkCallback(callback)
            networkCallback = callback
        } catch (e: Exception) {
            Log.e("NETWORK", "registerDefaultNetworkCallback failed", e)
        }
    }

    private fun unregisterNetworkCallback() {
        val callback = networkCallback ?: return
        try {
            (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            Log.e("NETWORK", "unregisterNetworkCallback failed", e)
        }
        networkCallback = null
    }

    /** Network is back: reconnect live updates and reload whatever was missed. */
    private fun onNetworkRestored() {
        if (isFinishing || isDestroyed || !isLoggedIn()) return
        Log.d("NETWORK", "Network restored, resyncing")
        if (::pusher.isInitialized) {
            // Skip the backoff wait: the network is back now.
            mainHandler.removeCallbacks(pusherReconnectRunnable)
            pusherReconnectAttempt = 0
            try {
                pusher.connect()
            } catch (e: Exception) {
                Log.e("Pusher", "Reconnect failed", e)
            }
        }
        refreshBroadcastChannels()
        if (!homeLoaded) {
            homeRetryAttempt = 0
            if (!homeLoadInFlight) type?.let { getHomeDetails(it) }
        } else {
            refreshListsFromServer()
        }
    }

    private fun createStatusBanner() {
        val banner = TextView(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            val pad = (8 * resources.displayMetrics.density).toInt()
            setPadding(pad * 2, pad, pad * 2, pad)
            setBackgroundColor(Color.parseColor("#E6B71C1C"))
            visibility = View.GONE
            isFocusable = false
        }
        // Overlay across the top so the token / video / ticker layout doesn't move.
        addContentView(
            banner,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.TOP
            )
        )
        statusBanner = banner
    }

    private fun updateStatusBanner() {
        val banner = statusBanner ?: return
        val pusherDownLong = !pusherConnected && pusherDownSinceMs != 0L &&
                System.currentTimeMillis() - pusherDownSinceMs >= PUSHER_BANNER_GRACE_MS
        val message = when {
            !networkAvailable -> getString(R.string.status_offline)
            !homeLoaded && !homeLoadInFlight && homeRetryAttempt > 0 -> getString(R.string.status_server_unreachable)
            homeLoaded && pusherDownLong -> getString(R.string.status_live_reconnecting)
            else -> null
        }
        if (message == null) {
            banner.visibility = View.GONE
            return
        }
        val lastUpdated = if (lastSyncedAtMs > 0L) {
            " · " + getString(
                R.string.status_last_updated,
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastSyncedAtMs))
            )
        } else ""
        banner.text = message + lastUpdated
        banner.visibility = View.VISIBLE
        banner.bringToFront()
    }

    private fun updateDoctorListPage() {
        if (paginatedDoctors.isNotEmpty()) {
            currentPage = (currentPage + 1) % paginatedDoctors.size
            doctorAdapter.setDoctors(paginatedDoctors[currentPage])
            handler.postDelayed(doctorPageRunnable, 15_000)
        }
        pusher.connect()
    }

    private fun updateCountersListPage() {
        if (paginatedCounters.isNotEmpty()) {
            counterCurrentPage = (counterCurrentPage + 1) % paginatedCounters.size
            counterAdapter.setCounters(paginatedCounters[counterCurrentPage])
            handler.postDelayed(counterPageRunnable, 15_000)
        }
        pusher.connect()
    }

    private fun openSettingsScreen() {
        if (type == "3" || type == "4") {
            openCounterSetupScreen()
        } else {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun openCounterSetupScreen() {
        counterSetupLauncher.launch(Intent(this, SettingsActivity::class.java))
    }

    private fun requiresCounterSetup(data: org.json.JSONObject): Boolean {
        val user = data.optJSONObject("user") ?: return false
        return hasEntityId(user, "pharmacy_id") || hasEntityId(user, "lab_id")
    }

    private fun hasEntityId(jsonObject: org.json.JSONObject, key: String): Boolean {
        if (!jsonObject.has(key) || jsonObject.isNull(key)) return false
        val value = jsonObject.opt(key).toString().trim()
        return value.isNotEmpty() &&
            !value.equals("null", ignoreCase = true) &&
            value != "-1" &&
            value != "0"
    }

    private fun isValidCounterId(counterId: String): Boolean {
        return counterId.isNotBlank() &&
            counterId != "-1" &&
            counterId != "0" &&
            !counterId.equals("null", ignoreCase = true)
    }

    private fun isCountersSet(data: org.json.JSONObject): Boolean {
        if (!data.has("is_counters_set") || data.isNull("is_counters_set")) return true
        val value = data.opt("is_counters_set")
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            else -> {
                val text = value.toString().trim()
                text.equals("true", ignoreCase = true) || text == "1"
            }
        }
    }

    private fun hasCounterDisplayData(counterObject: org.json.JSONObject): Boolean {
        val counterName = jsonFieldAsString(counterObject, "counter_name")
        return counterName.isNotEmpty() && !counterName.equals("null", ignoreCase = true)
    }

    private fun jsonFieldAsString(jsonObject: org.json.JSONObject, key: String): String {
        if (!jsonObject.has(key) || jsonObject.isNull(key)) return ""
        return jsonObject.opt(key).toString().trim()
    }

    private fun parseCounterTokens(jsonArray: org.json.JSONArray): List<CounterListingModel> {
        val countersById = LinkedHashMap<String, CounterListingModel>()
        for (index in 0 until jsonArray.length()) {
            val counterObject = jsonArray.getJSONObject(index)
            val counterId = jsonFieldAsString(counterObject, "counter_id")
            if (!isValidCounterId(counterId) || !hasCounterDisplayData(counterObject)) continue

            val scrollUp = jsonFieldAsString(counterObject, "scroll_up").ifEmpty { "1" }
            countersById[counterId] = CounterListingModel(
                counterId,
                jsonFieldAsString(counterObject, "token"),
                jsonFieldAsString(counterObject, "call_status"),
                jsonFieldAsString(counterObject, "counter_name"),
                jsonFieldAsString(counterObject, "booking_id"),
                scrollUp
            )
        }
        return countersById.values.toList()
    }

    private fun bindCountersToUi(type: String, counters: List<CounterListingModel>) {
        countersList.clear()
        countersList.addAll(counters)

        if (countersList.isEmpty()) {
            recyclerViewCounter?.isVisible = false
            if (type == "3" || type == "4") {
                btn_settings?.isVisible = true
            }
            return
        }

        recyclerViewCounter?.isVisible = true
        btn_settings?.isVisible = false
        setUpCounterListOnce()
        counterAdapter = recyclerViewCounter?.adapter as CounterAdapter

        val newPages = countersList.chunked(pagecounterSize)
        // Stay on the page being shown; only fall back to page 1 if it no longer exists.
        val page = if (counterCurrentPage in newPages.indices) counterCurrentPage else 0
        val samePages = newPages.size == paginatedCounters.size
        paginatedCounters = newPages
        counterCurrentPage = page

        if (samePages && counterAdapter.hasSameRows(newPages[page])) {
            // Same counters as on screen: update only the numbers that changed. No re-layout,
            // no page reset, page timer left running -> the screen doesn't jump.
            counterAdapter.updateTokensInPlace(newPages[page])
            return
        }

        // Counters added/removed/renamed (or first load): full redraw of the current page.
        counterAdapter.setCounters(newPages[page])
        handler.removeCallbacks(counterPageRunnable)
        if (paginatedCounters.size > 1) {
            handler.postDelayed(counterPageRunnable, 15_000)
        }

        // After first layout, force a second bind so names are visible on slow TVs.
        recyclerViewCounter?.post {
            if (!::counterAdapter.isInitialized) return@post
            if (paginatedCounters.isEmpty()) return@post
            val page = counterCurrentPage.coerceIn(0, paginatedCounters.lastIndex)
            counterAdapter.setCounters(paginatedCounters[page])
        }
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

    /**
     * Reloads the doctor list from tvHome_new and updates it in place (no screen jump).
     * [silent] is for background refreshes (events, reconnect, polling): no toasts, so an
     * offline TV isn't covered in "Poor network connection" messages every minute.
     */
    private fun getDoctorDetails(type: String, silent: Boolean = false) {
        currentIndex = 0
        doctorFetchInFlight = true
        val mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.getHome(type).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                doctorFetchInFlight = false
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful) {
                    // A background refresh must never crash the TV on an odd response.
                    try {
                        val responseFromServer = response.body()!!.string()
                        val jsonObject = JSONObject(responseFromServer)
                        val jObjData = jsonObject.getJSONObject("data")
                        val jsonArrayDoctors = jObjData.getJSONArray("doctors")

                        if (jsonArrayDoctors.length() > 0) {
                            doctorsList.clear()
                            for (k in 0 until jsonArrayDoctors.length()) {
                                val jObjDoctor = jsonArrayDoctors.getJSONObject(k)
                                val doctor_id = jObjDoctor.getString("doctor_id")
                                val name = jObjDoctor.getString("name")
                                val token_call_status = jObjDoctor.optString("call_status", "")
                                val room = jObjDoctor.optString("room", "")
                                val tkObject = jObjDoctor.optJSONObject("tk")
                                var token = tkObject?.optString("token", "") ?: ""
                                doctorsList.add(DoctorListingModel(doctor_id, name, room, token, "", token_call_status))
                            }

                            if (doctorsList.isNotEmpty()) {
                                bindDoctorsToUi(resetPage = false)
                            } else {
                                recyclerViewDoctor?.isVisible = false
                                btn_settings?.isVisible = true
                            }
                        }
                        markSynced()
                    } catch (e: Exception) {
                        Log.e("DoctorRefresh", "Could not read doctor list", e)
                    }
                } else if (!silent) {
                    when (response.code()) {
                        401 -> Toast.makeText(this@HomeActivity1, getString(R.string.unauthenticated), Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                doctorFetchInFlight = false
                if (silent || isFinishing || isDestroyed) return
                Toast.makeText(this@HomeActivity1, getString(R.string.response_failed), Toast.LENGTH_LONG).show()
            }
        })
    }

    /** Same as [getDoctorDetails], for the lab / pharmacy counters. */
    private fun getCountersDetails(type: String, silent: Boolean = false) {
        counterFetchInFlight = true
        val mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.getHome(type).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                counterFetchInFlight = false
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful) {
                    try {
                        val responseFromServer = response.body()!!.string()
                        val jsonObject = JSONObject(responseFromServer)
                        val jObjData = jsonObject.getJSONObject("data")
                        val jsonArray_counter_tokens = jObjData.getJSONArray("counter_tokens")
                        bindCountersToUi(type, parseCounterTokens(jsonArray_counter_tokens))
                        markSynced()
                    } catch (e: Exception) {
                        Log.e("CounterRefresh", "Could not read counter list", e)
                    }
                } else if (!silent) {
                    when (response.code()) {
                        401 -> Toast.makeText(this@HomeActivity1, getString(R.string.unauthenticated), Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                counterFetchInFlight = false
                if (silent || isFinishing || isDestroyed) return
                Toast.makeText(this@HomeActivity1, getString(R.string.response_failed), Toast.LENGTH_LONG).show()
            }
        })
    }

    // ---------------------------------------------------------------------
    // Keeping the lists up to date without a manual refresh.
    //  1. "doctor_list_refresh" events (doctor added / new booking) reload the doctor list.
    //  2. When Pusher reconnects, the lists are reloaded (events sent while offline are lost).
    //  3. A quiet poll every 60 s catches anything else that was missed.
    // All three use the in-place update, so the screen doesn't jump.
    // ---------------------------------------------------------------------

    @Volatile private var pusherConnectedBefore = false
    @Volatile private var doctorFetchInFlight = false
    @Volatile private var counterFetchInFlight = false
    private val doctorListRefreshRunnable = Runnable { type?.let { getDoctorDetails(it, silent = true) } }
    private val listPollRunnable = object : Runnable {
        override fun run() {
            refreshListsFromServer()
            mainHandler.postDelayed(this, LIST_POLL_MS)
        }
    }

    /**
     * Reload the doctor list shortly after a "doctor_list_refresh" event. Several events close
     * together (a burst of bookings) cost one API call.
     */
    private fun scheduleDoctorListRefresh() {
        if (type != "1" && type != "2") return
        mainHandler.removeCallbacks(doctorListRefreshRunnable)
        mainHandler.postDelayed(doctorListRefreshRunnable, DOCTOR_REFRESH_DEBOUNCE_MS)
    }

    private var pendingCounterRefreshType: String? = null
    private val counterListRefreshRunnable = Runnable {
        pendingCounterRefreshType?.let { getCountersDetails(it, silent = true) }
        pendingCounterRefreshType = null
    }

    /**
     * Reload the counters shortly after a "counter_refresh" event (or a call with no token).
     * Several events close together cost one API call; no announcement is played.
     * [refreshType] is "3" (lab) or "4" (pharmacy).
     */
    private fun scheduleCounterListRefresh(refreshType: String) {
        pendingCounterRefreshType = refreshType
        mainHandler.removeCallbacks(counterListRefreshRunnable)
        mainHandler.postDelayed(counterListRefreshRunnable, COUNTER_REFRESH_DEBOUNCE_MS)
    }

    /** Quietly reloads this TV's list (doctors or counters). Skipped if a reload is already running. */
    private fun refreshListsFromServer() {
        if (isFinishing || isDestroyed || !isLoggedIn()) return
        when (val currentType = type) {
            "1", "2" -> if (!doctorFetchInFlight) getDoctorDetails(currentType, silent = true)
            "3", "4" -> if (!counterFetchInFlight) getCountersDetails(currentType, silent = true)
        }
    }

    private fun startListPolling() {
        mainHandler.removeCallbacks(listPollRunnable)
        mainHandler.postDelayed(listPollRunnable, LIST_POLL_MS)
    }

    private fun stopListPolling() {
        mainHandler.removeCallbacks(listPollRunnable)
    }

    /** Pusher came back after a drop: rejoin channels and reload anything missed meanwhile. */
    private fun onPusherReconnected() {
        if (isFinishing || isDestroyed || !isLoggedIn()) return
        Log.d("Pusher", "Reconnected, reloading lists")
        refreshBroadcastChannels()
        refreshListsFromServer()
    }

    /**
     * After a counter token Pusher event:
     * 1) Optimistically update token on UI
     * 2) Play announcement unless skipped / outsource handled separately
     */
    private fun handleCounterTokenCalled(
        counterId: String,
        tokenNumber: String,
        bookingId: String?,
        announceOutsource: Boolean,
        outsourceCallId: String = "",
        skipAnnouncement: Boolean = false,
        refreshType: String? = null
    ) {
        val counterKnown = countersList.any { it.counter_id == counterId }
        updateCounterTokenOnUi(counterId, tokenNumber, bookingId)

        // The event already carries the new number, which is now on screen. Reloading the whole
        // list after every call redrew it (screen jump) and a slow response could roll the number
        // back. Reload only when this counter isn't on the TV yet (e.g. newly enabled).
        if (!counterKnown && !refreshType.isNullOrBlank()) {
            getCountersDetails(refreshType, silent = true)
        }

        if (skipAnnouncement) return

        ensureAnnouncementQueueReady()
        if (announceOutsource) {
            convertOutsourceTokenTextToSpeech(outsourceCallId, counterId, tokenNumber)
        } else {
            convertTokenTextToSpeech(tokenNumber, counterId, bookingId ?: "0")
        }
    }

    private fun ensureAnnouncementQueueReady() {
        if (!::announcementQueue.isInitialized) {
            announcementQueue = TokenAnnouncementQueue(
                context = this,
                apiService = RetrofitClient.ApiUtils.apiService1,
                onAnnouncementStart = { pauseVideoForAnnouncement() },
                onAllAnnouncementsFinished = { resumeVideoAfterAnnouncement() }
            )
        }
    }

    private fun updateDoctorTokenOnUi(doctorId: String, tokenNumber: String, roomNumber: String? = null) {
        if (doctorId.isEmpty() || doctorId == "-1" || doctorId == "null") return

        val listIndex = doctorsList.indexOfFirst { it.doctor_id == doctorId }
        if (listIndex >= 0) {
            val current = doctorsList[listIndex]
            doctorsList[listIndex] = current.copy(
                token = tokenNumber,
                room = roomNumber?.takeIf { it.isNotBlank() && it != "null" } ?: current.room
            )
            paginatedDoctors = doctorsList.chunked(pageSize)
        }

        if (::doctorAdapter.isInitialized && recyclerViewDoctor?.adapter != null) {
            doctorAdapter.updateTokenForDoctor(doctorId, tokenNumber, roomNumber)
            recyclerViewDoctor?.isVisible = true
            btn_settings?.isVisible = false
        }
    }

    private fun updateCounterTokenOnUi(counterId: String, tokenNumber: String, bookingId: String? = null) {
        if (counterId.isEmpty() || counterId == "-1") return

        val listIndex = countersList.indexOfFirst { it.counter_id == counterId }
        if (listIndex >= 0) {
            val current = countersList[listIndex]
            countersList[listIndex] = current.copy(
                token = tokenNumber,
                booking_id = bookingId ?: current.booking_id
            )
            paginatedCounters = countersList.chunked(pagecounterSize)
        }

        ensureCounterAdapterReady()
        if (::counterAdapter.isInitialized && recyclerViewCounter?.adapter != null) {
            // If counter not on current page, still try update; refresh from API will show it.
            counterAdapter.updateTokenForCounter(counterId, tokenNumber)
            recyclerViewCounter?.isVisible = true
            btn_settings?.isVisible = false
        }
    }

    /**
     * Sets up the counter list's layout manager and adapter the first time only. Replacing the
     * layout manager on every refresh threw away and redrew every row, which made the screen jump.
     */
    private fun setUpCounterListOnce() {
        val list = recyclerViewCounter ?: return
        if (list.layoutManager == null) {
            list.layoutManager = LinearLayoutManager(this@HomeActivity1)
            list.setHasFixedSize(true)
            list.isNestedScrollingEnabled = false
            list.setPadding(0, 0, 0, 0)
            list.clipToPadding = true
            list.clipChildren = true
            list.overScrollMode = View.OVER_SCROLL_NEVER
            // No fade when a number changes: it should just change.
            (list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
        list.setItemViewCacheSize(countersList.size.coerceAtLeast(4))
        if (list.adapter == null) {
            counterAdapter = CounterAdapter(listOf()) { openCounterSetupScreen() }
            list.adapter = counterAdapter
        }
    }

    /** Same as [setUpCounterListOnce], for the doctor list. */
    private fun setUpDoctorListOnce() {
        val list = recyclerViewDoctor ?: return
        if (list.layoutManager == null) {
            list.layoutManager = LinearLayoutManager(this@HomeActivity1)
            (list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
        if (list.adapter == null) {
            doctorAdapter = DoctorAdapter(listOf())
            list.adapter = doctorAdapter
        } else {
            doctorAdapter = list.adapter as DoctorAdapter
        }
    }

    /**
     * Shows [doctorsList]. When the doctors on the current page are unchanged, only the
     * token/room values that changed are redrawn, so a refresh doesn't make the screen jump.
     * [resetPage] is for the first load, where the list starts on page 1.
     */
    private fun bindDoctorsToUi(resetPage: Boolean) {
        recyclerViewDoctor?.isVisible = true
        btn_settings?.isVisible = false
        setUpDoctorListOnce()

        val newPages = doctorsList.chunked(pageSize)
        if (newPages.isEmpty()) return
        val page = if (!resetPage && currentPage in newPages.indices) currentPage else 0
        val samePages = newPages.size == paginatedDoctors.size
        paginatedDoctors = newPages
        currentPage = page

        if (!resetPage && samePages && doctorAdapter.hasSameRows(newPages[page])) {
            doctorAdapter.updateTokensInPlace(newPages[page])
            return
        }

        handler.removeCallbacks(doctorPageRunnable)
        doctorAdapter.setDoctors(newPages[page])
        handler.postDelayed(doctorPageRunnable, 15_000)
    }

    private fun ensureCounterAdapterReady() {
        if (recyclerViewCounter == null) return
        if (recyclerViewCounter?.adapter == null || !::counterAdapter.isInitialized) {
            setUpCounterListOnce()
            counterAdapter = recyclerViewCounter?.adapter as CounterAdapter
            if (paginatedCounters.isNotEmpty()) {
                val page = counterCurrentPage.coerceIn(0, paginatedCounters.lastIndex)
                counterAdapter.setCounters(paginatedCounters[page])
            } else if (countersList.isNotEmpty()) {
                paginatedCounters = countersList.chunked(pagecounterSize)
                counterCurrentPage = 0
                counterAdapter.setCounters(paginatedCounters[0])
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSessionFromPreferences()
        if (!isLoggedIn()) {
            disconnectOwnedPusher()
            return
        }
        type = baseClass.getSharedPreferance(this@HomeActivity1, "type", "")
        if (!::mPlayer.isInitialized) {
            mPlayer = SimpleExoPlayer.Builder(this@HomeActivity1).build()
            attachVideoPlayerListener()
        }
        playerView.player = mPlayer
        if (videoList.isNotEmpty()) {
            if (mPlayer.mediaItemCount == 0) {
                startVideoPlayback()
            } else if (!isVideoPausedForAnnouncement) {
                mPlayer.playWhenReady = true
            }
        }
        if (doctor_selected_or_unselected == true) {
            getDoctorDetails(type!!)
        }
        ensureAnnouncementQueueReady()
        // Keep Pusher alive after Settings / brief background.
        if (::pusher.isInitialized) {
            try {
                pusher.connect()
            } catch (e: Exception) {
                Log.e("Pusher", "Reconnect failed", e)
            }
            // Picks up channel changes (new doctor, re-login) and retries any channel whose
            // authorisation failed. Also runs right after onCreate, so it is the first fetch.
            refreshBroadcastChannels()
        }
        // Safety net for missed events: quietly reload the list every 60 s while on screen.
        startListPolling()
        // Re-bind visible counter names after returning to the screen
        if (::counterAdapter.isInitialized &&
            paginatedCounters.isNotEmpty() &&
            recyclerViewCounter?.isVisible == true
        ) {
            val page = counterCurrentPage.coerceIn(0, paginatedCounters.lastIndex)
            counterAdapter.setCounters(paginatedCounters[page])
        }
        // If lab/pharmacy and list empty, pull latest counters (covers counters enabled after boot).
        if ((type == "3" || type == "4") && countersList.isEmpty()) {
            type?.let { getCountersDetails(it) }
        }
    }

    private fun pauseVideoForAnnouncement() {
        if (!::mPlayer.isInitialized) return
        isVideoPausedForAnnouncement = true
        videoVolumeBeforeAnnouncement = mPlayer.volume
        mPlayer.playWhenReady = false
        mPlayer.pause()
        mPlayer.volume = 0f
    }

    private fun resumeVideoAfterAnnouncement() {
        if (!::mPlayer.isInitialized) return
        isVideoPausedForAnnouncement = false
        mPlayer.volume = videoVolumeBeforeAnnouncement
        mPlayer.playWhenReady = true
    }

    fun isPlayerPlayingWithSound(context: Context, player: SimpleExoPlayer): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isMuted = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
        return player.audioAttributes != null && !isMuted
    }

    override fun onDestroy() {
        releasePlayer()
        if (::announcementQueue.isInitialized) {
            announcementQueue.shutdown()
        }
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        mainHandler.removeCallbacks(channelRetryRunnable)
        mainHandler.removeCallbacks(doctorListRefreshRunnable)
        mainHandler.removeCallbacks(counterListRefreshRunnable)
        mainHandler.removeCallbacks(homeRetryRunnable)
        mainHandler.removeCallbacks(pusherReconnectRunnable)
        mainHandler.removeCallbacks(bannerRefreshRunnable)
        unregisterNetworkCallback()
        stopListPolling()
        disconnectOwnedPusher()
        if (activeSessionId == sessionId) {
            activeSessionId = 0L
        }
        super.onDestroy()
    }

    private fun refreshSessionFromPreferences() {
        token = baseClass.getSharedPreferance(this, "token", "")
        logged_clinic_id = baseClass.getSharedPreferance(this, "clinic_id", "")
        logged_doctor_id = baseClass.getSharedPreferance(this, "doctor_id", "")
        logged_lab_id = baseClass.getSharedPreferance(this, "lab_id", "")
        logged_pharmacy_id = baseClass.getSharedPreferance(this, "pharmacy_id", "")
        type = baseClass.getSharedPreferance(this, "type", "")
        if (!token.isNullOrBlank()) {
            RetrofitClient.bearer_token = token
        }
    }

    private fun isLoggedIn(): Boolean {
        val loginStatus = baseClass.getSharedPreferance(this, "login_status", "false")
        val currentToken = baseClass.getSharedPreferance(this, "token", "")
        return loginStatus == "true" && !currentToken.isNullOrBlank()
    }

    private fun shouldProcessPusherEvent(): Boolean {
        if (isFinishing || isDestroyed) return false
        if (activeSessionId != sessionId) return false
        refreshSessionFromPreferences()
        return isLoggedIn()
    }

    private fun disconnectOwnedPusher() {
        if (::pusher.isInitialized && activePusher === pusher) {
            disconnectGlobalPusher()
        }
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
        ensureAnnouncementQueueReady()
        announcementQueue.enqueueDoctorToken(token_number, room_number, doctor_id)
    }

    private fun convertTokenTextToSpeech(token_number: String, counter_id: String, booking_id: String) {
        ensureAnnouncementQueueReady()
        announcementQueue.enqueueCounterToken(token_number, counter_id, booking_id)
    }

    private fun convertOutsourceTokenTextToSpeech(outsource_call_id: String, counter_id: String, token_number: String) {
        Log.d("DEBUG_API", "Outsource ID: $outsource_call_id, Counter: $counter_id, Token: $token_number")
        ensureAnnouncementQueueReady()
        announcementQueue.enqueueOutsourceToken(outsource_call_id, counter_id, token_number)
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

    // ---------------------------------------------------------------------
    // Live token calls over private Pusher channels (bug report C2).
    //
    // The TV no longer listens on the public "medicino-tv-live" channel, which carried every
    // customer's calls. It asks the backend which private channels it may join
    // (GET tv/broadcastChannels, e.g. private-tv.clinic.2) and Pusher only lets it in after
    // POST tv/broadcasting/auth approves the TV's bearer token for that channel.
    // ---------------------------------------------------------------------

    /** Private channels this TV has joined (or is joining). Main thread only. */
    private val subscribedPrivateChannels = mutableSetOf<String>()
    private var channelFetchInFlight = false
    private val channelRetryRunnable = Runnable { refreshBroadcastChannels() }

    private fun setUpPusher() {
        val authorizer = HttpChannelAuthorizer(RetrofitClient.ApiUtils.BASE_URL + "tv/broadcasting/auth")
        val options = PusherOptions()
        options.setCluster(BuildConfig.PUSHER_CLUSTER)
        // Read the token on every authorisation so a re-login never sends a stale bearer.
        options.setChannelAuthorizer { channelName, socketId ->
            authorizer.setHeaders(
                hashMapOf(
                    "Authorization" to "Bearer ${baseClass.getSharedPreferance(this@HomeActivity1, "token", "")}",
                    "Accept" to "application/json"
                )
            )
            authorizer.authorize(channelName, socketId)
        }

        // Key comes from the build flavor (staging / production) in app/build.gradle.
        pusher = Pusher(BuildConfig.PUSHER_KEY, options)
        activePusher = pusher

        pusher.connect(object : ConnectionEventListener {
            override fun onConnectionStateChange(change: ConnectionStateChange) {
                Log.d("Pusher", "State changed from ${change.previousState} to ${change.currentState}")
                mainHandler.post { onPusherStateChanged(change.currentState) }
                if (change.currentState == ConnectionState.CONNECTED) {
                    // The first connect is the normal start-up (lists load anyway). Any later
                    // connect is a reconnect after a drop: reload what may have been missed.
                    if (pusherConnectedBefore) {
                        mainHandler.post { onPusherReconnected() }
                    }
                    pusherConnectedBefore = true
                }
            }

            override fun onError(message: String?, code: String?, e: Exception?) {
                Log.e("Pusher", "Error: $message, Code: $code, Exception: $e")
            }
        }, ConnectionState.ALL)
        // Channels are joined in refreshBroadcastChannels(), which onResume runs.
    }

    /** Asks the backend which private channels this TV belongs to, then joins/leaves to match. */
    private fun refreshBroadcastChannels() {
        if (!::pusher.isInitialized || channelFetchInFlight || !isLoggedIn()) return
        mainHandler.removeCallbacks(channelRetryRunnable)
        channelFetchInFlight = true
        RetrofitClient.ApiUtils.apiService1.getBroadcastChannels().enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                channelFetchInFlight = false
                if (isFinishing || isDestroyed) return
                val channels = if (response.isSuccessful) {
                    try {
                        parsePrivateChannels(response.body()?.string().orEmpty())
                    } catch (e: Exception) {
                        Log.e("Pusher", "Could not read broadcastChannels response", e)
                        emptyList()
                    }
                } else {
                    Log.e("Pusher", "broadcastChannels failed: HTTP ${response.code()}")
                    emptyList()
                }
                applyChannelSubscriptions(channels)
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                channelFetchInFlight = false
                if (isFinishing || isDestroyed) return
                Log.e("Pusher", "broadcastChannels request failed", t)
                applyChannelSubscriptions(emptyList())
            }
        })
    }

    /**
     * Expected: {"data": {"private_channels": ["private-tv.clinic.2", ...]}}. Also accepts
     * data:[...], data:{channels:[...]} or a top-level list; entries may be strings or
     * objects with "name"/"channel". Anything not starting with "private-" is dropped, so
     * the TV can never be pointed back at a public channel.
     */
    private fun parsePrivateChannels(body: String): List<String> {
        if (body.isBlank()) return emptyList()
        val root = JSONObject(body)
        val data = root.opt("data")
        val array = when {
            data is JSONArray -> data
            data is JSONObject && data.optJSONArray("private_channels") != null -> data.optJSONArray("private_channels")
            data is JSONObject && data.optJSONArray("channels") != null -> data.optJSONArray("channels")
            else -> root.optJSONArray("private_channels") ?: root.optJSONArray("channels")
        } ?: return emptyList()

        val names = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val name = when (val entry = array.opt(i)) {
                is String -> entry
                is JSONObject -> entry.optString("name").ifEmpty { entry.optString("channel") }
                else -> ""
            }.trim()
            if (name.startsWith("private-")) names.add(name)
        }
        return names.distinct()
    }

    private fun applyChannelSubscriptions(privateChannels: List<String>) {
        if (privateChannels.isEmpty()) {
            // Keep channels already joined (a failed refresh shouldn't drop working ones) and
            // ask again shortly. There is deliberately no fallback to the public channel.
            scheduleChannelRetry()
            return
        }

        // Leave channels the TV no longer belongs to (e.g. a doctor removed from the clinic).
        for (name in subscribedPrivateChannels - privateChannels.toSet()) {
            unsubscribeSafely(name)
            subscribedPrivateChannels.remove(name)
        }

        for (name in privateChannels) {
            if (name in subscribedPrivateChannels) continue
            try {
                pusher.subscribePrivate(name, object : PrivateChannelEventListener {
                    override fun onEvent(event: PusherEvent) = dispatchPusherEvent(event)

                    override fun onSubscriptionSucceeded(channelName: String) {
                        Log.d("Pusher", "Subscribed to $channelName")
                    }

                    override fun onAuthenticationFailure(message: String?, e: Exception?) {
                        // tv/broadcasting/auth refused or unreachable (expired token, blocked TV,
                        // network). Drop the channel and try again on the next refresh.
                        Log.e("Pusher", "Auth failed for $name: $message", e)
                        runOnUiThread {
                            if (isFinishing || isDestroyed) return@runOnUiThread
                            if (subscribedPrivateChannels.remove(name)) unsubscribeSafely(name)
                            scheduleChannelRetry()
                        }
                    }
                }, *PUSHER_EVENTS)
                subscribedPrivateChannels.add(name)
                Log.d("Pusher", "Subscribing to $name")
            } catch (e: Exception) {
                Log.e("Pusher", "subscribePrivate($name) failed", e)
            }
        }
    }

    private fun scheduleChannelRetry() {
        mainHandler.removeCallbacks(channelRetryRunnable)
        mainHandler.postDelayed(channelRetryRunnable, CHANNEL_RETRY_MS)
    }

    private fun unsubscribeSafely(channelName: String) {
        try {
            pusher.unsubscribe(channelName)
        } catch (e: Exception) {
            Log.e("Pusher", "unsubscribe($channelName) failed", e)
        }
    }

    private fun dispatchPusherEvent(event: PusherEvent) {
        Log.d("PUSHER_DEBUG", "${event.eventName} on ${event.channelName}: ${event.data}")
        runOnUiThread {
            if (!shouldProcessPusherEvent()) return@runOnUiThread
            // One malformed message must not crash every TV (bug report M6).
            val jsonObject = try {
                JSONObject(event.data)
            } catch (e: Exception) {
                Log.e("Pusher", "Bad ${event.eventName} payload: ${event.data}", e)
                return@runOnUiThread
            }
            when (event.eventName) {
                EVENT_TOKEN_CALLED -> handleTokenCalledEvent(jsonObject)
                EVENT_OUTSOURCE_TOKEN_CALLED -> handleOutsourceTokenCalledEvent(jsonObject)
            }
        }
    }

    private fun isIdMatch(eventId: String, loggedId: String?): Boolean =
        eventId.isNotEmpty() && eventId != "-1" && eventId != "null" &&
                !loggedId.isNullOrBlank() && loggedId == eventId

    private fun handleTokenCalledEvent(jsonObject: JSONObject) {
        val tokenNumber = jsonObject.optString("token_number", "")
        // Backend tags each event: "token_called" (a real call), "counter_refresh" or
        // "doctor_list_refresh" (no token number). Only real calls may be announced. Events
        // without a reason come from an older backend and are treated as before.
        val reason = jsonObject.optString("reason", "")
        val isRealCall = reason.isEmpty() || reason == REASON_TOKEN_CALLED
        val hasToken = tokenNumber.isNotEmpty() && tokenNumber != "null"

        // Clinic/Doctor: ID match → convertTextToSpeech
        val doctorId = jsonObject.opt("doctor_id")?.toString()?.trim().orEmpty()
        val clinicId = jsonObject.opt("clinic_id")?.toString()?.trim().orEmpty()
        val roomNumber = jsonObject.optString("room_number", "")
        val isClinicMatch = isIdMatch(clinicId, logged_clinic_id)
        val isDoctorMatch = isIdMatch(doctorId, logged_doctor_id)

        if (isClinicMatch || isDoctorMatch) {
            if (reason == REASON_DOCTOR_LIST_REFRESH || !hasToken) {
                // Doctor added / new booking: reload the doctor list. Nothing to announce.
                scheduleDoctorListRefresh()
                return
            }
            if (isRealCall) {
                updateDoctorTokenOnUi(doctorId, tokenNumber, roomNumber)
                convertTextToSpeech(tokenNumber, roomNumber, doctorId)
                return
            }
        }

        // Lab/Pharmacy: ID match → convertTokenTextToSpeech
        val labId = jsonObject.opt("lab_id")?.toString()?.trim().orEmpty()
        val pharmacyId = jsonObject.opt("pharmacy_id")?.toString()?.trim().orEmpty()
        val counterId = jsonObject.opt("counter_id")?.toString()?.trim().orEmpty()
        val bookingId = jsonObject.opt("counter_booking_id")?.toString()?.trim().orEmpty().ifEmpty { "0" }
        val isLabMatch = isIdMatch(labId, logged_lab_id)
        val isPharmacyMatch = isIdMatch(pharmacyId, logged_pharmacy_id)

        val hasInvalidCounterFields = !hasToken || counterId.isEmpty() || counterId == "-1" || counterId == "null"
        if (!isRealCall || hasInvalidCounterFields) {
            // A counter refresh (or a call with no token): reload the counters, announce nothing.
            if (isLabMatch) {
                scheduleCounterListRefresh("3")
            } else if (isPharmacyMatch) {
                scheduleCounterListRefresh("4")
            }
            return
        }

        if (!isLabMatch && !isPharmacyMatch) return

        handleCounterTokenCalled(
            counterId = counterId,
            tokenNumber = tokenNumber,
            bookingId = bookingId,
            announceOutsource = false,
            refreshType = if (isLabMatch) "3" else "4"
        )
    }

    private fun handleOutsourceTokenCalledEvent(jsonObject: JSONObject) {
        val outsourceCallId = jsonObject.optString("outsource_call_id", "")
        val counterId = jsonObject.opt("counter_id")?.toString()?.trim().orEmpty()
        val tokenNumber = jsonObject.optString("token_number", "")
        // Every outsource call is a real call (whatever its "reason"): announce it. Only an
        // event missing the token or counter falls back to a quiet counter reload.

        val labId = jsonObject.opt("lab_id")?.toString()?.trim().orEmpty()
        val pharmacyId = jsonObject.opt("pharmacy_id")?.toString()?.trim().orEmpty()
        val isLabMatch = isIdMatch(labId, logged_lab_id)
        val isPharmacyMatch = isIdMatch(pharmacyId, logged_pharmacy_id)

        val hasInvalidCounterFields = tokenNumber.isEmpty() || tokenNumber == "null" ||
                counterId.isEmpty() || counterId == "-1" || counterId == "null"
        if (hasInvalidCounterFields) {
            if (isLabMatch) {
                scheduleCounterListRefresh("3")
            } else if (isPharmacyMatch) {
                scheduleCounterListRefresh("4")
            }
            return
        }

        if (!isLabMatch && !isPharmacyMatch) {
            Log.d("DEBUG_OUTSOURCE", "ID mismatch. Skipping.")
            return
        }

        handleCounterTokenCalled(
            counterId = counterId,
            tokenNumber = tokenNumber,
            bookingId = null,
            announceOutsource = true,
            outsourceCallId = outsourceCallId,
            refreshType = if (isLabMatch) "3" else "4"
        )
    }

    companion object {
        private const val EVENT_TOKEN_CALLED = "token-called"
        private const val EVENT_OUTSOURCE_TOKEN_CALLED = "outsource-token-called"
        private val PUSHER_EVENTS = arrayOf(EVENT_TOKEN_CALLED, EVENT_OUTSOURCE_TOKEN_CALLED)
        private const val REASON_TOKEN_CALLED = "token_called"
        private const val REASON_DOCTOR_LIST_REFRESH = "doctor_list_refresh"
        private const val DOCTOR_REFRESH_DEBOUNCE_MS = 1_500L
        private const val COUNTER_REFRESH_DEBOUNCE_MS = 1_500L
        private const val LIST_POLL_MS = 60_000L
        private const val RETRY_BASE_MS = 5_000L
        private const val RETRY_MAX_MS = 60_000L
        private const val PUSHER_BANNER_GRACE_MS = 20_000L
        private const val CHANNEL_RETRY_MS = 30_000L

        @Volatile
        private var activeSessionId: Long = 0L

        @Volatile
        private var activePusher: Pusher? = null

        private fun disconnectGlobalPusher() {
            try {
                // Disconnecting drops every channel this Pusher instance had joined.
                activePusher?.disconnect()
            } catch (e: Exception) {
                Log.e("Pusher", "Global disconnect failed", e)
            }
            activePusher = null
        }
    }
}