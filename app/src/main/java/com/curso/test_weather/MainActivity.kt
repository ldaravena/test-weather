package com.curso.test_weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.widget.TextView
import androidx.core.app.ActivityCompat
import android.provider.Settings
import android.view.View
import android.widget.Toast
import com.google.android.gms.location.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
/*

Esta aplicación muestra en pantalla el clima actual de la ubicación del teléfono, usando como base los
códigos:

Código para obtener el tiempo actual: https://www.c-sharpcorner.com/article/how-to-use-retrofit-2-with-android-using-kotlin/
Código para obtener la ubicación: https://blog.frsarker.com/kotlin/detect-current-latitude-and-longitude-using-kotlin-in-android.html

Se deban agregar dependencias al gradle:

implementation 'com.squareup.retrofit2:retrofit:2.0.0'
implementation 'com.squareup.retrofit2:converter-gson:2.0.0'
implementation 'com.google.android.gms:play-services-location:17.0.0'

Agregar permisos al Manifest:

<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>

Se agrega un botón "refresh" para cargar la información. Esto se necesita cuando no está activada la
ubicación y se activa, no se cargan los datos automáticamente, por eso se puso el botón.
*/

class MainActivity : AppCompatActivity() {

    //ID único para el permiso de ubicación
    val PERMISSION_ID = 15

    //Textview
    var climaTexto:TextView? = null

    lateinit var mFusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        climaTexto= findViewById(R.id.climaActual)

        //Se utiliza la API de Google Fused Location Provider para obtener
        //la última ubicación conocida del smartphone
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //Se obtiene la ubicación del teléfono
        getLastLocation()
    }

    //Se obtiene la ubicación del teléfono
    @SuppressLint("MissingPermission")
    private fun getLastLocation(){
        //Se verifican que existen los permisos
        if (checkPermissions()) {

            //Se verifica que la ubicación está activida
            if (isLocationEnabled()) {

                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result

                    //Si no hay ubicación registrada
                    if (location == null) {
                        //Se pide la ubicación
                        requestNewLocationData()
                    }

                    //Si hay ubicación se llama a la función para mostrar el tiempo actual
                    else {

                        getCurrentData(Pair(location.latitude.toString(), location.longitude.toString()))
                    }
                }
            }
            //Si no está activada la ubicación
            else {
                //Se muestra un mensaje al usuario
                Toast.makeText(this, "Activar Ubicación", Toast.LENGTH_LONG).show()

                //Se inicia la actividad para activar la ubicación
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        //Si no hay permisos de ubicación se piden
        else {
            requestPermissions()
        }
    }

    //Obtiene la nueva ubicación
    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        //Se pide la ubicación con los siguientes párametros
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation

            //Se llama a la función para mostrar el tiempo actual
            getCurrentData(Pair(mLastLocation.latitude.toString(),mLastLocation.longitude.toString()))
        }
    }

    //Se verifican que existan los permisos
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            return true
        }
        return false
    }

    //Se solicitan los persmisos
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }

    //Retorna si la ubicación está activada
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    //Función para obtener el tiempo actual desde OpenWeather.com
    //Información de uso de la Api: https://openweathermap.org/current
    private fun getCurrentData(data: Pair<String, String>) {

        //Página para consultar el tiempo actual
        val BaseUrl = "http://api.openweathermap.org/"
        //ID para usar la api
        val AppId = "127ee1916524bda23a4e4ed6e1de2c25"
        //La unidad de medida de la temperatura es en celsius
        val units = "metric"
        //Entrega el resultado en español
        val lang = "es"
        //Latitud y longitud obtenidas desde la ubicación del teléfono
        val lat = data.first
        val lon= data.second

        //Construye las propiedades de Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(BaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(WeatherService::class.java)

        //Llama al servicio del tiempo con los párametros fijados
        val call = service.getCurrentWeatherData(lat, lon, AppId, units, lang)

        //Se obtiene la respuesta de la página
        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {

                //Si hay respuesta construye el string con los datos
                if (response.code() == 200) {
                    val weatherResponse = response.body()!!

                    val stringBuilder = "Lugar: " +
                            weatherResponse.name +
                            "\n" +
                            "Temperatura: " +
                            weatherResponse.main!!.temp +
                            "\n" +
                            "Estado: " +
                            weatherResponse.weather[0].description +
                            "\n" +
                            "Humedad: " +
                            weatherResponse.main!!.humidity +
                            "\n"

                    //Se muestra el resultado en pantalla
                    climaTexto!!.text = stringBuilder
                }
            }

            //En caso de que no haya respuesta se muestra el error
            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                climaTexto!!.text = t.message
            }
        })
    }

    //Función para el botón refresh
    fun refresh(view: View) {
        getLastLocation()
    }
}