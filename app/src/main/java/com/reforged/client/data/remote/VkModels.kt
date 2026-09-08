package com.reforged.client.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Long,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("photo_200") val photo200: String? = null,
    @SerialName("screen_name") val screenName: String? = null,
    val status: String? = null,
    val bdate: String? = null,
    val city: CityDto? = null,
    val country: CountryDto? = null,
    @SerialName("followers_count") val followersCount: Int? = null,
    val counters: UserCountersDto? = null,
    val about: String? = null
)

@Serializable
data class CityDto(val id: Int, val title: String)
@Serializable
data class CountryDto(val id: Int, val title: String)

@Serializable
data class UserCountersDto(
    val albums: Int? = null,
    val videos: Int? = null,
    val audios: Int? = null,
    val photos: Int? = null,
    val notes: Int? = null,
    val friends: Int? = null,
    val groups: Int? = null,
    val online_friends: Int? = null,
    val user_photos: Int? = null,
    val followers: Int? = null,
    val pages: Int? = null
)

@Serializable
data class WallResponse(
    val count: Int,
    val items: List<WallPostDto>,
    val profiles: List<UserDto>? = null,
    val groups: List<GroupDto>? = null
)

@Serializable
data class WallPostDto(
    val id: Int,
    @SerialName("owner_id") val ownerId: Long,
    @SerialName("from_id") val fromId: Long,
    val date: Long,
    val text: String,
    val attachments: List<AttachmentDto>? = null,
    val likes: LikesDto? = null,
    val reposts: RepostsDto? = null,
    val views: ViewsDto? = null,
    @SerialName("post_type") val postType: String
)

@Serializable
data class GroupDto(
    val id: Long,
    val name: String,
    @SerialName("screen_name") val screenName: String? = null,
    @SerialName("photo_200") val photo200: String? = null,
    val type: String? = null
)

@Serializable
data class AttachmentDto(
    val type: String,
    val photo: VkPhotoDto? = null,
    val video: VideoDto? = null,
    val audio: AudioTrackDto? = null,
    val doc: DocDto? = null,
    val link: LinkDto? = null,
    val sticker: StickerDto? = null,
    @SerialName("audio_message") val audioMessage: AudioMessageDto? = null,
    val graffiti: GraffitiDto? = null
)

@Serializable
data class StickerDto(
    @SerialName("sticker_id") val stickerId: Int,
    @SerialName("product_id") val productId: Int,
    val images: List<PhotoSizeDto>? = null
)

@Serializable
data class AudioMessageDto(
    val id: Long,
    @SerialName("owner_id") val ownerId: Long,
    val duration: Int,
    @SerialName("link_mp3") val linkMp3: String? = null,
    @SerialName("link_ogg") val linkOgg: String? = null
)

@Serializable
data class GraffitiDto(
    val id: Long,
    @SerialName("owner_id") val ownerId: Long,
    val url: String,
    val width: Int,
    val height: Int
)

@Serializable
data class VkPhotoDto(
    val id: Long,
    @SerialName("owner_id") val ownerId: Long,
    val sizes: List<PhotoSizeDto>? = null,
    val text: String? = null,
    val date: Long
)

@Serializable
data class PhotoSizeDto(
    val url: String,
    val width: Int,
    val height: Int,
    val type: String
)

@Serializable
data class VideoDto(
    val id: Long,
    @SerialName("owner_id") val ownerId: Long,
    val title: String,
    val duration: Int,
    @SerialName("image") val image: List<PhotoSizeDto>? = null,
    @SerialName("player") val playerUrl: String? = null
)

@Serializable
data class DocDto(
    val id: Long,
    @SerialName("owner_id") val ownerId: Long,
    val title: String,
    val size: Long,
    val ext: String,
    val url: String,
    val date: Long,
    val type: Int
)

@Serializable
data class LinkDto(
    val url: String,
    val title: String,
    val description: String? = null,
    val photo: VkPhotoDto? = null
)

@Serializable
data class LikesDto(val count: Int, @SerialName("user_likes") val userLikes: Int = 0)
@Serializable
data class RepostsDto(val count: Int, @SerialName("user_reposted") val userReposted: Int = 0)
@Serializable
data class ViewsDto(val count: Int)

@Serializable
data class ConversationsResponse(
    val count: Int,
    val items: List<ConversationItemDto>,
    @SerialName("unread_count") val unreadCount: Int? = null,
    val profiles: List<UserDto>? = null,
    val groups: List<GroupDto>? = null
)

