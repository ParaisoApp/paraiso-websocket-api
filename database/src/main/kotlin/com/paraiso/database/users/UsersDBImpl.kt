package com.paraiso.database.users

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.Filters.regex
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.auth.AuthId as AuthIdDomain
import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.RoleUpdate
import com.paraiso.domain.messageTypes.Tag
import com.paraiso.domain.users.User as UserDomain
import com.paraiso.domain.users.UserFavorite as UserFavoriteDomain
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UserSettings as UserSettingsDomain
import com.paraiso.domain.users.UsersDB
import com.paraiso.domain.util.Constants.ID
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import org.bson.Document
import java.util.Date

class UsersDBImpl(database: MongoDatabase) : UsersDB, Klogging {
    companion object {
        const val PARTIAL_RETRIEVE_LIM = 5
    }

    private val collection = database.getCollection("users", User::class.java)

    override suspend fun findById(id: String) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(eq(ID, id)).limit(1).firstOrNull()?.toDomain()
            } catch (ex: Exception){
                logger.error { "Error finding user by id: $ex" }
                null
            }
        }
    override suspend fun findByIdIn(ids: List<String>) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(`in`(ID, ids)).map { it.toDomain() }.toList()
            } catch (ex: Exception){
                logger.error { "Error finding users by ids: $ex" }
                emptyList()
            }
        }

    override suspend fun findByName(name: String) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(eq(User::name.name, name)).limit(1).firstOrNull()?.toDomain()
            } catch (ex: Exception){
                logger.error { "Error finding user by name: $ex" }
                null
            }
        }

    override suspend fun existsByName(name: String) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(eq(User::name.name, name))
                .limit(1)
                .firstOrNull() != null
            } catch (ex: Exception){
                logger.error { "Error checking if user exists by name: $ex" }
                false
            }
        }

    override suspend fun findByPartial(partial: String) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(regex(User::name.name, partial, "i"))
                    .limit(PARTIAL_RETRIEVE_LIM)
                    .map { it.toDomain() }
                    .toList()
            } catch (ex: Exception){
                logger.error { "Error finding users by partial: $ex" }
                emptyList()
            }
        }

    override suspend fun findUserByAuthId(authId: String) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(eq("${User::authIds.name}.id", authId)).limit(1).firstOrNull()?.toDomain()
            } catch (ex: Exception){
                logger.error { "Error finding user by auth id: $ex" }
                null
            }
        }

    override suspend fun getUserList(
        userIds: List<String>,
        filters: FilterTypes,
        followingList: List<String>
    ) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(
                    and(
                        `in`(ID, userIds),
                        or(
                            `in`(User::roles.name, filters.userRoles),
                            and(
                                `in`(User::roles.name, UserRole.FOLLOWING),
                                `in`(User::id.name, followingList)
                            )
                        )
                    )
                ).map { it.toDomain() }.toList()
            } catch (ex: Exception){
                logger.error { "Error getting user list: $ex" }
                emptyList()
            }
        }

    override suspend fun save(users: List<UserDomain>) =
        withContext(Dispatchers.IO) {
            val bulkOps = users.map { user ->
                val entity = user.toEntity()
                ReplaceOneModel(
                    eq(ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }

    override suspend fun addMentions(id: String) =
        withContext(Dispatchers.IO) {
            collection.findOneAndUpdate(
                eq(ID, id),
                combine(
                    inc(User::replies.name, 1),
                    set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                ),
                FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
            )?.id
        }
    override suspend fun addMentionsByName(name: String) =
        withContext(Dispatchers.IO) {
            collection.findOneAndUpdate(
                eq(User::name.name, name),
                combine(
                    inc(User::replies.name, 1),
                    set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                ),
                FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
            )?.id
        }
    override suspend fun updateUser(user: UserDomain) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, user.id),
                combine(
                    set(User::name.name, user.name),
                    set(User::fullName.name, user.fullName),
                    set(User::email.name, user.email),
                    set(User::about.name, user.about),
                    set(User::location.name, user.location),
                    set(User::birthday.name, user.birthday),
                    set(User::image.name, user.image.toEntity()),
                    set(User::settings.name, user.settings.toEntity()),
                    set(User::birthday.name, user.birthday),
                    set(User::tipLinks.name, user.tipLinks),
                    set(User::socialLinks.name, user.socialLinks),
                    set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }
    override suspend fun setSettings(id: String, settings: UserSettingsDomain) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, id),
                combine(
                    set(User::settings.name, settings.toEntity()),
                    set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }

    override suspend fun markNotifsRead(
        id: String
    ) = withContext(Dispatchers.IO) {
        collection.updateOne(
            eq(ID, id),
            combine(
                set(User::replies.name, 0),
                set(User::chats.name, 0),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        )
    }.modifiedCount

    override suspend fun markReportsRead(
        id: String
    ) = withContext(Dispatchers.IO) {
        collection.updateOne(
            eq(ID, id),
            combine(
                set(User::reports.name, 0),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        )
    }.modifiedCount

    override suspend fun addReport() =
        withContext(Dispatchers.IO) {
            collection.updateMany(
                `in`(User::roles.name, listOf(UserRole.ADMIN, UserRole.MOD)),
                combine(
                    inc(User::reports.name, 1),
                    set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }

    override suspend fun setFollowers(
        id: String,
        count: Int
    ) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, id),
                combine(
                    inc(User::followers.name, count),
                    set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }

    override suspend fun setFollowing(
        id: String,
        count: Int
    ) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, id),
                combine(
                    inc(User::following.name, count),
                    set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }

    override suspend fun addFavoriteRoute(
        id: String,
        routeId: String,
        routeFavorite: UserFavoriteDomain
    ) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, id),
                combine(
                    set("${User::routeFavorites.name}.$routeId", routeFavorite.toEntity()),
                    set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }

    override suspend fun removeFavoriteRoute(
        id: String,
        routeId: String
    ) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, id),
                combine(
                    Document("\$unset", Document("routeFavorites.$routeId", "")),
                    set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }
    override suspend fun setScore(
        id: String,
        score: Int
    ) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, id),
                combine(
                    inc(User::score.name, score),
                    set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }

    override suspend fun addChat(
        id: String
    ) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, id),
                combine(
                    inc(User::chats.name, 1),
                    set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }

    override suspend fun setUserTag(
        tag: Tag
    ) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, tag.userId),
                combine(
                    set(User::tag.name, tag.tag),
                    set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }

    override suspend fun setUserBanned(
        ban: Ban
    ) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, ban.userId),
                combine(
                    set(User::banned.name, true),
                    set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }

    override suspend fun syncUserAuth(authId: AuthIdDomain): Long  =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, authId.userId),
                combine(
                    addToSet(User::authIds.name, authId.toEntity()),
                    set(User::roles.name, UserRole.USER),
                    set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }

    override suspend fun setUserRole(roleUpdate: RoleUpdate): Long  =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, roleUpdate.userId),
                combine(
                    set(User::roles.name, roleUpdate.userRole),
                    set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }
}
