package com.reforged.client.data.remote

import com.google.gson.annotations.SerializedName

data class UserDto(
    val id: Long,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("photo_200") val photo200: String?,
    @SerializedName("screen_name") val screenName: String?,
    val status: String?,
    val bdate: String?,
    val city: CityDto?,
    val country: CountryDto?,
    @SerializedName("followers_count") val followersCount: Int?,
    val counters: UserCountersDto?,
    val about: String?
)

data class CityDto(val id: Int, val title: String)
data class CountryDto(val id: Int, val title: String)

data class UserCountersDto(
    val albums: Int?,
    val videos: Int?,
    val audios: Int?,
    val photos: Int?,
    val notes: Int?,
    val friends: Int?,
    val groups: Int?,
    val online_friends: Int?,
    val user_photos: Int?,
    val followers: Int?,
    val pages: Int?
)

data class WallResponse(
    val count: Int,
    val items: List<WallPostDto>,
    val profiles: List<UserDto>?,
    val groups: List<GroupDto>?
)

data class WallPostDto(
    val id: Int,
    @SerializedName("owner_id") val ownerId: Long,
    @SerializedName("from_id") val fromId: Long,
    val date: Long,
    val text: String,
    val attachments: List<AttachmentDto>?,
    val likes: LikesDto?,
    val reposts: RepostsDto?,
    val views: ViewsDto?,
    @SerializedName("post_type") val postType: String
)

data class GroupDto(
    val id: Long,
    val name: String,
    @SerializedName("screen_name") val screenName: String?,
    @SerializedName("photo_200") val photo200: String?,
    val type: String?
)

data class AttachmentDto(
    val type: String,
    val photo: VkPhotoDto? = null,
    val video: VideoDto? = null,
    val audio: AudioTrackDto? = null,
    val doc: DocDto? = null,
    val link: LinkDto? = null,
    val sticker: StickerDto? = null,
    @SerializedName("audio_message") val audioMessage: AudioMessageDto? = null,
    val graffiti: GraffitiDto? = null
)

data class StickerDto(
    @SerializedName("sticker_id") val stickerId: Int,
    @SerializedName("product_id") val productId: Int,
    val images: List<PhotoSizeDto>?
)

data class AudioMessageDto(
    val id: Long,
    @SerializedName("owner_id") val ownerId: Long,
    val duration: Int,
    @SerializedName("link_mp3") val linkMp3: String?,
    @SerializedName("link_ogg") val linkOgg: String?
)

data class GraffitiDto(
    val id: Long,
    @SerializedName("owner_id") val ownerId: Long,
    val url: String,
    val width: Int,
    val height: Int
)

data class VkPhotoDto(
    val id: Long,
    @SerializedName("owner_id") val ownerId: Long,
    val sizes: List<PhotoSizeDto>?,
    val text: String?,
    val date: Long
)

data class PhotoSizeDto(
    val url: String,
    val width: Int,
    val height: Int,
    val type: String
)

data class VideoDto(
    val id: Long,
    @SerializedName("owner_id") val ownerId: Long,
    val title: String,
    val duration: Int,
    @SerializedName("image") val image: List<PhotoSizeDto>?,
    @SerializedName("player") val playerUrl: String?
)

data class DocDto(
    val id: Long,
    @SerializedName("owner_id") val ownerId: Long,
    val title: String,
    val size: Long,
    val ext: String,
    val url: String,
    val date: Long,
    val type: Int
)

data class LinkDto(
    val url: String,
    val title: String,
    val description: String?,
    val photo: VkPhotoDto?
)

data class LikesDto(val count: Int, @SerializedName("user_likes") val userLikes: Int)
data class RepostsDto(val count: Int, @SerializedName("user_reposted") val userReposted: Int)
data class ViewsDto(val count: Int)

data class ConversationsResponse(
    val count: Int,
    val items: List<ConversationItemDto>,
    @SerializedName("unread_count") val unreadCount: Int?,
    val profiles: List<UserDto>?,
    val groups: List<GroupDto>?
)

data class ConversationItemDto(
    val conversation: ConversationDto,
    @SerializedName("last_message") val lastMessage: MessageDto?
)

