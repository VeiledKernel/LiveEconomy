package dev.liveeconomy.view.alert

import dev.liveeconomy.data.model.Alert

/**
 * Pre-computed view data for [dev.liveeconomy.gui.trading.PriceAlertGUI].
 */
data class AlertView(
    val alerts:    List<AlertEntry>,
    val limit:     Int,
    val canAddMore: Boolean,
    val alertCount:  Int        // pre-computed — GUI never calls .size
) {
    data class AlertEntry(
        val alert:        Alert,
        val currentPrice: Double,
        val distancePct:  Double,
        val isTriggered:  Boolean,
        val isAbove:      Boolean
    )
}
