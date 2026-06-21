package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.ContactWalletPermissionRequest;
import com.arenagamer.api.dto.response.ContactWalletPermissionResponse;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletAccessService {

    private final ContactRepository contactRepository;

    public boolean isPrimary(Contact contact) {
        return contact.getIsPrimary() != null && contact.getIsPrimary() == 1;
    }

    public boolean canViewWallet(Contact contact) {
        if (isPrimary(contact)) {
            return true;
        }
        return Boolean.TRUE.equals(contact.getWalletViewAllowed());
    }

    public boolean canUseWallet(Contact contact) {
        if (isPrimary(contact)) {
            return true;
        }
        return Boolean.TRUE.equals(contact.getWalletUseAllowed());
    }

    public void requireViewWallet(Contact contact) {
        if (!canViewWallet(contact)) {
            throw BusinessException.forbidden("Sem permissão para visualizar créditos");
        }
    }

    public void requireUseWallet(Contact contact) {
        if (!canUseWallet(contact)) {
            throw BusinessException.forbidden("Sem permissão para usar créditos");
        }
    }

    public void requirePrimary(Contact contact) {
        if (!isPrimary(contact)) {
            throw BusinessException.forbidden("Apenas o contato principal pode gerenciar permissões de créditos");
        }
    }

    public List<ContactWalletPermissionResponse> listSecondaryPermissions(Contact requester) {
        requirePrimary(requester);
        return contactRepository.findByUserid(requester.getUserid()).stream()
                .filter(contact -> !isPrimary(contact))
                .map(ContactWalletPermissionResponse::from)
                .toList();
    }

    @Transactional
    public ContactWalletPermissionResponse updateSecondaryPermissions(
            Contact requester, Integer targetContactId, ContactWalletPermissionRequest request) {
        requirePrimary(requester);

        Contact target = contactRepository.findById(targetContactId)
                .orElseThrow(() -> BusinessException.notFound("Contato não encontrado"));

        if (!requester.getUserid().equals(target.getUserid())) {
            throw BusinessException.forbidden("Contato não pertence à sua empresa");
        }

        if (isPrimary(target)) {
            throw BusinessException.badRequest("Permissões do contato principal não podem ser alteradas");
        }

        target.setWalletViewAllowed(Boolean.TRUE.equals(request.getWalletViewAllowed())
                || Boolean.TRUE.equals(request.getWalletUseAllowed()));
        target.setWalletUseAllowed(Boolean.TRUE.equals(request.getWalletUseAllowed()));

        Contact saved = contactRepository.save(target);

        return ContactWalletPermissionResponse.from(saved);
    }
}
