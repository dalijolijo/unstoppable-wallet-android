package bitcoin.wallet.core

import android.hardware.fingerprint.FingerprintManager
import io.reactivex.Observable
import io.realm.OrderedCollectionChangeSet

interface ILocalStorage {
    val savedWords: List<String>?
    fun saveWords(words: List<String>)
    fun clearAll()
    fun savePin(pin: String)
    fun getPin(): String?
}

interface IMnemonic {
    fun generateWords(): List<String>
    fun validateWords(words: List<String>): Boolean
}

interface IWalletDataProvider {
    val walletData: WalletData
}

interface IRandomProvider {
    fun getRandomIndexes(count: Int): List<Int>
}

interface INetworkManager {
    fun getJwtToken(identity: String, pubKeys: Map<Int, String>): Observable<String>
    fun getExchangeRates(): Observable<Map<String, Double>>
}

interface IEncryptionManager {
    fun encrypt(data: String): String
    fun decrypt(data: String): String
    fun getCryptoObject(): FingerprintManager.CryptoObject?
}

interface IClipboardManager {
    fun copyText(text: String)
    fun getCopiedText(): String
}

data class WalletData(val words: List<String>)

data class DatabaseChangeset<T>(val array: List<T>, val changeset: CollectionChangeset? = null)

data class CollectionChangeset(val deleted: List<Int> = listOf(), val inserted: List<Int> = listOf(), val updated: List<Int> = listOf()) {

    constructor(changeset: OrderedCollectionChangeSet) :
            this(changeset.deletions.toList(), changeset.insertions.toList(), changeset.changes.toList())

}
