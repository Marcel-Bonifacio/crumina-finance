package id.tirtawijata.crumina.data

import retrofit2.http.GET
import retrofit2.http.Query

interface Api {
    @GET("api/data") suspend fun data(): DataResponse
    @GET("api/fx") suspend fun fx(@Query("base") base: String): FxResponse
    @GET("api/sync") suspend fun sync(): SyncResponse
    @GET("api/statements") suspend fun statements(): StatementsResponse
    @GET("api/yf") suspend fun search(@Query("q") q: String): SearchResponse
    @GET("api/yf") suspend fun quote(@Query("symbol") symbol: String): QuoteResponse
}
