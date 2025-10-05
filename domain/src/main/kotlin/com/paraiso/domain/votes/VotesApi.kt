package com.paraiso.domain.votes

class VotesApi(
    private val votesDB: VotesDB
) {

    suspend fun vote(vote: VoteResponse) =
        votesDB.findByVoterIdAndPostId(vote.voterId, vote.postId)?.let { voteRef ->
            if (voteRef.upvote == vote.upvote) {
                // remove user's vote on post
                votesDB.delete(vote.voterId, vote.postId)
                // score is reversed as vote is removed
                if (vote.upvote) -1 else 1
            } else {
                // flip existing vote
                votesDB.setVote(vote.voterId, vote.postId, vote.upvote)
                // score is doubled as vote is flipped
                if (vote.upvote) 2 else -2
            }
        } ?: run {
            // create new vote
            votesDB.save(listOf(vote.toDomain()))
            // score is standard as vote is init
            if (vote.upvote) 1 else -1
        }

    suspend fun get(voterId: String, postId: String) =
        votesDB.findByVoterIdAndPostId(voterId, postId)
    suspend fun getByUserIdAndPostIdIn(userId: String, postIds: Set<String>) =
        votesDB.findByUserIdAndPostIdIn(userId, postIds).associateBy { it.postId }
}
