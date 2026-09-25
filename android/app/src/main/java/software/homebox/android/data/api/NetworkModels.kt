package software.homebox.android.data.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class StatusResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("build") val build: BuildInfo? = null
)

data class BuildInfo(
    @SerializedName("version") val version: String? = null,
    @SerializedName("commit") val commit: String? = null,
    @SerializedName("buildTime") val buildTime: String? = null
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class TokenResponse(
    @SerializedName("token") val token: String,
    @SerializedName("expiresAt") val expiresAt: String? = null,
    @SerializedName("attachmentToken") val attachmentToken: String? = null
)

data class EntityItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("typeId") val typeId: String? = null,
    @SerializedName("parentId") val parentId: String? = null,
    @SerializedName("quantity") val quantity: Int? = 1,
    @SerializedName("modelNumber") val modelNumber: String? = null,
    @SerializedName("serialNumber") val serialNumber: String? = null,
    @SerializedName("manufacturer") val manufacturer: String? = null,
    @SerializedName("purchasePrice") val purchasePrice: Double? = null,
    @SerializedName("purchaseTime") val purchaseTime: String? = null,
    @SerializedName("insured") val insured: Boolean? = false,
    @SerializedName("warrantyExpires") val warrantyExpires: String? = null,
    @SerializedName("parentName") val parentName: String? = null,
    @SerializedName("locationName") val locationName: String? = null,
    @SerializedName("attachments") val attachments: List<AttachmentItem>? = null
) : Serializable

data class AttachmentItem(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String? = null,
    @SerializedName("title") val title: String? = null
) : Serializable

data class EntitiesResponse(
    @SerializedName("items") val items: List<EntityItem>? = null,
    @SerializedName("total") val total: Int? = 0
)

data class CreateEntityRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("typeId") val typeId: String? = null,
    @SerializedName("parentId") val parentId: String? = null,
    @SerializedName("quantity") val quantity: Int = 1,
    @SerializedName("modelNumber") val modelNumber: String? = null,
    @SerializedName("serialNumber") val serialNumber: String? = null,
    @SerializedName("purchasePrice") val purchasePrice: Double? = null
)

data class LocationTreeItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("children") val children: List<LocationTreeItem>? = null,
    @SerializedName("itemCount") val itemCount: Int? = 0
) : Serializable
