package com.paraiso.domain.sport.sports

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.LeaderResponse
import com.paraiso.domain.sport.data.RosterResponse
import com.paraiso.domain.sport.data.toDomain
import com.paraiso.domain.sport.data.toResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Instant

class SportApi(private val sportDBs: SportDBs) {
    suspend fun getLeague(sport: String) = sportDBs.leaguesDBAdapter.findBySport(sport)?.toResponse()
    suspend fun getTeamByAbbr(sport: String, teamAbbr: String) = sportDBs.teamsDBAdapter.findBySportAndAbbr(sport, teamAbbr)?.toResponse()
    suspend fun getTeams(sport: String) = sportDBs.teamsDBAdapter.findBySport(sport).map { it.toResponse() }.associateBy { it.id }
    suspend fun getStandings(sport: String) =
        if(sport == SiteRoute.BASKETBALL.name) {
            sportDBs.standingsDBAdapter.findById(sport)?.standingsGroups?.associate { standingsGroup ->
                standingsGroup.confAbbr to standingsGroup.standings.map { it.toResponse() }
            }
        }else{
            sportDBs.standingsDBAdapter.findById(sport)?.standingsGroups?.flatMap { confGroup ->
                confGroup.subGroups
            }?.associate { standingsSubGroup ->
                standingsSubGroup.divName to standingsSubGroup.standings.map { it.toResponse() }
            }
        }
    suspend fun getLeaders(sport: String, season: String, type: String) =
        sportDBs.leadersDBAdapter.findBySportAndSeasonAndType(sport, season, type)?.categories?.let { categories ->
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

    suspend fun getLeaderCategories(sport: String) = sportDBs.leadersDBAdapter.findBySport(sport)?.categories?.map { it.displayName }
    suspend fun getTeamRoster(sport: String, teamId: String) = coroutineScope {
        sportDBs.rostersDBAdapter.findBySportAndTeamId(sport, teamId)?.let { rosterEntity ->
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

    suspend fun getTeamSchedule(
        sport: String,
        teamId: String,
        seasonYear: Int,
        seasonType: Int
    ) =
        sportDBs.schedulesDBAdapter.findBySportAndTeamIdAndYearAndType(
            sport,
            teamId,
            seasonYear,
            seasonType
        )?.let { schedule ->
            val competitions = sportDBs.competitionsDBAdapter.findByIdIn(schedule.events)
                .sortedBy { Instant.parse(it.date) }
            schedule.toDomain(competitions)
        }?.toResponse()
}
