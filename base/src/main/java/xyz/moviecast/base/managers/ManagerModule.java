package xyz.moviecast.base.managers;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import xyz.moviecast.base.providers.MovieProvider;
import xyz.moviecast.base.providers.ShowProvider;

@Module
public class ManagerModule {

    @Provides
    @Singleton
    ProviderManager provideProviderManager(MovieProvider movieProvider, ShowProvider showProvider) {
        return new ProviderManager(movieProvider, showProvider);
    }
}
