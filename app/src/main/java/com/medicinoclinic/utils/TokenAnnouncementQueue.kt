package com.medicinoclinic.utils

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.medicinoclinic.retrofit.APIService
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.LinkedList

class TokenAnnouncementQueue(
    private val context: Context,
    private val apiService: APIService,
    private val onAnnouncementStart: (() -> Unit)? = null,
    private val onAllAnnouncementsFinished: (() -> Unit)? = null
) {

    private sealed class Request {
        data class Doctor(
            val tokenNumber: String,
            val roomNumber: String,
            val doctorId: String
        ) : Request()

        data class Counter(
            val tokenNumber: String,
            val counterId: String,
            val bookingId: String
        ) : Request()

        data class Outsource(
            val outsourceCallId: String,
            val counterId: String,
            val tokenNumber: String
        ) : Request()
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val lock = Any()
    private val queue = LinkedList<Request>()
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var videoPaused = false

    fun enqueueDoctorToken(tokenNumber: String, roomNumber: String, doctorId: String) {
        synchronized(lock) {
            queue.add(Request.Doctor(tokenNumber, roomNumber, doctorId))
        }
        mainHandler.post { processNext() }
    }

    fun enqueueCounterToken(tokenNumber: String, counterId: String, bookingId: String) {
        synchronized(lock) {
            queue.add(Request.Counter(tokenNumber, counterId, bookingId))
        }
        mainHandler.post { processNext() }
    }

    fun enqueueOutsourceToken(outsourceCallId: String, counterId: String, tokenNumber: String) {
        synchronized(lock) {
            queue.add(Request.Outsource(outsourceCallId, counterId, tokenNumber))
        }
        mainHandler.post { processNext() }
    }

    private fun processNext() {
        val request: Request
        synchronized(lock) {
            if (isPlaying || queue.isEmpty()) return
            isPlaying = true
            request = queue.removeFirst()
        }

        if (!videoPaused) {
            videoPaused = true
            onAnnouncementStart?.invoke()
        }

        when (request) {
            is Request.Doctor -> fetchDoctorAudio(request)
            is Request.Counter -> fetchCounterAudio(request)
            is Request.Outsource -> fetchOutsourceAudio(request)
        }
    }

    private fun fetchDoctorAudio(request: Request.Doctor) {
        apiService.convertTextToSpeech(request.tokenNumber, request.roomNumber, request.doctorId)
            .enqueue(audioCallback("audio"))
    }

    private fun fetchCounterAudio(request: Request.Counter) {
        apiService.convertTokenTextToSpeech(request.tokenNumber, request.counterId, request.bookingId)
            .enqueue(audioCallback("audio"))
    }

    private fun fetchOutsourceAudio(request: Request.Outsource) {
        apiService.convertOutsourceTokenTextToSpeech(
            request.outsourceCallId,
            request.counterId,
            request.tokenNumber
        ).enqueue(audioCallback("audio_outsource"))
    }

    private fun audioCallback(filePrefix: String): Callback<ResponseBody> {
        return object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val audioBytes = response.body()?.bytes()
                    if (audioBytes != null) {
                        mainHandler.post { playAudio(audioBytes, filePrefix) }
                        return
                    }
                }
                mainHandler.post { onAnnouncementFinished() }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Announcement API failed: ${t.message}")
                mainHandler.post { onAnnouncementFinished() }
            }
        }
    }

    private fun playAudio(audioBytes: ByteArray, filePrefix: String) {
        try {
            val tempMp3 = File.createTempFile(filePrefix, ".mp3", context.cacheDir)
            FileOutputStream(tempMp3).use { it.write(audioBytes) }

            releasePlayer()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempMp3.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    mainHandler.post { onAnnouncementFinished() }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play announcement audio", e)
            onAnnouncementFinished()
        }
    }

    private fun onAnnouncementFinished() {
        releasePlayer()
        synchronized(lock) {
            isPlaying = false
        }
        if (queue.isEmpty() && videoPaused) {
            videoPaused = false
            onAnnouncementStart?.let { onAllAnnouncementsFinished?.invoke() }
        }
        processNext()
    }

    private fun releasePlayer() {
        mediaPlayer?.setOnCompletionListener(null)
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {}
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun shutdown() {
        synchronized(lock) {
            queue.clear()
            isPlaying = false
        }
        releasePlayer()
        if (videoPaused) {
            videoPaused = false
            onAllAnnouncementsFinished?.invoke()
        }
    }

    companion object {
        private const val TAG = "TokenAnnouncementQueue"
    }
}
