package com.arenagamer.api.util;

import java.util.regex.Pattern;

public final class NicknameRules {

    public static final String REGEX = "^[a-zA-Z0-9]+$";

    public static final String VALIDATION_MESSAGE =
            "Nickname só pode conter letras e números, sem espaços ou caracteres especiais";

    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private NicknameRules() {}

    public static boolean isValid(String nickname) {
        return nickname != null && PATTERN.matcher(nickname).matches();
    }
}
