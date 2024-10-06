package com.example.myapplication.api

import StudentBoardingStatus
import StudentExitPermission
import StudentMealTypeInfoToday
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    @GET("/api/student-boarding-status/{qrCode}")
    fun updateExitPermission(
        @Path("qrCode") qrCode: String
    ): Call<StudentExitPermission>

    @GET("/api/student-meal-type/{qrCode}")
    fun updateMealType(
        @Path("qrCode") qrCode: String
    ): Call<StudentMealTypeInfoToday>

    @GET("/api/student-boarding-status/{qrCode}")
    fun updateBoardingStatus(
        @Path("qrCode") qrCode: String
    ): Call<StudentBoardingStatus>

}

