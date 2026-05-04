package com.example.myapplication.models

import com.google.gson.annotations.SerializedName

data class TruckLocation(
    val id: Int,
    @SerializedName("driver_id") val driverId: Int,
    @SerializedName("truck_id") val truckId: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("driver_name") val driverName: String?
)

data class LocationsResponse(
    val success: Boolean,
    val message: String?,
    val data: List<TruckLocation>?
)