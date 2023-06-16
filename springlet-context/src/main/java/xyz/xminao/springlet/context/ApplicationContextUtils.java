package xyz.xminao.springlet.context;

import jakarta.annotation.Nonnull;

import java.util.Objects;

public class ApplicationContextUtils {
    private static ApplicationContext applicationContext = null;

    @Nonnull
    public static ApplicationContext getRequiredApplicationContext() {
        return Objects.requireNonNull(getApplicationContext());
    }

    @Nonnull
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    static void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }
}