@Serializable
data class ConversationItemDto(
    val conversation: ConversationDto,
    @SerialName("last_message") val lastMessage: MessageDto? = null
)

@Serializable
data class ConversationDto(
    val peer: PeerDto,
    @SerialName("last_message_id") val lastMessageId: Int,
    @SerialName("in_read") val inRead: Int,
    @SerialName("out_read") val outRead: Int,
    @SerialName("unread_count") val unreadCount: Int? = null,
    @SerialName("chat_settings") val chatSettings: ChatSettingsDto? = null
)

@Serializable
data class PeerDto(
    val id: Long,
    val type: String,
    @SerialName("local_id") val localId: Int? = null
)

@Serializable
data class MessageDto(
    val id: Int,
    val date: Long,
    @SerialName("peer_id") val peerId: Long,
    @SerialName("from_id") val fromId: Long,
    val text: String,
    @SerialName("random_id") val randomId: Int,
    val attachments: List<AttachmentDto>? = null,
    @SerialName("fwd_messages") val fwdMessages: List<MessageDto>? = null,
    @SerialName("reply_message") val replyMessage: MessageDto? = null,
    val action: MessageActionDto? = null,
    val out: Int? = null
)

@Serializable
data class ChatSettingsDto(
    val title: String,
    val members_count: Int? = null,
    val photo: ChatPhotoDto? = null
)

@Serializable
data class ChatPhotoDto(
    @SerialName("photo_50") val photo50: String? = null,
    @SerialName("photo_100") val photo100: String? = null,
    @SerialName("photo_200") val photo200: String? = null
)

@Serializable
data class MessageActionDto(
    val type: String,
    @SerialName("member_id") val memberId: Long? = null,
    val text: String? = null
)

@Serializable
data class HistoryResponse(
    val count: Int,
    val items: List<MessageDto>,
    val profiles: List<UserDto>? = null,
    val groups: List<GroupDto>? = null
)

@Serializable
data class LongPollParamsDto(
    val key: String,
    val server: String,
    val ts: String,
    val pts: Int? = null
)

@Serializable
data class NewsfeedResponse(
    val items: List<NewsItemDto>,
    val profiles: List<UserDto>? = null,
    val groups: List<GroupDto>? = null,
    @SerialName("next_from") val nextFrom: String? = null
)

@Serializable
data class NewsItemDto(
    val type: String,
    @SerialName("source_id") val sourceId: Long,
    val date: Long,
    @SerialName("post_id") val postId: Int? = null,
    val text: String? = null,
    val attachments: List<AttachmentDto>? = null,
    val comments: NewsItemCounterDto? = null,
    val likes: LikesDto? = null,
    val reposts: RepostsDto? = null,
    val views: ViewsDto? = null,
    val photos: NewsPhotosDto? = null
)

@Serializable
data class NewsItemCounterDto(val count: Int)
@Serializable
data class NewsPhotosDto(val count: Int, val items: List<VkPhotoDto>)

@Serializable
data class VkError(
    @SerialName("error_code") val errorCode: Int,
    @SerialName("error_msg") val errorMsg: String
)

@Serializable
data class NewsfeedResponseWrapper(
    val response: NewsfeedResponse? = null,
    val error: VkError? = null
)

@Serializable
data class UsersResponseWrapper(
    val response: List<UserDto>? = null,
    val error: VkError? = null
)

@Serializable
data class ConversationsResponseWrapper(
    val response: ConversationsResponse? = null,
    val error: VkError? = null
)

@Serializable
data class ConversationsByIdResponseWrapper(
    val response: List<ConversationDto>? = null,
    val error: VkError? = null
)

@Serializable
data class HistoryResponseWrapper(
    val response: HistoryResponse? = null,
    val error: VkError? = null
)

@Serializable
data class SendMessageResponseWrapper(
    val response: Int? = null,
    val error: VkError? = null
)

@Serializable
data class LongPollServerResponseWrapper(
    val response: LongPollParamsDto? = null,
    val error: VkError? = null
)

@Serializable
data class UploadServerResponseWrapper(
    val response: UploadServerDto? = null,
    val error: VkError? = null
)

@Serializable
data class UploadServerDto(
    @SerialName("upload_url") val uploadUrl: String
)

