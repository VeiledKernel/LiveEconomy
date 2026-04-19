package dev.liveeconomy.core.event.shock

import dev.liveeconomy.api.event.DomainEventBus
import dev.liveeconomy.api.event.ShockFiredEvent
import dev.liveeconomy.core.economy.PriceModelImpl
import dev.liveeconomy.core.economy.PriceServiceImpl

/**
 * Shared helper for applying a percentage shock to a category.
 * Injected into every ShockHandler so the shock math lives in one place.
 */
class ShockApplier(
    private val prices:   PriceServiceImpl,
    private val model:    PriceModelImpl,
    private val eventBus: DomainEventBus
) {
    fun applyToCategory(categoryId: String, percent: Double, shockType: String, message: String) {
        val items = prices.getItemsByCategory(categoryId)
        items.forEach { model.applyShock(it, percent) }
        if (message.isNotBlank()) {
            eventBus.publish(ShockFiredEvent(shockType, categoryId, percent, message))
        }
    }
}
