import com.medicinoclinic.retrofit.APIService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    var retrofit: Retrofit? = null
    var bearer_token: String? = null

    fun getClient(baseUrl: String): Retrofit? {
//        if (retrofit == null) {
            //TODO While release in Google Play Change the Level to NONE
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            val client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .connectTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .client(client)
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
//        }

        return retrofit

    }

    fun getClient1(baseUrl: String): Retrofit? {
//        if (retrofit == null) {
            //TODO While release in Google Play Change the Level to NONE
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            val client1 = OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val newRequest = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $bearer_token")
                        .addHeader("Content-Type", "application/json")
                        .build()
                    chain.proceed(newRequest)
                }.build()

            retrofit = Retrofit.Builder()
                .client(client1)
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
//        }

        return retrofit

    }
    object ApiUtils {

//        val BASE_URL = "https://ourworks.co.in/medicino-backend/public/api/"
//        val VIDEO_URL = "https://ourworks.co.in/medicino-backend/storage/app/public/"
//
//         val BASE_URL = "https://services.dev.medicinohealthtech.in/"
//         val VIDEO_URL = "https://services.dev.medicinohealthtech.in/storage/"

        val BASE_URL = "https://api.brightideainfotech.tech/"
        val VIDEO_URL = "https://api.brightideainfotech.tech/storage/"

        val apiService: APIService
            get() = RetrofitClient.getClient(BASE_URL)!!.create(APIService::class.java)

        val apiService1: APIService
            get() = RetrofitClient.getClient1(BASE_URL)!!.create(APIService::class.java)

    }
}