package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.Currency
import java.math.BigDecimal
import java.util.*

typealias CoinCode = String

data class TransactionViewItem(
        val wallet: Wallet,
        val transactionHash: String,
        val coinValue: CoinValue,
        val currencyValue: CurrencyValue?,
        val feeCoinValue: CoinValue?,
        val from: String?,
        val to: String?,
        val type: TransactionType,
        val showFromAddress: Boolean,
        val date: Date?,
        val status: TransactionStatus,
        val rate: CurrencyValue?,
        val lockInfo: TransactionLockInfo?) {

    fun itemTheSame(other: TransactionViewItem): Boolean {
        return wallet == other.wallet && transactionHash == other.transactionHash
    }

    fun contentTheSame(other: TransactionViewItem): Boolean {
        return currencyValue == other.currencyValue
                && date == other.date
                && status == other.status
                && rate == other.rate
    }
}


data class TransactionLockInfo(val lockedUntil: Date, val originalAddress: String, val amount: BigDecimal?)

sealed class TransactionStatus {
    object Pending : TransactionStatus()
    class Processing(val progress: Double) : TransactionStatus() //progress in 0.0 .. 1.0
    object Completed : TransactionStatus()
    object Failed : TransactionStatus()
}

object TransactionsModule {

    data class FetchData(val wallet: Wallet, val from: TransactionRecord?, val limit: Int)

    interface IView {
        fun showFilters(filters: List<Wallet?>)
        fun setItems(items: List<TransactionViewItem>)
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onTransactionItemClick(transaction: TransactionViewItem)
        fun onFilterSelect(wallet: Wallet?)
        fun onClear()

        val itemsCount: Int
        fun itemForIndex(index: Int): TransactionViewItem
        fun onBottomReached()
        fun onVisible()
    }

    interface IInteractor {
        fun initialFetch()
        fun clear()
        fun fetchRecords(fetchDataList: List<FetchData>, initial: Boolean)
        fun setSelectedWallets(selectedWallets: List<Wallet>)
        fun fetchLastBlockHeights()
        fun fetchRate(coin: Coin, timestamp: Long)
    }

    interface IInteractorDelegate {
        fun onUpdateWalletsData(allWalletsData: List<Triple<Wallet, Int, Int?>>)
        fun onUpdateSelectedWallets(selectedWallets: List<Wallet>)
        fun didFetchRecords(records: Map<Wallet, List<TransactionRecord>>, initial: Boolean)
        fun onUpdateLastBlockHeight(wallet: Wallet, lastBlockHeight: Int)
        fun onUpdateBaseCurrency()
        fun didFetchRate(rateValue: BigDecimal, coin: Coin, currency: Currency, timestamp: Long)
        fun didUpdateRecords(records: List<TransactionRecord>, wallet: Wallet)
        fun onConnectionRestore()
    }

    interface IRouter {
        fun openTransactionInfo(transactionViewItem: TransactionViewItem)
    }

    fun initModule(view: TransactionsViewModel, router: IRouter) {
        val dataSource = TransactionRecordDataSource(PoolRepo(), TransactionItemDataSource(), TransactionItemFactory())
        val interactor = TransactionsInteractor(App.walletManager, App.adapterManager, App.currencyManager, App.xRateManager, App.connectivityManager)
        val presenter = TransactionsPresenter(interactor, router, TransactionViewItemFactory(App.feeCoinProvider), dataSource, TransactionMetadataDataSource())

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }

}
