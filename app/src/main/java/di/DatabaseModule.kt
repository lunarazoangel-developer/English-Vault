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
import data.database.dao.UserProfileDao
import data.database.dao.WordDao
import javax.inject.Singleton

/**
 * Hilt graph for the database layer.
 *
 * Provides the singleton [AppDatabase] together with its DAOs. The
 * v1 → v2 migration is registered here so existing installs keep their
 * `words` data when the `user_profile` table is introduced.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // region: AppDatabase
    /**
     * Builds the Room database with the v1 → v2 migration attached.
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
            Migrations.MIGRATION_5_6
        )
        .build()
    // endregion

    // region: DAOs
    @Provides
    fun provideWordDao(database: AppDatabase): WordDao = database.wordDao()

    @Provides
    fun provideUserProfileDao(database: AppDatabase): UserProfileDao =
        database.userProfileDao()
    // endregion
}