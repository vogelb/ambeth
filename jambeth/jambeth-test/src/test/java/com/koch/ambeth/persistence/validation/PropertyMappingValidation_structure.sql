CREATE TABLE "ENTITY"
(
	"ID"			NUMBER(9,0) NOT NULL,
	"NAME"			VARCHAR2(80 BYTE) NOT NULL,
	"UPDATED_ON"	DATE,
	"CREATED_ON"	DATE,
	"UPDATED_BY"	VARCHAR2(16 BYTE),
	"CREATED_BY"	VARCHAR2(16 BYTE),
	"VERSION"		NUMBER(9,0),
	CONSTRAINT "ENTITY_PK" PRIMARY KEY ("ID") USING INDEX
);
CREATE SEQUENCE "ENTITY_SEQ" MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 10000 CACHE 20 NOORDER NOCYCLE ;
