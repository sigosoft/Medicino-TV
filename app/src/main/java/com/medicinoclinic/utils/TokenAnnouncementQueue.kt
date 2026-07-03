package com.medicinoclinic.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.medicinoclinic.retrofit.APIService
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayDeque

class TokenAnnouncementQueue(
    private val context: Context,
    private val apiService: APIService
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

    private val queue = ArrayDeque<Request>()
    private var mediaPlayer: MediaPlayer? = null
    private var isProcessing = false

    fun enqueueDoctorToken(tokenNumber: String, roomNumber: String, doctorId: String) {
        queue.addLast(Request.Doctor(tokenNumber, roomNumber, doctorId))
        processNext()
    }

    fun enqueueCounterToken(tokenNumber: String, counterId: String, bookingId: String) {
        queue.addLast(Request.Counter(tokenNumber, counterId, bookingId))
        processNext()
    }

    fun enqueueOutsourceToken(outsourceCallId: String, counterId: String, tokenNumber: String) {
        queue.addLast(Request.Outsource(outsourceCallId, counterId, tokenNumber))
        processNext()
    }

    private fun processNext() {
        if (isProcessing || queue.isEmpty()) return

        isProcessing = true
        when (val request = queue.removeFirst()) {
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
                        playAudio(audioBytes, filePrefix)
                        return
                    }
                }
                onAnnouncementFinished()
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Announcement API failed: ${t.message}")
                onAnnouncementFinished()
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
                    release()
                    mediaPlayer = null
                    onAnnouncementFinished()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play announcement audio", e)
            onAnnouncementFinished()
        }
    }

    private fun onAnnouncementFinished() {
        isProcessing = false
        processNext()
    }

    private fun releasePlayer() {
        mediaPlayer?.setOnCompletionListener(null)
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun shutdown() {
        queue.clear()
        releasePlayer()
        isProcessing = false
    }

    companion object {
        private const val TAG = "TokenAnnouncementQueue"
    }
}
