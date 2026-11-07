package dev.tsukuyomi.libra.utils;

public class Constants {

    public static final class TenantDAOConstants {

        private TenantDAOConstants() {
        }

        public static final String TABLE_NAME = "tenants";
        public static final String ID = "id";
        public static final String TENANT_CODE = "tenant_code";
        public static final String TENANT_NAME = "tenant_name";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
    }

    public static final class ScopeDAOConstants {

        private ScopeDAOConstants() {
        }

        public static final String TABLE_NAME = "scopes";
        public static final String ID = "id";
        public static final String TENANT_ID = "tenant_id";
        public static final String SCOPE_NAME = "scope_name";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
    }

    public static final class PolicyDAOConstants {

        private PolicyDAOConstants() {
        }

        public static final String TABLE_NAME = "policies";
        public static final String ID = "id";
        public static final String TENANT_ID = "tenant_id";
        public static final String SCOPE_ID = "scope_id";
        public static final String ENTITY_ID = "entity_id";
        public static final String CODE = "code";
        public static final String APPLIED_JS = "applied_js";
        public static final String EVALUATE_JS = "evaluate_js";
        public static final String STATUS = "status";
        public static final String VALID_FROM = "valid_from";
        public static final String VALID_TILL = "valid_till";
        public static final String VERSION = "version";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
    }

    public static final class PolicyEvaluationDAOConstants {

        private PolicyEvaluationDAOConstants() {
        }

        public static final String TABLE_NAME = "policy_evaluations";
        public static final String ID = "id";
        public static final String REQUEST_ID = "request_id";
        public static final String ENTITY_ID = "entity_id";
        public static final String POLICY_ID = "policy_id";
        public static final String INPUT_ATTRS = "input_attrs";
        public static final String RESULT = "result";
        public static final String ERRORS = "errors";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
    }

}
