package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist_stocks")
data class WatchlistEntity(
    @PrimaryKey
    val scripCode: String,
    val scripId: String,
    val companyName: String,
    val addedAtMillis: Long = System.currentTimeMillis(),
    val targetAlertPrice: Double? = null,
    val alertCondition: String = "ABOVE", // "ABOVE" or "BELOW"
    val researchNotes: String = ""
)

@Entity(tableName = "saved_projections")
data class SavedProjectionScenario(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scripCode: String,
    val scripId: String,
    val companyName: String,
    val scenarioName: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val revenueGrowthRate: Double,
    val operatingMargin: Double,
    val volatilityAssumption: Double,
    val discountRate: Double,
    val projected5yTargetPrice: Double,
    val projectedCagr: Double,
    val basePriceAtSimulation: Double
)
