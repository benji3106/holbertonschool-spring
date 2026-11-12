package com.holberton.realestate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "leases")
public class Lease {

    public static final int MIN_DURATION_MONTHS = 12;
    public static final int MAX_DURATION_MONTHS = 36;
    public static final int DEPOSIT_MONTHS = 2;
    public static final int INCOME_TO_RENT_RATIO = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private int durationMonths;

    @Column(nullable = false)
    private int monthlyRent;

    @Column(nullable = false)
    private boolean active;

    protected Lease() {
        // used by Hibernate
    }

    public Lease(Property property, Tenant tenant, int durationMonths, int monthlyRent) {
        this.property = property;
        this.tenant = tenant;
        this.durationMonths = durationMonths;
        this.monthlyRent = monthlyRent;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public Property getProperty() {
        return property;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public int getDurationMonths() {
        return durationMonths;
    }

    public int getMonthlyRent() {
        return monthlyRent;
    }

    public boolean isActive() {
        return active;
    }

    public int getTotalRent() {
        return monthlyRent * durationMonths;
    }

    public int getDeposit() {
        return monthlyRent * DEPOSIT_MONTHS;
    }

    public void end() {
        this.active = false;
    }
}