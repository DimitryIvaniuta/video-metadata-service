-- Global BIGINT sequence for all primary‐keys
CREATE SEQUENCE VM_UNIQUE_ID
    AS BIGINT
    MINVALUE 10000
    MAXVALUE 9223372036854775807
    START WITH 10000
    INCREMENT BY 1
    CACHE 10
    NO CYCLE;
