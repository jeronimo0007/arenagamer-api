package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.CreateTeamRequest;
import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Team;
import com.arenagamer.api.entity.TeamMember;
import com.arenagamer.api.entity.TeamSettings;
import com.arenagamer.api.entity.enums.AuthUserType;
import com.arenagamer.api.entity.enums.UserRole;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.dto.response.TeamJoinRequestResponse;
import com.arenagamer.api.repository.AvailabilityProfileRepository;
import com.arenagamer.api.repository.ClientRepository;
import com.arenagamer.api.repository.PresetRepository;
import com.arenagamer.api.repository.TeamAvailabilityChangeRequestRepository;
import com.arenagamer.api.repository.TeamJoinRequestRepository;
import com.arenagamer.api.repository.TeamMemberRepository;
import com.arenagamer.api.repository.TeamRankRepository;
import com.arenagamer.api.repository.TeamRepository;
import com.arenagamer.api.repository.TournamentParticipantRepository;
import com.arenagamer.api.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceClientLogicTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TeamRankRepository teamRankRepository;
    @Mock private PresetRepository presetRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private TournamentParticipantRepository tournamentParticipantRepository;
    @Mock private TeamJoinRequestRepository teamJoinRequestRepository;
    @Mock private TeamAvailabilityChangeRequestRepository teamAvailabilityChangeRequestRepository;
    @Mock private AvailabilityProfileRepository availabilityProfileRepository;
    @Mock private IdentityService identityService;
    @Mock private TeamSettingsService teamSettingsService;
    @Mock private TeamRankService teamRankService;
    @Mock private AvailabilityService availabilityService;
    @Mock private TeamJoinRequestService teamJoinRequestService;
    @Mock private TeamRosterService teamRosterService;
    @Mock private TournamentService tournamentService;

    @InjectMocks
    private TeamService teamService;

    private Client ownerClient;
    private Client memberClient;
    private Contact primaryOwnerContact;
    private Contact memberContact;
    private TeamSettings settings;

    @BeforeEach
    void setUp() {
        ownerClient = Client.builder().userId(10).company("Owner Co").build();
        memberClient = Client.builder().userId(20).company("Member Co").build();

        primaryOwnerContact = Contact.builder()
                .id(1)
                .userid(10)
                .firstname("Owner")
                .lastname("Primary")
                .email("owner@test.com")
                .isPrimary(1)
                .build();

        memberContact = Contact.builder()
                .id(2)
                .userid(20)
                .firstname("Member")
                .lastname("Primary")
                .email("member@test.com")
                .isPrimary(1)
                .build();

        settings = TeamSettings.builder()
                .id(TeamSettings.SINGLETON_ID)
                .maxOwnedTeamsPerClient(1)
                .maxParticipatedTeamsPerClient(3)
                .build();

        when(teamSettingsService.getSettings()).thenReturn(settings);
    }

    @Test
    void create_setsClientAsOwnerAndMember() {
        AuthenticatedUser auth = contactAuth(primaryOwnerContact);
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("Alpha");

        when(identityService.requireContact(auth)).thenReturn(primaryOwnerContact);
        when(clientRepository.findById(10)).thenReturn(Optional.of(ownerClient));
        when(teamRepository.countByClient_UserId(10)).thenReturn(0L);
        when(teamMemberRepository.countByClient_UserId(10)).thenReturn(0L);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            team.setId(100L);
            return team;
        });

        Team created = teamService.create(auth, request);

        assertThat(created.getClient().getUserId()).isEqualTo(10);

        ArgumentCaptor<TeamMember> memberCaptor = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberRepository).save(memberCaptor.capture());
        TeamMember savedMember = memberCaptor.getValue();
        assertThat(savedMember.getClient().getUserId()).isEqualTo(10);
        assertThat(savedMember.getIsCaptain()).isTrue();
    }

    @Test
    void create_rejectsWhenClientAlreadyOwnsTeam() {
        AuthenticatedUser auth = contactAuth(primaryOwnerContact);
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("Alpha");

        when(identityService.requireContact(auth)).thenReturn(primaryOwnerContact);
        when(clientRepository.findById(10)).thenReturn(Optional.of(ownerClient));
        when(teamRepository.countByClient_UserId(10)).thenReturn(1L);

        assertThatThrownBy(() -> teamService.create(auth, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("limite máximo de times");
    }

    @Test
    void addMemberClient_createsJoinRequestInsteadOfDirectMember() {
        AuthenticatedUser auth = contactAuth(primaryOwnerContact);
        TeamJoinRequestResponse expected = TeamJoinRequestResponse.builder()
                .id(1L)
                .teamId(100L)
                .invitedClientUserId(20)
                .build();

        when(teamJoinRequestService.inviteMember(100L, 20, auth)).thenReturn(expected);

        TeamJoinRequestResponse result = teamService.addMemberClient(100L, 20, auth);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getInvitedClientUserId()).isEqualTo(20);
        verify(teamJoinRequestService).inviteMember(100L, 20, auth);
        verify(teamMemberRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void delete_releasesTeamFromTournamentsBeforeRemoving() {
        AuthenticatedUser auth = contactAuth(primaryOwnerContact);
        Team team = team(100L, ownerClient);

        when(identityService.requireContact(auth)).thenReturn(primaryOwnerContact);
        when(teamRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(team));

        teamService.delete(100L, auth);

        verify(tournamentService).releaseTeamForDeletion(100L);
        verify(teamJoinRequestRepository).deleteByTeam_Id(100L);
        verify(teamAvailabilityChangeRequestRepository).deleteByTeam_Id(100L);
        verify(teamRepository).delete(team);
    }

    @Test
    void listMyTeams_usesMemberClientUserId() {
        AuthenticatedUser auth = contactAuth(memberContact);
        Team team = team(100L, ownerClient);

        when(identityService.requireContact(auth)).thenReturn(memberContact);
        when(teamRepository.findByMemberClientUserIdWithDetails(20)).thenReturn(List.of(team));
        when(teamMemberRepository.existsByTeamIdAndClient_UserId(100L, 20)).thenReturn(true);
        when(teamMemberRepository.countByTeam_Id(100L)).thenReturn(1L);

        var teams = teamService.listMyTeams(auth);

        assertThat(teams).hasSize(1);
        assertThat(teams.get(0).getClientUserId()).isEqualTo(10);
        verify(teamRepository).findByMemberClientUserIdWithDetails(20);
    }

    @Test
    void canManage_onlyPrimaryContactOfOwnerClient() {
        Team team = team(100L, ownerClient);
        Contact nonPrimary = Contact.builder()
                .id(3)
                .userid(10)
                .isPrimary(0)
                .build();

        assertThat(teamService.isPrimaryManagerOfTeam(primaryOwnerContact, team)).isTrue();
        assertThat(teamService.isPrimaryManagerOfTeam(nonPrimary, team)).isFalse();
        assertThat(teamService.isPrimaryManagerOfTeam(memberContact, team)).isFalse();
    }

    @Test
    void removeMemberClient_detachesFromLoadedTeamAndDeletesByClientUserId() {
        AuthenticatedUser auth = contactAuth(primaryOwnerContact);
        Team team = team(100L, ownerClient);
        TeamMember member = TeamMember.builder()
                .id(2L)
                .team(team)
                .client(memberClient)
                .build();
        team.getMembers().add(member);

        when(identityService.requireContact(auth)).thenReturn(primaryOwnerContact);
        when(teamRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsByTeamIdAndClient_UserId(100L, 20)).thenReturn(true);

        teamService.removeMemberClient(100L, 20, auth);

        verify(teamRosterService).handleMemberDeparture(team, 20, false, primaryOwnerContact);
        verify(teamJoinRequestService).onMemberLeftTeam(100L, 20);

        assertThat(team.getMembers()).isEmpty();
        verify(teamMemberRepository).deleteByTeamIdAndClient_UserId(100L, 20);
    }

    @Test
    void removeMemberClient_allowsMemberToLeaveOwnTeam() {
        AuthenticatedUser auth = contactAuth(memberContact);
        Team team = team(100L, ownerClient);
        TeamMember member = TeamMember.builder()
                .id(2L)
                .team(team)
                .client(memberClient)
                .build();
        team.getMembers().add(member);

        when(identityService.requireContact(auth)).thenReturn(memberContact);
        when(teamRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsByTeamIdAndClient_UserId(100L, 20)).thenReturn(true);

        teamService.removeMemberClient(100L, 20, auth);

        verify(teamRosterService).handleMemberDeparture(team, 20, true, memberContact);
        verify(teamJoinRequestService).onMemberLeftTeam(100L, 20);

        assertThat(team.getMembers()).isEmpty();
        verify(teamMemberRepository).deleteByTeamIdAndClient_UserId(100L, 20);
    }

    @Test
    void transferTeam_movesOwnershipToNewClient() {
        AuthenticatedUser auth = contactAuth(primaryOwnerContact);
        Client newOwner = Client.builder().userId(30).company("New Owner").build();
        Team team = team(100L, ownerClient);
        TeamMember oldCaptain = TeamMember.builder()
                .id(1L)
                .team(team)
                .client(memberClient)
                .isCaptain(true)
                .build();

        when(identityService.requireContact(auth)).thenReturn(primaryOwnerContact);
        when(teamRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(team));
        when(teamRepository.existsByClient_UserIdAndIdNot(30, 100L)).thenReturn(false);
        when(clientRepository.findById(30)).thenReturn(Optional.of(newOwner));
        when(teamMemberRepository.existsByTeamIdAndClient_UserId(100L, 30)).thenReturn(false);
        when(teamMemberRepository.countByClient_UserId(30)).thenReturn(0L);
        when(teamMemberRepository.findByTeamIdWithClient(100L)).thenReturn(List.of(oldCaptain));
        when(teamMemberRepository.findByTeamIdAndClient_UserId(100L, 30)).thenReturn(Optional.empty());

        teamService.transferTeam(100L, 30, auth);

        verify(teamMemberRepository).deleteByTeamIdAndClient_UserId(100L, 10);
        verify(teamRepository).save(team);
        assertThat(team.getClient().getUserId()).isEqualTo(30);
        assertThat(oldCaptain.getIsCaptain()).isFalse();

        ArgumentCaptor<TeamMember> memberCaptor = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberRepository, atLeast(2)).save(memberCaptor.capture());
        assertThat(memberCaptor.getAllValues()).anyMatch(member ->
                member.getClient().getUserId().equals(30) && Boolean.TRUE.equals(member.getIsCaptain()));
    }

    private static AuthenticatedUser contactAuth(Contact contact) {
        return AuthenticatedUser.builder()
                .id(contact.getId().longValue())
                .type(AuthUserType.CONTACT)
                .clientUserId(contact.getUserid())
                .email(contact.getEmail())
                .firstName(contact.getFirstname())
                .lastName(contact.getLastname())
                .role(UserRole.PLAYER)
                .isPrimary(contact.getIsPrimary() != null && contact.getIsPrimary() == 1)
                .build();
    }

    private static Team team(Long id, Client owner) {
        return Team.builder()
                .id(id)
                .name("Team " + id)
                .client(owner)
                .active(true)
                .build();
    }
}
