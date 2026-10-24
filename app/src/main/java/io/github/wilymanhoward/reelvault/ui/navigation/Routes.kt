package io.github.wilymanhoward.reelvault.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose destinations.

@Serializable data object OnboardingRoute
@Serializable data object HomeRoute
@Serializable data object CollectionsRoute
/** @property tag start with this tag filter applied (from the Tags screen). */
@Serializable data class SearchRoute(val tag: String? = null)
@Serializable data object TagsRoute
@Serializable data object SettingsRoute
@Serializable data object TagRulesRoute
@Serializable data object AboutRoute
@Serializable data class CollectionDetailRoute(val collectionId: String)
@Serializable data class ItemDetailRoute(val itemId: String)
@Serializable data class SharedCollectionRoute(val shareId: String)
