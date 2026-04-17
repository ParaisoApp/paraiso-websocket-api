package com.paraiso.domain.sport.sports

import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostStatus
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.posts.PostsDB
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.RoutesApi
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.BoxScore
import com.paraiso.domain.sport.data.Competition
import com.paraiso.domain.sport.data.Playoff
import com.paraiso.domain.sport.data.PlayoffMatchUp
import com.paraiso.domain.sport.data.PlayoffRound
import com.paraiso.domain.sport.data.PlayoffTeam
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.sport.data.Team
import com.paraiso.domain.sport.data.toBasic
import com.paraiso.domain.users.EventService
import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.Constants.GAME_PREFIX
import com.paraiso.domain.util.Constants.SPORT_PREFIX
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
import kotlinx.datetime.Instant
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
    companion object {
        const val POST_SEASON = 3
        const val PLAY_IN = 5
    }

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
                sportDBs.teamsDB.save(teamsRes, sport)
            }
        }
    }

    private suspend fun addTeamRoutes(sport: SiteRoute, teams: List<Team>) {
        teams.map {
            val now = Clock.System.now()
            RouteDetails(
                id = "${SPORT_PREFIX}/${sport.name.lowercase()}/t/${it.abbreviation}",
                route = sport,
                modifier = it.abbreviation,
                title = it.displayName,
                userFavorites = 0,
                about = null,
                pinnedPostIds = emptyList(),
                pinnedPosts = emptyMap(),
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
                // season type 4 is off season
                val leaders = if((league.activeSeasonType.toIntOrNull() ?: 4) <= 3){
                    sportClient.getLeaders(
                        sport,
                        league.activeSeasonYear,
                        league.activeSeasonType
                    )
                } else null
                leaders?.let { leadersRes ->
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
                        // season type 4 is off season
                        if((league.activeSeasonType.toIntOrNull() ?: 4) <= 3){
                            sportClient.getTeamLeaders(
                                sport,
                                league.activeSeasonYear,
                                league.activeSeasonType,
                                team.teamId
                            )
                        } else null
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
                    sportDBs.schedulesDB.save(schedulesRes)
                    sportDBs.competitionsDB.saveIfNew(schedulesRes.flatMap { it.events })
                    if (autoBuildPosts || manual) {
                        addPosts(sport, schedulesRes.flatMap { it.events }, manual)
                    }
                }
            }
        }
    }

    private suspend fun addPosts(
        sport: SiteRoute,
        competitions: List<Competition>,
        manual: Boolean
    ) {
        competitions.map { competition ->
            Post(
                id = "$GAME_PREFIX${competition.id}",
                userId = SYSTEM,
                title = competition.shortName,
                content = "${competition.date}||${competition.shortName}",
                type = PostType.EVENT,
                parentId = "/${sport.name}",
                rootId = "$GAME_PREFIX${competition.id}",
                data = sport.name,
                route = "$SPORT_PREFIX/${sport.name.lowercase()}",
                count = 0,
                score = 0,
                media = null,
                userVote = null,
                status = PostStatus.ACTIVE,
                createdOn = competition.date,
                updatedOn = competition.date
            )
        }.let { gamePosts ->
            if(manual || (autoBuild && autoBuildPosts)){
                postsDB.save(gamePosts)
            }else{
                postsDB.saveIfNew(gamePosts)
            }
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
                    sportDBs.rostersDB.save(rostersRes)
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
                    val lastScoreboardEntity = lastSentScoreboard[sport]?.toBasic()
                    if (scoreboard.toBasic() != lastScoreboardEntity) {
                        saveScoreboardAndGetBoxScores(
                            sport,
                            scoreboard,
                            scoreboard.competitions,
                            enableBoxScore = true,
                            initScoreboard = lastScoreboardEntity == null
                        )
                    } else {
                        delayBoxScore = determineActiveComps(sport, scoreboard, delayBoxScore)
                    }
                }
            }
        }
    }

    // returns next delayBoxScore state (incremented on every sb update)
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
            } else {
                // deep comparison for changes
                if (scoreboard != lastSentScoreboard[sport]) {
                    saveScoreboardAndGetBoxScores(
                        sport,
                        scoreboard,
                        activeComps,
                        // send boxscores every 60 seconds or when last comps are ending
                        delayBoxScore == 0 || activeComps.size == endingCompetitions.size,
                        initScoreboard = false
                    )
                    if ((scoreboard.season?.type == POST_SEASON || scoreboard.season?.type == PLAY_IN) && endingCompetitions.isNotEmpty()) {
                        savePlayoffResults(
                            endingCompetitions,
                            sport,
                            scoreboard.season.year,
                            scoreboard.season?.type
                        )
                    }
                }
                // retrieve scoreboard every ten seconds
                delay(10.seconds)
                // delay boxScore fetch for 6 ticks of delay (every 1 minute)
                if (delayBoxScore == 6) {
                    0
                } else {
                    delayBoxScore + 1
                }
            }
        } else {
            // else if current time is before the earliest time, delay until the earliest time
            if (earliestTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds() > 0) {
                delay(earliestTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds())
            }
            delayBoxScore
        }
    }

    private suspend fun saveScoreboardAndGetBoxScores(
        sport: SiteRoute,
        scoreboard: Scoreboard,
        activeCompetitions: List<Competition>,
        enableBoxScore: Boolean,
        initScoreboard: Boolean
    ) = coroutineScope {
        if (activeCompetitions.isNotEmpty()) {
            if (initScoreboard) {
                // don't overwrite existing records if new scoreboard (prevents deleting comp end time on startup)
                sportDBs.competitionsDB.findByIdIn(activeCompetitions.map { it.id }.toSet())
                    .associate { it.id to it.status.completedTime }.let { existingCompletedTimes ->
                        activeCompetitions.map {
                            it.copy(
                                status = it.status.copy(
                                    completedTime = existingCompletedTimes[it.id]
                                )
                            )
                        }.let { updatedComps ->
                            sportDBs.competitionsDB.save(updatedComps)
                        }
                    }
            } else {
                sportDBs.competitionsDB.save(activeCompetitions)
            }
            eventService.publish(
                MessageType.COMPS.name,
                "$sport:${Json.encodeToString(activeCompetitions)}"
            )
            if (enableBoxScore) {
                buildBoxScores(
                    sport,
                    activeCompetitions.map { it.id }
                )
            }
        }
        // send scoreboard last as it will trigger refresh of comp consumers
        val basicScoreboard = scoreboard.toBasic()
        if (basicScoreboard != lastSentScoreboard[sport]?.toBasic()) {
            sportDBs.scoreboardsDB.save(listOf(scoreboard))
            eventService.publish(
                MessageType.SCOREBOARD.name,
                "$sport:${Json.encodeToString(basicScoreboard)}"
            )
            addPosts(sport, activeCompetitions, false)
        }
        lastSentScoreboard[sport] = scoreboard
    }

    private suspend fun buildBoxScores(
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

    private suspend fun savePlayoffResults(
        comps: List<Competition>,
        sport: SiteRoute,
        year: Int,
        type: Int?
    ) {
        val playoff = sportDBs.playoffsDB.findBySportAndYear(sport.name, year)
            ?: Playoff("$sport-$year", sport, year, emptyMap())
        val round = playoff.rounds.values.maxByOrNull { it.round }
        val firstCompTeamIds = comps.first().teams.map { team -> team.teamId }
        // If one team is recognized from the current round but the pairing is different, it's a new round.
        val isNewRound = round?.matchUps?.values?.any { matchUp ->
            val existingIds = matchUp.teams.values.map { it.id }
            firstCompTeamIds.any { it in existingIds } && !firstCompTeamIds.all { it in existingIds }
        } ?: false
        val targetRound = if (isNewRound || round == null) {
            PlayoffRound(round = (round?.round ?: 0) + 1, winners = emptyList(), matchUps = emptyMap())
        } else {
            round
        }
        // merge existing matchUps with new or updated matchUps
        val mergeMatchUps = targetRound.matchUps.toMutableMap()
        comps.forEach { comp ->
            val matchUpId = comp.teams.map { team -> team.teamId }.sorted().joinToString("-")
            // grab existing matchUp to get existing series score if needed
            val matchUp = mergeMatchUps[matchUpId] ?: PlayoffMatchUp(
                id = matchUpId,
                teams = emptyMap()
            )
            // map comp teams to results (score and winner)
            val teams = comp.teams.map { team ->
                val (score, winner) = when (sport) {
                    SiteRoute.FOOTBALL -> (team.score?.toIntOrNull() ?: 0) to team.winner
                    else -> {
                        val currentScore = matchUp.teams.values.find { it.id == team.teamId }?.score ?: 0
                        val updatedScore = if (team.winner) currentScore + 1 else currentScore
                        val winner = if(sport == SiteRoute.BASKETBALL && type == PLAY_IN){
                            updatedScore == 1
                        }else{
                            updatedScore == 4
                        }
                        updatedScore to winner
                    }
                }
                PlayoffTeam(team.teamId, score, winner)
            }
            // copy updated teams into matchUp
            mergeMatchUps[matchUpId] = matchUp.copy(teams = teams.associateBy { it.id })
        }
        val finalRounds = playoff.rounds.values.filter { it.round != targetRound.round } +
            targetRound.copy(matchUps = mergeMatchUps)
        // remove old round if it exists and add updated round
        val updatedPlayoff = playoff.copy(
            rounds = finalRounds.sortedBy { it.round }.associateBy { it.round }
        )
        sportDBs.playoffsDB.save(listOf(updatedPlayoff))
    }

    suspend fun fillPlayoffs(
        sport: SiteRoute,
        year: Int
    ) =
        sportDBs.competitionsDB.findPlayoffsByYear(sport.name, year).let { comps ->
            val playoff = sportDBs.playoffsDB.findBySportAndYear(sport.name, year)
                ?: Playoff("$sport-$year", sport, year, emptyMap())

            // Sort by date to ensure chronological processing
            val sortedComps = comps.sortedBy { it.date }
            val rounds = mutableListOf<PlayoffRound>()
            var currentRound = PlayoffRound(round = 1, winners = emptyList(), matchUps = emptyMap())

            sortedComps.forEach { comp ->
                val teamIds = comp.teams.map { it.teamId }
                val matchUpId = teamIds.sorted().joinToString("-")

                // Determine if we need a new round based on this specific competition
                val isProgression = currentRound.matchUps.values.any { matchUp ->
                    val existingIds = matchUp.teams.values.map { it.id }
                    teamIds.any { it in existingIds } && !teamIds.all { it in existingIds }
                }

                if (isProgression) {
                    rounds.add(currentRound)
                    currentRound = PlayoffRound(round = currentRound.round + 1, winners = emptyList(), matchUps = emptyMap())
                }

                // Update the MatchUp within the Target Round
                val matchUpMap = currentRound.matchUps.toMutableMap()
                val existingMatchUp = matchUpMap[matchUpId] ?: PlayoffMatchUp(id = matchUpId, teams = emptyMap())

                val updatedTeams = comp.teams.map { team ->
                    val (score, winner) = when (sport) {
                        SiteRoute.FOOTBALL -> (team.score?.toIntOrNull() ?: 0) to team.winner
                        else -> {
                            val currentScore = existingMatchUp.teams.values.find { it.id == team.teamId }?.score ?: 0
                            val updatedScore = if (team.winner) currentScore + 1 else currentScore
                            val winner = if(sport == SiteRoute.BASKETBALL && comp.season?.type == PLAY_IN){
                                updatedScore == 1
                            }else{
                                updatedScore == 4
                            }
                            updatedScore to winner
                        }
                    }
                    PlayoffTeam(team.teamId, score, winner)
                }
                matchUpMap[matchUpId] = existingMatchUp.copy(teams = updatedTeams.associateBy { it.id })
                currentRound = currentRound.copy(matchUps = matchUpMap)
            }
            sportDBs.playoffsDB.save(listOf(playoff.copy(rounds = (rounds + currentRound).associateBy { it.round })))
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
