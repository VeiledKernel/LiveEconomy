package dev.liveeconomy.view.alert

import dev.liveeconomy.api.economy.PriceService
import dev.liveeconomy.core.alert.AlertService
import dev.liveeconomy.data.model.Direction
import org.bukkit.entity.Player

/**
 * Builds [AlertView] from services.
 *
 * Owns derived state that was in [dev.liveeconomy.gui.trading.PriceAlertGUI]:
 *  - current price per alert item
 *  - distance percentage calculation
 *  - triggered state
 *  - canAddMore flag
 */
class AlertViewBuilder(
    private val alertSvc: AlertService,
    private val price:    PriceService
) {
    fun build(player: Player): AlertView {
        val alerts = alertSvc.getAlerts(player.uniqueId)
        val limit  = alertSvc.getAlertLimit(player)

        val entries = alerts.map { alert ->
            val current     = price.getPrice(alert.item) ?: 0.0
            val isAbove     = alert.direction == Direction.ABOVE
            val isTriggered = alert.isTriggered(current)
            val distance    = if (alert.targetPrice > 0)
                Math.abs((current - alert.targetPrice) / alert.targetPrice * 100.0) else 0.0

            AlertView.AlertEntry(
                alert        = alert,
                currentPrice = current,
                distancePct  = distance,
                isTriggered  = isTriggered,
                isAbove      = isAbove
            )
        }

        return AlertView(
            alerts      = entries,
            limit       = limit,
            canAddMore  = alerts.size < limit,
            alertCount  = alerts.size
        )
    }
}
