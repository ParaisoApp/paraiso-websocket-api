package com.paraiso.database.userchats

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.not
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.follows.Follow
import com.paraiso.domain.userchats.DirectMessage
import com.paraiso.domain.userchats.DirectMessagesDB
import com.paraiso.domain.users.User
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.votes.Vote
import com.paraiso.domain.votes.VotesDB
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import java.util.Date

class DirectMessagesDBImpl(database: MongoDatabase) : DirectMessagesDB {

    private val collection = database.getCollection("directMessages", DirectMessage::class.java)

    override suspend fun findByIdIn(ids: List<String>) =
        if(ids.size == 1){
            collection.find(
                and(
                    eq(ID, ids.firstOrNull()),
                )
            ).toList()
        }else{
            collection.find(
                and(
                    `in`(ID, ids)
                )
            ).toList()
        }
    override suspend fun findByChatId(chatId: String) =
        collection.find(
            and(
                eq(DirectMessage::chatId.name, chatId),
            )
        ).toList()

    override suspend fun save(dms: List<DirectMessage>): Int {
        val bulkOps = dms.map { dm ->
            ReplaceOneModel(
                eq(ID, dm.id),
                dm,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }
}
