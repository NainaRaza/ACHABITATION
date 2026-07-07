ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_living_rest_non_negative CHECK (living_rest IS NULL OR living_rest >= 0),
    ADD CONSTRAINT ck_app_user_net_income_non_negative CHECK (net_income_after_tax IS NULL OR net_income_after_tax >= 0),
    ADD CONSTRAINT ck_app_user_rent_non_negative CHECK (rent IS NULL OR rent >= 0),
    ADD CONSTRAINT ck_app_user_credits_non_negative CHECK (credits IS NULL OR credits >= 0),
    ADD CONSTRAINT ck_app_user_fixed_charges_non_negative CHECK (fixed_charges IS NULL OR fixed_charges >= 0),
    ADD CONSTRAINT ck_app_user_transport_non_negative CHECK (transport IS NULL OR transport >= 0),
    ADD CONSTRAINT ck_app_user_insurance_non_negative CHECK (insurance IS NULL OR insurance >= 0),
    ADD CONSTRAINT ck_app_user_other_mandatory_non_negative CHECK (other_mandatory_expenses IS NULL OR other_mandatory_expenses >= 0),
    ADD CONSTRAINT ck_app_user_menstrual_non_negative CHECK (menstrual_protection IS NULL OR menstrual_protection >= 0);

ALTER TABLE trip
    ADD CONSTRAINT ck_trip_currency_iso CHECK (reference_currency ~ '^[A-Z]{3}$'),
    ADD CONSTRAINT ck_trip_dates_order CHECK (start_date IS NULL OR end_date IS NULL OR start_date <= end_date);

ALTER TABLE person
    ADD CONSTRAINT ck_person_living_rest_non_negative CHECK (living_rest >= 0),
    ADD CONSTRAINT ck_person_net_income_non_negative CHECK (net_income_after_tax >= 0),
    ADD CONSTRAINT ck_person_rent_non_negative CHECK (rent >= 0),
    ADD CONSTRAINT ck_person_credits_non_negative CHECK (credits >= 0),
    ADD CONSTRAINT ck_person_fixed_charges_non_negative CHECK (fixed_charges >= 0),
    ADD CONSTRAINT ck_person_transport_non_negative CHECK (transport >= 0),
    ADD CONSTRAINT ck_person_insurance_non_negative CHECK (insurance >= 0),
    ADD CONSTRAINT ck_person_other_mandatory_non_negative CHECK (other_mandatory_expenses >= 0),
    ADD CONSTRAINT ck_person_menstrual_non_negative CHECK (menstrual_protection >= 0);

ALTER TABLE presence_period
    ADD CONSTRAINT ck_presence_period_dates_order CHECK (start_date IS NULL OR end_date IS NULL OR start_date <= end_date);

ALTER TABLE expense
    ADD CONSTRAINT ck_expense_total_positive CHECK (total_amount > 0),
    ADD CONSTRAINT ck_expense_meat_non_negative CHECK (meat_amount >= 0),
    ADD CONSTRAINT ck_expense_alcohol_non_negative CHECK (alcohol_amount >= 0),
    ADD CONSTRAINT ck_expense_currency_iso CHECK (currency ~ '^[A-Z]{3}$'),
    ADD CONSTRAINT ck_expense_rate_positive CHECK (exchange_rate_to_trip_currency > 0),
    ADD CONSTRAINT ck_expense_details_lte_total CHECK ((meat_amount + alcohol_amount) <= total_amount);

ALTER TABLE expense_custom_constraint_amount
    ADD CONSTRAINT ck_expense_custom_constraint_amount_non_negative CHECK (amount >= 0);
