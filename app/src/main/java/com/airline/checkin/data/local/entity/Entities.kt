package com.airline.checkin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.airline.checkin.domain.model.BaggageData
import com.airline.checkin.domain.model.PassportData
import com.airline.checkin.domain.model.SeatData
import com.airline.checkin.domain.model.SpecialRequests
import org.json.JSONObject

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val email: String,
    val phone: String
)

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val lastName: String,
    val firstName: String,
    val flightNumber: String,
    val departure: String,
    val destination: String,
    val departureTime: String,
    val checkInStatus: Boolean,
    val seat: SeatData?,
    val passport: PassportData?,
    val baggage: BaggageData?,
    val specialRequests: SpecialRequests?
)

class Converters {
    @TypeConverter
    fun fromSeatData(value: SeatData?): String? {
        if (value == null) return null
        val json = JSONObject()
        json.put("seatId", value.seatId)
        json.put("seatNumber", value.seatNumber)
        return json.toString()
    }

    @TypeConverter
    fun toSeatData(value: String?): SeatData? {
        if (value == null) return null
        return try {
            val json = JSONObject(value)
            SeatData(
                seatId = json.optString("seatId", ""),
                seatNumber = json.optString("seatNumber", "")
            )
        } catch (e: Exception) { null }
    }

    @TypeConverter
    fun fromPassportData(value: PassportData?): String? {
        if (value == null) return null
        val json = JSONObject()
        json.put("number", value.number)
        json.put("dob", value.dob)
        json.put("nationality", value.nationality)
        return json.toString()
    }

    @TypeConverter
    fun toPassportData(value: String?): PassportData? {
        if (value == null) return null
        return try {
            val json = JSONObject(value)
            PassportData(
                number = json.optString("number", ""),
                dob = json.optString("dob", ""),
                nationality = json.optString("nationality", "")
            )
        } catch (e: Exception) { null }
    }

    @TypeConverter
    fun fromBaggageData(value: BaggageData?): String? {
        if (value == null) return null
        val json = JSONObject()
        json.put("cabin", value.cabin)
        json.put("checked", value.checked)
        return json.toString()
    }

    @TypeConverter
    fun toBaggageData(value: String?): BaggageData? {
        if (value == null) return null
        return try {
            val json = JSONObject(value)
            BaggageData(
                cabin = json.optInt("cabin", 0),
                checked = json.optInt("checked", 0)
            )
        } catch (e: Exception) { null }
    }

    @TypeConverter
    fun fromSpecialRequests(value: SpecialRequests?): String? {
        if (value == null) return null
        val json = JSONObject()
        json.put("dietary", value.dietary)
        json.put("wheelchair", value.wheelchair)
        json.put("infant", value.infant)
        json.put("pet", value.pet)
        return json.toString()
    }

    @TypeConverter
    fun toSpecialRequests(value: String?): SpecialRequests? {
        if (value == null) return null
        return try {
            val json = JSONObject(value)
            SpecialRequests(
                dietary = json.optString("dietary", ""),
                wheelchair = json.optBoolean("wheelchair", false),
                infant = json.optBoolean("infant", false),
                pet = json.optBoolean("pet", false)
            )
        } catch (e: Exception) { null }
    }
}
