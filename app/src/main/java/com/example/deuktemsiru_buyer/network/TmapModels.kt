package com.example.deuktemsiru_buyer.network

import com.google.gson.JsonElement
import com.google.gson.JsonObject

data class TmapRouteRequest(
    val startX: String,
    val startY: String,
    val endX: String,
    val endY: String,
    val reqCoordType: String = "WGS84GEO",
    val resCoordType: String = "WGS84GEO",
    val startName: String = "출발지",
    val endName: String,
)

data class TmapRouteResponse(
    val type: String,
    val features: List<TmapFeature>,
)

data class TmapFeature(
    val type: String,
    val geometry: TmapGeometry,
    val properties: JsonObject? = null,
)

data class TmapGeometry(
    val type: String,
    val coordinates: JsonElement,
)