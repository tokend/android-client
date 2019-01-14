package org.tokend.template.features.invest.logic

import io.reactivex.Completable
import io.reactivex.Single
import org.tokend.template.data.model.FavoriteRecord
import org.tokend.template.data.repository.favorites.FavoritesRepository

/**
 * Switches favorite state of given entry by [FavoriteEntry.type] and [FavoriteEntry.key]
 */
class SwitchFavoriteUseCase(
        private val favoriteEntry: FavoriteRecord,
        private val favoritesRepository: FavoritesRepository
) {
    fun perform(): Completable {
        return updateData()
                .flatMap {
                    val existingEntry = getExistingEntry()

                    if (existingEntry != null)
                        removeFromFavorites(existingEntry)
                    else
                        addToFavorites()
                }
                .ignoreElement()
    }

    private fun updateData(): Single<Boolean> {
        return favoritesRepository
                .updateIfNotFreshDeferred()
                .toSingleDefault(true)
    }

    private fun getExistingEntry(): FavoriteRecord? {
        return favoritesRepository
                .itemsList
                .find {
                    it.type == favoriteEntry.type
                            && it.key == favoriteEntry.key
                }
    }

    private fun addToFavorites(): Single<Boolean> {
        return favoritesRepository
                .addToFavorites(favoriteEntry)
                .toSingleDefault(true)
    }

    private fun removeFromFavorites(existingEntry: FavoriteRecord): Single<Boolean> {
        return favoritesRepository
                .removeFromFavorites(existingEntry.id)
                .toSingleDefault(true)
    }
}