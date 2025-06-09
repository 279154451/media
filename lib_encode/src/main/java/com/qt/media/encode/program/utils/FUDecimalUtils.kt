package com.qt.program.utils

import kotlin.math.abs

/**
 * 数值工具类
 *
 * Created on 2021/7/30
 */
object FUDecimalUtils {
    /**
     *  比对误差范围区间
     */
    @JvmField
    var THRESHOLD = 0.001f

    /**
     * 数值比较
     * @param a Float 数据 A
     * @param b Float 数据 B
     * @area area Float 误差区间
     * @return Boolean 是否在误差范围
     */
    @JvmStatic
    @JvmOverloads
    fun floatEquals(a: Float, b: Float, area: Float = THRESHOLD): Boolean {
        return abs(a - b) < area
    }

    /**
     * 数值比较
     * @param a Double 数据 A
     * @param b Double 数据 B
     * @area area Float 误差区间
     * @return Boolean 是否在误差范围
     */
    @JvmStatic
    @JvmOverloads
    fun doubleEquals(a: Double, b: Double, area: Float = THRESHOLD): Boolean {
        return abs(a - b) < area
    }

    /**
     * 数值比较
     * @param a FloatArray? 数据 A
     * @param b FloatArray? 数据 B
     * @area area Float 误差区间
     * @return Boolean 是否在误差范围
     */
    @JvmStatic
    @JvmOverloads
    fun floatArrayEquals(a: FloatArray?, b: FloatArray?, area: Float = THRESHOLD): Boolean {
        if (a == null && b == null) {
            return true
        } else if (a == null || b == null) {
            return false
        } else {
            if (a.size != b.size) {
                return false
            }
        }
        for (i in a.indices) {
            if (!floatEquals(a[i], b[i], area)) {
                return false
            }
        }
        return true
    }

    /**
     * 数值比较
     * @param a DoubleArray? 数据 A
     * @param b DoubleArray? 数据 B
     * @area area Float 误差区间
     * @return Boolean 是否在误差范围
     */
    @JvmStatic
    @JvmOverloads
    fun doubleArrayEquals(a: DoubleArray?, b: DoubleArray?, area: Float = THRESHOLD): Boolean {
        if (a == null && b == null) {
            return true
        } else if (a == null || b == null) {
            return false
        } else {
            if (a.size != b.size) {
                return false
            }
        }
        for (i in a.indices) {
            if (!doubleEquals(a[i], b[i], area)) {
                return false
            }
        }
        return true
    }

    /**
     * 数组深拷贝
     * @param array Array<String?>
     * @return Array<String?>
     */
    @JvmStatic
    fun copyArray(array: Array<String?>): Array<String?> {
        val ret = arrayOfNulls<String>(array.size)
        System.arraycopy(array, 0, ret, 0, array.size)
        return ret
    }

    /**
     * 数组深拷贝
     * @param array FloatArray
     * @return FloatArray
     */
    @JvmStatic
    fun copyArray(array: FloatArray): FloatArray {
        val ret = FloatArray(array.size)
        System.arraycopy(array, 0, ret, 0, array.size)
        return ret
    }

    /**
     * 数组深拷贝
     * @param array IntArray
     * @return IntArray
     */
    @JvmStatic
    fun copyArray(array: IntArray): IntArray {
        val ret = IntArray(array.size)
        System.arraycopy(array, 0, ret, 0, array.size)
        return ret
    }

    /**
     * 数组深拷贝
     * @param array ByteArray
     * @return ByteArray
     */
    @JvmStatic
    fun copyArray(array: ByteArray): ByteArray {
        val ret = ByteArray(array.size)
        System.arraycopy(array, 0, ret, 0, array.size)
        return ret
    }
}