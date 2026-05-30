package com.airline.checkin.di

import android.content.Context
import androidx.room.Room
import com.airline.checkin.data.local.AppDatabase
import com.airline.checkin.data.local.dao.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "airline_checkin_db"
        ).fallbackToDestructiveMigration().build()

    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideFlightDao(db: AppDatabase): FlightDao = db.flightDao()
    @Provides fun provideBookingDao(db: AppDatabase): BookingDao = db.bookingDao()
    @Provides fun providePassengerDao(db: AppDatabase): PassengerDao = db.passengerDao()
    @Provides fun provideSeatDao(db: AppDatabase): SeatDao = db.seatDao()
    @Provides fun provideBoardingPassDao(db: AppDatabase): BoardingPassDao = db.boardingPassDao()
    @Provides fun provideBaggageDao(db: AppDatabase): BaggageDeclarationDao = db.baggageDeclarationDao()
    @Provides fun provideSavedPassengerDao(db: AppDatabase): SavedPassengerDao = db.savedPassengerDao()
}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}
