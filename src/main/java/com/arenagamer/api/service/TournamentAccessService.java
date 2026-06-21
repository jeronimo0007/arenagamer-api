package com.arenagamer.api.service;

import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Tournament;
import com.arenagamer.api.entity.enums.UserRole;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.TournamentManagerPermissionRepository;
import com.arenagamer.api.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TournamentAccessService {

    private final TournamentManagerPermissionRepository managerPermissionRepository;
    private final IdentityService identityService;

    public boolean canManage(Tournament tournament, AuthenticatedUser auth) {
        if (auth.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (isDirectOwner(tournament, auth)) {
            return true;
        }
        if (auth.isContact() && tournament.getClient() != null) {
            return hasContactClientAccess(tournament, auth);
        }
        return false;
    }

    public void validateCanManage(Tournament tournament, AuthenticatedUser auth) {
        if (!canManage(tournament, auth)) {
            throw BusinessException.forbidden("Sem permissão para esta ação");
        }
    }

    public void validateCanGrantManager(Tournament tournament, AuthenticatedUser auth) {
        validateCanManage(tournament, auth);
        if (tournament.getClient() == null) {
            throw BusinessException.badRequest("Torneio não vinculado a um cliente");
        }
        if (auth.isStaff() && auth.getRole() == UserRole.ADMIN) {
            return;
        }
        if (!auth.isContact()) {
            throw BusinessException.forbidden("Apenas o contato principal pode gerenciar permissões");
        }
        Contact contact = identityService.requireContact(auth);
        if (!isPrimaryContact(contact)) {
            throw BusinessException.forbidden("Apenas o contato principal pode conceder ou revogar permissões");
        }
    }

    public boolean isDirectOwner(Tournament tournament, AuthenticatedUser auth) {
        return tournament.getOwnerType() == auth.getType()
                && tournament.getOwnerId().equals(auth.getId());
    }

    public boolean isPrimaryContact(Contact contact) {
        return contact.getIsPrimary() != null && contact.getIsPrimary() == 1;
    }

    private boolean hasContactClientAccess(Tournament tournament, AuthenticatedUser auth) {
        Contact contact = identityService.requireContact(auth);
        if (!contact.getUserid().equals(tournament.getClient().getUserId())) {
            return false;
        }
        if (isPrimaryContact(contact)) {
            return true;
        }
        return managerPermissionRepository.existsByTournamentIdAndContactId(tournament.getId(), contact.getId());
    }
}
