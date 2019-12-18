package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.TransactionItem
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule.FetchData

class TransactionRecordDataSource(
        private val poolRepo: PoolRepo,
        private val itemsDataSource: TransactionItemDataSource,
        private val factory: TransactionItemFactory,
        private val limit: Int = 10) {

    val itemsCount
        get() = itemsDataSource.count

    fun itemForIndex(index: Int): TransactionItem =
            itemsDataSource.itemForIndex(index)

    fun itemIndexesForTimestamp(coin: Coin, timestamp: Long): List<Int> =
            itemsDataSource.itemIndexesForTimestamp(coin, timestamp)


    fun itemIndexesForPending(wallet: Wallet, thresholdBlockHeight: Int): List<Int> =
            itemsDataSource.itemIndexesForPending(wallet, thresholdBlockHeight)

    fun getFetchDataList(): List<FetchData> = poolRepo.activePools.mapNotNull {
        it.getFetchData(limit)
    }

    fun handleNextRecords(records: Map<Wallet, List<TransactionRecord>>) {
        records.forEach { (wallet, transactionRecords) ->
            poolRepo.getPool(wallet)?.add(transactionRecords)
        }
    }

    fun handleUpdatedRecords(records: List<TransactionRecord>, wallet: Wallet): Boolean {
        val pool = poolRepo.getPool(wallet) ?: return false

        val updatedRecords = mutableListOf<TransactionRecord>()
        val insertedRecords = mutableListOf<TransactionRecord>()

        records.sorted().forEach {
            when (pool.handleUpdatedRecord(it)) {
                Pool.HandleResult.UPDATED -> updatedRecords.add(it)
                Pool.HandleResult.INSERTED -> insertedRecords.add(it)
                Pool.HandleResult.NEW_DATA -> {
                    if (itemsDataSource.shouldInsertRecord(it)) {
                        insertedRecords.add(it)
                        pool.increaseFirstUnusedIndex()
                    }
                }
                Pool.HandleResult.IGNORED -> {
                }
            }
        }

        if (!poolRepo.isPoolActiveByWallet(wallet)) return false

        val updatedItems = updatedRecords.map { factory.createTransactionItem(wallet, it) }
        val insertedItems = insertedRecords.map { factory.createTransactionItem(wallet, it) }

        itemsDataSource.handleModifiedItems(updatedItems, insertedItems)

        return true
    }

    fun increasePage(): Int {
        val unusedItems = mutableListOf<TransactionItem>()

        poolRepo.activePools.forEach { pool ->
            unusedItems.addAll(pool.unusedRecords.map { record ->
                factory.createTransactionItem(pool.wallet, record)
            })
        }

        if (unusedItems.isEmpty()) return 0

        unusedItems.sortDescending()

        val usedItems = unusedItems.take(limit)

        itemsDataSource.add(usedItems)

        usedItems.forEach {
            poolRepo.getPool(it.wallet)?.increaseFirstUnusedIndex()
        }

        return usedItems.size
    }

    fun setWallets(wallets: List<Wallet>) {
        poolRepo.allPools.forEach {
            it.resetFirstUnusedIndex()
        }
        poolRepo.activatePools(wallets)
        itemsDataSource.clear()
    }

    fun handleUpdatedWallets(wallets: List<Wallet>) {
        val unusedWallets = poolRepo.allPools.map { it.wallet }.filter { !wallets.contains(it) }

        poolRepo.deactivatePools(unusedWallets)

        setWallets(wallets)
    }

}

