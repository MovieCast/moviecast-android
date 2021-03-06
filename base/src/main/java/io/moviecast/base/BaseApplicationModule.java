/*
 * Copyright (c) MovieCast and it's contributors. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package io.moviecast.base;

import android.app.Application;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.moviecast.base.net.NetModule;
import io.moviecast.base.managers.ManagerModule;
import io.moviecast.base.providers.ProviderModule;

@Module(
        includes = {
                NetModule.class,
                ProviderModule.class,
                ManagerModule.class
        }
)
public class BaseApplicationModule {

    private BaseApplication app;

    public BaseApplicationModule(BaseApplication app) {
        this.app = app;
    }

    @Provides
    @Singleton
    Application provideApplication() {
        return app;
    }

    @Provides
    @Singleton
    Context provideContext() {
        return app.getApplicationContext();
    }
}
