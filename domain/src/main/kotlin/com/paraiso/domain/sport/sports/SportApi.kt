package com.paraiso.domain.sport.sports

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.LeaderResponse
import com.paraiso.domain.sport.data.RosterResponse
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.sport.data.ScoreboardResponse
import com.paraiso.domain.sport.data.toDomain
import com.paraiso.domain.sport.data.toResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

class SportApi(private val sportDBs: SportDBs) {
    suspend fun findLeague(sport: String) = sportDBs.leaguesDB.findBySport(sport)?.toResponse()
    suspend fun findTeamByAbbr(sport: String, teamAbbr: String) = sportDBs.teamsDB.findBySportAndAbbr(sport, teamAbbr)?.toResponse()
    suspend fun findTeams(sport: String) = sportDBs.teamsDB.findBySport(sport).map { it.toResponse() }.associateBy { it.id }
    suspend fun findTeamById(sport: String, id: String) = sportDBs.teamsDB.findBySportAndTeamId(sport, id)?.toResponse()
    suspend fun findCompetitionById(id: String) = sportDBs.competitionsDB.findById(id)?.toResponse()
    suspend fun findBoxScoresById(id: String) = sportDBs.boxscoresDB.findById(id)?.toResponse()
    suspend fun findStandings(sport: String) =
        if (sport == SiteRoute.BASKETBALL.name) {
            sportDBs.standingsDB.findById(sport)?.standingsGroups?.associate { standingsGroup ->
                standingsGroup.confAbbr to standingsGroup.standings.map { it.toResponse() }
            }
        } else {
            sportDBs.standingsDB.findById(sport)?.standingsGroups?.let {standingsGroups ->
                standingsGroups.associate { confGroup ->
                    val allStandings = confGroup.subGroups.flatMap { standingsSubGroup ->
                        standingsSubGroup.standings.map { it.toResponse() }
                    }
                    confGroup.confAbbr to allStandings
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
            val competitions = sportDBs.competitionsDB.findByIdIn(schedule.events)
                .sortedBy { it.date }
            schedule.toDomain(competitions)
        }?.toResponse()

    suspend fun findScoreboard(
        sport: String,
        year: Int,
        type: Int,
        modifier: String,
        past: Boolean
    ) =
        sportDBs.competitionsDB.findScoreboard(
            sport, year, type, modifier, past
        ).let { comps ->
            val compRef = comps.firstOrNull()
            val estZone = TimeZone.of("America/New_York")
            ScoreboardResponse(
                id = "",
                season = compRef?.season,
                week = compRef?.week,
                day = compRef?.date?.toLocalDateTime(estZone)?.date?.atStartOfDayIn(estZone),
                competitions = comps.map { it.toResponse() }
            )
        }
}
