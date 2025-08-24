package com.paraiso.domain.sport.sports.bball

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.adapters.AthletesDBAdapter
import com.paraiso.domain.sport.adapters.BoxscoresDBAdapter
import com.paraiso.domain.sport.adapters.CoachesDBAdapter
import com.paraiso.domain.sport.adapters.CompetitionsDBAdapter
import com.paraiso.domain.sport.adapters.LeadersDBAdapter
import com.paraiso.domain.sport.adapters.RostersDBAdapter
import com.paraiso.domain.sport.adapters.SchedulesDBAdapter
import com.paraiso.domain.sport.adapters.ScoreboardsDBAdapter
import com.paraiso.domain.sport.adapters.StandingsDBAdapter
import com.paraiso.domain.sport.adapters.TeamsDBAdapter
import com.paraiso.domain.sport.data.LeaderResponse
import com.paraiso.domain.sport.data.RosterResponse
import com.paraiso.domain.sport.data.ScoreboardResponse
import com.paraiso.domain.sport.data.toDomain
import com.paraiso.domain.sport.data.toResponse
import com.paraiso.domain.sport.sports.SportDBs
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Instant

class BBallApi(private val sportDBs: SportDBs) {
    suspend fun getLeague() = sportDBs.leaguesDBAdapter.findBySport(SiteRoute.BASKETBALL)?.toResponse()
    suspend fun getTeamByAbbr(teamAbbr: String) = sportDBs.teamsDBAdapter.findById("${SiteRoute.BASKETBALL}-$teamAbbr")?.toResponse()
    suspend fun getTeams() = sportDBs.teamsDBAdapter.findBySport(SiteRoute.BASKETBALL).map { it.toResponse() }.associateBy { it.id }
    suspend fun getStandings() = sportDBs.standingsDBAdapter.findById(SiteRoute.BASKETBALL.toString())?.standingsGroups?.associate { standingsGroup ->
        standingsGroup.confAbbr to standingsGroup.standings.map { it.toResponse() }
    }
    suspend fun getLeaders(season: String, type: String) =
        sportDBs.leadersDBAdapter.findBySportAndSeasonAndType(SiteRoute.BASKETBALL, season, type)?.categories?.let { categories ->
        // grab athletes from DB and associate with their id
        val athletes = categories.flatMap { category -> category.leaders.map { it.athleteId } }.let { athleteIds ->
            sportDBs.athletesDBAdapter.findByIdsIn(athleteIds.map { it.toString() })
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

    suspend fun getLeaderCategories() = sportDBs.leadersDBAdapter.findBySport(SiteRoute.BASKETBALL)?.categories?.map { it.displayName }
    suspend fun getTeamRoster(teamId: String) = coroutineScope {
        sportDBs.rostersDBAdapter.findById("${SiteRoute.BASKETBALL}-$teamId")?.let { rosterEntity ->
            val athletes = async {
                sportDBs.athletesDBAdapter.findByIdsIn(rosterEntity.athletes)
            }
            val coach = async {
                rosterEntity.coach?.let { sportDBs.coachesDBAdapter.findById(it) }
            }
            RosterResponse(
                id = rosterEntity.id,
                athletes = athletes.await().map { it.toResponse() },
                coach = coach.await()?.toResponse(),
                teamId = rosterEntity.teamId
            )
        }
    }

    suspend fun getScoreboard() = sportDBs.scoreboardsDBAdapter.findById(SiteRoute.BASKETBALL.name)?.let { sb ->
        ScoreboardResponse(
            competitions = sportDBs.competitionsDBAdapter.findByIdIn(sb.competitions).map { it.toResponse() }
        )
    }
    suspend fun getBoxscores(ids: List<String>) = sportDBs.boxscoresDBAdapter.findByIdsIn(ids)

    suspend fun getTeamSchedule(teamId: String, seasonYear: String, seasonType: String) =
        sportDBs.schedulesDBAdapter.findById("${SiteRoute.BASKETBALL}-$teamId-$seasonYear-$seasonType")?.let { schedule ->
            val competitions = sportDBs.competitionsDBAdapter.findByIdIn(schedule.events)
                .sortedBy { Instant.parse(it.date) }
            schedule.toDomain(competitions)
        }?.toResponse()
}