data class ConversationDto(
    val peer: PeerDto,
    @SerializedName("last_message_id") val lastMessageId: Int,
    @SerializedName("in_read") val inRead: Int,
    @SerializedName("out_read") val outRead: Int,
    @SerializedName("unread_count") val unreadCount: Int?,
    @SerializedName("chat_settings") val chatSettings: ChatSettingsDto?
)

data class PeerDto(
    val id: Long,
    val type: String,
    @SerializedName("local_id") val localId: Int?
)

data class MessageDto(
    val id: Int,
    val date: Long,
    @SerializedName("peer_id") val peerId: Long,
    @SerializedName("from_id") val fromId: Long,
    val text: String,
    @SerializedName("random_id") val randomId: Int,
    val attachments: List<AttachmentDto>?,
    @SerializedName("fwd_messages") val fwdMessages: List<MessageDto>?,
    @SerializedName("reply_message") val replyMessage: MessageDto?,
    val action: MessageActionDto?,
    val out: Int?
)

data class ChatSettingsDto(
    val title: String,
    val members_count: Int?,
    val photo: ChatPhotoDto?
)

data class ChatPhotoDto(
    @SerializedName("photo_50") val photo50: String?,
    @SerializedName("photo_100") val photo100: String?,
    @SerializedName("photo_200") val photo200: String?
)

data class MessageActionDto(
    val type: String,
    @SerializedName("member_id") val memberId: Long?,
    val text: String?
)

data class HistoryResponse(
    val count: Int,
    val items: List<MessageDto>,
    val profiles: List<UserDto>?,
    val groups: List<GroupDto>?
)

data class LongPollParamsDto(
    val key: String,
    val server: String,
    val ts: String,
    val pts: Int? = null
)

data class NewsfeedResponse(
    val items: List<NewsItemDto>,
    val profiles: List<UserDto>?,
    val groups: List<GroupDto>?,
    @SerializedName("next_from") val nextFrom: String?
)

data class NewsItemDto(
    val type: String,
    @SerializedName("source_id") val sourceId: Long,
    val date: Long,
    @SerializedName("post_id") val postId: Int?,
    val text: String?,
    val attachments: List<AttachmentDto>?,
    val comments: NewsItemCounterDto?,
    val likes: LikesDto?,
    val reposts: RepostsDto?,
    val views: ViewsDto?,
    val photos: NewsPhotosDto?
)

data class NewsItemCounterDto(val count: Int)
data class NewsPhotosDto(val count: Int, val items: List<VkPhotoDto>)

data class VkError(
    @SerializedName("error_code") val errorCode: Int,
    @SerializedName("error_msg") val errorMsg: String
)

data class NewsfeedResponseWrapper(
    val response: NewsfeedResponse? = null,
    val error: VkError? = null
)

data class UsersResponseWrapper(
    val response: List<UserDto>? = null,
    val error: VkError? = null
)

data class ConversationsResponseWrapper(
    val response: ConversationsResponse? = null,
    val error: VkError? = null
)

data class ConversationsByIdResponseWrapper(
    val response: List<ConversationDto>? = null,
    val error: VkError? = null
)

data class HistoryResponseWrapper(
    val response: HistoryResponse? = null,
    val error: VkError? = null
)

data class SendMessageResponseWrapper(
    val response: Int? = null,
    val error: VkError? = null
)

data class LongPollServerResponseWrapper(
    val response: LongPollParamsDto? = null,
    val error: VkError? = null
)

data class UploadServerResponseWrapper(
    val response: UploadServerDto? = null,
    val error: VkError? = null
)

data class UploadServerDto(
    @SerializedName("upload_url") val uploadUrl: String
)

data class SaveDocResponseWrapper(
    val response: SaveDocDto? = null,
    val error: VkError? = null
)

data class SaveDocDto(
    val type: String?,
    val doc: DocDto?
)

data class WallResponseWrapper(
    val response: WallResponse? = null,
    val error: VkError? = null
)

data class PhotosResponseWrapper(
    val response: PhotosResponse? = null,
    val error: VkError? = null
)

data class PhotosResponse(
    val count: Int,
    val items: List<VkPhotoDto>
)

data class VideoResponseWrapper(
    val response: VideoResponse? = null,
    val error: VkError? = null
)

data class VideoResponse(
    val count: Int,
    val items: List<VideoDto>
)

data class BaseOkResponseWrapper(
    val response: Int? = null,
    val error: VkError? = null
)
