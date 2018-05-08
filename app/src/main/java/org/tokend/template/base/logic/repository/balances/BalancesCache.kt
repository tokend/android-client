package org.tokend.template.base.logic.repository.balances

import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import org.tokend.sdk.api.models.BalanceDetails
import org.tokend.template.base.logic.repository.base.RepositoryCache

class BalancesCache : RepositoryCache<BalanceDetails>() {
    override fun isContentSame(first: BalanceDetails, second: BalanceDetails): Boolean {
        return first.asset == second.asset
                && first.balanceId == second.balanceId
                && first.balance == second.balance
                && first.lockedBalance == second.lockedBalance
    }

    override fun getAllFromDb(): Single<List<BalanceDetails>> =
            listOf<BalanceDetails>().toSingle()

    override fun addToDb(items: List<BalanceDetails>) {}

    override fun updateInDb(items: List<BalanceDetails>) {}

    override fun deleteFromDb(items: List<BalanceDetails>) {}

    override fun clearDb() {}
}