package com.example.plugins

import com.example.messageTypes.Ban
import com.example.messageTypes.Delete
import com.example.messageTypes.DirectMessage
import com.example.messageTypes.Login
import com.example.messageTypes.Message
import com.example.messageTypes.MessageType
import com.example.messageTypes.Route
import com.example.messageTypes.SiteRoute
import com.example.messageTypes.TypeMapping
import com.example.messageTypes.User
import com.example.messageTypes.UserInfo
import com.example.messageTypes.UserRole
import com.example.messageTypes.Vote
import com.example.messageTypes.randomGuestName
import com.example.messageTypes.sports.FullTeam
import com.example.messageTypes.sports.Scoreboard
import com.example.testRestClient.util.ApiConfig
import com.example.util.findCorrectConversion
import com.example.util.sendTypedMessage
import io.klogging.Klogging
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.converter
import io.ktor.websocket.close
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

class WebSocketHandler(private val sportHandler: SportHandler, private val apiConfig: ApiConfig) : Klogging {

    private val userList: MutableMap<String, User> = mutableMapOf()
    private val banList: MutableSet<String> = mutableSetOf()

    private val messageFlowMut = MutableSharedFlow<Message>(replay = 0)
    private val voteFlowMut = MutableSharedFlow<Vote>(replay = 0)
    private val deleteFlowMut = MutableSharedFlow<Delete>(replay = 0)
    private val basicFlowMut = MutableSharedFlow<String>(replay = 0)
    private val userLoginFlowMut = MutableSharedFlow<User>(replay = 0)
    private val userLeaveFlowMut = MutableSharedFlow<String>(replay = 0)
    private val banUserFlowMut = MutableSharedFlow<Ban>(replay = 0)

    private val flowList = listOf( // convert to immutable for send to client
        Pair(MessageType.MSG, messageFlowMut.asSharedFlow()),
        Pair(MessageType.VOTE, voteFlowMut.asSharedFlow()),
        Pair(MessageType.DELETE, deleteFlowMut.asSharedFlow()),
        Pair(MessageType.BASIC, basicFlowMut.asSharedFlow()),
        Pair(MessageType.USER_LOGIN, userLoginFlowMut.asSharedFlow()),
        Pair(MessageType.USER_LEAVE, userLeaveFlowMut.asSharedFlow()),
        Pair(MessageType.BAN, banUserFlowMut.asSharedFlow())
    )

    suspend fun handleGuest(session: WebSocketServerSession) {
        UUID.randomUUID().toString().let { id ->
            val currentUser = User(
                id = id,
                name = randomGuestName(),
                banned = false,
                roles = UserRole.GUEST,
                websocket = session
            )
            userList[id] = currentUser
            session.joinChat(currentUser)
        }
    }

    private suspend fun handleRoute(route: Route, session: WebSocketServerSession): List<Job> = coroutineScope {
        when(route.route){
            SiteRoute.HOME -> homeJobs(session)
            SiteRoute.PROFILE -> profileJobs(route.content, session)
            SiteRoute.SPORT -> sportJobs(session)
            SiteRoute.TEAM -> teamJobs(route.content, session)
        }
    }

    private suspend fun homeJobs(session: WebSocketServerSession) = coroutineScope {
        listOf(launch{})
    }
    private suspend fun profileJobs(content: String, session: WebSocketServerSession) = coroutineScope {
        listOf(launch{})
    }
    private suspend fun teamJobs(content: String, session: WebSocketServerSession) = coroutineScope {
        listOf(
            launch {
                var lastSentScoreboard: Scoreboard? = null
                while (isActive) {
                    val currentScoreboard = sportHandler.scoreboard
                    currentScoreboard?.let {sb ->
                        val filteredSb = currentScoreboard.copy(
                            competitions = sb.competitions.filter { comp -> comp.teams.map { it.team.id }.contains(content) }
                        )
                        if (lastSentScoreboard != filteredSb) {
                            session.sendTypedMessage(MessageType.SCOREBOARD, filteredSb)
                            lastSentScoreboard = filteredSb
                        }
                        delay(5 * 1000)
                    } ?: run { delay(5000L) }
                }
            },
            launch {
                while (isActive) {
                    if (sportHandler.teams.isNotEmpty()) {
                        session.sendTypedMessage(MessageType.TEAMS, sportHandler.teams)
                        delay(500000L)
                    } else {
                        delay(5000L)
                    }
                }
            },
            launch {
                while (isActive) {
                    val currentBoxScores = sportHandler.boxScores
                    val currentScoreboard = sportHandler.scoreboard

                    if (currentBoxScores.isNotEmpty() && currentScoreboard != null) {
                        currentScoreboard.competitions.firstOrNull { comp ->
                            comp.teams.map { it.team.id }.contains(content)
                        }?.teams?.map { it.team.id }
                            ?.let{teamIds ->
                            currentBoxScores.filter { boxScore -> teamIds.contains(boxScore.teamId) }
                                .let { filteredBoxScores ->
                                    session.sendTypedMessage(MessageType.BOX_SCORES, filteredBoxScores)
                                }
                        }
                        delay(500000L)
                    } else {
                        delay(5000L)
                    }
                }
            }
        )
    }

