package com.medicinoclinic.retrofit

import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface APIService {
    @POST("tv/login/clinic")
    @FormUrlEncoded
    fun loginPost(
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<ResponseBody>

    @POST("tv/login/doctor")
    @FormUrlEncoded
    fun loginDrPost(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("fcm") fcm: String
    ):  Call<ResponseBody>

    @POST("tv/login")
    @FormUrlEncoded
    fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("type") type: String,
        @Field("fcm") fcm: String
    ): Call<ResponseBody>


//    @GET("tv/doctors")
//    fun getDoctors(
//        @Query("type")type : String,
//    ): Call<ResponseBody>

    @GET("tv/getDoctors")
    fun getDoctors(
        @Query("type")type : String,
    ): Call<ResponseBody>

//    @GET("tv/home")
//    fun getHome(
//        @Query("type")type : String,
//    ): Call<ResponseBody>

    @GET("tv/tvHome")
    fun getHome(
        @Query("type")type : String,
    ): Call<ResponseBody>

//    @POST("tv/addDoctor")
//    @FormUrlEncoded
//    fun addDoctor(
//        @Field("doctor_id") doctor_id: String,
//        @Field("type") type: String,
//        @Field("room") room: String
//    ): Call<ResponseBody>

    @POST("tv/addDoctorNew")
    @FormUrlEncoded
    fun addDoctor(
        @Field("doctor_id") doctor_id: String,
        @Field("type") type: String,
        @Field("room") room: String
    ): Call<ResponseBody>

//    @POST("tv/addClinicDoctors")
//    fun selectDoctors(): Call<ResponseBody>

    @POST("tv/addClinicDoctorsNew")
    fun selectDoctors(): Call<ResponseBody>

    @GET("settings")
    fun settings(): Call<ResponseBody>

    @POST("tv/runTvShow")
    @FormUrlEncoded
    fun runTvShows(
        @Field("show_id") show_id: String,
        @Field("data_usage") data_usage: String
    ): Call<ResponseBody>

    @POST("tv/runScrollingContent")
    @FormUrlEncoded
    fun runScrollingContent(
        @Field("content_id") content_id: String):Call<ResponseBody>

    @POST("tv/changeTokenStatus")
    @FormUrlEncoded
    fun changeTokenStatus(
        @Field("doctor_id") doctor_id: String,
    ): Call<ResponseBody>

    @POST("tv/convertTextToSpeech")
    @FormUrlEncoded
    fun convertTextToSpeech(
        @Field("token_number") token_number: String,
        @Field("room_number") room_number: String,
        @Field("doctor_id") doctor_id: String,
    ): Call<ResponseBody>

    @POST("tv/convertTokenTextToSpeech")
    @FormUrlEncoded
    fun convertTokenTextToSpeech(
        @Field("token_number") token_number: String,
        @Field("counter_id") counter_id: String,
        @Field("booking_id") booking_id: String,
    ): Call<ResponseBody>

    @GET("api/logout")
    fun logout(): Call<ResponseBody>
}