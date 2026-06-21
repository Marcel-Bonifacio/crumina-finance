package id.tirtawijata.crumina.data

import id.tirtawijata.crumina.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** One shared client. Only the session Bearer is global; the refresh token and statement
 *  passwords are attached per-call (see Api) so they never ride on FX/quote/data requests. */
object Net {
    private var store: SecureStore? = null

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val b = chain.request().newBuilder()
                store?.session?.let { b.header("Authorization", "Bearer $it") }
                chain.proceed(b.build())
            })
            .build()
    }

    private val instance: Api by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Api::class.java)
    }

    fun api(store: SecureStore): Api { this.store = store; return instance }
}
