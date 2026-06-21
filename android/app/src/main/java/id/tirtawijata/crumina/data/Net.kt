package id.tirtawijata.crumina.data

import id.tirtawijata.crumina.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Net {
    fun api(store: SecureStore): Api {
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val builder = chain.request().newBuilder()
                store.session?.let { builder.header("Authorization", "Bearer $it") }
                store.refreshToken?.let { builder.header("X-Cr-Rt", it) }
                store.statementPw?.let {
                    if (it.isNotBlank()) {
                        builder.header("X-Cr-Pw", android.util.Base64.encodeToString(it.toByteArray(), android.util.Base64.NO_WRAP))
                    }
                }
                chain.proceed(builder.build())
            })
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Api::class.java)
    }
}
