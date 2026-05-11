package com.example.deuktemsiru_buyer.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface TmapApiService {
    @POST("tmap/routes/pedestrian")
    suspend fun getPedestrianRoute(
        @Query("version") version: Int = 1,
        @Header("appKey") appKey: String,
        @Body request: TmapRouteRequest,
    ): TmapRouteResponse
}