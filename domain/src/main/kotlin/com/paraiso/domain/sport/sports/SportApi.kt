package com.paraiso.domain.sport.sports

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.LeaderResponse
import com.paraiso.domain.sport.data.RosterResponse
import com.paraiso.domain.sport.data.ScoreboardResponse
import com.paraiso.domain.sport.data.StandingsResponse
import com.paraiso.domain.sport.data.toDomain
import com.paraiso.domain.sport.data.toResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

class SportApi(private val sportDBs: SportDBs) {
    companion object {
        // index off one week for conversion and modification in DB layer
        const val MAX_WEEK_REGULAR_SEASON = 19
        const val MAX_WEEK_PRE_SEASON = 4
        const val MAX_WEEK_POST_SEASON = 5
        const val WEEK_ONE = 0
        const val PRE_SEASON = 1
        const val POST_SEASON = 3
        const val UNKNOWN = 0
    }
    suspend fun findLeague(sport: String) = sportDBs.leaguesDB.findBySport(sport)?.toResponse()
    suspend fun findTeamByAbbr(sport: String, teamAbbr: String) = sportDBs.teamsDB.findBySportAndAbbr(sport, teamAbbr)?.toResponse()
    suspend fun findTeams(sport: String) = sportDBs.teamsDB.findBySport(sport).map { it.toResponse() }.associateBy { it.id }
    suspend fun findTeamById(sport: String, id: String) = sportDBs.teamsDB.findBySportAndTeamId(sport, id)?.toResponse()
    suspend fun findTeamsByIds(ids: Set<String>) = sportDBs.teamsDB.findByIds(ids).map { it.toResponse() }
    suspend fun findCompetitionById(id: String) = sportDBs.competitionsDB.findById(id)?.toResponse()
    suspend fun findCompetitionsByIds(ids: Set<String>) = sportDBs.competitionsDB.findByIdIn(ids).map { it.toResponse() }
    suspend fun findBoxScoresById(id: String) = sportDBs.boxscoresDB.findById(id)?.toResponse()
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
        sportDBs.rostersDB.findBySportAndTeamId(sport, teamId)?.let { rosterEntity ->
            val athletes = async {
                sportDBs.athletesDB.findByIdsIn(rosterEntity.athletes)
            }
            val coach = async {
                rosterEntity.coach?.let { sportDBs.coachesDB.findById(it) }
            }
            RosterResponse(
                id = rosterEntity.id,
                athletes = athletes.await().map { it.toResponse() },
                coach = coach.await()?.toResponse(),
                teamId = rosterEntity.teamId
            )
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
        )?.let { schedule ->
            val competitions = sportDBs.competitionsDB.findByIdIn(schedule.events.toSet())
                .sortedBy { it.date }
            schedule.toDomain(competitions)
        }?.toResponse()

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
                        1 -> Triple(year - 1, POST_SEASON, MAX_WEEK_POST_SEASON)
                        2 -> Triple(year, seasonType - 1, MAX_WEEK_PRE_SEASON)
                        3 -> Triple(year, seasonType - 1, MAX_WEEK_REGULAR_SEASON)
                        else -> Triple(UNKNOWN, UNKNOWN, UNKNOWN)
                    }
                } else {
                    if (seasonType == 3) {
                        Triple(year + 1, PRE_SEASON, WEEK_ONE)
                    } else {
                        Triple(year, seasonType + 1, WEEK_ONE)
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
            ScoreboardResponse(
                id = "$sport-${compRef?.season?.type}-${compRef?.season?.year}-$resolvedModifier",
                season = compRef?.season,
                week = compRef?.week,
                day = day?.atStartOfDayIn(estZone),
                competitions = resolvedComps.map { it.toResponse() }
            )
        }

    suspend fun findPlayoff(
        sport: String,
        year: Int
    ) =
        sportDBs.playoffsDB.findBySportAndYear(
            sport,
            year
        )?.toResponse()
}
