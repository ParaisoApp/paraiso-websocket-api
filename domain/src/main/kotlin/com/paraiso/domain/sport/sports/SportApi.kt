package com.paraiso.domain.sport.sports

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.LeaderResponse
import com.paraiso.domain.sport.data.Roster
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.sport.data.StandingsResponse
import com.paraiso.domain.sport.data.toResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

class SportApi(private val sportDBs: SportDBs) {
    companion object {
        // index off one week for conversion and modification in DB layer
        const val MAX_WEEK_PRE_SEASON = 4
        const val MAX_WEEK_REGULAR_SEASON = 19
        const val MAX_WEEK_POST_SEASON = 5
        const val WEEK_ONE = 0
        const val PRE_SEASON = 1
        const val REG_SEASON = 2
        const val POST_SEASON = 3
        const val PLAY_IN = 5
        const val UNKNOWN = 0
    }
    suspend fun findLeague(sport: String) = sportDBs.leaguesDB.findBySport(sport)
    suspend fun findTeamByAbbr(sport: String, teamAbbr: String) = sportDBs.teamsDB.findBySportAndAbbr(sport, teamAbbr)
    suspend fun findTeams(sport: String) = sportDBs.teamsDB.findBySport(sport).associateBy { it.id }
    suspend fun findTeamById(sport: String, id: String) = sportDBs.teamsDB.findBySportAndTeamId(sport, id)
    suspend fun findTeamsByIds(ids: Set<String>) = sportDBs.teamsDB.findByIds(ids)
    suspend fun findCompetitionById(id: String) = sportDBs.competitionsDB.findById(id)
    suspend fun findCompetitionsByIds(ids: Set<String>) = sportDBs.competitionsDB.findByIdIn(ids)
    suspend fun findBoxScoresById(id: String) = sportDBs.boxscoresDB.findById(id)
    suspend fun findStandings(sport: String): Map<String, List<StandingsResponse>>? = coroutineScope {
        val teamsRes = async { sportDBs.teamsDB.findBySport(sport).associateBy { it.teamId } }
        if (sport == SiteRoute.BASKETBALL.name) {
            sportDBs.standingsDB.findById(sport)?.standingsGroups?.associate { standingsGroup ->
                standingsGroup.confAbbr.uppercase() to standingsGroup.standings.map { standings ->
                    val team = teamsRes.await()[standings.teamId]
                    standings.toResponse(team, standingsGroup.confName, standingsGroup.confAbbr, null, null)
                }.sortedBy { it.seed }
            }
        } else {
            sportDBs.standingsDB.findById(sport)?.standingsGroups?.let { standingsGroups ->
                standingsGroups.associate { confGroup ->
                    val allStandings = confGroup.subGroups.flatMap { standingsSubGroup ->
                        standingsSubGroup.standings.map { standings ->
                            val team = teamsRes.await()[standings.teamId]
                            standings.toResponse(
                                team,
                                confGroup.confName,
                                confGroup.confAbbr,
                                standingsSubGroup.divName,
                                standingsSubGroup.divAbbr
                            )
                        }.sortedBy { it.seed }
                    }
                    confGroup.confAbbr.uppercase() to allStandings
                }
            }
        }
    }
    suspend fun findLeaders(sport: String, season: Int, type: Int) =
        sportDBs.leadersDB.findBySportAndSeasonAndType(sport, season, type)?.categories?.let { categories ->
            // grab athletes from DB and associate with their id
            val athletes = categories.flatMap { category -> category.leaders.map { it.athleteId } }.let { athleteIds ->
                sportDBs.athletesDB.findByIdsIn(athleteIds.map { it.toString() })
            }.associateBy { it.id }
            // associate each category with athlete name and stats
            categories.associate {
                it.displayName to it.leaders.mapNotNull { leader ->
                    athletes[leader.athleteId.toString()]?.let { athlete ->
                        LeaderResponse(
                            id = leader.athleteId,
                            athleteName = athlete.shortName,
                            leaderStat = leader.value,
                            teamAbbr = athlete.teamAbbr
                        )
                    }
                }
            }
        }
    suspend fun findTeamLeaders(sport: String, teamId: String, season: Int, type: Int) =
        sportDBs.leadersDB.findBySportAndSeasonAndTypeAndTeam(
            sport,
            teamId,
            season,
            type
        )?.categories?.let { categories ->
            // grab athletes from DB and associate with their id
            val athletes = categories.flatMap { category -> category.leaders.map { it.athleteId } }.let { athleteIds ->
                sportDBs.athletesDB.findByIdsIn(athleteIds.map { it.toString() })
            }.associateBy { it.id }
            // associate each category with athlete name and stats
            categories.associate {
                it.displayName to it.leaders.mapNotNull { leader ->
                    athletes[leader.athleteId.toString()]?.let { athlete ->
                        LeaderResponse(
                            id = leader.athleteId,
                            athleteName = athlete.shortName,
                            leaderStat = leader.value,
                            teamAbbr = athlete.teamAbbr
                        )
                    }
                }
            }
        }

