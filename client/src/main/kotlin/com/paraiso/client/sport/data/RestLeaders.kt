package com.paraiso.client.sport.data

import com.paraiso.domain.routes.SiteRoute
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import com.paraiso.domain.sport.data.Category as CategoryDomain
import com.paraiso.domain.sport.data.CategoryLeader as CategoryLeaderDomain
import com.paraiso.domain.sport.data.StatLeaders as StatLeadersDomain

@Serializable
data class RestLeaders(
    val categories: List<RestCategory>
)

@Serializable
data class RestCategory(
    val name: String,
    val displayName: String,
    val shortDisplayName: String,
    val leaders: List<RestLeader>
)

@Serializable
data class RestLeader(
    val displayValue: String,
    val value: Double,
    val athlete: RestAthleteLeader
)

@Serializable
data class RestAthleteLeader(
    @SerialName("\$ref")
    @Serializable(with = AthleteRefIdSerializer::class)
    val id: Int
)

fun RestLeaders.toDomain(
    sport: SiteRoute,
    season: String,
    type: String,
    teamId: String? = null
): StatLeadersDomain {
    var id = "$sport-$season-$type"
    if(teamId != null) id += "-$teamId"
    return StatLeadersDomain(
        id = id,
        sport = sport.name,
        season = season.toIntOrNull() ?: 0,
        type = type.toIntOrNull() ?: 0,
        teamId = teamId,
        categories = categories.map { it.toDomain() }
    )
}

fun RestCategory.toDomain() = CategoryDomain(
    name = name,
    displayName = displayName,
    shortDisplayName = shortDisplayName,
    leaders = leaders.map { it.toDomain() }
)

fun RestLeader.toDomain() = CategoryLeaderDomain(
    athleteId = athlete.id,
    value = value,
    displayValue = displayValue
)

object AthleteRefIdSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AthleteRefId", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        val ref = decoder.decodeString()
        val regex = """/athletes/(\d+)""".toRegex()
        val match = regex.find(ref)
        return match?.groupValues?.get(1)?.toInt()
            ?: throw SerializationException("No athlete ID found in ref: $ref")
    }

    override fun serialize(encoder: Encoder, value: Int) {
        // Optional
    }
}
