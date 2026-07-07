package com.medicinoclinic.utils

import android.content.Context
import android.media.AudioAttributes
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
    private var sessionGeneration = 0

    fun enqueueDoctorToken(tokenNumber: String, roomNumber: String, doctorId: String) {
        enqueue(Request.Doctor(tokenNumber, roomNumber, doctorId))
    }

    fun enqueueCounterToken(tokenNumber: String, counterId: String, bookingId: String) {
        enqueue(Request.Counter(tokenNumber, counterId, bookingId))
    }

    fun enqueueOutsourceToken(outsourceCallId: String, counterId: String, tokenNumber: String) {
        enqueue(Request.Outsource(outsourceCallId, counterId, tokenNumber))
    }

    private fun enqueue(request: Request) {
        synchronized(lock) {
            queue.add(request)
        }
        mainHandler.post {
            pauseVideoIfNeeded()
            processNext()
        }
    }

    private fun pauseVideoIfNeeded() {
        if (!videoPaused) {
            videoPaused = true
            onAnnouncementStart?.invoke()
        }
    }

    private fun processNext() {
        val request: Request
        synchronized(lock) {
            if (isPlaying || queue.isEmpty()) return
            isPlaying = true
            request = queue.removeFirst()
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
        val generation = sessionGeneration
        return object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (generation != sessionGeneration) return
                if (response.isSuccessful) {
                    val audioBytes = response.body()?.bytes()
                    if (audioBytes != null) {
                        mainHandler.post {
                            if (generation == sessionGeneration) {
                                playAudio(audioBytes, filePrefix, generation)
                            } else {
                                onAnnouncementFinished()
                            }
                        }
                        return
                    }
                }
                mainHandler.post { onAnnouncementFinished() }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                if (generation != sessionGeneration) return
                Log.e(TAG, "Announcement API failed: ${t.message}")
                mainHandler.post { onAnnouncementFinished() }
            }
        }
    }

    private fun playAudio(audioBytes: ByteArray, filePrefix: String, generation: Int) {
        if (generation != sessionGeneration) {
            onAnnouncementFinished()
            return
        }
        try {
            val tempMp3 = File.createTempFile(filePrefix, ".mp3", context.cacheDir)
            FileOutputStream(tempMp3).use { it.write(audioBytes) }

            releasePlayer()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(tempMp3.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    tempMp3.delete()
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
        val shouldResumeVideo = synchronized(lock) { queue.isEmpty() } && videoPaused
        if (shouldResumeVideo) {
            videoPaused = false
            onAllAnnouncementsFinished?.invoke()
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
            sessionGeneration++
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
