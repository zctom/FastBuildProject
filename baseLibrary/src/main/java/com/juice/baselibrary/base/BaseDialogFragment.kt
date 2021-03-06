package com.juice.baselibrary.base

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.juice.baselibrary.view.IVaryViewHelperController
import com.juice.baselibrary.view.IView
import com.juice.baselibrary.view.VaryViewHelperController
import com.scwang.smartrefresh.layout.SmartRefreshLayout
import com.tea.httplibrary.mvvm.BaseViewModel
import com.tea.httplibrary.utils.ToastUtils
import java.lang.reflect.ParameterizedType

/**
 * @date 2020/3/16.
 * module：
 * description：
 */
abstract class BaseDialogFragment<VM : BaseViewModel<*>, DB : ViewDataBinding> :
    DialogFragment(),
    IView {

    //viewmodel
    protected lateinit var mViewModel: VM
    //databing
    protected var mBinding: DB? = null

    @LayoutRes
    abstract fun getLayoutId(): Int

    /**
     * @return 该View 替换为显示loadingView 或者 emptyView 或者 netWorkErrorView
     */
    abstract fun getReplaceView(): View

    /**
     * 初始化
     */
    abstract fun init(savedInstanceState: Bundle?)

    /**
     * 替换view
     */
    private var viewController: IVaryViewHelperController? = null

    /**
     * 弹窗
     */
    private var dialog: ProgressDialog? = null

    /**
     * 刷新相关 因为单界面不存在加载，这样只针对是否开启刷新功能做处理，可设置为null，为null则不具备刷新相关能力
     */
    protected open fun getSmartRefreshLayout(): SmartRefreshLayout? {
        return null
    }

    private var mRefreshEnable = true //是否能进行下拉刷新

    open fun refreshData() {

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val cls =
            (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<*>
        if (ViewDataBinding::class.java != cls && ViewDataBinding::class.java.isAssignableFrom(cls)) {
            mBinding = DataBindingUtil.inflate(inflater, getLayoutId(), container, false)
            return mBinding?.root
        }
        return inflater.inflate(getLayoutId(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        createViewModel()
        viewController = initVaryViewHelperController()
        lifecycle.addObserver(mViewModel)
        //注册 UI事件
        registerViewChange()
        initRefresh()
        init(savedInstanceState)
        initViewModel()
        load()
    }

    open fun initViewModel() {
    }

    open fun load(){
    }

    /***
     * view
     */
    protected open fun initVaryViewHelperController(): IVaryViewHelperController? {
        return VaryViewHelperController(getReplaceView())
    }

    private fun initRefresh() {
        if (getSmartRefreshLayout() != null) {
            getSmartRefreshLayout()?.isEnabled = mRefreshEnable
            //不具备加载功能
            getSmartRefreshLayout()?.setEnableLoadMore(false)
            if (mRefreshEnable) {
                getSmartRefreshLayout()?.setOnRefreshListener {
                    refreshData()
                }
            }
        }
    }

    /**
     *
     *     actualTypeArguments[0]  BaseViewModel
     *     actualTypeArguments[1]  ViewDataBinding
     *
     */
    private fun createViewModel() {
        //创建viewmodel
        val type = javaClass.genericSuperclass
        if (type is ParameterizedType) {
            val tp = type.actualTypeArguments[0]
            val tClass = tp as? Class<VM> ?: BaseViewModel::class.java
            mViewModel = ViewModelProviders.of(this)[tClass] as VM
        }
    }

    /**
     * 注册 UI 事件
     */
    private fun registerViewChange() {
        mViewModel.viewChange.showLoading.observe(viewLifecycleOwner, Observer {
            viewController?.let {
                if (!it.isHasRestore) {
                    showLoading()
                }
            }
        })
        mViewModel.viewChange.showDialogProgress.observe(viewLifecycleOwner, Observer {
            showDialogProgress(it)
        })
        mViewModel.viewChange.dismissDialog.observe(viewLifecycleOwner, Observer {
            dismissDialog()
        })
        mViewModel.viewChange.showToast.observe(viewLifecycleOwner, Observer {
            showToast(it)
        })
        mViewModel.viewChange.showTips.observe(viewLifecycleOwner, Observer {
            showTips(it)
        })
        mViewModel.viewChange.showEmpty.observe(viewLifecycleOwner, Observer {
            showEmpty(it)
        })
        mViewModel.viewChange.showNetworkError.observe(viewLifecycleOwner, Observer {
            showNetworkError(it, mViewModel.listener)
        })
        mViewModel.viewChange.restore.observe(viewLifecycleOwner, Observer {
            viewController?.restore()
            //代表有设置刷新
            if (getSmartRefreshLayout() != null) {
                getSmartRefreshLayout()?.finishRefresh()
            }
        })
    }


    /**
     * 相关view替换
     */

    override fun showTips(msg: String) {
//        activity?.let {
//            val snackBar = TSnackbar.make(
//                it.findViewById(android.R.id.content),
//                msg,
//                TSnackbar.LENGTH_SHORT
//            )
//            val snackBarView = snackBar.view
//            snackBarView.setBackgroundColor(resources.getColor(R.color.colorAccent))
//            val textView =
//                snackBarView.findViewById<TextView>(com.androidadvance.topsnackbar.R.id.snackbar_text)
//            textView.setTextColor(resources.getColor(R.color.m90EE90))
//            snackBar.show()
//        }
    }

    override fun showDialogProgress(msg: String) {
        showDialogProgress(msg, true, null)
    }

    override fun showDialogProgress(
        msg: String,
        cancelable: Boolean,
        onCancelListener: DialogInterface.OnCancelListener?
    ) {
        try {
            if (dialog == null) {
                dialog = ProgressDialog(context)
                dialog?.setCancelable(cancelable)
                dialog?.setCanceledOnTouchOutside(cancelable)
                dialog?.setOnCancelListener(onCancelListener)
            }
            if (!TextUtils.isEmpty(msg)) dialog?.setMessage(msg)
            dialog?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun dismissDialog() {
        dialog?.let {
            if (it.isShowing) it.dismiss()
        }
    }

    override fun showLoading() {
        viewController?.showLoading()
    }

    override fun showLoading(msg: String?) {
        viewController?.showLoading(msg)
    }

    override fun showEmpty(content: String?) {
        viewController?.showEmpty(content)
    }

    override fun showEmpty(
        content: String?,
        clickListener: View.OnClickListener?
    ) {
        viewController?.showEmpty(content, clickListener)
    }

    override fun showNetworkError(listener: View.OnClickListener?) {
        viewController?.showNetworkError(listener)
    }

    override fun showNetworkError(
        msg: String?,
        listener: View.OnClickListener?
    ) {
        viewController?.showNetworkError(msg, listener)
    }

    override fun showCustomView(
        drawableInt: Int,
        title: String?,
        msg: String?,
        btnText: String?,
        listener: View.OnClickListener?
    ) {
        viewController?.showCustomView(drawableInt, title, msg, btnText, listener)
    }

    override fun restore() {
        viewController?.restore()
    }

    override val isHasRestore: Boolean
        get() = viewController?.isHasRestore ?: false

    override fun showToast(msg: String) {
        ToastUtils.showShortToastSafe(mActivity, msg)
    }

    override fun showToast(msg: Int) {
        mActivity?.let {
            ToastUtils.show(it, msg)
        }
    }

    override val mActivity: Activity?
        get() = activity

    override val mContext: Context?
        get() = context

    override val mAppContext: Context?
        get() = activity?.applicationContext


    /**
     *  @param refreshEnable 设置是否刷新操作
     */
    open fun setRefreshEnable(refreshEnable: Boolean) {
        //不为空才可以刷新
        if (getSmartRefreshLayout() != null) {
            mRefreshEnable = refreshEnable
            getSmartRefreshLayout()?.isEnabled = mRefreshEnable
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        //相关销毁，相关事件置空
        if (mBinding != null) {
            mBinding == null
        }
        if (viewController != null) {
            viewController == null
        }
        if (dialog != null) {
            dialog == null
        }
        if (getSmartRefreshLayout() != null) {
            getSmartRefreshLayout()?.setOnRefreshListener(null)
            getSmartRefreshLayout()?.setOnLoadMoreListener(null)
            getSmartRefreshLayout() == null
        }
    }

}