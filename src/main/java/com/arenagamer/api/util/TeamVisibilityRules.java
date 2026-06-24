package com.arenagamer.api.util;

import com.arenagamer.api.entity.enums.Visibility;

import java.util.List;

public final class TeamVisibilityRules {

    public static final List<Visibility> DISCOVERABLE = List.of(Visibility.PUBLIC, Visibility.PROTECTED);
    public static final List<Visibility> RANKED = List.of(Visibility.PUBLIC, Visibility.PROTECTED);

    private TeamVisibilityRules() {}
}
