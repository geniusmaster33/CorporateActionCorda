package com.template.model

import net.corda.core.serialization.CordaSerializable

/**
 * Created by acesuser on 4/15/2018.
 */
/**
 * A simple class representing an IOU.
 *
 * This is the data structure that the parties will reach agreement over. These data structures can be arbitrarily
 * complex. See https://github.com/corda/corda/blob/master/samples/irs-demo/src/main/kotlin/net/corda/irs/contract/IRS.kt
 * for a more complicated example.
 *
 * @param value the IOU's value.
 */

data class IOU(val value: Int,
               val recordDate : String ,
               val exDate: String,
               val payDate: String,
               val stock: String,
               val option: String,
               val type: String,
               val reference: String
)