package com.paraiso.server.plugins.jobs

import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.sport.data.Competition
import com.paraiso.domain.sport.data.toResponse
import com.paraiso.domain.sport.sports.SportApi
import com.paraiso.domain.sport.sports.SportState
import com.paraiso.server.util.sendTypedMessage
import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class SportJobs(
    private val sportApi: SportApi
) {

    companion object {
        // delays emitting flow for at lest 5ms, accumulates and batches updates
        private const val FLOW_DELAY = 5000L
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    suspend fun sportJobs(
        session: WebSocketServerSession,
        sport: String
    ) = withContext(Dispatchers.IO) {
        // gather triggers and rebuild combined flow if one of the competitions ends
        val combinedCompsFlow = SportState.getAllTriggers()[sport]?.flatMapLatest { _ ->
            val compFlowList: List<StateFlow<Competition>> =
                SportState.getCompetitionsBySport(sport)
                    ?.values
                    ?.toList()
                    ?: emptyList()
            combine(compFlowList) { it.toList() }
        }
        listOf(
            launch {
                // emit scoreboard info (generally once daily)
                SportState.getScoreboardFlow(sport).collect { sb ->
                    session.sendTypedMessage(
                        MessageType.SCOREBOARD,
                        sb.toResponse(sportApi.findCompetitionsByIds(sb.competitions.toSet()))
                    )
                }
            },
            launch {
                combinedCompsFlow?.sample(FLOW_DELAY)?.collect { comps ->
                    session.sendTypedMessage(MessageType.COMPS, comps.map { it.toResponse() }.associateBy { it.id })
                }
            }
        )
    }

    suspend fun teamJobs(
        content: String?,
        session: WebSocketServerSession,
        sport: String
    ) = withContext(Dispatchers.IO) {
        val teamId = content?.split("-")?.lastOrNull() ?: ""
        SportState.getCompetitionsBySport(sport)?.values?.firstOrNull { comp ->
            comp.value.teams.map { it.teamId }.contains(teamId)
        }.let { compFlow ->
            listOf(
                launch {
                    compFlow?.collect { comp ->
                        session.sendTypedMessage(MessageType.COMPS, mapOf(comp.id to comp.toResponse()))
                        if (comp.status.completed) coroutineContext.cancel() // cancel collecting on game end
                    }
                }
            )
        }
    }
    suspend fun boxScoreJobs(
        ids: Set<String>,
        activeSubs: ConcurrentHashMap<String, Job>,
        session: WebSocketServerSession
    ) = withContext(Dispatchers.IO) {
        val boxScores = SportState.getAllBoxScores()
        ids.forEach { id ->
            activeSubs[id] = launch {
                boxScores[id]?.collect { boxScore ->
                    session.sendTypedMessage(MessageType.BOX_SCORES, boxScore.toResponse())
                    if (boxScore.completed == true) coroutineContext.cancel()
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    suspend fun competitionJobs(
        ids: Set<String>,
        session: WebSocketServerSession
    ) = withContext(Dispatchers.IO) {
        val combinedTriggers = combine(SportState.getAllTriggers().values) { it.toList() }
        val combinedCompsFlow = combinedTriggers.flatMapLatest { _ ->
            val compFlowList: List<StateFlow<Competition>> =
                SportState.getCompetitionsIn(ids)
            combine(compFlowList) { it.toList() }
        }
        launch {
            combinedCompsFlow.sample(FLOW_DELAY).collect { comps ->
                session.sendTypedMessage(MessageType.COMPS, comps.map { it.toResponse() }.associateBy { it.id })
            }
        }
    }
}
