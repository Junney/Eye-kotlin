package com.amazing.eye.interfaces

import com.amazing.eye.bean.BaseBean

interface IDatasListener {

    fun getSuccess(baseBean: BaseBean)

    fun getfaild(baseBean: BaseBean)
}