package com.goshopping.android.data.service

import java.util.UUID

/**
 * ID生成サービス。
 * 仕様書 §6-3 に従い、タイムスタンプのみのIDは衝突リスクがあるため禁止。
 */
object DeviceIdService {

    /** SharedPreferences 等から保存された8文字のデバイスプレフィックスを返す。
     *  未設定の場合は UUID から生成して返す（永続化はアプリ初期化時に行うこと）。
     */
    private var _devicePrefix: String = UUID.randomUUID().toString()
        .replace("-", "")
        .substring(0, 8)

    fun setDevicePrefix(prefix: String) {
        _devicePrefix = prefix
    }

    fun getDevicePrefix(): String = _devicePrefix

    /**
     * グループID: {DevicePrefix}_{timestamp}
     * 例: a3f8c9d2_1707835200000
     */
    fun generateGroupId(devicePrefix: String = _devicePrefix): String =
        "${devicePrefix}_${System.currentTimeMillis()}"

    /**
     * リストID: {DevicePrefix}_{uuid8}
     * 例: a3f8c9d2_f3e1a7b4
     */
    fun generateListId(devicePrefix: String = _devicePrefix): String {
        val uuid8 = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
        return "${devicePrefix}_${uuid8}"
    }

    /**
     * QR招待トークン: INV_{UUID v4}
     * 例: INV_xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
     */
    fun generateInvitationToken(): String = "INV_${UUID.randomUUID()}"
}
