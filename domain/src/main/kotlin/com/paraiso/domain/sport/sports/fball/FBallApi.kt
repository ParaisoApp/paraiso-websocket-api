package com.paraiso.domain.sport.sports.fball

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

class FBallApi(private val sportDBs: SportDBs) {
    suspend fun getTeamByAbbr(teamAbbr: String) = sportDBs.teamsDBAdapter.findById("${SiteRoute.FOOTBALL}-$teamAbbr")?.toResponse()
    suspend fun getTeams() = sportDBs.teamsDBAdapter.findBySport(SiteRoute.FOOTBALL).map { it.toResponse() }.associateBy { it.id }
    suspend fun getStandings() = sportDBs.standingsDBAdapter.findById(SiteRoute.FOOTBALL.toString())?.standingsGroups?.flatMap { confGroup ->
        confGroup.subGroups
    }?.associate { standingsSubGroup ->
        standingsSubGroup.divName to standingsSubGroup.standings.map { it.toResponse() }
    }
    suspend fun getLeaders() = sportDBs.leadersDBAdapter.findBySport(SiteRoute.FOOTBALL)?.categories?.let { categories ->
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

    suspend fun getLeaderCategories() = sportDBs.leadersDBAdapter.findBySport(SiteRoute.FOOTBALL)?.categories?.map { it.displayName }
    suspend fun getTeamRoster(teamId: String) = coroutineScope {
        sportDBs.rostersDBAdapter.findById("${SiteRoute.FOOTBALL}-$teamId")?.let { rosterEntity ->
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

    suspend fun getScoreboard() = sportDBs.scoreboardsDBAdapter.findById(SiteRoute.FOOTBALL.name)?.let { sb ->
        ScoreboardResponse(
            competitions = sportDBs.competitionsDBAdapter.findByIdIn(sb.competitions).map { it.toResponse() }
        )
    }

    suspend fun getBoxscores(ids: List<String>) = sportDBs.boxscoresDBAdapter.findByIdsIn(ids)

    suspend fun getTeamSchedule(teamId: String, seasonYear: String, seasonType: String) =
        sportDBs.schedulesDBAdapter.findById("${SiteRoute.FOOTBALL}-$teamId-$seasonYear-$seasonType")?.let { schedule ->
            val competitions = sportDBs.competitionsDBAdapter.findByIdIn(schedule.events)
            schedule.toDomain(competitions)
        }?.toResponse()
}
