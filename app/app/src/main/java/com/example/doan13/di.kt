package com.example.doan13

import com.cloudinary.Cloudinary
import com.example.doan13.data.repositories.AuthRepositories
import com.example.doan13.data.repositories.AuthRepositoriesImpl
import com.example.doan13.data.repositories.FavoriteRepositories
import com.example.doan13.data.repositories.FavoriteRepositoriesImpl
import com.example.doan13.data.repositories.SongRepositories
import com.example.doan13.data.repositories.SongRepositoriesImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideSongRepository(): SongRepositories {
        return SongRepositoriesImpl() // Giả sử SongRepository nhận Cloudinary
    }
    @Provides
    @Singleton
    fun provideFavoriteRepository() : FavoriteRepositories{
        return FavoriteRepositoriesImpl()// Giả sử SongRepository nhận Cloudinary
    }

    @Provides
    @Singleton
    fun provideAuthRepository (): AuthRepositories{
        return AuthRepositoriesImpl()// Giả sử SongRepository nhận Cloudinary
    }
}