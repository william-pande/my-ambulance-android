package com.wilsofts.myambulance.utils.network

import android.graphics.Color
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.wilsofts.myambulance.utils.Utils
import org.json.JSONObject

class PointsParser(private val parseFinished: ParseFinished, private val activity: FragmentActivity) {
    private lateinit var routeData: JSONObject

    fun parseRoutes(routes: JSONObject, index: Int) {
        Thread {
            val route = routes.getJSONArray("routes").getJSONObject(index)
            this.routeData = routes.getJSONArray("routes").getJSONObject(index)
                .getJSONArray("legs").getJSONObject(0)
            this.drawPolyLines(path = this.parseRoute(route = route))
        }.start()
    }

    fun parseRoutes(route: JSONObject) {
        Thread {
            this.routeData = route.getJSONArray("legs").getJSONObject(0)
            this.drawPolyLines(path = this.parseRoute(route = route))
        }.start()
    }

    private fun drawPolyLines(path: MutableList<HashMap<String, String>>) {
        val lineOptions = PolylineOptions()
        val points: ArrayList<LatLng> = ArrayList()

        // Fetching all the points in i-th route
        for (path_index in path.indices) {
            val point = path[path_index]
            val lat = point["lat"]!!.toDouble()
            val lng = point["lng"]!!.toDouble()
            val position = LatLng(lat, lng)
            points.add(position)
        }
        // Adding all the points in the route to LineOptions
        lineOptions.addAll(points)
        lineOptions.width(7f)
        lineOptions.color(Color.parseColor("#2B2727"))

        val distance = this.routeData.getJSONObject("distance").getDouble("value")
        val duration = Utils.formatTime(time = this.routeData.getJSONObject("duration").getString("text"))

        // Drawing polyline in the Google Map for the i-th route
        this.activity.runOnUiThread {
            this.parseFinished.onTaskDone(line_options = lineOptions, distance = distance, duration = duration)
        }
    }

    private fun parseRoute(route: JSONObject): MutableList<HashMap<String, String>> {
        val legs = route.getJSONArray("legs")
        val path: MutableList<HashMap<String, String>> = ArrayList()

        /* Traversing all legs */
        for (legIndex in 0 until legs.length()) {
            val steps = (legs[legIndex] as JSONObject).getJSONArray("steps")

            /* Traversing all steps */
            for (step_index in 0 until steps.length()) {
                val polyline = ((steps[step_index] as JSONObject)["polyline"] as JSONObject)["points"] as String
                val list = this.decodePoly(polyline)

                /* Traversing all points */
                for (l in list.indices) {
                    val hm = HashMap<String, String>()
                    hm["lat"] = list[l].latitude.toString()
                    hm["lng"] = list[l].longitude.toString()
                    path.add(hm)
                }
            }
        }
        return path
    }

    /**
     * Method to decode polyline points
     * Courtesy : https://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
     */
    private fun decodePoly(encoded: String): List<LatLng> {
        val poly: MutableList<LatLng> = ArrayList()
        var index = 0
        var latitude = 0
        var longitude = 0
        while (index < encoded.length) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val lat =
                if (result and 1 != 0) {
                    (result shr 1).inv()
                } else {
                    result shr 1
                }
            latitude += lat
            shift = 0
            result = 0

            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val long =
                if (result and 1 != 0) {
                    (result shr 1).inv()
                } else {
                    result shr 1
                }
            longitude += long
            val p = LatLng(latitude.toDouble() / 1E5, longitude.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

    interface ParseFinished {
        fun onTaskDone(line_options: PolylineOptions, distance: Double, duration: Int)
    }
}