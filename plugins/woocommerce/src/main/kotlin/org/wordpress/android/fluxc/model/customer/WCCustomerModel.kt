package org.wordpress.android.fluxc.model.customer

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

/**
 * Single Woo customer - see https://woocommerce.github.io/woocommerce-rest-api-docs/#customer-properties
 */
@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCCustomerModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var avatarUrl: String = ""
    @Column var dateCreated: String = ""
    @Column var dateCreatedGmt: String = ""
    @Column var dateModified: String = ""
    @Column var dateModifiedGmt: String = ""
    @Column var email: String = ""
    @Column var firstName: String = ""
    @Column var remoteCustomerId: Long = 0L
    @Column var isPayingCustomer: Boolean = false
        @JvmName("setIsPayingCustomer")
        set
    @Column var lastName: String = ""
    @Column var role: String = ""
    @Column var username: String = ""

    @Column var localSiteId = 0

    @Column var billingAddress1: String = ""
    @Column var billingAddress2: String = ""
    @Column var billingCity: String = ""
    @Column var billingCompany: String = ""
    @Column var billingCountry: String = ""
    @Column var billingEmail: String = ""
    @Column var billingFirstName: String = ""
    @Column var billingLastName: String = ""
    @Column var billingPhone: String = ""
    @Column var billingPostcode: String = ""
    @Column var billingState: String = ""

    @Column var shippingAddress1: String = ""
    @Column var shippingAddress2: String = ""
    @Column var shippingCity: String = ""
    @Column var shippingCompany: String = ""
    @Column var shippingCountry: String = ""
    @Column var shippingFirstName: String = ""
    @Column var shippingLastName: String = ""
    @Column var shippingPostcode: String = ""
    @Column var shippingState: String = ""

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }
}