@Serializable
data class SaveDocResponseWrapper(
    val response: SaveDocDto? = null,
    val error: VkError? = null
)

@Serializable
data class SaveDocDto(
    val type: String? = null,
    val doc: DocDto? = null
)

@Serializable
data class WallResponseWrapper(
    val response: WallResponse? = null,
    val error: VkError? = null
)

@Serializable
data class PhotosResponseWrapper(
    val response: PhotosResponse? = null,
    val error: VkError? = null
)

@Serializable
data class PhotosResponse(
    val count: Int,
    val items: List<VkPhotoDto>
)

@Serializable
data class VideoResponseWrapper(
    val response: VideoResponse? = null,
    val error: VkError? = null
)

@Serializable
data class VideoResponse(
    val count: Int,
    val items: List<VideoDto>
)

@Serializable
data class BaseOkResponseWrapper(
    val response: Int? = null,
    val error: VkError? = null
)

// --- Auth Response ---
@Serializable
data class AuthResponse(
    @SerialName("access_token") val access_token: String? = null,
    @SerialName("user_id") val user_id: Long? = null,
    val secret: String? = null,
    @SerialName("expires_in") val expires_in: Int? = null,
    val error: String? = null,
    @SerialName("error_description") val error_description: String? = null,
    @SerialName("captcha_sid") val captcha_sid: String? = null,
    @SerialName("captcha_img") val captcha_img: String? = null,
    @SerialName("validation_type") val validation_type: String? = null,
    @SerialName("validation_sid") val validation_sid: String? = null,
    @SerialName("phone_mask") val phone_mask: String? = null
)

// --- Music (Audio) Models ---
@Serializable
data class AudioResponseWrapper(
    val response: AudioResponse? = null,
    val error: VkError? = null
)

@Serializable
data class AudioResponse(
    val count: Int,
    val items: List<AudioTrackDto>
)

@Serializable
data class AudioListResponseWrapper(
    val response: List<AudioTrackDto>? = null,
    val error: VkError? = null
)

@Serializable
data class PlaylistsResponseWrapper(
    val response: PlaylistsResponse? = null,
    val error: VkError? = null
)

@Serializable
data class PlaylistsResponse(
    val count: Int,
    val items: List<PlaylistDto>
)

@Serializable
data class PlaylistResponseWrapper(
    val response: PlaylistDto? = null,
    val error: VkError? = null
)

@Serializable
data class CatalogResponseWrapper(
    val response: CatalogResponse? = null,
    val error: VkError? = null
)

@Serializable
data class CatalogResponse(
    val items: List<CatalogSectionDto>
)

@Serializable
data class CatalogSectionDto(
    val id: String? = null,
    val title: String? = null,
    val type: String? = null, // "list", "blocks", etc.
    val items: List<CatalogItemDto>? = null,
    val playlists: List<PlaylistDto>? = null,
    val audios: List<AudioTrackDto>? = null
)

@Serializable
data class CatalogItemDto(
    val id: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val type: String? = null, // "playlist", "audio", etc.
    val playlist: PlaylistDto? = null,
    val audio: AudioTrackDto? = null
)

@Serializable
data class PlaylistDto(
    val id: Long,
    @SerialName("owner_id") val ownerId: Long,
    val title: String,
    val description: String? = null,
    val photo: AudioPhotoDto? = null,
    val count: Int
)

@Serializable
data class AudioPhotoDto(
    val photo_300: String? = null,
    val photo_600: String? = null,
    val photo_1200: String? = null
)

@Serializable
data class AudioTrackDto(
    val id: Long,
    @SerialName("owner_id") val ownerId: Long,
    val artist: String,
    val title: String,
    val duration: Int,
    val url: String? = null,
    @SerialName("track_code") val trackCode: String? = null,
    val ads: String? = null,
    val album: AlbumDto? = null
)

@Serializable
data class AlbumDto(
    val id: Long,
    val thumb: AudioPhotoDto? = null,
    val title: String? = null
)

// --- Badge Response ---
@Serializable
data class BadgeResponse(
    val status: String,
    val data: BadgeData? = null,
    val message: String? = null
)

@Serializable
data class BadgeData(
    @SerialName("vk_id") val vk_id: Long,
    val badges: List<BadgeDto>
)

@Serializable
data class BadgeDto(
    val type: Int,
    val slug: String,
    val label: String,
    val icon: String,
    val priority: Int
)
