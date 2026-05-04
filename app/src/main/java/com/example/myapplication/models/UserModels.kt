package com.example.myapplication.models

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val username: String,
    val name: String,
    val email: String,
    val password: String,
    val role: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("area_id") val areaId: Int? = null,
    @SerializedName("purok") val purok: String? = null,
    @SerializedName("complete_address") val completeAddress: String? = null,
    @SerializedName("license_number") val licenseNumber: String? = null,
    @SerializedName("preferred_truck") val preferredTruck: String? = null
)

data class LoginRequest(
    val username_or_email: String,
    val password: String
)

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val user: UserData? = null
)

data class UserData(
    @SerializedName("user_id") val userId: Int,
    val username: String,
    val name: String,
    val email: String,
    val role: String,
    val phone: String? = null,
    val purok: String? = null,
    @SerializedName("complete_address") val completeAddress: String? = null,
    @SerializedName("license_number") val licenseNumber: String? = null,
    @SerializedName("preferred_truck") val preferredTruck: String? = null
)