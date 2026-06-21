package id.tirtawijata.crumina.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface Api {
    @GET("api/data") suspend fun data(): DataResponse
    @GET("api/fx") suspend fun fx(@Query("base") base: String): FxResponse
    @GET("api/sync") suspend fun sync(@Header("X-Cr-Rt") rt: String?): SyncResponse
    @GET("api/statements") suspend fun statements(@Header("X-Cr-Rt") rt: String?, @Header("X-Cr-Pw") pw: String?): StatementsResponse
    @GET("api/statements") suspend fun discoverBanks(@Query("discover") discover: Int, @Header("X-Cr-Rt") rt: String?): DiscoverResponse
    @GET("api/yf") suspend fun search(@Query("q") q: String): SearchResponse
    @GET("api/yf") suspend fun quote(@Query("symbol") symbol: String): QuoteResponse
    @POST("api/upload") suspend fun upload(@Body body: UploadReq): UploadResponse
}
