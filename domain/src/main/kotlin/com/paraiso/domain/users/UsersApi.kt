package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.Block
import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Follow
import com.paraiso.domain.posts.PostsApi
import com.paraiso.domain.util.Constants.UNKNOWN
import com.paraiso.domain.util.ServerState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

class UsersApi {

    companion object {
        const val PARTIAL_RETRIEVE_LIM = 5
    }
    fun getUserById(userId: String) =
        ServerState.userList[userId]?.buildUserResponse()

    fun getUserByName(userName: String): UserResponse? =
        ServerState.userList.values.find { it.name == userName }?.buildUserResponse()


    fun saveUser(user: UserResponse) {
        ServerState.userList[user.id] = user.copy(
            updatedOn = Clock.System.now()
        ).toUser()
    }

    fun updateBlockList(sessionUserId: String, block: Block) {
        ServerState.userList[sessionUserId]?.let { sessionUser ->
            sessionUser.blockList.toMutableSet().let { mutableBlockList ->
                mutableBlockList.add(block.userId)
                ServerState.userList[sessionUser.id] =
                    sessionUser.copy(blockList = mutableBlockList)
            }
        }
    }

    fun getUserByPartial(search: String) =
        ServerState.userList.values
            .filter { it.name.lowercase().contains(search.lowercase()) }
            .take(PARTIAL_RETRIEVE_LIM)
            .map { it.buildUserResponse() }

    fun getUserList(filters: FilterTypes, userId: String) =
        ServerState.userList[userId]?.following?.let { followingList ->
            ServerState.userList.values
                .filter { user ->
                    user.status != UserStatus.DISCONNECTED &&
                        (
                            filters.userRoles.contains(user.roles) ||
                                (filters.userRoles.contains(UserRole.FOLLOWING) && followingList.contains(user.id))
                            )
                }.associate { user -> user.id to user.buildUserResponse() }
        }

    fun setSettings(userId: String, settings: UserSettings) =
        ServerState.userList[userId]?.let { user ->
            ServerState.userList[userId] = user.copy(
                settings = settings,
                updatedOn = Clock.System.now()
            )
        }

    fun getFollowingById(userId: String) =
        ServerState.userList.values.filter {
            it.following.contains(userId)
        }.map { it.buildUserResponse() }

    fun getFollowersById(userId: String) =
        ServerState.userList.values.filter {
            it.followers.contains(userId)
        }.map { it.buildUserResponse() }

    fun getOrPutUserChat(chatId: String, userId: String, otherUserId: String) =
        ServerState.userChatList[chatId]?.toReturn() ?: run {
            Clock.System.now().let { now ->
                val user = ServerState.userList[userId]
                val otherUser = ServerState.userList[otherUserId]
                if(user != null && otherUser != null){
                    UUID.randomUUID().toString().let { newChatId ->
                        val newUserChat = UserChat(
                            id = newChatId,
                            users = setOf(user, otherUser),
                            dms = emptySet(),
                            createdOn = now,
                            updatedOn = now
                        )
                        ServerState.userChatList[newChatId] = newUserChat
                        newUserChat.toReturn()
                    }
                }else{
                    null
                }
            }
        }

    private fun updateChatForUser(
        dm: DirectMessage,
        userId: String,
        otherUserId: String,
        isUser: Boolean,
        now: Instant
    ) =
        ServerState.userList[userId]?.let{ user ->
            user.chats.toMutableMap().let { mutableChat ->
                mutableChat[otherUserId] = ChatRef(
                    mostRecentDm = dm,
                    chatId = dm.chatId,
                    viewed = !isUser
                )
                ServerState.userList[userId] = user.copy(
                    chats = mutableChat,
                    updatedOn = now
                )
            }
        }

    suspend fun putDM(dm: DirectMessage) = coroutineScope {
        Clock.System.now().let { now ->
            launch{ updateChatForUser(dm, dm.userId, dm.userReceiveId, true, now) } // update chat for receiving user
            launch { updateChatForUser(dm, dm.userReceiveId, dm.userId, false, now) } // update chat for receiving user
            ServerState.userChatList[dm.chatId]?.let { chat ->
                ServerState.userChatList[dm.chatId] = chat.copy(
                    dms = chat.dms + dm.copy(
                        createdOn = now
                    ),
                    updatedOn = now
                )
            }
        }
    }

    suspend fun markNotifsRead(userId: String, userNotifs: UserNotifs) = coroutineScope {
        ServerState.userList[userId]?.let{user ->
            //grab chats and make mutable
            val userChats = async {
                user.chats.filter { userNotifs.userChatIds.contains(it.key) }
                    .toMutableMap().let { mutableChats ->
                        //find chat and set to viewed
                        mutableChats.map {
                            mutableChats[it.key] = it.value.copy(
                                viewed = true
                            )
                        }
                        mutableChats
                    }
            }
            //grab replies and make mutable
            val replies =  async {
                user.replies.filter { userNotifs.replyIds.contains(it.key) }
                    .toMutableMap().let { mutableReplies ->
                        //find reply and set to viewed
                        mutableReplies.map {
                            mutableReplies[it.key] = true
                        }
                        mutableReplies
                    }
            }
            //update user
            ServerState.userList[userId] = user.copy(
                chats = userChats.await(),
                replies = replies.await(),
                updatedOn = Clock.System.now()
            )
        }
    }

    suspend fun markReportNotifsRead(userId: String, userReportNotifs: UserReportNotifs) = coroutineScope {
        ServerState.userList[userId]?.let{ user ->
            //grab user reports and make mutable
            val userReports = async {
                user.userReports.filter { userReportNotifs.userIds.contains(it.key) }
                    .toMutableMap().let { mutableUserReports ->
                        //find user report and set to viewed
                        mutableUserReports.map {
                            mutableUserReports[it.key] = true
                        }
                        mutableUserReports
                    }
            }
            //grab post reports and make mutable
            val postReports = async {
                user.postReports.filter { userReportNotifs.postIds.contains(it.key) }
                    .toMutableMap().let { mutablePostReports ->
                        //find post report and set to viewed
                        mutablePostReports.map {
                            mutablePostReports[it.key] = true
                        }
                        mutablePostReports
                    }
            }

            //update user
            ServerState.userList[userId] = user.copy(
                userReports = userReports.await(),
                postReports = postReports.await(),
                updatedOn = Clock.System.now()
            )
        }
    }

    suspend fun follow(follow: Follow) = coroutineScope {
        val now = Clock.System.now()
        // add follower to followers list of followee user
        launch {
            ServerState.userList[follow.followeeId]?.let { followee ->
                followee.followers.toMutableSet().apply {
                    if (followee.followers.contains(follow.followerId)) {
                        remove(follow.followerId)
                    } else {
                        add(follow.followerId)
                    }
                }.let { updatedFollowers ->
                    ServerState.userList[followee.id] = followee.copy(
                        followers = updatedFollowers,
                        updatedOn = now
                    )
                }
            }
        }
        // add followee to following list of follower user
        ServerState.userList[follow.followerId]?.let { follower ->
            follower.following.toMutableSet().apply {
                if (follower.following.contains(follow.followeeId)) {
                    remove(follow.followeeId)
                } else {
                    add(follow.followeeId)
                }
            }.let { updatedFollowing ->
                ServerState.userList[follower.id] = follower.copy(
                    following = updatedFollowing,
                    updatedOn = now
                )
            }
        }
    }
}
