package otus.homework.reactivecats

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.net.UnknownHostException

@SuppressLint("CheckResult")
class CatsViewModel(
    catsService: CatsService,
    localCatFactsGenerator: LocalCatFactsGenerator,
    context: Context
) : ViewModel() {

    private val _catsLiveData = MutableLiveData<Result>()
    val catsLiveData: LiveData<Result> = _catsLiveData

    private val compositeDisposable = CompositeDisposable()

    init {
        val disposable = catsService.getCatFact()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ fact ->
                _catsLiveData.value = Success(fact)
            }, { error ->
                if (error is HttpException && error.code() == 500) {
                    _catsLiveData.value = ServerError
                } else {
                    _catsLiveData.value =
                        Error(error.message ?: context.getString(R.string.default_error_text))
                }
            })

        compositeDisposable.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

    fun getFacts() {}
}

class CatsViewModelFactory(
    private val catsRepository: CatsService,
    private val localCatFactsGenerator: LocalCatFactsGenerator,
    private val context: Context
) :
    ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CatsViewModel(catsRepository, localCatFactsGenerator, context) as T
}

sealed class Result
data class Success(val fact: Fact) : Result()
data class Error(val message: String) : Result()
object ServerError : Result()