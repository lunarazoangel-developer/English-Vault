package di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import data.database.AppDatabase
import data.database.Migrations
import data.database.dao.CategoryProgressDao
import data.database.dao.UserProfileDao
import data.database.dao.WordDao
import javax.inject.Singleton

/**
 * Hilt graph for the database layer.
 *
 * Provides the singleton [AppDatabase] together with its DAOs. All
 * schema upgrades from version 1 up to the current schema version
 * are registered here so existing installs keep their data instead
 * of being wiped by a destructive fallback.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // region: AppDatabase
    /**
     * Builds the Room database with every migration registered.
     *
     * If the schema is ever reset (during development) the database
     * file is wiped automatically thanks to `fallbackToDestructiveMigration`.
     * For production this should be removed and explicit migrations
     * provided for every version bump.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME
    )
        .addMigrations(
            Migrations.MIGRATION_1_2,
            Migrations.MIGRATION_2_3,
            Migrations.MIGRATION_3_4,
            Migrations.MIGRATION_4_5,
            Migrations.MIGRATION_5_6,
            Migrations.MIGRATION_6_7,
            Migrations.MIGRATION_7_8,
            Migrations.MIGRATION_8_9
        )
        .build()
    // endregion

    // region: DAOs
    @Provides
    fun provideWordDao(database: AppDatabase): WordDao = database.wordDao()

    @Provides
    fun provideUserProfileDao(database: AppDatabase): UserProfileDao =
        database.userProfileDao()

    @Provides
    fun provideCategoryProgressDao(database: AppDatabase): CategoryProgressDao =
        database.categoryProgressDao()
    // endregion
}