    private suspend fun sportJobs(session: WebSocketServerSession) = coroutineScope {
        listOf(
            launch {
                var lastSentScoreboard: Scoreboard? = null
                while (isActive) {
                    if (sportHandler.scoreboard != null && lastSentScoreboard != sportHandler.scoreboard) {
                        session.sendTypedMessage(MessageType.SCOREBOARD, sportHandler.scoreboard)
                        lastSentScoreboard = sportHandler.scoreboard
                    }
                    delay(5 * 1000)
                }
            },
            launch {
                while (isActive) {
                    if (sportHandler.teams.isNotEmpty()) {
                        session.sendTypedMessage(MessageType.TEAMS, sportHandler.teams)
                        delay(500000L)
                    } else {
                        delay(5000L)
                    }
                }
            },
            launch {
                while (isActive) {
                    sportHandler.standings?.let {
                        session.sendTypedMessage(MessageType.STANDINGS, it)
                        delay(5000000L)
                    } ?: run { delay(5000L) }
                }
            },
            launch {
                var lastSentBoxScores = listOf<FullTeam>()
                while (isActive) {
                    if (sportHandler.boxScores.isNotEmpty() && lastSentBoxScores != sportHandler.boxScores) {
                        session.sendTypedMessage(MessageType.BOX_SCORES, sportHandler.boxScores)
                        lastSentBoxScores = sportHandler.boxScores
                    }
                    delay(5 * 1000)
                }
            }
//            launch {
//                sportHandler.boxScoreFlow.collect { message ->
//                    sendTypedMessage(MessageType.BOX_SCORES, message)
//                }
//            }
        )
    }

    private suspend fun WebSocketServerSession.joinChat(user: User) {
        var sessionUser = user.copy()
        sendTypedMessage(MessageType.USER, sessionUser.copy(websocket = null))
        sendTypedMessage(MessageType.USER_LIST, userList.values.map { it.copy(websocket = null) })

        val messageCollectionJobs = flowList.map { (type, sharedFlow) ->
            launch {
                sharedFlow.collect { message ->
                    when (type) {
                        MessageType.MSG -> sendTypedMessage(type, message as Message)
                        MessageType.VOTE -> sendTypedMessage(type, message as Vote)
                        MessageType.DELETE -> sendTypedMessage(type, message as Delete)
                        MessageType.BASIC -> sendTypedMessage(type, message as String)
                        MessageType.USER_LOGIN -> sendTypedMessage(type, message as User)
                        MessageType.USER_LEAVE -> sendTypedMessage(type, message as String)
                        MessageType.BAN -> {
                            val ban = message as? Ban
                            ban?.let{bannedMsg ->
                                if(sessionUser.id == bannedMsg.userId){
                                    sessionUser = sessionUser.copy(banned = true)
                                }
                            }
                        }
                        else -> logger.error { "Found unknown type when sending typed message from flow $sharedFlow" }
                    }
                }
            }
        }

        userLoginFlowMut.emit(sessionUser.copy(websocket = null))
        //holds the active jobs for given route
        var activeJobs: Job? = null
        try {
            incoming.consumeEach { frame ->
                val messageWithType = converter?.findCorrectConversion<TypeMapping<String>>(frame)
                    ?.typeMapping?.entries?.first()
                when (messageWithType?.key) {
                    MessageType.MSG -> {
                        val message = Json.decodeFromString<Message>(messageWithType.value).copy(
                            id = UUID.randomUUID().toString(),
                            userId = sessionUser.id
                        )
                        if(sessionUser.banned){
                            sendTypedMessage(MessageType.MSG, message)
                        }else{
                            messageFlowMut.emit(message)
                        }
                    }
                    MessageType.DM -> {
                        val message = Json.decodeFromString<DirectMessage>(messageWithType.value).copy(
                            id = UUID.randomUUID().toString(),
                            userId = sessionUser.id
                        )
                        launch { sendTypedMessage(MessageType.DM, message) }
                        if(!sessionUser.banned){
                            userList[message.userReceiveId]?.websocket?.sendTypedMessage(MessageType.DM, message)
                        }
                    }
                    MessageType.VOTE -> {
                        val vote = Json.decodeFromString<Vote>(messageWithType.value).copy(userId = sessionUser.id)
                        if(sessionUser.banned){
                            sendTypedMessage(MessageType.VOTE, vote)
                        }else{
                            voteFlowMut.emit(vote)
                        }
                    }
                    MessageType.DELETE -> {
                        val delete = Json.decodeFromString<Delete>(messageWithType.value).copy(userId = sessionUser.id)
                        if(sessionUser.banned){
                            sendTypedMessage(MessageType.DELETE, delete)
                        }else{
                            deleteFlowMut.emit(delete)
                        }
                    }
                    MessageType.LOGIN -> {
                        val login = Json.decodeFromString<Login>(messageWithType.value)
                        if(login.password == apiConfig.admin){
                            sessionUser.copy(
                                roles = UserRole.ADMIN,
                                name = "Breeze"
                            ).let{ admin ->
                                sessionUser = admin
                                userList[sessionUser.id] = admin
                                admin.copy(websocket = null).let{adminRef ->
                                    sendTypedMessage(MessageType.USER, adminRef)
                                    userLoginFlowMut.emit(adminRef)
                                }
                            }
                        }
                    }
                    MessageType.BAN -> {
                        val ban = Json.decodeFromString<Ban>(messageWithType.value)
                        if(sessionUser.roles == UserRole.ADMIN){
                            banUserFlowMut.emit(ban)
                            banList.add(ban.userId)
                        }
                    }
                    MessageType.ROUTE -> {
                        val route = Json.decodeFromString<Route>(messageWithType.value)
                        activeJobs?.cancelAndJoin()
                        val session = this
                        activeJobs = launch {
                            handleRoute(route, session)
                        }
                    }
                    else -> logger.error { "Invalid message type received $messageWithType" }
                }
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Error parsing incoming data" }
        } finally {
            messageCollectionJobs.forEach { it.cancelAndJoin() }
            activeJobs?.cancelAndJoin()
            userLeaveFlowMut.emit(sessionUser.id)
            userList.remove(sessionUser.id)
            sessionUser.websocket?.close()
        }
    }
}
