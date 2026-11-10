package id.tirtawijata.crumina.data

import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface Api {
    @GET("api/data") suspend fun data(): DataResponse
    @POST("api/auth/callback") suspend fun authCode(@Body body: AuthCodeReq): AuthCodeResp
    @GET("api/guest") suspend fun guestToken(@Query("client") client: String = "android"): GuestResponse
    @GET("api/fx") suspend fun fx(@Query("base") base: String): FxResponse
    @GET("api/guest") suspend fun config(@Query("config") config: Int = 1): ConfigResponse
    // Email (IMAP) sign-in with an app password; the encrypted credentials come back for X-Cr-Imap.
    @FormUrlEncoded @POST("api/guest")
    suspend fun imapLogin(@Field("email") email: String, @Field("password") password: String, @Field("provider") provider: String, @Query("client") client: String = "android"): ImapLoginResponse
    // The mailbox is either Gmail through OAuth (X-Cr-Rt) or IMAP (X-Cr-Imap); the server uses whichever is sent.
    @GET("api/sync") suspend fun sync(@Header("X-Cr-Rt") rt: String?, @Header("X-Cr-Imap") imap: String?): SyncResponse
    @GET("api/statements") suspend fun statements(@Header("X-Cr-Rt") rt: String?, @Header("X-Cr-Pw") pw: String?, @Header("X-Cr-Imap") imap: String?): StatementsResponse
    @GET("api/statements") suspend fun discoverBanks(@Query("discover") discover: Int, @Header("X-Cr-Rt") rt: String?, @Header("X-Cr-Imap") imap: String?): DiscoverResponse
    @GET("api/yf") suspend fun search(@Query("q") q: String): SearchResponse
    @GET("api/yf") suspend fun quote(@Query("symbol") symbol: String): QuoteResponse
    @POST("api/upload") suspend fun upload(@Body body: UploadReq): UploadResponse
}
