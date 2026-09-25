package software.homebox.android.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface HomeboxApiService {

    @GET("api/v1/status")
    suspend fun getStatus(): Response<StatusResponse>

    @POST("api/v1/users/login")
    suspend fun login(@Body body: LoginRequest): Response<TokenResponse>

    @GET("api/v1/entities")
    suspend fun getEntities(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("type") type: String? = null,
        @Query("location_id") locationId: String? = null
    ): Response<EntitiesResponse>

    @POST("api/v1/entities")
    suspend fun createEntity(@Body body: CreateEntityRequest): Response<EntityItem>

    @GET("api/v1/entities/{id}")
    suspend fun getEntity(@Path("id") id: String): Response<EntityItem>

    @PUT("api/v1/entities/{id}")
    suspend fun updateEntity(
        @Path("id") id: String,
        @Body body: CreateEntityRequest
    ): Response<EntityItem>

    @DELETE("api/v1/entities/{id}")
    suspend fun deleteEntity(@Path("id") id: String): Response<Unit>

    @GET("api/v1/entities/tree")
    suspend fun getLocationTree(): Response<List<LocationTreeItem>>
}
