package com.reforged.client.data.remote

import com.reforged.client.data.repository.SettingsRepository
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject

class NewsfeedInterceptor(
    private val settingsRepository: SettingsRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        
        val url = request.url.toString()
        if (url.contains("method/newsfeed.get") || url.contains("method/newsfeed.getRecommended")) {
            val bodyString = response.body?.string() ?: return response
            
            try {
                val json = JSONObject(bodyString)
                if (json.has("response")) {
                    val responseJson = json.getJSONObject("response")
                    if (responseJson.has("items")) {
                        val items = responseJson.getJSONArray("items")
                        val filteredItems = JSONArray()
                        
                        val blockAds = settingsRepository.blockAds
                        val blockRecommended = settingsRepository.blockRecommended
                        
                        for (i in 0 until items.length()) {
                            val item = items.getJSONObject(i)
                            var type = item.optString("type")
                            
                            // Map 'clip' to 'video' to avoid crash and show in feed
                            if (type == "clip") {
                                item.put("type", "video")
                                type = "video"
                            }
                            
                            val isAd = type == "ads" || type == "ads_promotion" || type == "ads_easy_promote"
                            val isRecommended = type == "recommended" || type == "recommended_groups" || 
                                              type == "recommended_playlists" || type == "recommended_audios"
                            
                            // Filter out "added photo" notifications which are often just spammy in the feed
                            val isPhotoNotification = type == "photo" || type == "photos"
                            
                            // CRITICAL: We MUST remove types that are not supported by the SDK to avoid crashes
                            val isUnsupportedBySdk = type == "recommended_groups" || 
                                              type == "recommended_audios" || 
                                              type == "recommended_playlists" ||
                                              type == "stories" ||
                                              type == "clips"
                            
                            if ((!isAd || !blockAds) && (!isRecommended || !blockRecommended) && !isUnsupportedBySdk && !isPhotoNotification) {
                                filteredItems.put(item)
                            }
                        }
                        
                        responseJson.put("items", filteredItems)
                    }
                }
                
                val newBody = json.toString().toResponseBody(response.body?.contentType())
                return response.newBuilder().body(newBody).build()
            } catch (e: Exception) {
                return response.newBuilder().body(bodyString.toResponseBody(response.body?.contentType())).build()
            }
        }
        
        return response
    }
}
