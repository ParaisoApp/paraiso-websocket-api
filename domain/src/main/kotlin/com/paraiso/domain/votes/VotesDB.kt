package com.paraiso.domain.votes

import com.paraiso.domain.messageTypes.Vote

interface VotesDB{
    suspend fun findByVoterIdAndPostId(voterId: String, postId: String): Vote?
    suspend fun save(votes: List<Vote>): Int
    suspend fun setVote(voterId: String, postId: String, vote: Boolean): Long
    suspend fun delete(voterId: String, postId: String): Long
}
