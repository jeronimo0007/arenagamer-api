package com.arenagamer.api.service;

import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Staff;
import com.arenagamer.api.entity.enums.AuthUserType;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.ContactRepository;
import com.arenagamer.api.repository.StaffRepository;
import com.arenagamer.api.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdentityService {

    private final StaffRepository staffRepository;
    private final ContactRepository contactRepository;

    public AuthenticatedUser resolve(AuthUserType type, Long id) {
        if (type == AuthUserType.STAFF) {
            Staff staff = staffRepository.findById(id.intValue())
                    .orElseThrow(() -> BusinessException.notFound("Staff não encontrado"));
            return AuthenticatedUser.fromStaff(staff);
        }
        Contact contact = contactRepository.findById(id.intValue())
                .orElseThrow(() -> BusinessException.notFound("Contato não encontrado"));
        return AuthenticatedUser.fromContact(contact);
    }

    public Contact getContactById(Integer contactId) {
        return contactRepository.findById(contactId)
                .orElseThrow(() -> BusinessException.notFound("Contato não encontrado"));
    }

    public Contact requireContact(AuthenticatedUser user) {
        if (!user.isContact()) {
            throw BusinessException.forbidden("Ação disponível apenas para clientes");
        }
        return contactRepository.findById(user.getId().intValue())
                .orElseThrow(() -> BusinessException.notFound("Contato não encontrado"));
    }
}
