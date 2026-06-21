package id.tirtawijata.crumina.data

import retrofit2.http.GET

data class Profile(val email: String? = null, val name: String? = null, val picture: String? = null)
data class DataResponse(val profile: Profile? = null)
data class FxResponse(val base: String? = null, val rates: Map<String, Double>? = null)

interface Api {
    @GET("api/data") suspend fun data(): DataResponse
    @GET("api/fx") suspend fun fx(): FxResponse
}
