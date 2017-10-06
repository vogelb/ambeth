CREATE TABLE "ENTITY_WITH_LOB"
  (
    "ID"   NUMBER(9,0) NOT NULL,
    "BLOB" BLOB NOT NULL,
	"CLOB" CLOB NOT NULL,
	"VERSION"   NUMBER(9,0) NOT NULL,
    CONSTRAINT "ENTITY_WITH_LOB_PK" PRIMARY KEY ("ID") USING INDEX
  );
  CREATE SEQUENCE "ENTITY_WITH_LOB_SEQ"  MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 10000 CACHE 20 NOORDER NOCYCLE ;
