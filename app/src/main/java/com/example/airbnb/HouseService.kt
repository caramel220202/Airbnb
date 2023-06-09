package com.example.airbnb

import retrofit2.Call
import retrofit2.http.GET

interface HouseService {
    @GET("v3/6f6abfa1-0a9a-4cf8-8a00-c70078e97f69")
    fun getHouseList():Call<HouseDto>
}