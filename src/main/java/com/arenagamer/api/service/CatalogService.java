package com.arenagamer.api.service;

import com.arenagamer.api.entity.Preset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final PresetService presetService;

    public List<Preset> listActivePresets() {
        return presetService.listActive();
    }

    public List<Preset> listAllPresets() {
        return presetService.listAll();
    }

    public List<Preset> searchPresets(String query, boolean activeOnly) {
        return presetService.search(query, activeOnly);
    }
}
