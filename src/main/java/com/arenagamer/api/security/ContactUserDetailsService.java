package com.arenagamer.api.security;

import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Staff;
import com.arenagamer.api.repository.ContactRepository;
import com.arenagamer.api.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("contactUserDetailsService")
@RequiredArgsConstructor
public class ContactUserDetailsService implements UserDetailsService {

    private final StaffRepository staffRepository;
    private final ContactRepository contactRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return staffRepository.findByEmail(email)
                .map(this::toStaffUser)
                .orElseGet(() -> contactRepository.findByEmail(email)
                        .map(this::toContactUser)
                        .orElseThrow(() -> new UsernameNotFoundException("Email ou senha incorreto")));
    }

    private UserDetails toStaffUser(Staff staff) {
        String role = staff.getAdmin() != null && staff.getAdmin() == 1 ? "ROLE_ADMIN" : "ROLE_MANAGER";
        return User.builder()
                .username(staff.getEmail())
                .password(staff.getPassword())
                .disabled(staff.getActive() == null || staff.getActive() != 1)
                .authorities(role)
                .build();
    }

    private UserDetails toContactUser(Contact contact) {
        return User.builder()
                .username(contact.getEmail())
                .password(contact.getPassword() != null ? contact.getPassword() : "")
                .disabled(contact.getActive() == null || !contact.getActive())
                .authorities("ROLE_PLAYER")
                .build();
    }
}
