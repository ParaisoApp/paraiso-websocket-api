package com.paraiso.domain.sport.sports

import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.posts.PostsDB
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.RoutesApi
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.BoxScore
import com.paraiso.domain.sport.data.Competition
import com.paraiso.domain.sport.data.Schedule
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.sport.data.Team
import com.paraiso.domain.sport.data.toEntity
import com.paraiso.domain.users.EventService
import com.paraiso.domain.util.Constants.GAME_PREFIX
import com.paraiso.domain.util.Constants.SYSTEM
import com.paraiso.domain.util.ServerConfig.autoBuild
import com.paraiso.domain.util.ServerConfig.autoBuildPosts
import io.klogging.Klogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class SportHandler(
    private val sportClient: SportClient,
    private val routesApi: RoutesApi,
    private val sportDBs: SportDBs,
    private val eventService: EventService,
    private val postsDB: PostsDB
) : Klogging {
    private var lastSentScoreboard = ConcurrentHashMap<SiteRoute, Scoreboard>()
    private var lastSentBoxScores = listOf<BoxScore>()

    suspend fun bootJobs(sport: SiteRoute) = coroutineScope {
        launch { buildLeague(sport, manual = false) }
        launch { buildScoreboard(sport) }
        launch { buildStandings(sport) }
        launch { buildTeams(sport, manual = false) }
        launch { buildLeaders(sport) }
        launch { buildTeamLeaders(sport) }
        launch { buildRosters(sport, manual = false) }
        launch { buildSchedules(sport, manual = false) }
    }

    suspend fun buildLeague(sport: SiteRoute, manual: Boolean) = coroutineScope {
        if (autoBuild || manual) {
            sportClient.getLeague(sport)?.let { leagueRes ->
                sportDBs.leaguesDB.save(listOf(leagueRes))
            }
        }
    }

    private suspend fun buildStandings(sport: SiteRoute) = coroutineScope {
        while (isActive) {
            sportClient.getStandings(sport)?.let { standingsRes ->
                sportDBs.standingsDB.save(listOf(standingsRes))
            }
            delay(1.hours)
        }
    }

    suspend fun buildTeams(sport: SiteRoute, manual: Boolean) = coroutineScope {
        if (autoBuild || manual) {
            sportClient.getTeams(sport).let { teamsRes ->
                launch {
                    addTeamRoutes(sport, teamsRes)
                }
                sportDBs.teamsDB.save(teamsRes)
            }
        }
    }

    private suspend fun addTeamRoutes(sport: SiteRoute, teams: List<Team>) {
        teams.map {
            val now = Clock.System.now()
            RouteDetails(
                id = "/s/${sport.name.lowercase()}/t/${it.abbreviation}",
                route = sport,
                modifier = it.abbreviation,
                title = it.displayName,
                userFavorites = 0,
                about = null,
                createdOn = now,
                updatedOn = now
            )
        }.let {
            routesApi.saveRoutes(it)
        }
    }

    private suspend fun buildLeaders(sport: SiteRoute) = coroutineScope {
        while (isActive) {
            sportDBs.leaguesDB.findBySport(sport.name)?.let { league ->
                sportClient.getLeaders(
                    sport,
                    league.activeSeasonYear,
                    league.activeSeasonType
                )?.let { leadersRes ->
                    sportDBs.leadersDB.save(listOf(leadersRes))
                }
            }
            delay(1.hours)
        }
    }

    private suspend fun buildTeamLeaders(sport: SiteRoute) = coroutineScope {
        while (isActive) {
            sportDBs.leaguesDB.findBySport(sport.name)?.let { league ->
                sportDBs.teamsDB.findBySport(sport.name).map { team ->
                    async {
                        sportClient.getTeamLeaders(
                            sport,
                            league.activeSeasonYear,
                            league.activeSeasonType,
                            team.teamId
                        )
                    }
                }.awaitAll().filterNotNull().let { leadersRes ->
                    if (leadersRes.isNotEmpty()) {
                        sportDBs.leadersDB.save(leadersRes)
                    }
                }
            }
            delay(1.hours)
        }
    }

    suspend fun buildSchedules(sport: SiteRoute, manual: Boolean) = coroutineScope {
        if (autoBuild || manual) {
            sportDBs.teamsDB.findBySport(sport.name).map { team ->
                async {
                    sportClient.getSchedule(sport, team.teamId)
                }
            }.awaitAll().filterNotNull().let { schedulesRes ->
                if (schedulesRes.isNotEmpty()) {
                    sportDBs.schedulesDB.save(schedulesRes.map { it.toEntity() })
                    sportDBs.competitionsDB.saveIfNew(schedulesRes.flatMap { it.events })
                    if (autoBuildPosts) {
                        addTeamPosts(sport, schedulesRes)
                    }
                }
            }
        }
    }

    private suspend fun addTeamPosts(
        sport: SiteRoute,
        schedules: List<Schedule>
    ) {
        // add posts for team sport route - separate for focused discussion
        schedules.map { it.events }.flatMap {
            it.map { competition ->
                Post(
                    id = "$GAME_PREFIX${competition.id}",
                    userId = SYSTEM,
                    title = competition.shortName,
                    content = "${competition.date}||${competition.shortName}",
                    type = PostType.EVENT,
                    parentId = "/${sport.name}",
                    rootId = "$GAME_PREFIX${competition.id}",
                    data = sport.name,
                    route = "/s/${sport.name.lowercase()}",
                    createdOn = competition.date,
                    updatedOn = competition.date
                )
            }
        }.let { combinedGamePosts ->
            postsDB.save(combinedGamePosts)
        }
    }

    suspend fun buildRosters(sport: SiteRoute, manual: Boolean) = coroutineScope {
        if (autoBuild || manual) {
            sportDBs.teamsDB.findBySport(sport.name).map { it.teamId }.map { teamId ->
                async {
                    sportClient.getRoster(sport, teamId)
                }
            }.awaitAll().filterNotNull().let { rostersRes ->
                if (rostersRes.isNotEmpty()) {
                    sportDBs.rostersDB.save(rostersRes.map { it.toEntity() })
                    sportDBs.athletesDB.save(rostersRes.flatMap { it.athletes })
                    sportDBs.coachesDB.save(rostersRes.mapNotNull { it.coach })
                }
            }
        }
    }
    private suspend fun buildScoreboard(sport: SiteRoute) {
        coroutineScope {
            var delayBoxScore = 0
            while (isActive) {
                sportClient.getScoreboard(sport)?.let { scoreboard ->
                    // if completely new scoreboard save it and generate game posts
                    if (scoreboard.toEntity() != lastSentScoreboard[sport]?.toEntity()) {
                        saveScoreboardAndGetBoxScores(
                            sport,
                            scoreboard,
                            scoreboard.competitions,
                            true
                        )
                    } else {
                        delayBoxScore = determineActiveComps(sport, scoreboard, delayBoxScore)
                    }
                }
            }
        }
    }
    //returns next delayBoxScore state (incremented on every sb update)
    private suspend fun determineActiveComps(sport: SiteRoute, scoreboard: Scoreboard, delayBoxScore: Int) = coroutineScope {
        // rolling window for active comps
        val now = Clock.System.now()
        val sixHoursPast = now.minus(6.hours)
        val sixHoursFuture = now.plus(6.hours)
        // grab earliest game's start time and state of all games
        val earliestTime = scoreboard.competitions.filter {
            val gameTime = it.date ?: Instant.DISTANT_PAST
            gameTime in sixHoursPast..sixHoursFuture
        }.minOfOrNull { it.date ?: Instant.DISTANT_PAST } ?: Instant.DISTANT_PAST
        val lastCompCompletedStates = lastSentScoreboard[sport]?.competitions?.associate { it.id to it.status.completed } ?: emptyMap()
        // if some games are past the earliest start time update scoreboard and box scores
        if (now > earliestTime) {
            // ending comps - filter to last completed false and cur completed true, copy in the end time
            val endingCompetitions = scoreboard.competitions.filter {
                lastCompCompletedStates[it.id] == false && it.status.completed
            }.map { it.copy(status = it.status.copy(completedTime = now)) }
            // ensure ending posts are saved (take as active comps)
            val activeComps = scoreboard.competitions.filter { !it.status.completed } + endingCompetitions
            // delay an hour if all games ended - will trigger as long as scoreboard is still prev day
            if (activeComps.isEmpty()) {
                delay(1.hours)
                delayBoxScore
            }else{
                // deep comparison for changes
                if (scoreboard != lastSentScoreboard[sport]) {
                    saveScoreboardAndGetBoxScores(
                        sport,
                        scoreboard,
                        activeComps,
                        // send boxscores every 60 seconds or when last comps are ending
                        delayBoxScore == 0 || activeComps.size == endingCompetitions.size
                    )
                }
                // retrieve scoreboard every ten seconds
                delay(10.seconds)
                // delay boxscore fetch for 6 ticks of delay (every 1 minute)
                if (delayBoxScore == 6) {
                    0
                } else {
                    delayBoxScore + 1
                }
            }
        } else {
            // else if current time is before the earliest time, delay until the earliest time
            if(earliestTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds() > 0){
                delay(earliestTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds())
            }
            delayBoxScore
        }
    }

    private suspend fun saveScoreboardAndGetBoxScores(
        sport: SiteRoute,
        scoreboard: Scoreboard,
        activeCompetitions: List<Competition>,
        enableBoxScore: Boolean
    ) = coroutineScope {
        if (activeCompetitions.isNotEmpty()) {
            sportDBs.competitionsDB.save(activeCompetitions)
            eventService.publish(
                MessageType.COMPS.name,
                "$sport:${Json.encodeToString(activeCompetitions)}"
            )
            if (enableBoxScore) {
                buildBoxscores(
                    sport,
                    activeCompetitions.map { it.id }
                )
            }
        }
        // send scoreboard last as it will trigger refresh of comp consumers
        val scoreboardEntity = scoreboard.toEntity()
        if (scoreboardEntity != lastSentScoreboard[sport]?.toEntity()) {
            sportDBs.scoreboardsDB.save(listOf(scoreboardEntity))
            eventService.publish(
                MessageType.SCOREBOARD.name,
                "$sport:${Json.encodeToString(scoreboardEntity)}"
            )
        }
        lastSentScoreboard[sport] = scoreboard
    }

    private suspend fun buildBoxscores(
        sport: SiteRoute,
        competitionIds: List<String>
    ) = coroutineScope {
        competitionIds.map { gameId ->
            async {
                sportClient.getGameStats(sport, gameId)
            }
        }.awaitAll().filterNotNull().let { newBoxScores ->
            // map result to teams
            if (newBoxScores != lastSentBoxScores) {
                // only send in progress or just completed box scores
                sportDBs.boxscoresDB.save(newBoxScores)
                eventService.publish(
                    MessageType.BOX_SCORES.name,
                    "$sport:${Json.encodeToString(newBoxScores)}"
                )
                lastSentBoxScores = newBoxScores
            }
        }
    }

    suspend fun fillCompetitionData(
        sport: SiteRoute,
        dates: String
    ) {
        // add posts for base sport route
        sportClient.getScoreboardWithDate(sport, dates)?.competitions?.let { competitions ->
            sportDBs.competitionsDB.save(competitions)
            competitions.mapNotNull { competition ->
                sportClient.getGameStats(sport, competition.id)
            }.let { boxScores ->
                sportDBs.boxscoresDB.save(boxScores)
            }
        }
    }
}
