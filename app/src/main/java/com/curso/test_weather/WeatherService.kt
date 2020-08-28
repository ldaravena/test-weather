package com.curso.test_weather

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

//Interfaz para realizar la petición a la página del tiempo

interface WeatherService {
    @GET("data/2.5/weather?")
    fun getCurrentWeatherData(@Query("lat") lat: String, @Query("lon") lon: String, @Query("APPID") app_id: String, @Query("units") units: String, @Query("lang") lang: String): Call<WeatherResponse>
}