    suspend fun findTeamRoster(sport: String, teamId: String) = coroutineScope {
        sportDBs.rostersDB.findBySportAndTeamId(sport, teamId).let { (roster, athleteIds, coachId) ->
            val athletes = async {
                sportDBs.athletesDB.findByIdsIn(athleteIds)
            }
            val coach = async {
                coachId?.let { sportDBs.coachesDB.findById(it) }
            }
            roster?.let {
                Roster(
                    id = it.id,
                    sport = it.sport,
                    athletes = athletes.await(),
                    coach = coach.await(),
                    teamId = it.teamId
                )
            }
        }
    }

    suspend fun findTeamSchedule(
        sport: String,
        teamId: String,
        seasonYear: Int,
        seasonType: Int
    ) =
        sportDBs.schedulesDB.findBySportAndTeamIdAndYearAndType(
            sport,
            teamId,
            seasonYear,
            seasonType
        )?.let { (schedule, eventIds) ->
            val competitions = sportDBs.competitionsDB.findByIdIn(eventIds.toSet())
                .sortedBy { it.date }
            schedule?.copy(events = competitions)
        }

    suspend fun findScoreboard(
        sport: String,
        year: Int,
        seasonType: Int,
        modifier: String,
        past: Boolean
    ) =
        sportDBs.competitionsDB.findScoreboard(
            sport,
            year,
            seasonType,
            modifier,
            past
        ).let { comps ->
            // if comps is empty, try diff type or year
            val resolvedComps = comps.ifEmpty {
                val (nextYear, nextType, nextWeek) = if (past) {
                    when (seasonType) {
                        PRE_SEASON -> Triple(year - 1, POST_SEASON, MAX_WEEK_POST_SEASON)
                        REG_SEASON -> Triple(year, PRE_SEASON, MAX_WEEK_PRE_SEASON)
                        PLAY_IN -> Triple(year, REG_SEASON, MAX_WEEK_REGULAR_SEASON)
                        POST_SEASON -> {
                            if(sport == SiteRoute.BASKETBALL.name){
                                //handle play in season type (5)
                                Triple(year, PLAY_IN, 0) // max week ignored for non-football
                            }else{
                                Triple(year, REG_SEASON, MAX_WEEK_REGULAR_SEASON)
                            }
                        }
                        else -> Triple(UNKNOWN, UNKNOWN, UNKNOWN)
                    }
                } else {
                    when (seasonType) {
                        PRE_SEASON -> Triple(year, REG_SEASON, WEEK_ONE)
                        REG_SEASON -> {
                            if(sport == SiteRoute.BASKETBALL.name) {
                                Triple(year, PLAY_IN, WEEK_ONE)
                            } else {
                                Triple(year, POST_SEASON, WEEK_ONE)
                            }
                        }
                        PLAY_IN -> Triple(year, POST_SEASON, WEEK_ONE)
                        POST_SEASON -> {
                            Triple(year + 1, PRE_SEASON, WEEK_ONE)
                        }
                        else -> Triple(UNKNOWN, UNKNOWN, UNKNOWN)
                    }
                }
                val resolvedModifier = nextWeek.toString()
                    .takeIf { sport == SiteRoute.FOOTBALL.name } ?: modifier
                sportDBs.competitionsDB.findScoreboard(
                    sport,
                    nextYear,
                    nextType,
                    resolvedModifier,
                    past
                )
            }
            val compRef = resolvedComps.firstOrNull()
            val estZone = TimeZone.of("America/New_York")
            val day = compRef?.date?.toLocalDateTime(estZone)?.date
            val resolvedModifier = compRef?.week.toString()
                .takeIf { sport == SiteRoute.FOOTBALL.name } ?: day
            Scoreboard(
                id = "$sport-${compRef?.season?.type}-${compRef?.season?.year}-$resolvedModifier",
                sport = sport,
                season = compRef?.season,
                week = compRef?.week,
                day = day?.atStartOfDayIn(estZone),
                competitions = resolvedComps
            )
        }

    suspend fun findPlayoff(
        sport: String,
        year: Int
    ) =
        sportDBs.playoffsDB.findBySportAndYear(
            sport,
            year
        )
}
